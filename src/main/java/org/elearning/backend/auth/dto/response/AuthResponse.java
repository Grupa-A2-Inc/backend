package org.elearning.backend.auth.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {

    private String message;
    private String accessToken;
    private String refreshToken;
    private UserDataResponse user;

    public AuthResponse(String message) {
        this.message = message;
    }

    public AuthResponse(String message, String accessToken, String refreshToken, UserDataResponse user) {
        this.message = message;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
    }
}