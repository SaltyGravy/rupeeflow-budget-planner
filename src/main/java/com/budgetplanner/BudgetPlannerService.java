package com.budgetplanner;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Core Service: Handles all financial calculations, transactions, and user accounts.
 */
@Service
class BudgetPlannerService {

    private final UserRepository userRepo;
    private final TransactionRepository txRepo;
    private final BudgetRepository budgetRepo;
    private final SavingsGoalRepository goalRepo;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public BudgetPlannerService(UserRepository userRepo, TransactionRepository txRepo,
                                BudgetRepository budgetRepo, SavingsGoalRepository goalRepo) {
        this.userRepo = userRepo;
        this.txRepo = txRepo;
        this.budgetRepo = budgetRepo;
        this.goalRepo = goalRepo;
    }

    // ===================================================================
    // 1. USER AUTHENTICATION
    // ===================================================================

    public Map<String, Object> register(String fullName, String email, String password) {
        String cleanEmail = email.trim().toLowerCase();
        if (userRepo.existsByEmail(cleanEmail)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        User user = new User(cleanEmail, passwordEncoder.encode(password), fullName.trim());
        User saved = userRepo.save(user);
        return Map.of("id", saved.getId(), "email", saved.getEmail(), "fullName", saved.getFullName());
    }

    public User authenticate(String email, String password) {
        String cleanEmail = email.trim().toLowerCase();
        User user = userRepo.findByEmail(cleanEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }
        return user;
    }

    public Map<String, Object> getUserById(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        return Map.of("id", user.getId(), "email", user.getEmail(), "fullName", user.getFullName());
    }

    // ===================================================================
    // 2. TRANSACTION MANAGEMENT (ADD, EDIT, DELETE, FILTER)
    // ===================================================================

    public List<Transaction> getTransactions(Long userId, TransactionType type, String category,
                                             LocalDate fromDate, LocalDate toDate, String search) {
        List<Transaction> all = txRepo.findByUserIdOrderByOccurredOnDesc(userId);
        List<Transaction> filtered = new ArrayList<>();
        String query = (search != null) ? search.trim().toLowerCase() : "";

        for (Transaction t : all) {
            if (type != null && t.getType() != type) continue;
            if (category != null && !category.isBlank() && !t.getCategory().equalsIgnoreCase(category)) continue;
            if (fromDate != null && t.getOccurredOn().isBefore(fromDate)) continue;
            if (toDate != null && t.getOccurredOn().isAfter(toDate)) continue;
            if (!query.isEmpty()) {
                String desc = (t.getDescription() != null ? t.getDescription() : "").toLowerCase();
                String cat = t.getCategory().toLowerCase();
                String amt = String.valueOf(t.getAmount());
                if (!desc.contains(query) && !cat.contains(query) && !amt.contains(query)) {
                    continue;
                }
            }
            filtered.add(t);
        }
        return filtered;
    }

    public Transaction createTransaction(Long userId, TransactionType type, String category,
                                         double amount, LocalDate date, String description) {
        Transaction tx = new Transaction(userId, type, category, amount, date, description);
        return txRepo.save(tx);
    }

    public Transaction updateTransaction(Long userId, Long txId, TransactionType type,
                                         String category, double amount, LocalDate date, String description) {
        Transaction tx = txRepo.findById(txId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found."));
        if (!tx.getUserId().equals(userId)) {
            throw new SecurityException("Unauthorized to modify this transaction.");
        }
        tx.setType(type);
        tx.setCategory(category);
        tx.setAmount(amount);
        tx.setOccurredOn(date);
        tx.setDescription(description);
        return txRepo.save(tx);
    }

    public void deleteTransaction(Long userId, Long txId) {
        Transaction tx = txRepo.findById(txId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found."));
        if (!tx.getUserId().equals(userId)) {
            throw new SecurityException("Unauthorized to delete this transaction.");
        }
        txRepo.delete(tx);
    }

    // ===================================================================
    // 3. MONTHLY BUDGET & SAVINGS GOALS
    // ===================================================================

    public Budget getBudget(Long userId, String month) {
        return budgetRepo.findByUserIdAndMonth(userId, month)
                .orElse(new Budget(userId, month, 0.0));
    }

    public Budget setBudget(Long userId, String month, double amount) {
        Budget budget = budgetRepo.findByUserIdAndMonth(userId, month)
                .orElse(new Budget(userId, month, amount));
        budget.setAmount(amount);
        return budgetRepo.save(budget);
    }

    public SavingsGoal getSavingsGoal(Long userId) {
        return goalRepo.findByUserId(userId)
                .orElse(new SavingsGoal(userId, "Savings goal", 0.0));
    }

    public SavingsGoal setSavingsGoal(Long userId, String title, double targetAmount) {
        SavingsGoal goal = goalRepo.findByUserId(userId)
                .orElse(new SavingsGoal(userId, title, targetAmount));
        goal.setTitle(title);
        goal.setTargetAmount(targetAmount);
        return goalRepo.save(goal);
    }

    // ===================================================================
    // 4. DASHBOARD ANALYTICS & 80% BUDGET ALERTS
    // ===================================================================

    public Map<String, Object> getDashboardStats(Long userId, String targetMonthStr) {
        YearMonth targetMonth = parseMonth(targetMonthStr);
        String monthKey = targetMonth.toString(); // "YYYY-MM"
        String monthLabel = targetMonth.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH));

        List<Transaction> all = txRepo.findByUserIdOrderByOccurredOnDesc(userId);

        // Step 1: Calculate This Month's Income & Expenses
        double monthIncome = 0;
        double monthExpense = 0;
        List<Transaction> monthTransactions = new ArrayList<>();

        for (Transaction t : all) {
            if (YearMonth.from(t.getOccurredOn()).equals(targetMonth)) {
                monthTransactions.add(t);
                if (t.isIncome()) monthIncome += t.getAmount();
                if (t.isExpense()) monthExpense += t.getAmount();
            }
        }

        double remainingBalance = monthIncome - monthExpense;
        double savingsThisMonth = Math.max(remainingBalance, 0);

        // Step 2: Calculate All-Time Cumulative Savings
        double allIncome = 0;
        double allExpense = 0;
        for (Transaction t : all) {
            if (t.isIncome()) allIncome += t.getAmount();
            if (t.isExpense()) allExpense += t.getAmount();
        }
        double totalSavingsAllTime = Math.max(allIncome - allExpense, 0);

        // Step 3: Monthly Budget & 80% Expense Alert
        Budget budget = getBudget(userId, monthKey);
        double monthlyBudget = budget.getAmount();
        double budgetRemaining = Math.max(monthlyBudget - monthExpense, 0);
        double budgetUsedPercent = monthlyBudget > 0 ? (monthExpense / monthlyBudget) * 100 : 0;

        // Triggers alert if user has spent 80% or more of their monthly budget
        boolean budgetAlert = monthlyBudget > 0 && budgetUsedPercent >= 80.0;
        String budgetAlertMessage = budgetAlert
                ? String.format("You have used %.0f%% of your monthly budget. ₹%.0f remaining.", budgetUsedPercent, budgetRemaining)
                : "";

        // Step 4: Savings Goal Progress
        SavingsGoal goal = getSavingsGoal(userId);
        double goalPercent = goal.getTargetAmount() > 0
                ? Math.min(100.0, (totalSavingsAllTime / goal.getTargetAmount()) * 100) : 0;

        // Step 5: Category Breakdown
        List<Map<String, Object>> categoryBreakdown = computeCategories(monthTransactions, monthExpense);

        // Step 6: 6-Month Income vs Expense Bar Chart Data
        List<Map<String, Object>> sixMonthTrend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = targetMonth.minusMonths(i);
            double inc = 0;
            double exp = 0;
            for (Transaction t : all) {
                if (YearMonth.from(t.getOccurredOn()).equals(ym)) {
                    if (t.isIncome()) inc += t.getAmount();
                    if (t.isExpense()) exp += t.getAmount();
                }
            }
            sixMonthTrend.add(Map.of(
                    "month", ym.toString(),
                    "monthLabel", ym.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)),
                    "income", inc,
                    "expense", exp
            ));
        }

        // Top 5 recent transactions
        List<Transaction> recent = all.size() > 5 ? all.subList(0, 5) : all;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("month", monthKey);
        stats.put("monthLabel", monthLabel);
        stats.put("totalIncome", monthIncome);
        stats.put("totalExpense", monthExpense);
        stats.put("remainingBalance", remainingBalance);
        stats.put("savingsThisMonth", savingsThisMonth);
        stats.put("savingsTotalAllTime", totalSavingsAllTime);
        stats.put("monthlyBudget", monthlyBudget);
        stats.put("budgetUsedAmount", monthExpense);
        stats.put("budgetRemainingAmount", budgetRemaining);
        stats.put("budgetUsedPercentage", budgetUsedPercent);
        stats.put("budgetAlert", budgetAlert);
        stats.put("budgetAlertMessage", budgetAlertMessage);
        stats.put("savingsGoalTitle", goal.getTitle());
        stats.put("savingsGoalTarget", goal.getTargetAmount());
        stats.put("savingsGoalPercentage", goalPercent);
        stats.put("categoryBreakdown", categoryBreakdown);
        stats.put("sixMonthTrend", sixMonthTrend);
        stats.put("recentTransactions", recent);
        return stats;
    }

    // ===================================================================
    // 5. MONTHLY FINANCIAL REPORTS (12-MONTH TREND)
    // ===================================================================

    public Map<String, Object> getMonthlyReport(Long userId, String targetMonthStr) {
        YearMonth targetMonth = parseMonth(targetMonthStr);
        String monthKey = targetMonth.toString();
        String monthLabel = targetMonth.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH));

        List<Transaction> all = txRepo.findByUserIdOrderByOccurredOnDesc(userId);

        double totalIncome = 0;
        double totalExpense = 0;
        List<Transaction> monthTransactions = new ArrayList<>();

        for (Transaction t : all) {
            if (YearMonth.from(t.getOccurredOn()).equals(targetMonth)) {
                monthTransactions.add(t);
                if (t.isIncome()) totalIncome += t.getAmount();
                if (t.isExpense()) totalExpense += t.getAmount();
            }
        }

        double balance = totalIncome - totalExpense;
        double savings = Math.max(balance, 0);

        List<Map<String, Object>> categories = computeCategories(monthTransactions, totalExpense);
        String highestCategory = categories.isEmpty() ? "—" : (String) categories.get(0).get("category");
        double highestAmount = categories.isEmpty() ? 0 : (double) categories.get(0).get("amount");

        Budget budget = getBudget(userId, monthKey);
        double budgetAmount = budget.getAmount();
        double budgetRemaining = Math.max(budgetAmount - totalExpense, 0);

        // 12-month spending trend line chart
        List<Map<String, Object>> twelveMonthTrend = new ArrayList<>();
        List<String> availableMonths = new ArrayList<>();

        for (int i = 11; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            double spend = 0;
            for (Transaction t : all) {
                if (YearMonth.from(t.getOccurredOn()).equals(ym) && t.isExpense()) {
                    spend += t.getAmount();
                }
            }
            twelveMonthTrend.add(Map.of(
                    "month", ym.toString(),
                    "monthLabel", ym.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)),
                    "spending", spend
            ));
            availableMonths.add(ym.toString());
        }
        Collections.reverse(availableMonths); // Show latest month first in dropdown

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("month", monthKey);
        report.put("monthLabel", monthLabel);
        report.put("totalIncome", totalIncome);
        report.put("totalExpense", totalExpense);
        report.put("totalSavings", savings);
        report.put("remainingBalance", balance);
        report.put("highestSpendingCategory", highestCategory);
        report.put("highestSpendingCategoryAmount", highestAmount);
        report.put("budgetAmount", budgetAmount);
        report.put("budgetRemaining", budgetRemaining);
        report.put("categoryBreakdown", categories);
        report.put("twelveMonthTrend", twelveMonthTrend);
        report.put("availableMonths", availableMonths);
        return report;
    }

    // Helper: Compute Category Totals & Percentages
    private List<Map<String, Object>> computeCategories(List<Transaction> transactions, double totalExpense) {
        Map<String, Double> map = new HashMap<>();
        for (Transaction t : transactions) {
            if (t.isExpense()) {
                map.put(t.getCategory(), map.getOrDefault(t.getCategory(), 0.0) + t.getAmount());
            }
        }

        List<Map.Entry<String, Double>> list = new ArrayList<>(map.entrySet());
        list.sort((a, b) -> Double.compare(b.getValue(), a.getValue())); // highest spending first

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Double> entry : list) {
            double percent = totalExpense > 0 ? (entry.getValue() / totalExpense) * 100 : 0;
            result.add(Map.of(
                    "category", entry.getKey(),
                    "amount", entry.getValue(),
                    "percentage", percent
            ));
        }
        return result;
    }

    private YearMonth parseMonth(String str) {
        if (str == null || str.isBlank()) return YearMonth.now();
        try {
            return YearMonth.parse(str.trim());
        } catch (Exception e) {
            return YearMonth.now();
        }
    }
}
