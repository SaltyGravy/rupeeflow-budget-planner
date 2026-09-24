package com.budgetplanner.service;

import com.budgetplanner.dto.BudgetDto;
import com.budgetplanner.model.Budget;
import com.budgetplanner.repository.BudgetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;

    public BudgetService(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Budget> getBudget(Long userId, String month) {
        return budgetRepository.findByUserIdAndMonth(userId, month);
    }

    @Transactional
    public BudgetDto setBudget(Long userId, BudgetDto dto) {
        Budget budget = budgetRepository.findByUserIdAndMonth(userId, dto.getMonth())
                .orElse(new Budget(userId, dto.getMonth(), dto.getAmount()));

        budget.setAmount(dto.getAmount());
        Budget saved = budgetRepository.save(budget);
        return new BudgetDto(saved.getMonth(), saved.getAmount());
    }
}
