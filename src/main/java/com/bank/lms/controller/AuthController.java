package com.bank.lms.controller;

import com.bank.lms.entity.User;
import com.bank.lms.repository.UserRepository;
import com.bank.lms.security.JwtUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        User user = userRepository.findByUsername(req.getUsername()).orElse(null);

        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole(), user.getCustomerId());

        return ResponseEntity.ok(Map.of(
                "token", token,
                "username", user.getUsername(),
                "role", user.getRole(),
                "customerId", user.getCustomerId() == null ? "" : user.getCustomerId(),
                "fullName", user.getFullName() == null ? "" : user.getFullName(),
                "email", user.getEmail() == null ? "" : user.getEmail(),
                "phone", user.getPhone() == null ? "" : user.getPhone(),
                "address", user.getAddress() == null ? "" : user.getAddress(),
                "assignedOfficerName", user.getAssignedOfficerName() == null ? "" : user.getAssignedOfficerName()
        ));
    }

    // Get current logged-in user profile
    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(user);
    }

    // List all registered customers for the Officer dropdown
    @GetMapping("/customers")
    @PreAuthorize("hasRole('OFFICER')")
    public ResponseEntity<List<User>> listCustomers() {
        return ResponseEntity.ok(userRepository.findByRole("ROLE_CUSTOMER"));
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
}