package org.elearning.backend.auth.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {

    private String message;
    private String accessToken;
    private String refreshToken;

    public AuthResponse(String message) {
        this.message = message;
        this.accessToken = null;
        this.refreshToken = null;
    }

    public AuthResponse(String message, String accessToken, String refreshToken) {
        this.message = message;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}