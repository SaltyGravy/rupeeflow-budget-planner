package com.budgetplanner.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public class BudgetDto {

    @NotBlank(message = "Month is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "Month must be in YYYY-MM format")
    private String month;

    @NotNull(message = "Budget amount is required")
    @DecimalMin(value = "0.01", message = "Budget must be greater than 0")
    private BigDecimal amount;

    public BudgetDto() {}

    public BudgetDto(String month, BigDecimal amount) {
        this.month = month;
        this.amount = amount;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
