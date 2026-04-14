package org.elearning.backend.auth.service;

import org.elearning.backend.auth.entity.RevokedAccessToken;
import org.elearning.backend.auth.repository.RevokedAccessTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private RevokedAccessTokenRepository revokedAccessTokenRepository;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    @Test
    void revokeAccessToken_savesEntityWithCorrectFields() {
        String rawToken = "some.access.token";
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        when(revokedAccessTokenRepository.existsByTokenHash(anyString())).thenReturn(false);

        tokenBlacklistService.revokeAccessToken(rawToken, expiresAt);

        ArgumentCaptor<RevokedAccessToken> captor = ArgumentCaptor.forClass(RevokedAccessToken.class);
        verify(revokedAccessTokenRepository).save(captor.capture());

        RevokedAccessToken saved = captor.getValue();
        assertThat(saved.getTokenHash()).isNotNull().isNotBlank();
        assertThat(saved.getTokenHash()).isNotEqualTo(rawToken);
        assertThat(saved.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(saved.getRevokedAt()).isNotNull();
    }

    @Test
    void revokeAccessToken_alreadyRevoked_doesNotSaveAgain() {
        String rawToken = "already.revoked.token";
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        when(revokedAccessTokenRepository.existsByTokenHash(anyString())).thenReturn(true);

        tokenBlacklistService.revokeAccessToken(rawToken, expiresAt);

        verify(revokedAccessTokenRepository, never()).save(any());
    }

    @Test
    void isRevoked_tokenInBlacklist_returnsTrue() {
        when(revokedAccessTokenRepository.existsByTokenHash(anyString())).thenReturn(true);

        assertThat(tokenBlacklistService.isRevoked("some.token")).isTrue();
    }

    @Test
    void isRevoked_tokenNotInBlacklist_returnsFalse() {
        when(revokedAccessTokenRepository.existsByTokenHash(anyString())).thenReturn(false);

        assertThat(tokenBlacklistService.isRevoked("some.token")).isFalse();
    }

    @Test
    void revokeAccessToken_hashIsDeterministic_sameTokenProducesSameHash() {
        String rawToken = "deterministic.token";
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        when(revokedAccessTokenRepository.existsByTokenHash(anyString())).thenReturn(false);

        tokenBlacklistService.revokeAccessToken(rawToken, expiresAt);

        tokenBlacklistService.isRevoked(rawToken);

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(revokedAccessTokenRepository, atLeastOnce()).existsByTokenHash(hashCaptor.capture());

        assertThat(hashCaptor.getAllValues()).allMatch(h -> h.equals(hashCaptor.getAllValues().get(0)));
    }

    @Test
    void cleanupExpiredTokens_callsRepositoryWithCurrentTime() {
        LocalDateTime before = LocalDateTime.now();

        tokenBlacklistService.cleanupExpiredTokens();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(revokedAccessTokenRepository).deleteAllExpiredBefore(captor.capture());

        LocalDateTime after = LocalDateTime.now();
        assertThat(captor.getValue()).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    }
}