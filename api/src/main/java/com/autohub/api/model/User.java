package com.autohub.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) // Removed unique=true to allow duplicate display names
    private String username;

    @Column(unique = true, nullable = false) // Email is now the unique login key
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String businessName;

    @Column(columnDefinition = "TEXT")
    private String address;

    @JsonIgnore
    private String resetToken;

    @JsonIgnore
    private LocalDateTime resetTokenExpiry;

    @JsonIgnore
    private String refreshToken;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean mfaEnabled = false;

    @JsonIgnore
    private String mfaSecret;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Cart cart;

    public User() {}

    // --- UserDetails Overrides ---

    /**
     * IMPORTANT: This tells Spring Security that for all authentication
     * and JWT purposes, the 'username' is actually the user's email.
     */
    @Override
    @JsonIgnore
    public String getUsername() {
        return email;
    }

    @Override @JsonIgnore public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) return List.of();
        return List.of(new SimpleGrantedAuthority(role.getName()));
    }
    @Override @JsonIgnore public boolean isAccountNonExpired() { return true; }
    @Override @JsonIgnore public boolean isAccountNonLocked() { return true; }
    @Override @JsonIgnore public boolean isCredentialsNonExpired() { return true; }
    @Override @JsonIgnore public boolean isEnabled() { return true; }

    // --- Getters and Setters ---

    public Long getId() { return id; }

    // We keep this getter for cases where we need the actual display name
    public String getActualUsername() { return username; }

    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getBusinessName() { return businessName; }
    public String getAddress() { return address; }
    public boolean isMfaEnabled() { return mfaEnabled; }
    public String getMfaSecret() { return mfaSecret; }
    public Role getRole() { return role; }
    public Cart getCart() { return cart; }
    public String getResetToken() { return resetToken; }
    public LocalDateTime getResetTokenExpiry() { return resetTokenExpiry; }
    public String getRefreshToken() { return refreshToken; }

    public void setId(Long id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public void setAddress(String address) { this.address = address; }
    public void setMfaEnabled(boolean mfaEnabled) { this.mfaEnabled = mfaEnabled; }
    public void setMfaSecret(String mfaSecret) { this.mfaSecret = mfaSecret; }
    public void setRole(Role role) { this.role = role; }
    public void setCart(Cart cart) { this.cart = cart; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }
    public void setResetTokenExpiry(LocalDateTime resetTokenExpiry) { this.resetTokenExpiry = resetTokenExpiry; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}