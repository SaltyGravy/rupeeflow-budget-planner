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

        // Step 1: Filter transactions for THIS month
        List<Transaction> monthTransactions = new ArrayList<>();
        for (Transaction t : all) {
            if (YearMonth.from(t.getOccurredOn()).equals(targetMonth)) {
                monthTransactions.add(t);
            }
        }

        // Calculations delegated to BudgetCalculator (simple, beginner-friendly methods)
        double monthIncome = BudgetCalculator.calculateTotalIncome(monthTransactions);
        double monthExpense = BudgetCalculator.calculateTotalExpense(monthTransactions);
        double remainingBalance = BudgetCalculator.calculateBalance(monthIncome, monthExpense);
        double savingsThisMonth = BudgetCalculator.calculateSavings(monthIncome, monthExpense);

        // Step 2: All-time cumulative savings
        double allIncome = BudgetCalculator.calculateTotalIncome(all);
        double allExpense = BudgetCalculator.calculateTotalExpense(all);
        double totalSavingsAllTime = BudgetCalculator.calculateSavings(allIncome, allExpense);

        // Step 3: Monthly budget & 80% spending alert
        Budget budget = getBudget(userId, monthKey);
        double monthlyBudget = budget.getAmount();
        double budgetRemaining = BudgetCalculator.calculateBudgetRemaining(monthlyBudget, monthExpense);
        double budgetUsedPercent = BudgetCalculator.calculateBudgetUsedPercentage(monthlyBudget, monthExpense);
        boolean budgetAlert = BudgetCalculator.isBudgetAlert(monthlyBudget, monthExpense);
        String budgetAlertMessage = BudgetCalculator.getBudgetAlertMessage(monthlyBudget, monthExpense);

        // Step 4: Savings goal progress percentage
        SavingsGoal goal = getSavingsGoal(userId);
        double goalPercent = BudgetCalculator.calculateSavingsGoalPercentage(totalSavingsAllTime, goal.getTargetAmount());

        // Step 5: Category breakdown (Food, Shopping, Bills, etc.)
        List<Map<String, Object>> categoryBreakdown = BudgetCalculator.calculateCategoryBreakdown(monthTransactions, monthExpense);

        // Step 6: 6-Month Income vs Expense trend for chart
        List<Map<String, Object>> sixMonthTrend = BudgetCalculator.calculateMonthlyTrend(all, targetMonth, 6);

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

        List<Transaction> monthTransactions = new ArrayList<>();
        for (Transaction t : all) {
            if (YearMonth.from(t.getOccurredOn()).equals(targetMonth)) {
                monthTransactions.add(t);
            }
        }

        double totalIncome = BudgetCalculator.calculateTotalIncome(monthTransactions);
        double totalExpense = BudgetCalculator.calculateTotalExpense(monthTransactions);
        double balance = BudgetCalculator.calculateBalance(totalIncome, totalExpense);
        double savings = BudgetCalculator.calculateSavings(totalIncome, totalExpense);

        List<Map<String, Object>> categories = BudgetCalculator.calculateCategoryBreakdown(monthTransactions, totalExpense);
        String highestCategory = categories.isEmpty() ? "—" : (String) categories.get(0).get("category");
        double highestAmount = categories.isEmpty() ? 0 : (double) categories.get(0).get("amount");

        Budget budget = getBudget(userId, monthKey);
        double budgetAmount = budget.getAmount();
        double budgetRemaining = BudgetCalculator.calculateBudgetRemaining(budgetAmount, totalExpense);

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
 * Automatically creates the sample Demo Account (Aarav Kumar) with facade data.
 *
 * All the numbers below are made-up placeholder values used only to make the
 * dashboard, charts and reports look realistic when someone tries the demo.
 *
 * Two rules keep the demo believable:
 *   1. Nothing is ever dated in the future (you cannot spend money tomorrow).
 *   2. Every month has slightly different values, so the 6-month chart is not flat.
 */
@Component
class DemoDataLoader implements CommandLineRunner {

    // The demo login shown on the sign-in screen
    private static final String DEMO_EMAIL = "demo@rupeeflow.com";
    private static final String DEMO_PASSWORD = "password123";
    private static final String DEMO_NAME = "Aarav Kumar";

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
        // Create the demo account only if it is missing.
        // Accounts registered by real people are never touched.
        if (userRepo.existsByEmail(DEMO_EMAIL)) return;

        User demo = userRepo.save(new User(DEMO_EMAIL, passwordEncoder.encode(DEMO_PASSWORD), DEMO_NAME));
        Long userId = demo.getId();

        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.from(today);

        // ====================================================================
        // 1. SAVINGS GOAL
        // ====================================================================
        // Target is kept well above the demo's saved-up amount so the progress
        // bar shows real partial progress (~65%) instead of sitting at 100%.
        goalRepo.save(new SavingsGoal(userId, "Emergency Fund & MacBook", 500000.0));

        // ====================================================================
        // 2. FIVE PAST MONTHS (gives the 6-month chart a varied history)
        // ====================================================================
        // Each row is one month. Column order:
        // salary, extraIncome, food, bills, travel, shopping, fun, education, misc, budget
        double[][] pastMonths = {
            { 72000, 2000,  7200, 4100, 2800, 5400, 2100, 1800,  900, 38000 }, // 5 months ago - steady
            { 72000, 8000,  8100, 4600, 3300, 7200, 3100, 2400,    0, 38000 }, // 4 months ago - freelance project
            { 75000,    0,  6900, 5200, 2600, 4100, 1900,    0, 1200, 40000 }, // 3 months ago - quiet month
            { 75000, 15500, 7800, 4300, 4100, 9600, 3600, 2000,    0, 35000 }, // 2 months ago - festival overspend (fires the 80% alert)
            { 78000, 1500,  7400, 4400, 2900, 5100, 2300, 2600, 1100, 42000 }  // last month    - back to normal
        };

