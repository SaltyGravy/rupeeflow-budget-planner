package com.budgetplanner;

import jakarta.persistence.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ============================================================================
 * RUPEEFLOW - ALL-IN-ONE PERSONAL BUDGET PLANNER
 * ============================================================================
 * This single Java file contains the entire backend:
 * 1. Main Application Entry Point
 * 2. Database Models (User, Transaction, Budget, SavingsGoal)
 * 3. Database Repositories (save, find, delete)
 * 4. Business Logic Service (math, budget alerts, 6-month trends)
 * 5. REST API Controller (web endpoints for the frontend)
 * 6. Web Configuration & Demo Data Seeder
 * ============================================================================
 */

// ============================================================================
// SECTION 1: MAIN APPLICATION ENTRY POINT
// ============================================================================

@SpringBootApplication
public class BudgetPlannerApplication {

    public static void main(String[] args) {
        // Starts the Spring Boot web server on http://localhost:8080
        SpringApplication.run(BudgetPlannerApplication.class, args);
    }
}

// ============================================================================
// SECTION 2: DATA MODELS (DATABASE ENTITIES)
// ============================================================================

enum TransactionType {
    INCOME,
    EXPENSE
}

/**
 * Stores registered users in the database table "users".
 */
@Entity
@Table(name = "users")
class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    private LocalDateTime createdAt = LocalDateTime.now();

    public User() {}

    public User(String email, String password, String fullName) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}

/**
 * Stores an income or expense transaction in the table "transactions".
 */
@Entity
@Table(name = "transactions")
class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type; // INCOME or EXPENSE

    @Column(nullable = false)
    private String category; // Food, Travel, Bills, Salary, etc.

    @Column(nullable = false)
    private double amount; // Amount in Rupees

    @Column(nullable = false)
    private LocalDate occurredOn; // Date of transaction

    private String description;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Transaction() {}

    public Transaction(Long userId, TransactionType type, String category, double amount, LocalDate occurredOn, String description) {
        this.userId = userId;
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.occurredOn = occurredOn;
        this.description = description;
    }

    // Helper checks to make logic easy to read
    public boolean isIncome() {
        return this.type == TransactionType.INCOME;
    }

    public boolean isExpense() {
        return this.type == TransactionType.EXPENSE;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public LocalDate getOccurredOn() { return occurredOn; }
    public void setOccurredOn(LocalDate occurredOn) { this.occurredOn = occurredOn; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}

/**
 * Stores the monthly budget target set by the user in "budgets".
 */
@Entity
@Table(name = "budgets")
class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    // "budget_month" avoids SQL reserved word conflict
    @Column(name = "budget_month", nullable = false)
    private String month; // format: "YYYY-MM"

    @Column(nullable = false)
    private double amount; // Budget limit in Rupees

    public Budget() {}

    public Budget(Long userId, String month, double amount) {
        this.userId = userId;
        this.month = month;
        this.amount = amount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}

/**
 * Stores the user's savings goal in "savings_goals".
 */
@Entity
@Table(name = "savings_goals")
class SavingsGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title = "Savings goal";

    @Column(nullable = false)
    private double targetAmount; // Target in Rupees

    public SavingsGoal() {}

    public SavingsGoal(Long userId, String title, double targetAmount) {
        this.userId = userId;
        this.title = title;
        this.targetAmount = targetAmount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public double getTargetAmount() { return targetAmount; }
    public void setTargetAmount(double targetAmount) { this.targetAmount = targetAmount; }
}

// ============================================================================
// SECTION 3: DATABASE REPOSITORIES
// ============================================================================
// Spring Data JPA automatically provides standard CRUD methods (save, findById, delete).

@Repository
interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}

@Repository
interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserIdOrderByOccurredOnDesc(Long userId);
}

@Repository
interface BudgetRepository extends JpaRepository<Budget, Long> {
    Optional<Budget> findByUserIdAndMonth(Long userId, String month);
}

@Repository
interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {
    Optional<SavingsGoal> findByUserId(Long userId);
}

