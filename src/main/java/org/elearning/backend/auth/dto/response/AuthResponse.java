package org.elearning.backend.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AuthResponse {

    private String message;
    private String accessToken;
    private String refreshToken;
    private UserDataResponse user;

    public AuthResponse(String message) {
        this.message = message;
    }

}