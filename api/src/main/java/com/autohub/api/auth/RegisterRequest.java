package com.autohub.api.auth;

public class RegisterRequest {
    private String username;
    private String password;

    public RegisterRequest() {}
    public RegisterRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
}