// ============================================================================
// SECTION 4: SERVICE / BUSINESS LOGIC (SIMPLE & EASY TO READ)
// ============================================================================

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

    // ------------------------------------------------------------------------
    // A. USER AUTHENTICATION
    // ------------------------------------------------------------------------

    public Map<String, Object> register(String fullName, String email, String password) {
        String cleanEmail = email.trim().toLowerCase();
        if (userRepo.existsByEmail(cleanEmail)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        User newUser = new User(cleanEmail, passwordEncoder.encode(password), fullName.trim());
        User saved = userRepo.save(newUser);
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

    // ------------------------------------------------------------------------
    // B. TRANSACTIONS (ADD, EDIT, DELETE, FILTER)
    // ------------------------------------------------------------------------

    public List<Transaction> getTransactions(Long userId, TransactionType type, String category,
                                             LocalDate fromDate, LocalDate toDate, String search) {
        List<Transaction> all = txRepo.findByUserIdOrderByOccurredOnDesc(userId);
        List<Transaction> filtered = new ArrayList<>();
        String query = (search != null) ? search.trim().toLowerCase() : "";

        // Plain for loop to filter transactions
        for (Transaction t : all) {
            // Filter by type (income or expense)
            if (type != null && t.getType() != type) {
                continue;
            }
            // Filter by category
            if (category != null && !category.isBlank() && !t.getCategory().equalsIgnoreCase(category)) {
                continue;
            }
            // Filter by date range
            if (fromDate != null && t.getOccurredOn().isBefore(fromDate)) {
                continue;
            }
            if (toDate != null && t.getOccurredOn().isAfter(toDate)) {
                continue;
            }
            // Filter by search text
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

    // ------------------------------------------------------------------------
    // C. BUDGET & SAVINGS GOAL
    // ------------------------------------------------------------------------

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

    // ------------------------------------------------------------------------
    // D. DASHBOARD CALCULATIONS (EASY MATH & STEP-BY-STEP)
    // ------------------------------------------------------------------------

    public Map<String, Object> getDashboardStats(Long userId, String targetMonthStr) {
        YearMonth targetMonth = parseMonth(targetMonthStr);
        String monthKey = targetMonth.toString();
        String monthLabel = targetMonth.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH));

        List<Transaction> all = txRepo.findByUserIdOrderByOccurredOnDesc(userId);

        // Step 1: Calculate income & expense for THIS selected month
        double monthIncome = 0;
        double monthExpense = 0;
        List<Transaction> monthTransactions = new ArrayList<>();

        for (Transaction t : all) {
            if (YearMonth.from(t.getOccurredOn()).equals(targetMonth)) {
                monthTransactions.add(t);
                if (t.isIncome()) {
                    monthIncome += t.getAmount();
                } else if (t.isExpense()) {
                    monthExpense += t.getAmount();
                }
            }
        }

        double remainingBalance = monthIncome - monthExpense;
        double savingsThisMonth = Math.max(remainingBalance, 0);

        // Step 2: Calculate all-time cumulative savings across all months
        double allIncome = 0;
        double allExpense = 0;
        for (Transaction t : all) {
            if (t.isIncome()) allIncome += t.getAmount();
            if (t.isExpense()) allExpense += t.getAmount();
        }
        double totalSavingsAllTime = Math.max(allIncome - allExpense, 0);

        // Step 3: Check monthly budget and 80% spending alert
        Budget budget = getBudget(userId, monthKey);
        double monthlyBudget = budget.getAmount();
        double budgetRemaining = Math.max(monthlyBudget - monthExpense, 0);
        double budgetUsedPercent = monthlyBudget > 0 ? (monthExpense / monthlyBudget) * 100 : 0;

        // Triggers alert if 80% or more of the budget is spent
        boolean budgetAlert = monthlyBudget > 0 && budgetUsedPercent >= 80.0;
        String budgetAlertMessage = budgetAlert
                ? String.format("You have used %.0f%% of your monthly budget. ₹%.0f remaining.", budgetUsedPercent, budgetRemaining)
                : "";

        // Step 4: Savings goal progress percentage
        SavingsGoal goal = getSavingsGoal(userId);
        double goalPercent = goal.getTargetAmount() > 0
                ? Math.min(100.0, (totalSavingsAllTime / goal.getTargetAmount()) * 100) : 0;

        // Step 5: Category breakdown (Food, Travel, Bills, etc.)
        List<Map<String, Object>> categoryBreakdown = computeCategories(monthTransactions, monthExpense);

        // Step 6: 6-Month Income vs Expense trend for chart
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

        // Put all calculated data into a response map
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

    // ------------------------------------------------------------------------
    // E. MONTHLY REPORT (12-MONTH SPENDING TREND)
    // ------------------------------------------------------------------------

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

        // 12-month spending trend for line chart
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
        Collections.reverse(availableMonths);

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

    // Helper: Calculate spending per category and percentage
    private List<Map<String, Object>> computeCategories(List<Transaction> transactions, double totalExpense) {
        Map<String, Double> map = new HashMap<>();
        for (Transaction t : transactions) {
            if (t.isExpense()) {
                map.put(t.getCategory(), map.getOrDefault(t.getCategory(), 0.0) + t.getAmount());
            }
        }

        List<Map.Entry<String, Double>> list = new ArrayList<>(map.entrySet());
        list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

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

// ============================================================================
// SECTION 5: REST API CONTROLLER (TALKS TO FRONTEND)
// ============================================================================

@RestController
@RequestMapping("/api")
class ApiController {

    private final BudgetPlannerService service;

    public ApiController(BudgetPlannerService service) {
        this.service = service;
    }

    // 1. AUTHENTICATION ENDPOINTS
    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body, HttpSession session) {
        String name = body.get("fullName");
        String email = body.get("email");
        String password = body.get("password");

        if (name == null || email == null || password == null || password.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Name, email and password (min 6 chars) are required."));
        }

        Map<String, Object> user = service.register(name, email, password);
        session.setAttribute(AppConfig.SESSION_USER_KEY, user.get("id"));
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpSession session) {
        String email = body.get("email");
        String password = body.get("password");

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required."));
        }

        User user = service.authenticate(email, password);
        session.setAttribute(AppConfig.SESSION_USER_KEY, user.getId());
        return ResponseEntity.ok(Map.of("id", user.getId(), "email", user.getEmail(), "fullName", user.getFullName()));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/auth/me")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute(AppConfig.SESSION_USER_KEY);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("authenticated", false));
        }
        return ResponseEntity.ok(service.getUserById(userId));
    }

    // 2. TRANSACTION ENDPOINTS
    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String search,
            HttpSession session) {
        Long userId = (Long) session.getAttribute(AppConfig.SESSION_USER_KEY);
        return ResponseEntity.ok(service.getTransactions(userId, type, category, from, to, search));
    }

    @PostMapping("/transactions")
    public ResponseEntity<?> createTransaction(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = (Long) session.getAttribute(AppConfig.SESSION_USER_KEY);
        TransactionType type = TransactionType.valueOf((String) body.get("type"));
        String category = (String) body.get("category");
        double amount = Double.parseDouble(body.get("amount").toString());
        LocalDate date = LocalDate.parse((String) body.get("occurredOn"));
        String desc = (String) body.get("description");

        Transaction tx = service.createTransaction(userId, type, category, amount, date, desc);
        return ResponseEntity.status(HttpStatus.CREATED).body(tx);
    }

    @PutMapping("/transactions/{id}")
    public ResponseEntity<?> updateTransaction(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = (Long) session.getAttribute(AppConfig.SESSION_USER_KEY);
        TransactionType type = TransactionType.valueOf((String) body.get("type"));
        String category = (String) body.get("category");
        double amount = Double.parseDouble(body.get("amount").toString());
        LocalDate date = LocalDate.parse((String) body.get("occurredOn"));
        String desc = (String) body.get("description");

        Transaction tx = service.updateTransaction(userId, id, type, category, amount, date, desc);
        return ResponseEntity.ok(tx);
    }

    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<?> deleteTransaction(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute(AppConfig.SESSION_USER_KEY);
        service.deleteTransaction(userId, id);
        return ResponseEntity.ok(Map.of("message", "Transaction deleted successfully"));
    }

    // 3. BUDGET & SAVINGS GOAL ENDPOINTS
    @GetMapping("/budgets/current")
    public ResponseEntity<?> getCurrentBudget(@RequestParam(required = false) String month, HttpSession session) {
        Long userId = (Long) session.getAttribute(AppConfig.SESSION_USER_KEY);
        String target = (month != null && !month.isBlank()) ? month.trim() : YearMonth.now().toString();
        Budget budget = service.getBudget(userId, target);
        return ResponseEntity.ok(Map.of("month", target, "amount", budget.getAmount()));
    }

    @PostMapping("/budgets")
    public ResponseEntity<?> setBudget(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = (Long) session.getAttribute(AppConfig.SESSION_USER_KEY);
        String month = (String) body.get("month");
        double amount = Double.parseDouble(body.get("amount").toString());
        Budget saved = service.setBudget(userId, month, amount);
        return ResponseEntity.ok(Map.of("month", saved.getMonth(), "amount", saved.getAmount()));
    }

    @GetMapping("/savings-goals")
    public ResponseEntity<?> getSavingsGoal(HttpSession session) {
        Long userId = (Long) session.getAttribute(AppConfig.SESSION_USER_KEY);
        SavingsGoal goal = service.getSavingsGoal(userId);
        return ResponseEntity.ok(Map.of("title", goal.getTitle(), "targetAmount", goal.getTargetAmount()));
    }

    @PostMapping("/savings-goals")
    public ResponseEntity<?> setSavingsGoal(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = (Long) session.getAttribute(AppConfig.SESSION_USER_KEY);
        String title = (String) body.get("title");
        double amount = Double.parseDouble(body.get("targetAmount").toString());
        SavingsGoal saved = service.setSavingsGoal(userId, title, amount);
        return ResponseEntity.ok(Map.of("title", saved.getTitle(), "targetAmount", saved.getTargetAmount()));
    }

    // 4. DASHBOARD & REPORT ANALYTICS
    @GetMapping("/dashboard/stats")
    public ResponseEntity<?> getDashboardStats(@RequestParam(required = false) String month, HttpSession session) {
        Long userId = (Long) session.getAttribute(AppConfig.SESSION_USER_KEY);
        return ResponseEntity.ok(service.getDashboardStats(userId, month));
    }

    @GetMapping("/reports")
    public ResponseEntity<?> getReport(@RequestParam(required = false) String month, HttpSession session) {
        Long userId = (Long) session.getAttribute(AppConfig.SESSION_USER_KEY);
        return ResponseEntity.ok(service.getMonthlyReport(userId, month));
    }

    // 5. ERROR HANDLING
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadInput(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorized(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", ex.getMessage() != null ? ex.getMessage() : "Unexpected error"));
    }
}

// ============================================================================
// SECTION 6: CONFIGURATION & SAMPLE DEMO DATA
// ============================================================================

@Configuration
class AppConfig implements WebMvcConfigurer {

    public static final String SESSION_USER_KEY = "USER_ID";
    private final AuthInterceptor authInterceptor;

    public AppConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Protect all /api/* routes except login/register
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve HTML, CSS, JS frontend directly from static folder
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
}

/**
 * Checks if the user is logged in before accessing private API endpoints.
 */
@Component
class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth") || !path.startsWith("/api/") || path.startsWith("/h2-console")) {
            return true;
        }
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(AppConfig.SESSION_USER_KEY) != null) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Please sign in to continue.\"}");
        return false;
    }
}

