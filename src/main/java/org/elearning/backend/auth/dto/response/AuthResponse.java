package org.elearning.backend.auth.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {

    private String message;
    private String token;

    public AuthResponse(String message) {
        this.message = message;
        this.token = null;
    }

    public AuthResponse(String message, String token) {
        this.message = message;
        this.token = token;
    }
}