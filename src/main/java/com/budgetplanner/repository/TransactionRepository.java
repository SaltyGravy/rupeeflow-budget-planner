package com.budgetplanner.repository;

import com.budgetplanner.model.Transaction;
import com.budgetplanner.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserIdOrderByOccurredOnDescCreatedAtDesc(Long userId);

    List<Transaction> findByUserIdAndOccurredOnBetweenOrderByOccurredOnDescCreatedAtDesc(
            Long userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId " +
           "AND (:type IS NULL OR t.type = :type) " +
           "AND (:category IS NULL OR :category = '' OR t.category = :category) " +
           "AND (:startDate IS NULL OR t.occurredOn >= :startDate) " +
           "AND (:endDate IS NULL OR t.occurredOn <= :endDate) " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "     LOWER(COALESCE(t.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(t.category) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     CAST(t.amount AS string) LIKE CONCAT('%', :keyword, '%')) " +
           "ORDER BY t.occurredOn DESC, t.createdAt DESC")
    List<Transaction> filterTransactions(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("category") String category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("keyword") String keyword);
}
