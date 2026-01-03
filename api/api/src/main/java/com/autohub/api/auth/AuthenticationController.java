package com.autohub.api.auth;

import com.autohub.api.service.RateLimitService;
import io.github.bucket4j.Bucket;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    private final AuthenticationService service;
    private final RateLimitService rateLimitService; // 1. Inject

    public AuthenticationController(AuthenticationService service, RateLimitService rateLimitService) {
        this.service = service;
        this.rateLimitService = rateLimitService;
    }

    private final AuthenticationService service;

    public AuthenticationController(AuthenticationService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(service.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticate(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        Bucket bucket = rateLimitService.resolveBucket(clientIp);

        // 2. Check the bucket
        if (bucket.tryConsume(1)) {
            return ResponseEntity.ok(service.authenticate(request));
        } else {
            // 3. Block if too many attempts
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Too many login attempts. Please try again in a minute.");
        }
    }
}