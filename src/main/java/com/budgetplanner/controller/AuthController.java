package com.budgetplanner.controller;

import com.budgetplanner.config.AuthInterceptor;
import com.budgetplanner.dto.AuthRequest;
import com.budgetplanner.dto.RegisterRequest;
import com.budgetplanner.dto.UserResponse;
import com.budgetplanner.model.User;
import com.budgetplanner.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request, HttpSession session) {
        UserResponse response = userService.register(request);
        session.setAttribute(AuthInterceptor.SESSION_USER_KEY, response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody AuthRequest request, HttpSession session) {
        User user = userService.authenticate(request);
        session.setAttribute(AuthInterceptor.SESSION_USER_KEY, user.getId());
        return ResponseEntity.ok(new UserResponse(user.getId(), user.getEmail(), user.getFullName()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute(AuthInterceptor.SESSION_USER_KEY);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("authenticated", false));
        }
        UserResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }
}
