package com.budgetplanner;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ============================================================================
 * BUDGET CALCULATOR - CORE LOGIC & FINANCIAL MATH
 * ============================================================================
 * This file contains ALL the calculation logic for the budget planner.
 * Written in simple, beginner-friendly Java:
 * - No complex database or web framework code here.
 * - Simple functions with clear inputs and outputs.
 * - Easy-to-read math using standard operators (+, -, *, /).
 * - Standard for loops and clear step-by-step comments.
 * ============================================================================
 */
public class BudgetCalculator {

    // ========================================================================
    // 1. BASIC FINANCIAL TOTALS (INCOME, EXPENSE, BALANCE, SAVINGS)
    // ========================================================================

    /**
     * Calculates total income by adding up all income transactions.
     */
    public static double calculateTotalIncome(List<Transaction> transactions) {
        double total = 0.0;
        for (Transaction t : transactions) {
            if (t.isIncome()) {
                total += t.getAmount();
            }
        }
        return total;
    }

    /**
     * Calculates total expenses by adding up all expense transactions.
     */
    public static double calculateTotalExpense(List<Transaction> transactions) {
        double total = 0.0;
        for (Transaction t : transactions) {
            if (t.isExpense()) {
                total += t.getAmount();
            }
        }
        return total;
    }

    /**
     * Calculates remaining balance = Total Income - Total Expense.
     * Can be positive (surplus) or negative (deficit).
     */
    public static double calculateBalance(double totalIncome, double totalExpense) {
        return totalIncome - totalExpense;
    }

    /**
     * Calculates savings. If balance is positive, that's your savings.
     * If expenses exceeded income, savings is 0 (not negative).
     */
    public static double calculateSavings(double totalIncome, double totalExpense) {
        double balance = totalIncome - totalExpense;
        return balance > 0 ? balance : 0.0;
    }

    // ========================================================================
    // 2. MONTHLY BUDGET & 80% SPENDING ALERTS
    // ========================================================================

    /**
     * Calculates how much budget is left: Budget - Spent.
     * If spent more than budget, remaining is 0.
     */
    public static double calculateBudgetRemaining(double monthlyBudget, double totalExpense) {
        if (monthlyBudget <= 0) return 0.0;
        double remaining = monthlyBudget - totalExpense;
        return remaining > 0 ? remaining : 0.0;
    }

    /**
     * Calculates percentage of budget used: (Spent / Budget) * 100.
     */
    public static double calculateBudgetUsedPercentage(double monthlyBudget, double totalExpense) {
        if (monthlyBudget <= 0) return 0.0;
        return (totalExpense / monthlyBudget) * 100.0;
    }

    /**
     * Checks if the 80% budget alert should trigger:
     * Returns true if user has spent 80% or more of their monthly budget.
     */
    public static boolean isBudgetAlert(double monthlyBudget, double totalExpense) {
        if (monthlyBudget <= 0) return false;
        double usedPercentage = calculateBudgetUsedPercentage(monthlyBudget, totalExpense);
        return usedPercentage >= 80.0;
    }

    /**
     * Generates a friendly alert message if budget used >= 80%.
     */
    public static String getBudgetAlertMessage(double monthlyBudget, double totalExpense) {
        if (!isBudgetAlert(monthlyBudget, totalExpense)) {
            return "";
        }
        double percentage = calculateBudgetUsedPercentage(monthlyBudget, totalExpense);
        double remaining = calculateBudgetRemaining(monthlyBudget, totalExpense);
        return String.format("You have used %.0f%% of your monthly budget. ₹%.0f remaining.", percentage, remaining);
    }

    // ========================================================================
    // 3. SAVINGS GOAL PROGRESS
    // ========================================================================

    /**
     * Calculates savings goal progress percentage (capped at 100%).
     */
    public static double calculateSavingsGoalPercentage(double totalSavings, double targetGoal) {
        if (targetGoal <= 0) return 0.0;
        double percent = (totalSavings / targetGoal) * 100.0;
        return percent > 100.0 ? 100.0 : percent;
    }

