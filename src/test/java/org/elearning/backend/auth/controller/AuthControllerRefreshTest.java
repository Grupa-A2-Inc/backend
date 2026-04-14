package org.elearning.backend.auth.controller;

import org.elearning.backend.auth.exception.InvalidCredentialsException;
import org.elearning.backend.auth.service.AuthService;
import org.elearning.backend.auth.service.PasswordResetService;
import org.elearning.backend.auth.service.RefreshTokenService;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.jwt.JwtUtil;
import org.elearning.backend.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import org.elearning.backend.role.entity.Role;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerRefreshTest {

    @Mock private AuthService authService;
    @Mock private PasswordResetService resetService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authController, "secureCookies", false);
    }

    @Test
    void refresh_nullCookie_throwsUnauthorized() {
        assertThatThrownBy(() -> authController.refresh(null))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Refresh token missing");

        verifyNoInteractions(refreshTokenService, jwtUtil);
    }

    @Test
    void refresh_blankCookie_throwsUnauthorized() {
        assertThatThrownBy(() -> authController.refresh("   "))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Refresh token missing");

        verifyNoInteractions(refreshTokenService, jwtUtil);
    }

    @Test
    void refresh_validToken_returnsNewAccessToken() {
        User user = buildUser();
        String rawToken = "valid.refresh.token";
        String newAccessToken = "new.access.token";

        when(refreshTokenService.getUserFromToken(rawToken)).thenReturn(user);
        when(jwtUtil.generateAccessToken(user.getId(), RoleName.ORGANIZATION_ADMIN)).thenReturn(newAccessToken);

        var response = authController.refresh(rawToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccessToken()).isEqualTo(newAccessToken);
    }

    @Test
    void refresh_serviceThrowsUnauthorized_propagatesException() {
        when(refreshTokenService.getUserFromToken(anyString()))
                .thenThrow(new InvalidCredentialsException("Refresh token has been revoked"));

        assertThatThrownBy(() -> authController.refresh("some.token"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Refresh token has been revoked");
    }

    @Test
    void logout_withToken_revokesTokenAndClearsCookie() {
        String rawToken = "valid.refresh.token";

        var response = authController.logout(rawToken);

        verify(refreshTokenService).revokeForToken(rawToken);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        String setCookieHeader = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeader)
                .contains("refresh_token=")
                .contains("Max-Age=0");
    }

    @Test
    void logout_withNullToken_skipRevocationAndStillClearsCookie() {
        var response = authController.logout(null);

        verifyNoInteractions(refreshTokenService); // nothing to revoke
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        String setCookieHeader = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeader).contains("Max-Age=0");
    }

    @Test
    void logout_withBlankToken_skipRevocationAndStillClearsCookie() {
        var response = authController.logout("  ");

        verifyNoInteractions(refreshTokenService);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    private User buildUser() {
        Role role = new Role();
        role.setName(RoleName.ORGANIZATION_ADMIN);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(role);
        return user;
    }
}