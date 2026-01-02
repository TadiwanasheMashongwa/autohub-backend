package com.autohub.api.auth;

import lombok.Data;

@Data
public class AuthenticationResponse {
    private String token;
    private String role;
    private String username;

    // MANUAL NO-ARGS CONSTRUCTOR
    public AuthenticationResponse() {
    }

    // MANUAL ALL-ARGS CONSTRUCTOR - This fixes the compiler error
    public AuthenticationResponse(String token, String role, String username) {
        this.token = token;
        this.role = role;
        this.username = username;
    }

    // Static helper method
    public static AuthenticationResponse of(String token, String role, String username) {
        return new AuthenticationResponse(token, role, username);
    }
}