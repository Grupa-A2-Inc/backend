package org.elearning.backend.auth.controller;

import org.elearning.backend.auth.dto.request.LoginRequest;
import org.elearning.backend.auth.dto.request.RegisterRequest;
import org.elearning.backend.auth.dto.response.AuthResponse;
import org.elearning.backend.auth.service.AuthService;
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

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

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
                .contains("SameSite=none")
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
                .contains("SameSite=none")
                .doesNotContain("Secure");
        assertThat(response.getBody().getRefreshToken()).isNull();
    }
}
