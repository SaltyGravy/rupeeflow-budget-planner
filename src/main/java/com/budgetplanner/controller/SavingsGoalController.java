package com.budgetplanner.controller;

import com.budgetplanner.config.AuthInterceptor;
import com.budgetplanner.dto.SavingsGoalDto;
import com.budgetplanner.model.SavingsGoal;
import com.budgetplanner.service.SavingsGoalService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Optional;

@RestController
@RequestMapping("/api/savings-goals")
public class SavingsGoalController {

    private final SavingsGoalService savingsGoalService;

    public SavingsGoalController(SavingsGoalService savingsGoalService) {
        this.savingsGoalService = savingsGoalService;
    }

    @GetMapping
    public ResponseEntity<SavingsGoalDto> getSavingsGoal(HttpSession session) {
        Long userId = (Long) session.getAttribute(AuthInterceptor.SESSION_USER_KEY);
        Optional<SavingsGoal> goal = savingsGoalService.getSavingsGoal(userId);

        if (goal.isPresent()) {
            return ResponseEntity.ok(new SavingsGoalDto(goal.get().getTitle(), goal.get().getTargetAmount()));
        } else {
            return ResponseEntity.ok(new SavingsGoalDto("Savings goal", BigDecimal.ZERO));
        }
    }

    @PostMapping
    public ResponseEntity<SavingsGoalDto> setSavingsGoal(
            @Valid @RequestBody SavingsGoalDto dto,
            HttpSession session) {

        Long userId = (Long) session.getAttribute(AuthInterceptor.SESSION_USER_KEY);
        SavingsGoalDto saved = savingsGoalService.setSavingsGoal(userId, dto);
        return ResponseEntity.ok(saved);
    }
}
