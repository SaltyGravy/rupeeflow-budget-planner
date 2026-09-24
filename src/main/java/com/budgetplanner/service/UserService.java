package com.budgetplanner.service;

import com.budgetplanner.dto.AuthRequest;
import com.budgetplanner.dto.RegisterRequest;
import com.budgetplanner.dto.UserResponse;
import com.budgetplanner.model.User;
import com.budgetplanner.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String cleanEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(cleanEmail, hashedPassword, request.getFullName().trim());
        User saved = userRepository.save(user);

        return new UserResponse(saved.getId(), saved.getEmail(), saved.getFullName());
    }

    @Transactional(readOnly = true)
    public User authenticate(AuthRequest request) {
        String cleanEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        return user;
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName());
    }
}
