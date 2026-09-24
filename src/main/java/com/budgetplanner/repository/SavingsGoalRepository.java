package com.budgetplanner.repository;

import com.budgetplanner.model.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {
    Optional<SavingsGoal> findByUserId(Long userId);
}
