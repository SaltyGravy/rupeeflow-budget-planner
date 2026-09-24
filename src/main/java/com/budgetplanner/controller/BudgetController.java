package com.budgetplanner.controller;

import com.budgetplanner.config.AuthInterceptor;
import com.budgetplanner.dto.BudgetDto;
import com.budgetplanner.model.Budget;
import com.budgetplanner.service.BudgetService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Optional;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping("/current")
    public ResponseEntity<BudgetDto> getCurrentBudget(
            @RequestParam(required = false) String month,
            HttpSession session) {

        Long userId = (Long) session.getAttribute(AuthInterceptor.SESSION_USER_KEY);
        String targetMonth = (month != null && !month.isBlank()) ? month.trim() : YearMonth.now().toString();

        Optional<Budget> budget = budgetService.getBudget(userId, targetMonth);
        BigDecimal amount = budget.map(Budget::getAmount).orElse(BigDecimal.ZERO);

        return ResponseEntity.ok(new BudgetDto(targetMonth, amount));
    }

    @PostMapping
    public ResponseEntity<BudgetDto> setBudget(
            @Valid @RequestBody BudgetDto dto,
            HttpSession session) {

        Long userId = (Long) session.getAttribute(AuthInterceptor.SESSION_USER_KEY);
        BudgetDto saved = budgetService.setBudget(userId, dto);
        return ResponseEntity.ok(saved);
    }
}
