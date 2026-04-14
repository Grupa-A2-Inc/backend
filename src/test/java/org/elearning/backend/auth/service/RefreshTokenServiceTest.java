package org.elearning.backend.auth.service;

import org.elearning.backend.auth.entity.RefreshToken;
import org.elearning.backend.auth.exception.InvalidCredentialsException;
import org.elearning.backend.auth.repository.RefreshTokenRepository;
import org.elearning.backend.security.jwt.JwtUtil;
import org.elearning.backend.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User user;

    private static final String RAW_TOKEN = "someRawRefreshToken";

    @BeforeEach
    void setUp() {
        user = new User();
    }

    @Test
    void storeRefreshToken_savesEntityWithHashedTokenAndCorrectExpiry() {
        refreshTokenService.storeRefreshToken(user, RAW_TOKEN);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getTokenHash()).isNotEqualTo(RAW_TOKEN);
        assertThat(saved.getTokenHash()).hasSize(64);
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(6));
    }

    @Test
    void rotateRefreshToken_tokenNotFound_throwsUnauthorized() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(RAW_TOKEN))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid refresh token");

        verifyNoInteractions(jwtUtil);
    }

    @Test
    void rotateRefreshToken_tokenRevoked_throwsUnauthorized() {
        RefreshToken revoked = new RefreshToken();
        revoked.setUser(user);
        revoked.setRevokedAt(LocalDateTime.now().minusMinutes(1)); // already revoked
        revoked.setExpiresAt(LocalDateTime.now().plusDays(7));
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(RAW_TOKEN))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Refresh token has been revoked");

        verifyNoInteractions(jwtUtil);
    }

    @Test
    void rotateRefreshToken_tokenExpired_throwsUnauthorized() {
        RefreshToken expired = new RefreshToken();
        expired.setUser(user);
        expired.setRevokedAt(null);
        expired.setExpiresAt(LocalDateTime.now().minusSeconds(1)); // expired
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(RAW_TOKEN))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Refresh token has expired");

        verifyNoInteractions(jwtUtil);
    }

    @Test
    void rotateRefreshToken_validToken_revokesOldTokenAndStoresNewOne() {
        user.setId(java.util.UUID.randomUUID());

        RefreshToken active = new RefreshToken();
        active.setUser(user);
        active.setRevokedAt(null);
        active.setExpiresAt(LocalDateTime.now().plusDays(7));

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(active));
        when(jwtUtil.generateRefreshToken(user.getId())).thenReturn("new-refresh-token");

        String result = refreshTokenService.rotateRefreshToken(RAW_TOKEN);

        assertThat(result).isEqualTo("new-refresh-token");
        assertThat(active.getRevokedAt()).isNotNull();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(captor.capture());
        List<RefreshToken> savedTokens = captor.getAllValues();
        assertThat(savedTokens.get(0)).isSameAs(active);
        assertThat(savedTokens.get(1).getUser()).isEqualTo(user);
        assertThat(savedTokens.get(1).getTokenHash()).hasSize(64);
        assertThat(savedTokens.get(1).getTokenHash()).isNotEqualTo("new-refresh-token");

        verify(jwtUtil).validateToken(RAW_TOKEN);
        verify(jwtUtil).generateRefreshToken(user.getId());
    }

    @Test
    void getUserFromToken_tokenFound_returnsUser() {
        RefreshToken valid = new RefreshToken();
        valid.setUser(user);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(valid));

        User result = refreshTokenService.getUserFromToken(RAW_TOKEN);

        assertThat(result).isEqualTo(user);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void getUserFromToken_tokenNotFound_throwsInvalidCredentials() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.getUserFromToken(RAW_TOKEN))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid refresh token");
    }

    @Test
    void revokeForToken_tokenFound_setsRevokedAtAndSaves() {
        RefreshToken active = new RefreshToken();
        active.setUser(user);
        active.setRevokedAt(null);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(active));

        refreshTokenService.revokeForToken(RAW_TOKEN);

        assertThat(active.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(active);
    }

    @Test
    void revokeForToken_tokenNotFound_doesNothing() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatCode(() -> refreshTokenService.revokeForToken(RAW_TOKEN))
                .doesNotThrowAnyException();

        verify(refreshTokenRepository, never()).save(any());
    }
}