    // ========================================================================
    // 4. CATEGORY BREAKDOWN (FOOD, SHOPPING, BILLS, ETC.)
    // ========================================================================

    /**
     * Groups expenses by category (e.g. Food: ₹10,000, Shopping: ₹8,000)
     * and calculates what % of total spending each category represents.
     * Sorted highest spending first.
     */
    public static List<Map<String, Object>> calculateCategoryBreakdown(List<Transaction> transactions, double totalExpense) {
        Map<String, Double> categoryTotals = new HashMap<>();

        // Step 1: Add up expenses for each category
        for (Transaction t : transactions) {
            if (t.isExpense()) {
                String cat = t.getCategory();
                double current = categoryTotals.getOrDefault(cat, 0.0);
                categoryTotals.put(cat, current + t.getAmount());
            }
        }

        // Step 2: Sort categories by amount from highest to lowest
        List<Map.Entry<String, Double>> sortedList = new ArrayList<>(categoryTotals.entrySet());
        sortedList.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // Step 3: Build the final list with percentages
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Double> entry : sortedList) {
            double amount = entry.getValue();
            double percentage = totalExpense > 0 ? (amount / totalExpense) * 100.0 : 0.0;

            Map<String, Object> item = new HashMap<>();
            item.put("category", entry.getKey());
            item.put("amount", amount);
            item.put("percentage", percentage);
            result.add(item);
        }
        return result;
    }

    // ========================================================================
    // 5. HISTORICAL MONTHLY TRENDS (FOR CHARTS)
    // ========================================================================

    /**
     * Calculates income and expense totals for the past N months.
     * Used directly by the bar chart on the dashboard.
     */
    public static List<Map<String, Object>> calculateMonthlyTrend(List<Transaction> allTransactions, YearMonth targetMonth, int pastMonthsCount) {
        List<Map<String, Object>> trend = new ArrayList<>();

        for (int i = pastMonthsCount - 1; i >= 0; i--) {
            YearMonth ym = targetMonth.minusMonths(i);
            double monthInc = 0;
            double monthExp = 0;

            for (Transaction t : allTransactions) {
                if (YearMonth.from(t.getOccurredOn()).equals(ym)) {
                    if (t.isIncome()) monthInc += t.getAmount();
                    if (t.isExpense()) monthExp += t.getAmount();
                }
            }

            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", ym.toString());
            monthData.put("monthLabel", ym.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)));
            monthData.put("income", monthInc);
            monthData.put("expense", monthExp);
            trend.add(monthData);
        }
        return trend;
    }

    // ========================================================================
    // OPTIONAL STANDALONE RUNNER (TEST THE MATH DIRECTLY)
    // ========================================================================

    /**
     * Beginners can run this file directly in terminal/IDE to test the calculations:
     * javac BudgetCalculator.java && java BudgetCalculator
     */
    public static void main(String[] args) {
        System.out.println("=== RUPEEFLOW BUDGET CALCULATOR TEST ===");
        double income = 75000.0;
        double expense = 31500.0;
        double budget = 35000.0;
        double goal = 100000.0;

        System.out.println("Income: ₹" + income);
        System.out.println("Expense: ₹" + expense);
        System.out.println("Remaining Balance: ₹" + calculateBalance(income, expense));
        System.out.println("Savings: ₹" + calculateSavings(income, expense));
        System.out.println("Budget Used: " + (int) calculateBudgetUsedPercentage(budget, expense) + "%");
        System.out.println("Budget Alert Triggered? " + isBudgetAlert(budget, expense));
        System.out.println("Alert Message: " + getBudgetAlertMessage(budget, expense));
        System.out.println("Savings Goal Progress: " + (int) calculateSavingsGoalPercentage(43500, goal) + "%");
    }
}
