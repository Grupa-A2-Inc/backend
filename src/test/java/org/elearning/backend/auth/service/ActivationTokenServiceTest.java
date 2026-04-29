package org.elearning.backend.auth.service;

import org.elearning.backend.auth.entity.ActivationToken;
import org.elearning.backend.auth.exception.AuthBadRequestException;
import org.elearning.backend.auth.repository.ActivationTokenRepository;
import org.elearning.backend.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@org.springframework.test.context.ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class ActivationTokenServiceTest {

    @Mock
    private ActivationTokenRepository activationTokenRepository;

    @InjectMocks
    private ActivationTokenService activationTokenService;

    @Test
    void generateActivationToken_returnsRawToken() {
        User user = new User();
        user.setEmail("user@example.com");

        when(activationTokenRepository.save(any(ActivationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String rawToken = activationTokenService.generateActivationToken(user);

        assertThat(rawToken)
                .isNotBlank()
                .doesNotContain("=");
    }

    @Test
    void generateActivationToken_savesHashedTokenNotRawToken() {
        User user = new User();
        user.setEmail("user@example.com");

        when(activationTokenRepository.save(any(ActivationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String rawToken = activationTokenService.generateActivationToken(user);

        ArgumentCaptor<ActivationToken> captor = ArgumentCaptor.forClass(ActivationToken.class);
        verify(activationTokenRepository).save(captor.capture());

        ActivationToken saved = captor.getValue();

        assertThat(saved.getTokenHash()).isEqualTo(hashToken(rawToken));
        assertThat(saved.getTokenHash()).isNotEqualTo(rawToken);
    }

    @Test
    void generateActivationToken_savesCorrectUserAndTimestamps() {
        User user = new User();
        user.setEmail("user@example.com");

        when(activationTokenRepository.save(any(ActivationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime before = LocalDateTime.now();
        activationTokenService.generateActivationToken(user);
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<ActivationToken> captor = ArgumentCaptor.forClass(ActivationToken.class);
        verify(activationTokenRepository).save(captor.capture());

        ActivationToken saved = captor.getValue();

        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getUsedAt()).isNull();
        assertThat(saved.getCreatedAt()).isBetween(before, after);
        assertThat(saved.getExpiresAt()).isBetween(
                before.plusMinutes(60),
                after.plusMinutes(60)
        );
    }

    @Test
    void validateAndConsumeToken_throwsWhenTokenNotFound() {
        when(activationTokenRepository.findByTokenHash(hashToken("raw-token")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> activationTokenService.validateAndConsumeToken("raw-token"))
                .isInstanceOf(AuthBadRequestException.class)
                .hasMessage("Invalid activation token.");
    }

    @Test
    void validateAndConsumeToken_throwsWhenTokenAlreadyUsed() {
        ActivationToken token = new ActivationToken();
        token.setUsedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now().plusMinutes(60));

        when(activationTokenRepository.findByTokenHash(hashToken("raw-token")))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> activationTokenService.validateAndConsumeToken("raw-token"))
                .isInstanceOf(AuthBadRequestException.class)
                .hasMessage("This activation token has already been used.");
    }

    @Test
    void validateAndConsumeToken_throwsWhenTokenExpired() {
        ActivationToken token = new ActivationToken();
        token.setUsedAt(null);
        token.setExpiresAt(LocalDateTime.now().minusSeconds(1));

        when(activationTokenRepository.findByTokenHash(hashToken("raw-token")))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> activationTokenService.validateAndConsumeToken("raw-token"))
                .isInstanceOf(AuthBadRequestException.class)
                .hasMessage("Activation token has expired.");
    }

    @Test
    void validateAndConsumeToken_marksTokenAsUsedAndReturnsUser() {
        User user = new User();
        user.setEmail("user@example.com");

        ActivationToken token = new ActivationToken();
        token.setUser(user);
        token.setUsedAt(null);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(60));

        when(activationTokenRepository.findByTokenHash(hashToken("raw-token")))
                .thenReturn(Optional.of(token));
        when(activationTokenRepository.save(any(ActivationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime before = LocalDateTime.now();
        User returned = activationTokenService.validateAndConsumeToken("raw-token");
        LocalDateTime after = LocalDateTime.now();

        assertThat(returned).isSameAs(user);
        assertThat(token.getUsedAt()).isBetween(before, after);
        verify(activationTokenRepository).save(token);
    }

    @Test
    void validateAndConsumeToken_searchesByHashNotByRawToken() {
        when(activationTokenRepository.findByTokenHash(hashToken("raw-token")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> activationTokenService.validateAndConsumeToken("raw-token"))
                .isInstanceOf(AuthBadRequestException.class);

        verify(activationTokenRepository).findByTokenHash(hashToken("raw-token"));
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
