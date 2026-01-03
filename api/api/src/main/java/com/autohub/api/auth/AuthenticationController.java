package com.autohub.api.auth;

import com.autohub.api.model.User;
import com.autohub.api.repository.UserRepository;
import com.autohub.api.auth.AuthenticationService;
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
        return ResponseEntity.ok(service.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticate(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        // 1. Rate Limiting Check
        String clientIp = httpRequest.getRemoteAddr();
        Bucket bucket = rateLimitService.resolveBucket(clientIp);

        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Too many login attempts. Please try again in a minute.");
        }

        // 2. Initial Password Check
        User user = userRepository.findByUsername(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // We assume service.verifyPassword checks the encoded password
        if (!service.isValidCredentials(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }

        // 3. MFA Challenge Check
        if (user.isMfaEnabled()) {
            return ResponseEntity.ok(Map.of(
                    "mfaRequired", true,
                    "username", user.getUsername(),
                    "message", "Multi-Factor Authentication Required"
            ));
        }

        // 4. Standard JWT Return if no MFA
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
            // Generate full JWT response after successful MFA
            return ResponseEntity.ok(service.generateTokenForUser(user));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid MFA code");
        }
    }

    @PostMapping("/setup-mfa")
    public ResponseEntity<?> setupMfa(@RequestParam String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        String secret = mfaService.generateSecret();
        user.setMfaSecret(secret);
        userRepository.save(user);

        String qrCodeUri = mfaService.generateQrCodeUri(secret, user.getUsername());
        return ResponseEntity.ok(Map.of(
                "qrCode", qrCodeUri,
                "secret", secret,
                "instructions", "Scan this QR code with Google Authenticator or Authy"
        ));
    }
}