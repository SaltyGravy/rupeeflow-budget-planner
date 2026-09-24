package com.budgetplanner.dto;

import com.budgetplanner.model.Transaction;
import com.budgetplanner.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TransactionResponse {

    private Long id;
    private TransactionType type;
    private String category;
    private BigDecimal amount;
    private LocalDate occurredOn;
    private String description;
    private LocalDateTime createdAt;

    public TransactionResponse() {}

    public TransactionResponse(Transaction tx) {
        this.id = tx.getId();
        this.type = tx.getType();
        this.category = tx.getCategory();
        this.amount = tx.getAmount();
        this.occurredOn = tx.getOccurredOn();
        this.description = tx.getDescription();
        this.createdAt = tx.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getOccurredOn() {
        return occurredOn;
    }

    public void setOccurredOn(LocalDate occurredOn) {
        this.occurredOn = occurredOn;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
