package com.budgetplanner;

import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Single, clean REST Controller routing frontend requests to the service layer.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private final BudgetPlannerService service;

    public ApiController(BudgetPlannerService service) {
        this.service = service;
    }

    // ===================================================================
    // 1. AUTHENTICATION ENDPOINTS
    // ===================================================================

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

    // ===================================================================
    // 2. TRANSACTION ENDPOINTS (CRUD & FILTERS)
    // ===================================================================

    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String search,
            HttpSession session) {
        Long userId = (Long) session.getAttribute(AppConfig.SESSION_USER_KEY);
        List<Transaction> list = service.getTransactions(userId, type, category, from, to, search);
        return ResponseEntity.ok(list);
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

    // ===================================================================
    // 3. BUDGET & SAVINGS GOAL ENDPOINTS
    // ===================================================================

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

    // ===================================================================
    // 4. DASHBOARD & REPORT ANALYTICS
    // ===================================================================

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

    // ===================================================================
    // 5. ERROR HANDLING
    // ===================================================================

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
