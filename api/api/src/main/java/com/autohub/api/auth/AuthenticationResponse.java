package com.autohub.api.auth;

public class AuthenticationResponse {
    private String token;
    private String role;
    private String username;

    public AuthenticationResponse() {}

    public AuthenticationResponse(String token, String role, String username) {
        this.token = token;
        this.role = role;
        this.username = username;
    }

    // MANUAL GETTERS - CRITICAL for Postman to show data
    public String getToken() { return token; }
    public String getRole() { return role; }
    public String getUsername() { return username; }

    // MANUAL SETTERS
    public void setToken(String token) { this.token = token; }
    public void setRole(String role) { this.role = role; }
    public void setUsername(String username) { this.username = username; }
}