/**
 * Automatically creates a sample Demo Account (Aarav Kumar) on first run.
 */
@Component
class DemoDataLoader implements CommandLineRunner {

    private final UserRepository userRepo;
    private final TransactionRepository txRepo;
    private final BudgetRepository budgetRepo;
    private final SavingsGoalRepository goalRepo;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DemoDataLoader(UserRepository userRepo, TransactionRepository txRepo,
                          BudgetRepository budgetRepo, SavingsGoalRepository goalRepo) {
        this.userRepo = userRepo;
        this.txRepo = txRepo;
        this.budgetRepo = budgetRepo;
        this.goalRepo = goalRepo;
    }

    @Override
    public void run(String... args) {
        // If data already exists, do not recreate
        if (userRepo.count() > 0) return;

        // 1. Create Demo User
        User demo = userRepo.save(new User("demo@rupeeflow.com", passwordEncoder.encode("password123"), "Aarav Kumar"));
        Long userId = demo.getId();

        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.from(today);
        String monthKey = thisMonth.toString();

        // 2. Set Monthly Budget (₹35,000) and Savings Goal (₹100,000)
        budgetRepo.save(new Budget(userId, monthKey, 35000.0));
        goalRepo.save(new SavingsGoal(userId, "Emergency Fund & Laptop", 100000.0));

        // 3. Add Sample Transactions for This Month
        txRepo.save(new Transaction(userId, TransactionType.INCOME, "Salary", 75000.0, thisMonth.atDay(1), "Tech Corp Monthly Salary"));
        txRepo.save(new Transaction(userId, TransactionType.INCOME, "Freelancing", 15000.0, thisMonth.atDay(5), "Web Development Project"));
        txRepo.save(new Transaction(userId, TransactionType.EXPENSE, "Food", 6800.0, thisMonth.atDay(3), "Groceries & Supermarket"));
        txRepo.save(new Transaction(userId, TransactionType.EXPENSE, "Bills", 4500.0, thisMonth.atDay(4), "Electricity & High-speed Wifi"));
        txRepo.save(new Transaction(userId, TransactionType.EXPENSE, "Shopping", 8200.0, thisMonth.atDay(8), "Clothing & Home Essentials"));
        txRepo.save(new Transaction(userId, TransactionType.EXPENSE, "Travel", 3400.0, thisMonth.atDay(11), "Fuel & Metro Card"));
        txRepo.save(new Transaction(userId, TransactionType.EXPENSE, "Entertainment", 2500.0, thisMonth.atDay(14), "Movies & Dinner with Friends"));
        txRepo.save(new Transaction(userId, TransactionType.EXPENSE, "Food", 3200.0, thisMonth.atDay(18), "Weekend Cafe & Dining"));
        txRepo.save(new Transaction(userId, TransactionType.EXPENSE, "Education", 2900.0, thisMonth.atDay(20), "Online Courses & Books"));

        // 4. Add Historical Transactions (Previous 5 Months for 6-Month charts)
        for (int i = 1; i <= 5; i++) {
            YearMonth pastMonth = thisMonth.minusMonths(i);
            txRepo.save(new Transaction(userId, TransactionType.INCOME, "Salary", 72000.0, pastMonth.atDay(1), "Monthly Salary"));
            txRepo.save(new Transaction(userId, TransactionType.EXPENSE, "Food", 7500.0, pastMonth.atDay(4), "Groceries"));
            txRepo.save(new Transaction(userId, TransactionType.EXPENSE, "Bills", 4200.0, pastMonth.atDay(7), "Utilities"));
            txRepo.save(new Transaction(userId, TransactionType.EXPENSE, "Travel", 3100.0, pastMonth.atDay(12), "Commute"));
            txRepo.save(new Transaction(userId, TransactionType.EXPENSE, "Shopping", 6000.0, pastMonth.atDay(16), "Shopping"));
            txRepo.save(new Transaction(userId, TransactionType.EXPENSE, "Entertainment", 2800.0, pastMonth.atDay(22), "Outings"));
        }
    }
}
