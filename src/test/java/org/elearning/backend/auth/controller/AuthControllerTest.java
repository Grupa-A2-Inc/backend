package org.elearning.backend.auth.controller;

import org.elearning.backend.auth.dto.request.*;
import org.elearning.backend.auth.dto.response.AuthResponse;
import org.elearning.backend.auth.dto.response.ResetPasswordResponse;
import org.elearning.backend.auth.service.*;
import org.elearning.backend.security.jwt.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@org.springframework.test.context.ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private PasswordResetService resetService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthController authController;

    @Mock
    private TokenBlackListService tokenBlacklistService;

    @Mock
    private AccountActivationService accountActivationService;

    @Test
    void login_setsSecureRefreshCookieAndClearsTokenFromBody() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("secret");

        AuthResponse authResponse = new AuthResponse("Login successful", "access-token", "refresh-token", null);
        when(authService.login(request)).thenReturn(authResponse);

        ReflectionTestUtils.setField(authController, "secureCookies", true);

        ResponseEntity<AuthResponse> response = authController.login(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .contains("refresh_token=refresh-token")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=None")
                .contains("Path=/api/v1/auth");
        assertThat(response.getBody().getRefreshToken()).isNull();
    }

    @Test
    void register_omitsSecureAttributeWhenSecureCookiesAreDisabled() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("admin@example.com");

        AuthResponse authResponse = new AuthResponse("User registered successfully", "access-token", "refresh-token", null);
        when(authService.register(request)).thenReturn(authResponse);

        ReflectionTestUtils.setField(authController, "secureCookies", false);

        ResponseEntity<AuthResponse> response = authController.register(request);

        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .contains("refresh_token=refresh-token")
                .contains("SameSite=None")
                .doesNotContain("Secure");
        assertThat(response.getBody().getRefreshToken()).isNull();
    }

    @Test
    void logout_omitsSecureAttributeWhenSecureCookiesAreDisabled() {
        ReflectionTestUtils.setField(authController, "secureCookies", false);

        ResponseEntity<Void> response = authController.logout(null, null);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .contains("refresh_token=")
                .contains("HttpOnly")
                .contains("SameSite=None")
                .contains("Path=/api/v1/auth")
                .doesNotContain("Secure");
    }

    @Test
    void logout_ignoresNonBearerAuthorizationHeader() {
        ResponseEntity<Void> response = authController.logout(null, "Basic credentials");

        assertThat(response.getStatusCode().value()).isEqualTo(204);
    }

    @Test
    void forgotPassword_delegatesToResetService() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("user@example.com");

        when(resetService.forgotPassword(request)).thenReturn(new ResetPasswordResponse("sent"));

        ResponseEntity<ResetPasswordResponse> response = authController.forgotPassword(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getMessage()).isEqualTo("sent");
    }

    @Test
    void resetPassword_delegatesToResetService() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("token");
        request.setNewPassword("new-password");
        request.setConfirmPassword("new-password");

        when(resetService.resetPassword(request)).thenReturn(new ResetPasswordResponse("changed"));

        ResponseEntity<ResetPasswordResponse> response = authController.resetPassword(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getMessage()).isEqualTo("changed");
    }

    @Test
    void setPassword_delegatesToAccountActivationService() {
        SetPasswordRequest request = new SetPasswordRequest();
        request.setToken("activation-token");
        request.setPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        when(accountActivationService.setPassword(request))
                .thenReturn(new ResetPasswordResponse("Account activated successfully."));

        ResponseEntity<ResetPasswordResponse> response = authController.setPassword(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getMessage()).isEqualTo("Account activated successfully.");
    }
}
