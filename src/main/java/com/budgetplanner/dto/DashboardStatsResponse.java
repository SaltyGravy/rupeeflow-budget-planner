package com.budgetplanner.dto;

import java.math.BigDecimal;
import java.util.List;

public class DashboardStatsResponse {

    private String month;
    private String monthLabel;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal remainingBalance;
    private BigDecimal savingsThisMonth;
    private BigDecimal savingsTotalAllTime;

    private BigDecimal monthlyBudget;
    private BigDecimal budgetUsedAmount;
    private BigDecimal budgetRemainingAmount;
    private double budgetUsedPercentage;
    private boolean budgetAlert;
    private String budgetAlertMessage;

    private String savingsGoalTitle;
    private BigDecimal savingsGoalTarget;
    private double savingsGoalPercentage;

    private List<CategoryStat> categoryBreakdown;
    private List<MonthTrend> sixMonthTrend;
    private List<TransactionResponse> recentTransactions;

    public static class CategoryStat {
        private String category;
        private BigDecimal amount;
        private double percentage;

        public CategoryStat() {}

        public CategoryStat(String category, BigDecimal amount, double percentage) {
            this.category = category;
            this.amount = amount;
            this.percentage = percentage;
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

        public double getPercentage() {
            return percentage;
        }

        public void setPercentage(double percentage) {
            this.percentage = percentage;
        }
    }

    public static class MonthTrend {
        private String month;
        private String monthLabel;
        private BigDecimal income;
        private BigDecimal expense;

        public MonthTrend() {}

        public MonthTrend(String month, String monthLabel, BigDecimal income, BigDecimal expense) {
            this.month = month;
            this.monthLabel = monthLabel;
            this.income = income;
            this.expense = expense;
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

        public BigDecimal getIncome() {
            return income;
        }

        public void setIncome(BigDecimal income) {
            this.income = income;
        }

        public BigDecimal getExpense() {
            return expense;
        }

        public void setExpense(BigDecimal expense) {
            this.expense = expense;
        }
    }

    public DashboardStatsResponse() {}

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

    public BigDecimal getRemainingBalance() {
        return remainingBalance;
    }

    public void setRemainingBalance(BigDecimal remainingBalance) {
        this.remainingBalance = remainingBalance;
    }

    public BigDecimal getSavingsThisMonth() {
        return savingsThisMonth;
    }

    public void setSavingsThisMonth(BigDecimal savingsThisMonth) {
        this.savingsThisMonth = savingsThisMonth;
    }

    public BigDecimal getSavingsTotalAllTime() {
        return savingsTotalAllTime;
    }

    public void setSavingsTotalAllTime(BigDecimal savingsTotalAllTime) {
        this.savingsTotalAllTime = savingsTotalAllTime;
    }

    public BigDecimal getMonthlyBudget() {
        return monthlyBudget;
    }

    public void setMonthlyBudget(BigDecimal monthlyBudget) {
        this.monthlyBudget = monthlyBudget;
    }

    public BigDecimal getBudgetUsedAmount() {
        return budgetUsedAmount;
    }

    public void setBudgetUsedAmount(BigDecimal budgetUsedAmount) {
        this.budgetUsedAmount = budgetUsedAmount;
    }

    public BigDecimal getBudgetRemainingAmount() {
        return budgetRemainingAmount;
    }

    public void setBudgetRemainingAmount(BigDecimal budgetRemainingAmount) {
        this.budgetRemainingAmount = budgetRemainingAmount;
    }

    public double getBudgetUsedPercentage() {
        return budgetUsedPercentage;
    }

    public void setBudgetUsedPercentage(double budgetUsedPercentage) {
        this.budgetUsedPercentage = budgetUsedPercentage;
    }

    public boolean isBudgetAlert() {
        return budgetAlert;
    }

    public void setBudgetAlert(boolean budgetAlert) {
        this.budgetAlert = budgetAlert;
    }

    public String getBudgetAlertMessage() {
        return budgetAlertMessage;
    }

    public void setBudgetAlertMessage(String budgetAlertMessage) {
        this.budgetAlertMessage = budgetAlertMessage;
    }

    public String getSavingsGoalTitle() {
        return savingsGoalTitle;
    }

    public void setSavingsGoalTitle(String savingsGoalTitle) {
        this.savingsGoalTitle = savingsGoalTitle;
    }

    public BigDecimal getSavingsGoalTarget() {
        return savingsGoalTarget;
    }

    public void setSavingsGoalTarget(BigDecimal savingsGoalTarget) {
        this.savingsGoalTarget = savingsGoalTarget;
    }

    public double getSavingsGoalPercentage() {
        return savingsGoalPercentage;
    }

    public void setSavingsGoalPercentage(double savingsGoalPercentage) {
        this.savingsGoalPercentage = savingsGoalPercentage;
    }

    public List<CategoryStat> getCategoryBreakdown() {
        return categoryBreakdown;
    }

    public void setCategoryBreakdown(List<CategoryStat> categoryBreakdown) {
        this.categoryBreakdown = categoryBreakdown;
    }

    public List<MonthTrend> getSixMonthTrend() {
        return sixMonthTrend;
    }

    public void setSixMonthTrend(List<MonthTrend> sixMonthTrend) {
        this.sixMonthTrend = sixMonthTrend;
    }

    public List<TransactionResponse> getRecentTransactions() {
        return recentTransactions;
    }

    public void setRecentTransactions(List<TransactionResponse> recentTransactions) {
        this.recentTransactions = recentTransactions;
    }
}
