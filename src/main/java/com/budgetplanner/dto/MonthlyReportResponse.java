package com.budgetplanner.dto;

import java.math.BigDecimal;
import java.util.List;

public class MonthlyReportResponse {

    private String month;
    private String monthLabel;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal totalSavings;
    private BigDecimal remainingBalance;
    private String highestSpendingCategory;
    private BigDecimal highestSpendingCategoryAmount;
    private BigDecimal budgetAmount;
    private BigDecimal budgetRemaining;
    private List<DashboardStatsResponse.CategoryStat> categoryBreakdown;
    private List<SpendingTrend> twelveMonthTrend;
    private List<String> availableMonths;

    public static class SpendingTrend {
        private String month;
        private String monthLabel;
        private BigDecimal spending;

        public SpendingTrend() {}

        public SpendingTrend(String month, String monthLabel, BigDecimal spending) {
            this.month = month;
            this.monthLabel = monthLabel;
            this.spending = spending;
        }

        public String getMonth() {
            return month;
        }

        public void setMonth(String month) {
            this.month = month;
        }

        public String getMonthLabel() {
            return monthLabel;
        }

        public void setMonthLabel(String monthLabel) {
            this.monthLabel = monthLabel;
        }

        public BigDecimal getSpending() {
            return spending;
        }

        public void setSpending(BigDecimal spending) {
            this.spending = spending;
        }
    }

    public MonthlyReportResponse() {}

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getMonthLabel() {
        return monthLabel;
    }

    public void setMonthLabel(String monthLabel) {
        this.monthLabel = monthLabel;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(BigDecimal totalExpense) {
        this.totalExpense = totalExpense;
    }

    public BigDecimal getTotalSavings() {
        return totalSavings;
    }

    public void setTotalSavings(BigDecimal totalSavings) {
        this.totalSavings = totalSavings;
    }

    public BigDecimal getRemainingBalance() {
        return remainingBalance;
    }

    public void setRemainingBalance(BigDecimal remainingBalance) {
        this.remainingBalance = remainingBalance;
    }

    public String getHighestSpendingCategory() {
        return highestSpendingCategory;
    }

    public void setHighestSpendingCategory(String highestSpendingCategory) {
        this.highestSpendingCategory = highestSpendingCategory;
    }

    public BigDecimal getHighestSpendingCategoryAmount() {
        return highestSpendingCategoryAmount;
    }

    public void setHighestSpendingCategoryAmount(BigDecimal highestSpendingCategoryAmount) {
        this.highestSpendingCategoryAmount = highestSpendingCategoryAmount;
    }

    public BigDecimal getBudgetAmount() {
        return budgetAmount;
    }

    public void setBudgetAmount(BigDecimal budgetAmount) {
        this.budgetAmount = budgetAmount;
    }

    public BigDecimal getBudgetRemaining() {
        return budgetRemaining;
    }

    public void setBudgetRemaining(BigDecimal budgetRemaining) {
        this.budgetRemaining = budgetRemaining;
    }

    public List<DashboardStatsResponse.CategoryStat> getCategoryBreakdown() {
        return categoryBreakdown;
    }

    public void setCategoryBreakdown(List<DashboardStatsResponse.CategoryStat> categoryBreakdown) {
        this.categoryBreakdown = categoryBreakdown;
    }

    public List<SpendingTrend> getTwelveMonthTrend() {
        return twelveMonthTrend;
    }

    public void setTwelveMonthTrend(List<SpendingTrend> twelveMonthTrend) {
        this.twelveMonthTrend = twelveMonthTrend;
    }

    public List<String> getAvailableMonths() {
        return availableMonths;
    }

    public void setAvailableMonths(List<String> availableMonths) {
        this.availableMonths = availableMonths;
    }
}
