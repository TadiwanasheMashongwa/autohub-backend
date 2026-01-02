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

@Service
public class AuthenticationService {
    private final UserRepository repository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

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
        Role userRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Error: ROLE_CUSTOMER not found."));

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(userRole);

        repository.save(user);

        String jwtToken = jwtService.generateToken(user);
        return new AuthenticationResponse(
                jwtToken,
                user.getRole().getName(),
                user.getUsername()
        );
    }

    public AuthenticationResponse authenticate(RegisterRequest request) {
        // This will now use the BCryptPasswordEncoder defined in SecurityConfig
        System.out.println("Attempting login for: " + request.getUsername());
        System.out.println("Password received: " + (request.getPassword() != null ? "[PROTECTED]" : "NULL"));
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = repository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));

        String jwtToken = jwtService.generateToken(user);

        return new AuthenticationResponse(
                jwtToken,
                user.getRole().getName(),
                user.getUsername()
        );
    }
}