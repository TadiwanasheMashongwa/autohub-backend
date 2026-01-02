package com.autohub.api.config;

import com.autohub.api.model.Role;
import com.autohub.api.model.User;
import com.autohub.api.repository.RoleRepository;
import com.autohub.api.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 1. Ensure Roles exist
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_ADMIN")));

        // 2. FORCE REFRESH the Admin password
        userRepository.findByUsername("admin@autohub.co.zw").ifPresentOrElse(
                (existingAdmin) -> {
                    // Force update existing user with the current encoder's hash
                    existingAdmin.setPassword(passwordEncoder.encode("password"));
                    existingAdmin.setRole(adminRole);
                    userRepository.save(existingAdmin);
                    System.out.println(">>> DATA: Admin password force-updated.");
                },
                () -> {
                    // Create fresh if not exists
                    User admin = new User();
                    admin.setUsername("admin@autohub.co.zw");
                    admin.setPassword(passwordEncoder.encode("password"));
                    admin.setRole(adminRole);
                    userRepository.save(admin);
                    System.out.println(">>> DATA: Admin account created fresh.");
                }
        );

    }
}