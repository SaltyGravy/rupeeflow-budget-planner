package com.budgetplanner.repository;

import com.budgetplanner.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    Optional<Budget> findByUserIdAndMonth(Long userId, String month);
    List<Budget> findByUserId(Long userId);
}
