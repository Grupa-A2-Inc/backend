package org.elearning.backend.auth.service;

import org.elearning.backend.auth.dto.request.ForgotPasswordRequest;
import org.elearning.backend.auth.dto.request.ResetPasswordRequest;
import org.elearning.backend.auth.dto.response.ResetPasswordResponse;
import org.elearning.backend.auth.entity.PasswordResetToken;
import org.elearning.backend.auth.exception.AuthBadRequestException;
import org.elearning.backend.auth.repository.PasswordResetTokenRepository;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final String GENERIC_MESSAGE = "If an account exists for this email, a reset token has been sent.";

    @Mock
    private EmailService emailService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @Test
    void forgotPassword_returnsGenericMessageWhenUserDoesNotExist() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("missing@example.com");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        ResetPasswordResponse response = passwordResetService.forgotPassword(request);

        assertThat(response.getMessage()).isEqualTo(GENERIC_MESSAGE);
        verify(userRepository).findByEmail("missing@example.com");
        verifyNoInteractions(emailService, passwordResetTokenRepository);
    }

    @Test
    void forgotPassword_savesHashedTokenAndSendsRawTokenWhenUserExists() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("user@example.com");

        User user = new User();
        user.setEmail("user@example.com");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime beforeCall = LocalDateTime.now();
        ResetPasswordResponse response = passwordResetService.forgotPassword(request);
        LocalDateTime afterCall = LocalDateTime.now();

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        ArgumentCaptor<String> rawTokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        verify(emailService).sendPasswordResetEmail(eq("user@example.com"), rawTokenCaptor.capture());

        String rawToken = rawTokenCaptor.getValue();
        PasswordResetToken savedToken = tokenCaptor.getValue();

        assertThat(response.getMessage()).isEqualTo(GENERIC_MESSAGE);
        assertThat(rawToken)
                .isNotBlank()
                .doesNotContain("=");
        assertThat(savedToken.getUser()).isSameAs(user);
        assertThat(savedToken.getTokenHash()).isEqualTo(hashToken(rawToken));
        assertThat(savedToken.getUsedAt()).isNull();
        assertThat(savedToken.getCreatedAt()).isBetween(beforeCall, afterCall);
        assertThat(savedToken.getExpiresAt()).isBetween(beforeCall.plusMinutes(10), afterCall.plusMinutes(10));
    }

    @Test
    void resetPassword_returnsMessageWhenConfirmationDoesNotMatch() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("token");
        request.setNewPassword("new-password");
        request.setConfirmPassword("different-password");

        ResetPasswordResponse response = passwordResetService.resetPassword(request);

        assertThat(response.getMessage()).isEqualTo("Confirmed password does not match new password.");
        verifyNoInteractions(passwordEncoder, passwordResetTokenRepository);
    }

    @Test
    void resetPassword_throwsWhenTokenIsMissing() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("raw-token");
        request.setNewPassword("new-password");
        request.setConfirmPassword("new-password");

        when(passwordResetTokenRepository.findByTokenHash(hashToken("raw-token"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword(request))
                .isInstanceOf(AuthBadRequestException.class)
                .hasMessage("Invalid or expired reset token.");
    }

    @Test
    void resetPassword_throwsWhenTokenWasAlreadyUsed() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("raw-token");
        request.setNewPassword("new-password");
        request.setConfirmPassword("new-password");

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setTokenHash(hashToken("raw-token"));
        resetToken.setUsedAt(LocalDateTime.now());
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(passwordResetTokenRepository.findByTokenHash(hashToken("raw-token"))).thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> passwordResetService.resetPassword(request))
                .isInstanceOf(AuthBadRequestException.class)
                .hasMessage("This reset token has already been used.");
    }

    @Test
    void resetPassword_throwsWhenTokenExpired() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("raw-token");
        request.setNewPassword("new-password");
        request.setConfirmPassword("new-password");

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setTokenHash(hashToken("raw-token"));
        resetToken.setUsedAt(null);
        resetToken.setExpiresAt(LocalDateTime.now().minusSeconds(1));

        when(passwordResetTokenRepository.findByTokenHash(hashToken("raw-token"))).thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> passwordResetService.resetPassword(request))
                .isInstanceOf(AuthBadRequestException.class)
                .hasMessage("Reset token has expired.");
    }

    @Test
    void resetPassword_updatesPasswordAndMarksTokenAsUsed() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("raw-token");
        request.setNewPassword("new-password");
        request.setConfirmPassword("new-password");

        User user = new User();
        user.setEmail("user@example.com");

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(hashToken("raw-token"));
        resetToken.setUsedAt(null);
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(passwordResetTokenRepository.findByTokenHash(hashToken("raw-token"))).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-password");

        ResetPasswordResponse response = passwordResetService.resetPassword(request);

        assertThat(response.getMessage()).isEqualTo("Password changed successfully.");
        assertThat(user.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(resetToken.getUsedAt()).isNotNull();

        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(resetToken);
        verify(passwordEncoder).encode("new-password");
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
