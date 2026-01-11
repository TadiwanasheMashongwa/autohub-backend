package com.autohub.api.config;

import com.autohub.api.model.*;
import com.autohub.api.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CategoryRepository categoryRepository;
    private final PartRepository partRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           RoleRepository roleRepository,
                           CategoryRepository categoryRepository,
                           PartRepository partRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.categoryRepository = categoryRepository;
        this.partRepository = partRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 1. Ensure all 3 roles exist in the database
        List.of("ROLE_ADMIN", "ROLE_CLERK", "ROLE_CUSTOMER").forEach(roleName -> {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(new Role(null, roleName));
            }
        });

        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow();

        // 2. Ensure Admin account exists and is linked to ROLE_ADMIN
        String adminUsername = "admin@autohub.co.zw";
        userRepository.findByUsername(adminUsername).ifPresentOrElse(
                (existingAdmin) -> {
                    existingAdmin.setPassword(passwordEncoder.encode("password"));
                    existingAdmin.setRole(adminRole);
                    existingAdmin.setEmail(adminUsername); // Ensure email is set on update
                    userRepository.save(existingAdmin);
                },
                () -> {
                    User admin = new User();
                    admin.setUsername(adminUsername);
                    admin.setEmail(adminUsername); // FIX: Mandatory field now populated
                    admin.setPassword(passwordEncoder.encode("password"));
                    admin.setRole(adminRole);
                    admin.setFirstName("System");
                    admin.setLastName("Admin");
                    userRepository.save(admin);
                }
        );

        // 3. Seed sample data if empty
        if (categoryRepository.count() == 0) {
            Category engineCategory = new Category();
            engineCategory.setName("Engine Parts");
            categoryRepository.save(engineCategory);

            Part oilFilter = new Part();
            oilFilter.setName("Premium Oil Filter");
            oilFilter.setSku("OF-TOY-001");
            oilFilter.setPrice(new BigDecimal("15.99"));
            oilFilter.setCategory(engineCategory);
            partRepository.save(oilFilter);
        }
    }
}