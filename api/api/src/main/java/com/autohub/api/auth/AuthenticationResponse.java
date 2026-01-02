package com.autohub.api.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResponse {
    private String token;
    private String role;
    private String username;

    // Manual constructor for safety since the builder is failing
    public static AuthenticationResponse of(String token, String role, String username) {
        return new AuthenticationResponse(token, role, username);
    }
}