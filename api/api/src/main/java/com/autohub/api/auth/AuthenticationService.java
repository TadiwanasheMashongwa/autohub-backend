package com.autohub.api.auth;

import com.autohub.api.model.User;
import com.autohub.api.model.Role;
import com.autohub.api.repository.UserRepository;
import com.autohub.api.repository.RoleRepository;
import com.autohub.api.service.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.Set;

@Service
public class AuthenticationService {
    private final UserRepository repository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    private final Set<String> tokenBlacklist = new HashSet<>();

    public AuthenticationService(UserRepository repository,
                                 RoleRepository roleRepository,
                                 PasswordEncoder passwordEncoder,
                                 JwtService jwtService,
                                 AuthenticationManager authenticationManager) {
        this.repository = repository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthenticationResponse register(RegisterRequest request) {
        Role userRole = roleRepository.findByName("ROLE_CUSTOMER").orElseThrow();
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(userRole);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        repository.save(user);
        return generateTokenForUser(user);
    }

    public AuthenticationResponse authenticate(RegisterRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        User user = repository.findByUsername(request.getUsername()).orElseThrow();
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
                user.getUsername()
        );
    }

    public AuthenticationResponse refreshToken(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);
        User user = repository.findByUsername(username).orElseThrow();

        if (jwtService.isTokenValid(refreshToken, user) && refreshToken.equals(user.getRefreshToken())) {
            return generateTokenForUser(user);
        }
        throw new RuntimeException("Invalid Refresh Token");
    }

    public void logout(String username) {
        User user = repository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRefreshToken(null);
        repository.save(user);
    }

    public User createInternalUser(RegisterRequest request, String roleName) {
        Role targetRole = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(targetRole);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        return repository.save(user);
    }

    public boolean isValidCredentials(RegisterRequest request) {
        User user = repository.findByUsername(request.getUsername()).orElse(null);
        return user != null && passwordEncoder.matches(request.getPassword(), user.getPassword());
    }

    public void blacklistToken(String token) { tokenBlacklist.add(token); }
    public boolean isTokenBlacklisted(String token) { return tokenBlacklist.contains(token); }
}