package com.budgetplanner.service;

import com.budgetplanner.dto.DashboardStatsResponse;
import com.budgetplanner.dto.MonthlyReportResponse;
import com.budgetplanner.dto.TransactionResponse;
import com.budgetplanner.model.Budget;
import com.budgetplanner.model.SavingsGoal;
import com.budgetplanner.model.Transaction;
import com.budgetplanner.model.TransactionType;
import com.budgetplanner.repository.BudgetRepository;
import com.budgetplanner.repository.SavingsGoalRepository;
import com.budgetplanner.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatsService {

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final SavingsGoalRepository savingsGoalRepository;

    private static final DateTimeFormatter MONTH_LABEL_FMT = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    public StatsService(TransactionRepository transactionRepository,
                        BudgetRepository budgetRepository,
                        SavingsGoalRepository savingsGoalRepository) {
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.savingsGoalRepository = savingsGoalRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats(Long userId, String targetMonthStr) {
        YearMonth targetMonth = parseYearMonth(targetMonthStr);
        String monthKey = targetMonth.toString(); // YYYY-MM
        String monthLabel = targetMonth.format(MONTH_LABEL_FMT);

        List<Transaction> allTransactions = transactionRepository.findByUserIdOrderByOccurredOnDescCreatedAtDesc(userId);

        // Filter this month's transactions
        List<Transaction> monthTransactions = allTransactions.stream()
                .filter(t -> YearMonth.from(t.getOccurredOn()).equals(targetMonth))
                .toList();

        BigDecimal monthIncome = monthTransactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthExpense = monthTransactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthBalance = monthIncome.subtract(monthExpense);
        BigDecimal monthSavings = monthBalance.compareTo(BigDecimal.ZERO) > 0 ? monthBalance : BigDecimal.ZERO;

        // All time savings
        BigDecimal allTimeIncome = allTransactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal allTimeExpense = allTransactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal allTimeNet = allTimeIncome.subtract(allTimeExpense);
        BigDecimal totalSavingsAllTime = allTimeNet.compareTo(BigDecimal.ZERO) > 0 ? allTimeNet : BigDecimal.ZERO;

        // Budget
        Optional<Budget> budgetOpt = budgetRepository.findByUserIdAndMonth(userId, monthKey);
        BigDecimal monthlyBudget = budgetOpt.map(Budget::getAmount).orElse(BigDecimal.ZERO);
        BigDecimal budgetRemaining = monthlyBudget.compareTo(BigDecimal.ZERO) > 0
                ? monthlyBudget.subtract(monthExpense).max(BigDecimal.ZERO)
                : BigDecimal.ZERO;

        double budgetUsedPercentage = 0.0;
        if (monthlyBudget.compareTo(BigDecimal.ZERO) > 0) {
            budgetUsedPercentage = monthExpense.divide(monthlyBudget, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
        }

        boolean isAlert = monthlyBudget.compareTo(BigDecimal.ZERO) > 0 && budgetUsedPercentage >= 80.0;
        String alertMsg = "";
        if (isAlert) {
            alertMsg = String.format("You have used %.0f%% of your monthly budget. ₹%s remaining.",
                    budgetUsedPercentage, budgetRemaining.setScale(0, RoundingMode.HALF_UP).toPlainString());
        }

        // Savings Goal
        Optional<SavingsGoal> goalOpt = savingsGoalRepository.findByUserId(userId);
        String goalTitle = goalOpt.map(SavingsGoal::getTitle).orElse("Savings goal");
        BigDecimal goalTarget = goalOpt.map(SavingsGoal::getTargetAmount).orElse(BigDecimal.ZERO);
        double goalPercentage = 0.0;
        if (goalTarget.compareTo(BigDecimal.ZERO) > 0) {
            goalPercentage = Math.min(100.0, totalSavingsAllTime.divide(goalTarget, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue());
        }

        // Category Breakdown
        List<DashboardStatsResponse.CategoryStat> categoryStats = calculateCategoryBreakdown(monthTransactions, monthExpense);

        // 6 Month Trend
        List<DashboardStatsResponse.MonthTrend> sixMonthTrend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = targetMonth.minusMonths(i);
            String ymKey = ym.toString();
            String ymLabel = ym.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH));

            BigDecimal inc = allTransactions.stream()
                    .filter(t -> YearMonth.from(t.getOccurredOn()).equals(ym) && t.getType() == TransactionType.INCOME)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal exp = allTransactions.stream()
                    .filter(t -> YearMonth.from(t.getOccurredOn()).equals(ym) && t.getType() == TransactionType.EXPENSE)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            sixMonthTrend.add(new DashboardStatsResponse.MonthTrend(ymKey, ymLabel, inc, exp));
        }

        // Recent Transactions (top 5)
        List<TransactionResponse> recent = allTransactions.stream()
                .limit(5)
                .map(TransactionResponse::new)
                .collect(Collectors.toList());

        DashboardStatsResponse resp = new DashboardStatsResponse();
        resp.setMonth(monthKey);
        resp.setMonthLabel(monthLabel);
        resp.setTotalIncome(monthIncome);
        resp.setTotalExpense(monthExpense);
        resp.setRemainingBalance(monthBalance);
        resp.setSavingsThisMonth(monthSavings);
        resp.setSavingsTotalAllTime(totalSavingsAllTime);

        resp.setMonthlyBudget(monthlyBudget);
        resp.setBudgetUsedAmount(monthExpense);
        resp.setBudgetRemainingAmount(budgetRemaining);
        resp.setBudgetUsedPercentage(budgetUsedPercentage);
        resp.setBudgetAlert(isAlert);
        resp.setBudgetAlertMessage(alertMsg);

        resp.setSavingsGoalTitle(goalTitle);
        resp.setSavingsGoalTarget(goalTarget);
        resp.setSavingsGoalPercentage(goalPercentage);

        resp.setCategoryBreakdown(categoryStats);
        resp.setSixMonthTrend(sixMonthTrend);
        resp.setRecentTransactions(recent);

        return resp;
    }

    @Transactional(readOnly = true)
    public MonthlyReportResponse getMonthlyReport(Long userId, String targetMonthStr) {
        YearMonth targetMonth = parseYearMonth(targetMonthStr);
        String monthKey = targetMonth.toString();
        String monthLabel = targetMonth.format(MONTH_LABEL_FMT);

        List<Transaction> allTransactions = transactionRepository.findByUserIdOrderByOccurredOnDescCreatedAtDesc(userId);

        List<Transaction> monthTransactions = allTransactions.stream()
                .filter(t -> YearMonth.from(t.getOccurredOn()).equals(targetMonth))
                .toList();

        BigDecimal monthIncome = monthTransactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthExpense = monthTransactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthBalance = monthIncome.subtract(monthExpense);
        BigDecimal monthSavings = monthBalance.compareTo(BigDecimal.ZERO) > 0 ? monthBalance : BigDecimal.ZERO;

        // Highest spending category
        List<DashboardStatsResponse.CategoryStat> categoryStats = calculateCategoryBreakdown(monthTransactions, monthExpense);
        String highestCategory = "—";
        BigDecimal highestCategoryAmount = BigDecimal.ZERO;
        if (!categoryStats.isEmpty()) {
            highestCategory = categoryStats.get(0).getCategory();
            highestCategoryAmount = categoryStats.get(0).getAmount();
        }

        // Budget
        Optional<Budget> budgetOpt = budgetRepository.findByUserIdAndMonth(userId, monthKey);
        BigDecimal budgetAmount = budgetOpt.map(Budget::getAmount).orElse(BigDecimal.ZERO);
        BigDecimal budgetRemaining = budgetAmount.compareTo(BigDecimal.ZERO) > 0
                ? budgetAmount.subtract(monthExpense).max(BigDecimal.ZERO)
                : BigDecimal.ZERO;

        // 12 Month Trend
        List<MonthlyReportResponse.SpendingTrend> twelveMonthTrend = new ArrayList<>();
        List<String> availableMonths = new ArrayList<>();

        for (int i = 11; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            String ymKey = ym.toString();
            String ymLabel = ym.format(MONTH_LABEL_FMT);

            BigDecimal exp = allTransactions.stream()
                    .filter(t -> YearMonth.from(t.getOccurredOn()).equals(ym) && t.getType() == TransactionType.EXPENSE)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            twelveMonthTrend.add(new MonthlyReportResponse.SpendingTrend(ymKey, ymLabel, exp));
            availableMonths.add(ymKey);
        }

        Collections.reverse(availableMonths); // latest first for dropdown

        MonthlyReportResponse resp = new MonthlyReportResponse();
        resp.setMonth(monthKey);
        resp.setMonthLabel(monthLabel);
        resp.setTotalIncome(monthIncome);
        resp.setTotalExpense(monthExpense);
        resp.setTotalSavings(monthSavings);
        resp.setRemainingBalance(monthBalance);
        resp.setHighestSpendingCategory(highestCategory);
        resp.setHighestSpendingCategoryAmount(highestCategoryAmount);
        resp.setBudgetAmount(budgetAmount);
        resp.setBudgetRemaining(budgetRemaining);
        resp.setCategoryBreakdown(categoryStats);
        resp.setTwelveMonthTrend(twelveMonthTrend);
        resp.setAvailableMonths(availableMonths);

        return resp;
    }

    private List<DashboardStatsResponse.CategoryStat> calculateCategoryBreakdown(
            List<Transaction> transactions, BigDecimal totalExpense) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (Transaction t : transactions) {
            if (t.getType() == TransactionType.EXPENSE) {
                map.merge(t.getCategory(), t.getAmount(), BigDecimal::add);
            }
        }

        return map.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> {
                    double pct = 0.0;
                    if (totalExpense.compareTo(BigDecimal.ZERO) > 0) {
                        pct = e.getValue().divide(totalExpense, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100)).doubleValue();
                    }
                    return new DashboardStatsResponse.CategoryStat(e.getKey(), e.getValue(), pct);
                })
                .collect(Collectors.toList());
    }

    private YearMonth parseYearMonth(String str) {
        if (str == null || str.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(str.trim());
        } catch (Exception e) {
            return YearMonth.now();
        }
    }
}
