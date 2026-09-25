package com.budgetplanner;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.time.YearMonth;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    public static final String SESSION_USER_KEY = "USER_ID";
    private final AuthInterceptor authInterceptor;

    public AppConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
}

/**
 * Checks if the user is signed in before allowing access to /api/*
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
 * Pre-populates a realistic Demo Account (Aarav Kumar) on first startup
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
        if (userRepo.count() > 0) return; // already initialized

        // 1. Create Demo User
        User demo = userRepo.save(new User("demo@rupeeflow.com", passwordEncoder.encode("password123"), "Aarav Kumar"));
        Long userId = demo.getId();

        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.from(today);
        String monthKey = thisMonth.toString();

        // 2. Set Monthly Budget (₹35,000) and Savings Goal (₹100,000)
        budgetRepo.save(new Budget(userId, monthKey, 35000.0));
        goalRepo.save(new SavingsGoal(userId, "Emergency Fund & Laptop", 100000.0));

        // 3. Add Sample Transactions for this month
        txRepo.save(new Transaction(userId, TransactionType.INCOME, "Salary", 75000.0, thisMonth.atDay(1), "Tech Corp Monthly Salary"));
        txRepo.save(new Transaction(userId, TransactionType.INCOME, "Freelancing", 15000.0, thisMonth.atDay(5), "Web Development Project"));
        txRepo.save(new Transaction(userId, TransactionType.EXPENSE, "Food", 6800.0, thisMonth.atDay(3), "Groceries & Supermarket"));
        txRepo.save(new Transaction(userId, TransactionType.EXPENSE, "Bills", 4500.0, thisMonth.atDay(4), "Electricity & High-speed Wifi"));
        txRepo.save(new Transaction(userId, TransactionType.EXPENSE, "Shopping", 8200.0, thisMonth.atDay(8), "Clothing & Home Essentials"));
        txRepo.save(new Transaction(userId, TransactionType.EXPENSE, "Travel", 3400.0, thisMonth.atDay(11), "Fuel & Metro Card"));
        txRepo.save(new Transaction(userId, TransactionType.EXPENSE, "Entertainment", 2500.0, thisMonth.atDay(14), "Movies & Dinner with Friends"));
        txRepo.save(new Transaction(userId, TransactionType.EXPENSE, "Food", 3200.0, thisMonth.atDay(18), "Weekend Cafe & Dining"));
        txRepo.save(new Transaction(userId, TransactionType.EXPENSE, "Education", 2900.0, thisMonth.atDay(20), "Online Courses & Books"));

        // 4. Add Historical Transactions for the past 5 months
        for (int i = 1; i <= 5; i++) {
            YearMonth prev = thisMonth.minusMonths(i);
            txRepo.save(new Transaction(userId, TransactionType.INCOME, "Salary", 72000.0, prev.atDay(1), "Monthly Salary"));
            txRepo.save(new Transaction(userId, TransactionType.EXPENSE, "Food", 7500.0, prev.atDay(4), "Groceries"));
            txRepo.save(new Transaction(userId, TransactionType.EXPENSE, "Bills", 4200.0, prev.atDay(7), "Utilities"));
            txRepo.save(new Transaction(userId, TransactionType.EXPENSE, "Travel", 3100.0, prev.atDay(12), "Commute"));
            txRepo.save(new Transaction(userId, TransactionType.EXPENSE, "Shopping", 6000.0, prev.atDay(16), "Shopping"));
            txRepo.save(new Transaction(userId, TransactionType.EXPENSE, "Entertainment", 2800.0, prev.atDay(22), "Outings"));
            budgetRepo.save(new Budget(userId, prev.toString(), 32000.0));
        }
    }
}
