package com.budgetplanner.dto;

import com.budgetplanner.model.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionRequest {

    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    @NotBlank(message = "Category is required")
    private String category;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Date is required")
    private LocalDate occurredOn;

    private String description;

    public TransactionRequest() {}

    public TransactionRequest(TransactionType type, String category, BigDecimal amount, LocalDate occurredOn, String description) {
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.occurredOn = occurredOn;
        this.description = description;
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
}
