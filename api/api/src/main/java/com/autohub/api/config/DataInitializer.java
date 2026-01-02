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
        // 1. Create Roles if they don't exist
        if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) {
            roleRepository.save(new Role(null, "ROLE_ADMIN"));
        }
        if (roleRepository.findByName("ROLE_CUSTOMER").isEmpty()) {
            roleRepository.save(new Role(null, "ROLE_CUSTOMER"));
        }

        // 2. Create Admin if it doesn't exist
        if (userRepository.findByUsername("admin@autohub.co.zw").isEmpty()) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found"));

            User admin = new User();
            admin.setUsername("admin@autohub.co.zw");
            // This hashes the password using YOUR specific BCrypt bean
            admin.setPassword(passwordEncoder.encode("password"));
            admin.setRole(adminRole);

            userRepository.save(admin);
            System.out.println("--- INITIALIZATION: Admin user created successfully ---");
        }
    }
}