        // Where the extra income in each month came from
        String[] extraIncomeCategory = { "Pocket money", "Freelancing", "", "Freelancing", "Pocket money" };
        String[] extraIncomeNote = {
            "Gift from family", "Freelance website project", "",
            "Festive freelance bonus", "Gift from family"
        };

        for (int i = 0; i < pastMonths.length; i++) {
            double[] row = pastMonths[i];

            // Row 0 is the oldest month, so it goes back the furthest
            int monthsAgo = pastMonths.length - i;
            YearMonth month = thisMonth.minusMonths(monthsAgo);

            // Monthly budget the user had set for that month
            budgetRepo.save(new Budget(userId, month.toString(), row[9]));

            // Income
            save(userId, TransactionType.INCOME, "Salary", row[0], month.atDay(1), "Monthly salary - Tech Corp");
            if (row[1] > 0) {
                save(userId, TransactionType.INCOME, extraIncomeCategory[i], row[1], month.atDay(6), extraIncomeNote[i]);
            }

            // Expenses (a zero amount simply means that category was unused that month)
            save(userId, TransactionType.EXPENSE, "Food",          row[2], month.atDay(4),  "Groceries & dining");
            save(userId, TransactionType.EXPENSE, "Bills",         row[3], month.atDay(7),  "Electricity, water & broadband");
            save(userId, TransactionType.EXPENSE, "Travel",        row[4], month.atDay(11), "Fuel, metro & cabs");
            save(userId, TransactionType.EXPENSE, "Shopping",      row[5], month.atDay(15), "Clothing & household");
            save(userId, TransactionType.EXPENSE, "Entertainment", row[6], month.atDay(19), "Movies & eating out");
            save(userId, TransactionType.EXPENSE, "Education",     row[7], month.atDay(23), "Courses & books");
            save(userId, TransactionType.EXPENSE, "Other",         row[8], month.atDay(26), "Gifts & miscellaneous");
        }

        // ====================================================================
        // 3. CURRENT MONTH (day by day, stopping at today)
        // ====================================================================
        budgetRepo.save(new Budget(userId, thisMonth.toString(), 42000.0));

        // Income for this month
        saveIfNotFuture(userId, today, TransactionType.INCOME, "Salary", 78000, 1, "Monthly salary - Tech Corp");
        saveIfNotFuture(userId, today, TransactionType.INCOME, "Freelancing", 14500, 6, "Freelance project milestone");
        saveIfNotFuture(userId, today, TransactionType.INCOME, "Other income", 2400, 17, "Cashback & interest credited");

        // Expenses for this month, spread across the days of the month.
        // Bigger fixed costs sit on the first few days, so the dashboard still
        // looks active even when today is early in the month.
        saveIfNotFuture(userId, today, TransactionType.EXPENSE, "Bills",         4450, 1,  "Electricity & broadband bill");
        saveIfNotFuture(userId, today, TransactionType.EXPENSE, "Food",          2850, 2,  "Monthly grocery run");
        saveIfNotFuture(userId, today, TransactionType.EXPENSE, "Travel",         780, 3,  "Metro card recharge");
        saveIfNotFuture(userId, today, TransactionType.EXPENSE, "Food",          1240, 5,  "Weekend dinner out");
        saveIfNotFuture(userId, today, TransactionType.EXPENSE, "Shopping",      3300, 6,  "Running shoes");
        saveIfNotFuture(userId, today, TransactionType.EXPENSE, "Entertainment",  920, 8,  "Movie tickets");
        saveIfNotFuture(userId, today, TransactionType.EXPENSE, "Food",          1680, 10, "Groceries top-up");
        saveIfNotFuture(userId, today, TransactionType.EXPENSE, "Travel",        1450, 12, "Cab rides & fuel");
        saveIfNotFuture(userId, today, TransactionType.EXPENSE, "Education",     2200, 14, "Online course subscription");
        saveIfNotFuture(userId, today, TransactionType.EXPENSE, "Shopping",      2750, 16, "Home essentials");
        saveIfNotFuture(userId, today, TransactionType.EXPENSE, "Food",          1520, 18, "Cafe & takeaway");
        saveIfNotFuture(userId, today, TransactionType.EXPENSE, "Entertainment", 1350, 20, "Concert tickets");
        saveIfNotFuture(userId, today, TransactionType.EXPENSE, "Bills",         1180, 22, "Mobile & OTT subscriptions");
        saveIfNotFuture(userId, today, TransactionType.EXPENSE, "Travel",         860, 24, "Auto & metro");
        saveIfNotFuture(userId, today, TransactionType.EXPENSE, "Food",          1960, 26, "Groceries");
        saveIfNotFuture(userId, today, TransactionType.EXPENSE, "Other",         1100, 28, "Gifts & miscellaneous");
    }

    /**
     * Saves one transaction. Amounts of zero are skipped so an unused
     * category does not show up as a ₹0 row.
     */
    private void save(Long userId, TransactionType type, String category,
                      double amount, LocalDate date, String description) {
        if (amount <= 0) return;
        txRepo.save(new Transaction(userId, type, category, amount, date, description));
    }

    /**
     * Saves a transaction for the given day of THIS month, but only if that day
     * has already happened. This keeps the demo free of future-dated money.
     */
    private void saveIfNotFuture(Long userId, LocalDate today, TransactionType type, String category,
                                 double amount, int dayOfMonth, String description) {
        // Skip days that have not arrived yet
        if (dayOfMonth > today.getDayOfMonth()) return;

        LocalDate date = YearMonth.from(today).atDay(dayOfMonth);
        save(userId, type, category, amount, date, description);
    }
}
