package com.budgetplanner;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
class BudgetPlannerService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final DateTimeFormatter MONTH_LABEL_FMT = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    public BudgetPlannerService(UserRepository userRepository,
                                TransactionRepository transactionRepository,
                                BudgetRepository budgetRepository,
                                SavingsGoalRepository savingsGoalRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.savingsGoalRepository = savingsGoalRepository;
    }

    // --- User Auth ---
    @Transactional
    public Map<String, Object> register(String fullName, String email, String password) {
        String cleanEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        User user = new User(cleanEmail, passwordEncoder.encode(password), fullName.trim());
        User saved = userRepository.save(user);
        return Map.of("id", saved.getId(), "email", saved.getEmail(), "fullName", saved.getFullName());
    }

    @Transactional(readOnly = true)
    public User authenticate(String email, String password) {
        String cleanEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        return Map.of("id", user.getId(), "email", user.getEmail(), "fullName", user.getFullName());
    }

    // --- Transactions ---
    @Transactional(readOnly = true)
    public List<Transaction> getTransactions(Long userId, TransactionType type, String category,
                                             LocalDate startDate, LocalDate endDate, String search) {
        return transactionRepository.filterTransactions(userId, type, category, startDate, endDate, search);
    }

    @Transactional
    public Transaction createTransaction(Long userId, TransactionType type, String category,
                                         BigDecimal amount, LocalDate occurredOn, String description) {
        Transaction tx = new Transaction(userId, type, category.trim(), amount, occurredOn,
                description != null ? description.trim() : "");
        return transactionRepository.save(tx);
    }

    @Transactional
    public Transaction updateTransaction(Long userId, Long transactionId, TransactionType type,
                                         String category, BigDecimal amount, LocalDate occurredOn, String description) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found."));
        if (!tx.getUserId().equals(userId)) {
            throw new SecurityException("Unauthorized to modify this transaction.");
        }
        tx.setType(type);
        tx.setCategory(category.trim());
        tx.setAmount(amount);
        tx.setOccurredOn(occurredOn);
        tx.setDescription(description != null ? description.trim() : "");
        return transactionRepository.save(tx);
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

    // --- Budget ---
    @Transactional(readOnly = true)
    public Optional<Budget> getBudget(Long userId, String month) {
        return budgetRepository.findByUserIdAndMonth(userId, month);
    }

    @Transactional
    public Budget setBudget(Long userId, String month, BigDecimal amount) {
        Budget budget = budgetRepository.findByUserIdAndMonth(userId, month)
                .orElse(new Budget(userId, month, amount));
        budget.setAmount(amount);
        return budgetRepository.save(budget);
    }

    // --- Savings Goal ---
    @Transactional(readOnly = true)
    public Optional<SavingsGoal> getSavingsGoal(Long userId) {
        return savingsGoalRepository.findByUserId(userId);
    }

    @Transactional
    public SavingsGoal setSavingsGoal(Long userId, String title, BigDecimal targetAmount) {
        SavingsGoal goal = savingsGoalRepository.findByUserId(userId)
                .orElse(new SavingsGoal(userId, title.trim(), targetAmount));
        goal.setTitle(title.trim());
        goal.setTargetAmount(targetAmount);
        return savingsGoalRepository.save(goal);
    }

    // --- Analytics ---
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats(Long userId, String targetMonthStr) {
        YearMonth targetMonth = parseYearMonth(targetMonthStr);
        String monthKey = targetMonth.toString();
        String monthLabel = targetMonth.format(MONTH_LABEL_FMT);

        List<Transaction> allTransactions = transactionRepository.findByUserIdOrderByOccurredOnDescCreatedAtDesc(userId);
        List<Transaction> monthTransactions = allTransactions.stream()
                .filter(t -> YearMonth.from(t.getOccurredOn()).equals(targetMonth))
                .toList();

        BigDecimal monthIncome = sum(monthTransactions, TransactionType.INCOME);
        BigDecimal monthExpense = sum(monthTransactions, TransactionType.EXPENSE);
        BigDecimal monthBalance = monthIncome.subtract(monthExpense);
        BigDecimal monthSavings = monthBalance.max(BigDecimal.ZERO);

        BigDecimal allTimeIncome = sum(allTransactions, TransactionType.INCOME);
        BigDecimal allTimeExpense = sum(allTransactions, TransactionType.EXPENSE);
        BigDecimal totalSavingsAllTime = allTimeIncome.subtract(allTimeExpense).max(BigDecimal.ZERO);

        BigDecimal monthlyBudget = budgetRepository.findByUserIdAndMonth(userId, monthKey)
                .map(Budget::getAmount).orElse(BigDecimal.ZERO);
        BigDecimal budgetRemaining = monthlyBudget.compareTo(BigDecimal.ZERO) > 0
                ? monthlyBudget.subtract(monthExpense).max(BigDecimal.ZERO) : BigDecimal.ZERO;

        double budgetUsedPercentage = monthlyBudget.compareTo(BigDecimal.ZERO) > 0
                ? monthExpense.divide(monthlyBudget, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0;

        boolean isAlert = monthlyBudget.compareTo(BigDecimal.ZERO) > 0 && budgetUsedPercentage >= 80.0;
        String alertMsg = isAlert
                ? String.format("You have used %.0f%% of your monthly budget. ₹%s remaining.",
                        budgetUsedPercentage, budgetRemaining.setScale(0, RoundingMode.HALF_UP).toPlainString()) : "";

        Optional<SavingsGoal> goalOpt = savingsGoalRepository.findByUserId(userId);
        String goalTitle = goalOpt.map(SavingsGoal::getTitle).orElse("Savings goal");
        BigDecimal goalTarget = goalOpt.map(SavingsGoal::getTargetAmount).orElse(BigDecimal.ZERO);
        double goalPercentage = goalTarget.compareTo(BigDecimal.ZERO) > 0
                ? Math.min(100.0, totalSavingsAllTime.divide(goalTarget, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()) : 0.0;

        List<Map<String, Object>> categoryStats = categoryBreakdown(monthTransactions, monthExpense);

        List<Map<String, Object>> sixMonthTrend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = targetMonth.minusMonths(i);
            BigDecimal inc = sum(allTransactions.stream().filter(t -> YearMonth.from(t.getOccurredOn()).equals(ym)).toList(), TransactionType.INCOME);
            BigDecimal exp = sum(allTransactions.stream().filter(t -> YearMonth.from(t.getOccurredOn()).equals(ym)).toList(), TransactionType.EXPENSE);
            sixMonthTrend.add(Map.of("month", ym.toString(), "monthLabel", ym.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)), "income", inc, "expense", exp));
        }

        List<Transaction> recent = allTransactions.stream().limit(5).collect(Collectors.toList());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("month", monthKey);
        resp.put("monthLabel", monthLabel);
        resp.put("totalIncome", monthIncome);
        resp.put("totalExpense", monthExpense);
        resp.put("remainingBalance", monthBalance);
        resp.put("savingsThisMonth", monthSavings);
        resp.put("savingsTotalAllTime", totalSavingsAllTime);
        resp.put("monthlyBudget", monthlyBudget);
        resp.put("budgetUsedAmount", monthExpense);
        resp.put("budgetRemainingAmount", budgetRemaining);
        resp.put("budgetUsedPercentage", budgetUsedPercentage);
        resp.put("budgetAlert", isAlert);
        resp.put("budgetAlertMessage", alertMsg);
        resp.put("savingsGoalTitle", goalTitle);
        resp.put("savingsGoalTarget", goalTarget);
        resp.put("savingsGoalPercentage", goalPercentage);
        resp.put("categoryBreakdown", categoryStats);
        resp.put("sixMonthTrend", sixMonthTrend);
        resp.put("recentTransactions", recent);
        return resp;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMonthlyReport(Long userId, String targetMonthStr) {
        YearMonth targetMonth = parseYearMonth(targetMonthStr);
        String monthKey = targetMonth.toString();
        String monthLabel = targetMonth.format(MONTH_LABEL_FMT);

        List<Transaction> allTransactions = transactionRepository.findByUserIdOrderByOccurredOnDescCreatedAtDesc(userId);
        List<Transaction> monthTransactions = allTransactions.stream()
                .filter(t -> YearMonth.from(t.getOccurredOn()).equals(targetMonth))
                .toList();

        BigDecimal monthIncome = sum(monthTransactions, TransactionType.INCOME);
        BigDecimal monthExpense = sum(monthTransactions, TransactionType.EXPENSE);
        BigDecimal monthBalance = monthIncome.subtract(monthExpense);
        BigDecimal monthSavings = monthBalance.max(BigDecimal.ZERO);

        List<Map<String, Object>> categoryStats = categoryBreakdown(monthTransactions, monthExpense);
        String highestCategory = categoryStats.isEmpty() ? "—" : (String) categoryStats.get(0).get("category");
        BigDecimal highestCategoryAmount = categoryStats.isEmpty() ? BigDecimal.ZERO : (BigDecimal) categoryStats.get(0).get("amount");

        BigDecimal budgetAmount = budgetRepository.findByUserIdAndMonth(userId, monthKey)
                .map(Budget::getAmount).orElse(BigDecimal.ZERO);
        BigDecimal budgetRemaining = budgetAmount.compareTo(BigDecimal.ZERO) > 0
                ? budgetAmount.subtract(monthExpense).max(BigDecimal.ZERO) : BigDecimal.ZERO;

        List<Map<String, Object>> twelveMonthTrend = new ArrayList<>();
        List<String> availableMonths = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            BigDecimal exp = sum(allTransactions.stream().filter(t -> YearMonth.from(t.getOccurredOn()).equals(ym)).toList(), TransactionType.EXPENSE);
            twelveMonthTrend.add(Map.of("month", ym.toString(), "monthLabel", ym.format(MONTH_LABEL_FMT), "spending", exp));
            availableMonths.add(ym.toString());
        }
        Collections.reverse(availableMonths);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("month", monthKey);
        resp.put("monthLabel", monthLabel);
        resp.put("totalIncome", monthIncome);
        resp.put("totalExpense", monthExpense);
        resp.put("totalSavings", monthSavings);
        resp.put("remainingBalance", monthBalance);
        resp.put("highestSpendingCategory", highestCategory);
        resp.put("highestSpendingCategoryAmount", highestCategoryAmount);
        resp.put("budgetAmount", budgetAmount);
        resp.put("budgetRemaining", budgetRemaining);
        resp.put("categoryBreakdown", categoryStats);
        resp.put("twelveMonthTrend", twelveMonthTrend);
        resp.put("availableMonths", availableMonths);
        return resp;
    }

    private BigDecimal sum(List<Transaction> list, TransactionType type) {
        return list.stream()
                .filter(t -> t.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Map<String, Object>> categoryBreakdown(List<Transaction> transactions, BigDecimal totalExpense) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (Transaction t : transactions) {
            if (t.getType() == TransactionType.EXPENSE) {
                map.merge(t.getCategory(), t.getAmount(), BigDecimal::add);
            }
        }
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> {
                    double pct = totalExpense.compareTo(BigDecimal.ZERO) > 0
                            ? e.getValue().divide(totalExpense, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0;
                    return Map.<String, Object>of("category", e.getKey(), "amount", e.getValue(), "percentage", pct);
                })
                .collect(Collectors.toList());
    }

    private YearMonth parseYearMonth(String str) {
        if (str == null || str.isBlank()) return YearMonth.now();
        try { return YearMonth.parse(str.trim()); } catch (Exception e) { return YearMonth.now(); }
    }
}
