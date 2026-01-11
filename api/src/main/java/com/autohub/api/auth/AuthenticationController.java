package com.autohub.api.auth;

import com.autohub.api.auth.AuthenticationResponse;
import com.autohub.api.auth.AuthenticationService;
import com.autohub.api.auth.RegisterRequest;
import com.autohub.api.model.User;
import com.autohub.api.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationService service;
    private final UserRepository userRepository;

    public AuthenticationController(AuthenticationService service, UserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            return ResponseEntity.ok(service.register(request));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", "Registration failed: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticate(@RequestBody RegisterRequest request) {
        // Authenticate by Email
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null || !service.isValidCredentials(request)) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
        return ResponseEntity.ok(service.authenticate(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        try {
            return ResponseEntity.ok(service.refreshToken(refreshToken));
        } catch (Exception e) {
            return ResponseEntity.status(403).body("Session expired. Please log in again.");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        service.logout(email);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        try {
            service.initiatePasswordReset(email);
            return ResponseEntity.ok(Map.of("message", "Reset link sent. Check your inbox."));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("message", "If an account exists, a reset link has been sent."));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");
        try {
            service.completePasswordReset(token, newPassword);
            return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
}