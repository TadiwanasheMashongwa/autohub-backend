package com.autohub.api.auth;

public class RegisterRequest {
    private String username;
    private String password;

    public RegisterRequest() {}

    public RegisterRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // GETTERS
    public String getUsername() { return username; }
    public String getPassword() { return password; }

    // SETTERS (CRITICAL: Jackson needs these to read your Postman JSON)
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
}