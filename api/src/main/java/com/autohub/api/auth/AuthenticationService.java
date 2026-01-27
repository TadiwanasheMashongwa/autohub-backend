package com.autohub.api.auth;

import com.autohub.api.model.User;
import com.autohub.api.model.Role;
import com.autohub.api.repository.UserRepository;
import com.autohub.api.repository.RoleRepository;
import com.autohub.api.service.JwtService;
import com.autohub.api.service.EmailService;
import com.autohub.api.service.MfaService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthenticationService {
    private final UserRepository repository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final MfaService mfaService;

    public AuthenticationService(UserRepository repository,
                                 RoleRepository roleRepository,
                                 PasswordEncoder passwordEncoder,
                                 JwtService jwtService,
                                 AuthenticationManager authenticationManager,
                                 EmailService emailService,
                                 MfaService mfaService) {
        this.repository = repository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
        this.mfaService = mfaService;
    }

    /**
     * AUDIT #1.1: Customer Registration.
     * Automatically assigns ROLE_CUSTOMER and creates a unique profile.
     */
    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        if (repository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("An account with this email already exists.");
        }

        Role userRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Default Role not found."));

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(userRole);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setUsername(request.getFirstName() + " " + request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setBusinessName(request.getBusinessName());
        user.setAddress(request.getAddress());

        repository.save(user);
        return generateTokenForUser(user);
    }

    /**
     * AUDIT #1.2: Standard Login with MFA check.
     */
    public AuthenticationResponse authenticate(RegisterRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = repository.findByEmail(request.getEmail()).orElseThrow();

        if (user.isMfaEnabled()) {
            AuthenticationResponse mfaResponse = new AuthenticationResponse();
            mfaResponse.setRole(user.getRole().getName());
            mfaResponse.setUsername(user.getEmail());
            mfaResponse.setAccessToken("MFA_REQUIRED");
            return mfaResponse;
        }

        return generateTokenForUser(user);
    }

    /**
     * PHASE 6: MFA Verification.
     */
    public AuthenticationResponse verifyMfa(String email, String code) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!mfaService.verifyCode(user.getMfaSecret(), code)) {
            throw new RuntimeException("Invalid MFA code");
        }

        return generateTokenForUser(user);
    }

    /**
     * PHASE 6: Refresh Token Rotation logic.
     * Prevents session hijacking by issuing a new refresh token on every use.
     */
    /**
     * PHASE 6: Hardened Refresh Token Rotation.
     */
    @Transactional
    public AuthenticationResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.equals("null")) {
            throw new RuntimeException("Refresh token is missing");
        }

        String email = jwtService.extractUsername(refreshToken);
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Equality check for Rotation
        if (jwtService.isTokenValid(refreshToken, user) && refreshToken.equals(user.getRefreshToken())) {
            return generateTokenForUser(user);
        }

        // If we get here, it means the token is either invalid OR it's an old token
        // from a race condition. We don't kill the session immediately; we let the user re-auth.
        throw new RuntimeException("Session expired. Please sign in.");
    }

    /**
     * Shared logic to create a fresh JWT pair.
     */
    public AuthenticationResponse generateTokenForUser(User user) {
        String accessToken = jwtService.generateToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        user.setRefreshToken(newRefreshToken);
        repository.save(user);

        return new AuthenticationResponse(
                accessToken,
                newRefreshToken,
                user.getRole().getName(),
                user.getEmail()
        );
    }

    /**
     * AUDIT #11.1: Clerk Onboarding.
     * Used by Admins to create internal staff accounts.
     */
    @Transactional
    public User createInternalUser(RegisterRequest request, String roleName) {
        Role targetRole = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(targetRole);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setUsername(request.getFirstName() + " " + request.getLastName());
        return repository.save(user);
    }

    /**
     * AUDIT #1.3: Secure Logout.
     */
    @Transactional
    public void logout(String email) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRefreshToken(null);
        repository.save(user);
    }

    /**
     * AUDIT #1.9: Initiate Password Recovery.
     */
    @Transactional
    public void initiatePasswordReset(String email) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        repository.save(user);
        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    /**
     * AUDIT #1.10: Complete Password Recovery.
     * FIX: Invalidates active sessions by clearing the Refresh Token.
     */
    @Transactional
    public void completePasswordReset(String token, String newPassword) {
        User user = repository.findByResetToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired token"));

        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }

        // 1. Update Password
        user.setPassword(passwordEncoder.encode(newPassword));

        // 2. Clear Reset Metadata
        user.setResetToken(null);
        user.setResetTokenExpiry(null);

        // 3. FORCE LOGOUT: Clear the stored refresh token to invalidate existing sessions
        user.setRefreshToken(null);

        repository.save(user);
    }

    public boolean isValidCredentials(RegisterRequest request) {
        User user = repository.findByEmail(request.getEmail()).orElse(null);
        return user != null && passwordEncoder.matches(request.getPassword(), user.getPassword());
    }
}