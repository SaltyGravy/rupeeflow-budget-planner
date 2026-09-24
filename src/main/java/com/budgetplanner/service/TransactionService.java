package com.budgetplanner.service;

import com.budgetplanner.dto.TransactionRequest;
import com.budgetplanner.dto.TransactionResponse;
import com.budgetplanner.model.Transaction;
import com.budgetplanner.model.TransactionType;
import com.budgetplanner.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(Long userId, TransactionType type, String category,
                                                     LocalDate startDate, LocalDate endDate, String search) {
        List<Transaction> list = transactionRepository.filterTransactions(
                userId, type, category, startDate, endDate, search);
        return list.stream().map(TransactionResponse::new).collect(Collectors.toList());
    }

    @Transactional
    public TransactionResponse createTransaction(Long userId, TransactionRequest request) {
        Transaction tx = new Transaction(
                userId,
                request.getType(),
                request.getCategory().trim(),
                request.getAmount(),
                request.getOccurredOn(),
                request.getDescription() != null ? request.getDescription().trim() : ""
        );
        Transaction saved = transactionRepository.save(tx);
        return new TransactionResponse(saved);
    }

    @Transactional
    public TransactionResponse updateTransaction(Long userId, Long transactionId, TransactionRequest request) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found."));

        if (!tx.getUserId().equals(userId)) {
            throw new SecurityException("Unauthorized to modify this transaction.");
        }

        tx.setType(request.getType());
        tx.setCategory(request.getCategory().trim());
        tx.setAmount(request.getAmount());
        tx.setOccurredOn(request.getOccurredOn());
        tx.setDescription(request.getDescription() != null ? request.getDescription().trim() : "");

        Transaction updated = transactionRepository.save(tx);
        return new TransactionResponse(updated);
    }

    @Transactional
    public void deleteTransaction(Long userId, Long transactionId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found."));

        if (!tx.getUserId().equals(userId)) {
            throw new SecurityException("Unauthorized to delete this transaction.");
        }

        transactionRepository.delete(tx);
    }
}
