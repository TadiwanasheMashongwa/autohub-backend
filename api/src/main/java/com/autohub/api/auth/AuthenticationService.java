package com.autohub.api.auth;

import com.autohub.api.model.User;
import com.autohub.api.model.Role;
import com.autohub.api.repository.UserRepository;
import com.autohub.api.repository.RoleRepository;
import com.autohub.api.service.JwtService;
import com.autohub.api.service.EmailService;
import com.autohub.api.service.MfaService; // New Import
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthenticationService {
    private final UserRepository repository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final MfaService mfaService; // Injected Service

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

    // UPDATED: Registration logic remains same but ensures internal consistency
    public AuthenticationResponse register(RegisterRequest request) {
        if (repository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("An account with this email already exists.");
        }

        Role userRole = roleRepository.findByName("ROLE_CUSTOMER").orElseThrow();
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

    // UPDATED: Login flow now handles MFA checks
    public AuthenticationResponse authenticate(RegisterRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = repository.findByEmail(request.getEmail()).orElseThrow();

        // If MFA is enabled, we DO NOT return the tokens yet.
        // We return a response that tells the frontend to show the MFA input.
        if (user.isMfaEnabled()) {
            AuthenticationResponse mfaResponse = new AuthenticationResponse();
            mfaResponse.setRole(user.getRole().getName());
            mfaResponse.setUsername(user.getEmail());
            mfaResponse.setAccessToken("MFA_REQUIRED"); // Sentinel value for frontend
            return mfaResponse;
        }

        return generateTokenForUser(user);
    }

    /**
     * NEW: Verification method for the 2nd Factor
     */
    public AuthenticationResponse verifyMfa(String email, String code) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!mfaService.verifyCode(user.getMfaSecret(), code)) {
            throw new RuntimeException("Invalid MFA code");
        }

        return generateTokenForUser(user);
    }

    public AuthenticationResponse generateTokenForUser(User user) {
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        user.setRefreshToken(refreshToken);
        repository.save(user);

        return new AuthenticationResponse(
                accessToken,
                refreshToken,
                user.getRole().getName(),
                user.getEmail()
        );
    }

    // RESTORED: Internal user creation for Clerks/Admins
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

    public AuthenticationResponse refreshToken(String refreshToken) {
        String email = jwtService.extractUsername(refreshToken);
        User user = repository.findByEmail(email).orElseThrow();

        if (jwtService.isTokenValid(refreshToken, user) && refreshToken.equals(user.getRefreshToken())) {
            return generateTokenForUser(user);
        }
        throw new RuntimeException("Invalid Refresh Token");
    }

    public void logout(String email) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRefreshToken(null);
        repository.save(user);
    }

    public void initiatePasswordReset(String email) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        repository.save(user);
        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    public void completePasswordReset(String token, String newPassword) {
        User user = repository.findByResetToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));
        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        repository.save(user);
    }

    public boolean isValidCredentials(RegisterRequest request) {
        User user = repository.findByEmail(request.getEmail()).orElse(null);
        return user != null && passwordEncoder.matches(request.getPassword(), user.getPassword());
    }
}