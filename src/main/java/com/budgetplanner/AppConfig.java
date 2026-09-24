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

import java.math.BigDecimal;
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
        response.getWriter().write("{\"error\":\"Unauthorized. Please sign in.\"}");
        return false;
    }
}

@Component
class DemoDataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DemoDataLoader(UserRepository userRepository,
                          TransactionRepository transactionRepository,
                          BudgetRepository budgetRepository,
                          SavingsGoalRepository savingsGoalRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.savingsGoalRepository = savingsGoalRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) return;

        User demoUser = userRepository.save(new User("demo@rupeeflow.com", passwordEncoder.encode("password123"), "Aarav Kumar"));
        Long userId = demoUser.getId();

        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.from(today);
        String currentMonthKey = thisMonth.toString();

        budgetRepository.save(new Budget(userId, currentMonthKey, new BigDecimal("35000.00")));
        savingsGoalRepository.save(new SavingsGoal(userId, "Emergency Fund & Laptop", new BigDecimal("100000.00")));

        transactionRepository.save(new Transaction(userId, TransactionType.INCOME, "Salary", new BigDecimal("75000.00"), thisMonth.atDay(1), "Tech Corp Monthly Salary"));
        transactionRepository.save(new Transaction(userId, TransactionType.INCOME, "Freelancing", new BigDecimal("15000.00"), thisMonth.atDay(5), "Web Development Project"));
        transactionRepository.save(new Transaction(userId, TransactionType.EXPENSE, "Food", new BigDecimal("6800.00"), thisMonth.atDay(3), "Groceries & Supermarket"));
        transactionRepository.save(new Transaction(userId, TransactionType.EXPENSE, "Bills", new BigDecimal("4500.00"), thisMonth.atDay(4), "Electricity & High-speed Wifi"));
        transactionRepository.save(new Transaction(userId, TransactionType.EXPENSE, "Shopping", new BigDecimal("8200.00"), thisMonth.atDay(8), "Clothing & Home Essentials"));
        transactionRepository.save(new Transaction(userId, TransactionType.EXPENSE, "Travel", new BigDecimal("3400.00"), thisMonth.atDay(11), "Fuel & Metro Card"));
        transactionRepository.save(new Transaction(userId, TransactionType.EXPENSE, "Entertainment", new BigDecimal("2500.00"), thisMonth.atDay(14), "Movies & Dinner with Friends"));
        transactionRepository.save(new Transaction(userId, TransactionType.EXPENSE, "Food", new BigDecimal("3200.00"), thisMonth.atDay(18), "Weekend Cafe & Dining"));
        transactionRepository.save(new Transaction(userId, TransactionType.EXPENSE, "Education", new BigDecimal("2900.00"), thisMonth.atDay(20), "Online Courses & Books"));

        for (int i = 1; i <= 5; i++) {
            YearMonth prev = thisMonth.minusMonths(i);
            transactionRepository.save(new Transaction(userId, TransactionType.INCOME, "Salary", new BigDecimal("72000.00"), prev.atDay(1), "Monthly Salary"));
            transactionRepository.save(new Transaction(userId, TransactionType.EXPENSE, "Food", new BigDecimal("7500.00"), prev.atDay(4), "Groceries"));
            transactionRepository.save(new Transaction(userId, TransactionType.EXPENSE, "Bills", new BigDecimal("4200.00"), prev.atDay(7), "Utilities"));
            transactionRepository.save(new Transaction(userId, TransactionType.EXPENSE, "Travel", new BigDecimal("3100.00"), prev.atDay(12), "Commute"));
            transactionRepository.save(new Transaction(userId, TransactionType.EXPENSE, "Shopping", new BigDecimal("6000.00"), prev.atDay(16), "Shopping"));
            transactionRepository.save(new Transaction(userId, TransactionType.EXPENSE, "Entertainment", new BigDecimal("2800.00"), prev.atDay(22), "Outings"));
            budgetRepository.save(new Budget(userId, prev.toString(), new BigDecimal("32000.00")));
        }
    }
}
