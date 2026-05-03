package org.elearning.backend.auth;

import org.elearning.backend.auth.dto.request.ForgotPasswordRequest;
import org.elearning.backend.auth.dto.request.ResetPasswordRequest;
import org.elearning.backend.auth.dto.response.ResetPasswordResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@org.springframework.test.context.ActiveProfiles("test")
class PasswordResetTypesTest {

    @Test
    void forgotPasswordRequest_storesEmail() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();

        request.setEmail("user@example.com");

        assertThat(request.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void resetPasswordRequest_storesAllFields() {
        ResetPasswordRequest request = new ResetPasswordRequest();

        request.setToken("token");
        request.setNewPassword("new-password");
        request.setConfirmPassword("new-password");

        assertThat(request.getToken()).isEqualTo("token");
        assertThat(request.getNewPassword()).isEqualTo("new-password");
        assertThat(request.getConfirmPassword()).isEqualTo("new-password");
    }

    @Test
    void resetPasswordResponse_exposesMutableMessage() {
        ResetPasswordResponse response = new ResetPasswordResponse("initial");

        response.setMessage("updated");

        assertThat(response.getMessage()).isEqualTo("updated");
    }
}
