package com.autohub.api.config;

import com.autohub.api.model.*;
import com.autohub.api.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

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
        // 1. Ensure Roles exist
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_ADMIN")));

        roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_CUSTOMER")));

        // 2. FORCE REFRESH the Admin account
        userRepository.findByUsername("admin@autohub.co.zw").ifPresentOrElse(
                (existingAdmin) -> {
                    existingAdmin.setPassword(passwordEncoder.encode("password"));
                    existingAdmin.setRole(adminRole);
                    userRepository.save(existingAdmin);
                    System.out.println(">>> DATA: Admin password force-updated.");
                },
                () -> {
                    User admin = new User();
                    admin.setUsername("admin@autohub.co.zw");
                    admin.setPassword(passwordEncoder.encode("password"));
                    admin.setRole(adminRole);
                    userRepository.save(admin);
                    System.out.println(">>> DATA: Admin account created fresh.");
                }
        );

        // 3. Seed Sample Categories and Parts if empty (for Visual Catalog)
        if (categoryRepository.count() == 0) {
            Category engineCategory = new Category();
            engineCategory.setName("Engine Parts");
            categoryRepository.save(engineCategory);

            Part oilFilter = new Part();
            oilFilter.setName("Premium Oil Filter");
            oilFilter.setSku("OF-TOY-001");
            oilFilter.setBarcode("123456789");
            oilFilter.setBrand("Bosch");
            oilFilter.setPrice(new BigDecimal("15.99"));
            oilFilter.setStockQuantity(50);
            oilFilter.setCategory(engineCategory);
            // Using a generic placeholder for the visual catalog
            oilFilter.setImageUrl("https://images.unsplash.com/photo-1486262715619-67b85e0b08d3?q=80&w=400&auto=format&fit=crop");
            partRepository.save(oilFilter);

            System.out.println(">>> DATA: Visual sample parts seeded.");
        }
    }
}