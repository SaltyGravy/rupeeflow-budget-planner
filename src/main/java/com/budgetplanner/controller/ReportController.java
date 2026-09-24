package com.budgetplanner.controller;

import com.budgetplanner.config.AuthInterceptor;
import com.budgetplanner.dto.MonthlyReportResponse;
import com.budgetplanner.service.StatsService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final StatsService statsService;

    public ReportController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    public ResponseEntity<MonthlyReportResponse> getReport(
            @RequestParam(required = false) String month,
            HttpSession session) {

        Long userId = (Long) session.getAttribute(AuthInterceptor.SESSION_USER_KEY);
        MonthlyReportResponse report = statsService.getMonthlyReport(userId, month);
        return ResponseEntity.ok(report);
    }
}
