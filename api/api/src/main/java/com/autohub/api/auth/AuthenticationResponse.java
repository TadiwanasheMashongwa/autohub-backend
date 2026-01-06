package com.autohub.api.auth;

/**
 * Data Transfer Object for authentication responses.
 * Workflow v2.9: Added support for Refresh Token rotation.
 */
public class AuthenticationResponse {
    private String accessToken;
    private String refreshToken;
    private String role;
    private String username;

    public AuthenticationResponse() {}

    public AuthenticationResponse(String accessToken, String refreshToken, String role, String username) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.role = role;
        this.username = username;
    }

    // --- GETTERS ---
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public String getRole() { return role; }
    public String getUsername() { return username; }

    // --- SETTERS ---
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public void setRole(String role) { this.role = role; }
    public void setUsername(String username) { this.username = username; }
}