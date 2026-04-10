package org.elearning.backend.auth.dto.response;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordResponse {
    private String message;
    public ResetPasswordResponse(String message) {
        this.message = message;
    }
}
