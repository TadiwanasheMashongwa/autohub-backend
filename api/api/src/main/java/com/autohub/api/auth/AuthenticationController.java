package com.autohub.api.auth;

import com.autohub.api.model.User;
import com.autohub.api.repository.UserRepository;
import com.autohub.api.service.MfaService;
import com.autohub.api.service.RateLimitService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationService service;
    private final RateLimitService rateLimitService;
    private final MfaService mfaService;
    private final UserRepository userRepository;

    public AuthenticationController(
            AuthenticationService service,
            RateLimitService rateLimitService,
            MfaService mfaService,
            UserRepository userRepository) {
        this.service = service;
        this.rateLimitService = rateLimitService;
        this.mfaService = mfaService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        // Log for debugging (remove in production)
        System.out.println("Registering user: " + request.getUsername());
        return ResponseEntity.ok(service.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticate(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        Bucket bucket = rateLimitService.resolveBucket(clientIp);

        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Too many login attempts. Please try again in a minute.");
        }

        // FIXED: Using username from the DTO to match Registration/Login consistency
        String loginId = request.getUsername();
        if (loginId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username/Email is required");
        }

        User user = userRepository.findByUsername(loginId)
                .orElse(null);

        if (user == null || !service.isValidCredentials(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }

        if (user.isMfaEnabled()) {
            return ResponseEntity.ok(Map.of(
                    "mfaRequired", true,
                    "username", user.getUsername(),
                    "message", "Multi-Factor Authentication Required"
            ));
        }

        return ResponseEntity.ok(service.authenticate(request));
    }

    @PostMapping("/verify-mfa")
    public ResponseEntity<?> verifyMfa(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String code = request.get("code");

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isCodeValid = mfaService.verifyCode(user.getMfaSecret(), code);

        if (isCodeValid) {
            return ResponseEntity.ok(service.generateTokenForUser(user));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid MFA code");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            service.blacklistToken(token);
            return ResponseEntity.ok("Successfully logged out.");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid logout request.");
    }
}