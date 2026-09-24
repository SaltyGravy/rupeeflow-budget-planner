package com.budgetplanner.controller;

import com.budgetplanner.config.AuthInterceptor;
import com.budgetplanner.dto.TransactionRequest;
import com.budgetplanner.dto.TransactionResponse;
import com.budgetplanner.model.TransactionType;
import com.budgetplanner.service.TransactionService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String search,
            HttpSession session) {

        Long userId = (Long) session.getAttribute(AuthInterceptor.SESSION_USER_KEY);
        List<TransactionResponse> result = transactionService.getTransactions(
                userId, type, category, from, to, search);
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody TransactionRequest request,
            HttpSession session) {

        Long userId = (Long) session.getAttribute(AuthInterceptor.SESSION_USER_KEY);
        TransactionResponse response = transactionService.createTransaction(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request,
            HttpSession session) {

        Long userId = (Long) session.getAttribute(AuthInterceptor.SESSION_USER_KEY);
        TransactionResponse response = transactionService.updateTransaction(userId, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteTransaction(
            @PathVariable Long id,
            HttpSession session) {

        Long userId = (Long) session.getAttribute(AuthInterceptor.SESSION_USER_KEY);
        transactionService.deleteTransaction(userId, id);
        return ResponseEntity.ok(Map.of("message", "Transaction deleted successfully"));
    }
}
