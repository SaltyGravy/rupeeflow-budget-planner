package com.budgetplanner.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String SESSION_USER_KEY = "USER_ID";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Allow CORS preflight requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();

        // Allow static resources, favicon, and auth endpoints
        if (path.startsWith("/api/auth") || !path.startsWith("/api/")) {
            return true;
        }

        // H2 console
        if (path.startsWith("/h2-console")) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(SESSION_USER_KEY) != null) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Unauthorized. Please sign in.\"}");
        return false;
    }
}
