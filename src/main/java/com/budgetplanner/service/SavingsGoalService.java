package com.budgetplanner.service;

import com.budgetplanner.dto.SavingsGoalDto;
import com.budgetplanner.model.SavingsGoal;
import com.budgetplanner.repository.SavingsGoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class SavingsGoalService {

    private final SavingsGoalRepository savingsGoalRepository;

    public SavingsGoalService(SavingsGoalRepository savingsGoalRepository) {
        this.savingsGoalRepository = savingsGoalRepository;
    }

    @Transactional(readOnly = true)
    public Optional<SavingsGoal> getSavingsGoal(Long userId) {
        return savingsGoalRepository.findByUserId(userId);
    }

    @Transactional
    public SavingsGoalDto setSavingsGoal(Long userId, SavingsGoalDto dto) {
        SavingsGoal goal = savingsGoalRepository.findByUserId(userId)
                .orElse(new SavingsGoal(userId, dto.getTitle().trim(), dto.getTargetAmount()));

        goal.setTitle(dto.getTitle().trim());
        goal.setTargetAmount(dto.getTargetAmount());
        SavingsGoal saved = savingsGoalRepository.save(goal);
        return new SavingsGoalDto(saved.getTitle(), saved.getTargetAmount());
    }
}
