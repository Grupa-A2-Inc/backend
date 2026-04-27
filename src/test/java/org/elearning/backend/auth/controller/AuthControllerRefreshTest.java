package org.elearning.backend.auth.controller;

import org.elearning.backend.auth.exception.InvalidCredentialsException;
import org.elearning.backend.auth.service.AuthService;
import org.elearning.backend.auth.service.PasswordResetService;
import org.elearning.backend.auth.service.RefreshTokenService;
import org.elearning.backend.auth.service.TokenBlackListService;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerRefreshTest {

    @Mock private AuthService authService;
    @Mock private PasswordResetService resetService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private TokenBlackListService tokenBlacklistService;
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
    void logout_withToken_revokesRefreshTokenAndClearsCookie() {
        String rawRefreshToken = "valid.refresh.token";

        var response = authController.logout(rawRefreshToken, null);

        verify(refreshTokenService).revokeForToken(rawRefreshToken);
        verifyNoInteractions(tokenBlacklistService);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        String setCookieHeader = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeader)
                .contains("refresh_token=")
                .contains("Max-Age=0");
    }

    @Test
    void logout_withNullToken_skipRevocationAndStillClearsCookie() {
        var response = authController.logout(null, null);

        verifyNoInteractions(refreshTokenService);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        String setCookieHeader = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeader).contains("Max-Age=0");
    }

    @Test
    void logout_withBlankToken_skipRevocationAndStillClearsCookie() {
        var response = authController.logout("  ", null);

        verifyNoInteractions(refreshTokenService);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void logout_withValidAccessToken_blacklistsAccessToken() {
        String rawAccessToken = "valid.access.token";
        String authHeader = "Bearer " + rawAccessToken;
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

        when(jwtUtil.extractExpiration(rawAccessToken)).thenReturn(expiresAt);

        var response = authController.logout(null, authHeader);

        verify(tokenBlacklistService).revokeAccessToken(rawAccessToken, expiresAt);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void logout_withExpiredAccessToken_ignoresBlacklistingGracefully() {
        String rawAccessToken = "expired.access.token";
        String authHeader = "Bearer " + rawAccessToken;

        when(jwtUtil.extractExpiration(rawAccessToken))
                .thenThrow(new RuntimeException("Token expired"));

        // nu aruncă excepție — e prins în catch din controller
        assertThatCode(() -> authController.logout(null, authHeader))
                .doesNotThrowAnyException();

        verifyNoInteractions(tokenBlacklistService);
    }

    @Test
    void logout_withBothTokens_revokesRefreshAndBlacklistsAccess() {
        String rawRefreshToken = "valid.refresh.token";
        String rawAccessToken = "valid.access.token";
        String authHeader = "Bearer " + rawAccessToken;
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(8);

        when(jwtUtil.extractExpiration(rawAccessToken)).thenReturn(expiresAt);

        var response = authController.logout(rawRefreshToken, authHeader);

        verify(refreshTokenService).revokeForToken(rawRefreshToken);
        verify(tokenBlacklistService).revokeAccessToken(rawAccessToken, expiresAt);
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