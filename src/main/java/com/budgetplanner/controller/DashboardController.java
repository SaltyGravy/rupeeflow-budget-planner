package com.budgetplanner.controller;

import com.budgetplanner.config.AuthInterceptor;
import com.budgetplanner.dto.DashboardStatsResponse;
import com.budgetplanner.service.StatsService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final StatsService statsService;

    public DashboardController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getStats(
            @RequestParam(required = false) String month,
            HttpSession session) {

        Long userId = (Long) session.getAttribute(AuthInterceptor.SESSION_USER_KEY);
        DashboardStatsResponse stats = statsService.getDashboardStats(userId, month);
        return ResponseEntity.ok(stats);
    }
}
