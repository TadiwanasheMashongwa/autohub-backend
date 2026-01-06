package com.autohub.api.controller;

import com.autohub.api.auth.AuthenticationResponse;
import com.autohub.api.auth.AuthenticationService;
import com.autohub.api.auth.RegisterRequest;
import com.autohub.api.model.User;
import com.autohub.api.repository.UserRepository;
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

    @PostMapping("/login")
    public ResponseEntity<?> authenticate(@RequestBody RegisterRequest request) {
        User user = userRepository.findByUsername(request.getUsername()).orElse(null);
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
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        service.logout(username);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}