package org.elearning.backend.user.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserImportResult {
    private String email;
    private boolean success;
    private UserResponse user;
    private String errorMessage;

    public static UserImportResult succeeded(UserResponse user) {
        UserImportResult result = new UserImportResult();
        result.email = user.getEmail();
        result.success = true;
        result.user = user;
        result.errorMessage = null;
        return result;
    }

    public static UserImportResult failed(String email, String errorMessage) {
        UserImportResult result = new UserImportResult();
        result.email = email;
        result.success = false;
        result.user = null;
        result.errorMessage = errorMessage;
        return result;
    }
}