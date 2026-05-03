package org.elearning.backend.auth.service;

import org.elearning.backend.auth.dto.request.SetPasswordRequest;
import org.elearning.backend.auth.dto.response.ResetPasswordResponse;
import org.elearning.backend.auth.exception.AuthBadRequestException;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.entity.UserStatus;
import org.elearning.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@org.springframework.test.context.ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class AccountActivationServiceTest {

    @Mock
    private ActivationTokenService activationTokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountActivationService accountActivationService;

    @Test
    void setPassword_success_activatesUserAndSetsPassword() {
        SetPasswordRequest request = new SetPasswordRequest();
        request.setToken("raw-token");
        request.setPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        User user = new User();
        user.setStatus(UserStatus.PENDING);

        when(activationTokenService.validateAndConsumeToken("raw-token")).thenReturn(user);
        when(passwordEncoder.encode("newPassword123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        ResetPasswordResponse response = accountActivationService.setPassword(request);

        assertThat(response.getMessage()).isEqualTo("Account activated successfully.");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void setPassword_passwordsDoNotMatch_throwsException() {
        SetPasswordRequest request = new SetPasswordRequest();
        request.setToken("raw-token");
        request.setPassword("newPassword123");
        request.setConfirmPassword("differentPassword");

        assertThatThrownBy(() -> accountActivationService.setPassword(request))
                .isInstanceOf(AuthBadRequestException.class)
                .hasMessage("Passwords do not match.");

        verify(activationTokenService, never()).validateAndConsumeToken(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void setPassword_invalidToken_throwsException() {
        SetPasswordRequest request = new SetPasswordRequest();
        request.setToken("invalid-token");
        request.setPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        when(activationTokenService.validateAndConsumeToken("invalid-token"))
                .thenThrow(new AuthBadRequestException("Invalid activation token."));

        assertThatThrownBy(() -> accountActivationService.setPassword(request))
                .isInstanceOf(AuthBadRequestException.class)
                .hasMessage("Invalid activation token.");

        verify(userRepository, never()).save(any());
    }

    @Test
    void setPassword_expiredToken_throwsException() {
        SetPasswordRequest request = new SetPasswordRequest();
        request.setToken("expired-token");
        request.setPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        when(activationTokenService.validateAndConsumeToken("expired-token"))
                .thenThrow(new AuthBadRequestException("Activation token has expired."));

        assertThatThrownBy(() -> accountActivationService.setPassword(request))
                .isInstanceOf(AuthBadRequestException.class)
                .hasMessage("Activation token has expired.");

        verify(userRepository, never()).save(any());
    }

    @Test
    void setPassword_alreadyUsedToken_throwsException() {
        SetPasswordRequest request = new SetPasswordRequest();
        request.setToken("used-token");
        request.setPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        when(activationTokenService.validateAndConsumeToken("used-token"))
                .thenThrow(new AuthBadRequestException("This activation token has already been used."));

        assertThatThrownBy(() -> accountActivationService.setPassword(request))
                .isInstanceOf(AuthBadRequestException.class)
                .hasMessage("This activation token has already been used.");

        verify(userRepository, never()).save(any());
    }

    @Test
    void setPassword_encodesPasswordBeforeSaving() {
        SetPasswordRequest request = new SetPasswordRequest();
        request.setToken("raw-token");
        request.setPassword("plaintext");
        request.setConfirmPassword("plaintext");

        User user = new User();
        user.setStatus(UserStatus.PENDING);

        when(activationTokenService.validateAndConsumeToken("raw-token")).thenReturn(user);
        when(passwordEncoder.encode("plaintext")).thenReturn("$2a$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        accountActivationService.setPassword(request);

        verify(passwordEncoder).encode("plaintext");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$encoded");
        assertThat(captor.getValue().getPasswordHash()).isNotEqualTo("plaintext");
    }
}
