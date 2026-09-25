package com.budgetplanner;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

enum TransactionType {
    INCOME,
    EXPENSE
}

/**
 * User Account Entity
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
 * Income / Expense Transaction Entity
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
    private String category;      // Food, Travel, Salary, etc.

    @Column(nullable = false)
    private double amount;        // Transaction amount in Rupees

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

    public boolean isIncome() { return type == TransactionType.INCOME; }
    public boolean isExpense() { return type == TransactionType.EXPENSE; }
}

/**
 * Monthly Budget Limit Entity
 */
@Entity
@Table(name = "budgets")
class Budget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

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
 * Savings Goal Entity
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
    private double targetAmount; // Goal amount in Rupees

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
