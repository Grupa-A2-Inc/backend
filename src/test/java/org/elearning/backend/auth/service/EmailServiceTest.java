package org.elearning.backend.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@org.springframework.test.context.ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    void sendPasswordResetEmail_sendsExpectedMessage() {
        ReflectionTestUtils.setField(emailService, "from", "noreply@example.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "https://frontend-teal-five-57.vercel.app");

        emailService.sendPasswordResetEmail("user@example.com", "raw-reset-token");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();

        assertThat(message.getFrom()).isEqualTo("noreply@example.com");
        assertThat(message.getTo()).containsExactly("user@example.com");
        assertThat(message.getSubject()).isEqualTo("Reset password");
        assertThat(message.getText()).contains("Click the link below to reset your password:");
        assertThat(message.getText()).contains("https://frontend-teal-five-57.vercel.app/reset-password?token=raw-reset-token");
        assertThat(message.getText()).contains("This link expires in 60 minutes.");
        assertThat(message.getText()).contains("If you did not request a password reset, please ignore this email.");
    }

    @Test
    void sendActivationEmail_sendsExpectedMessage() {
        ReflectionTestUtils.setField(emailService, "from", "noreply@example.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:3000");

        emailService.sendActivationEmail("user@example.com", "Ion", "raw-activation-token");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();

        assertThat(message.getFrom()).isEqualTo("noreply@example.com");
        assertThat(message.getTo()).containsExactly("user@example.com");
        assertThat(message.getSubject()).isEqualTo("Activate your account");
        assertThat(message.getText()).contains("Ion");
        assertThat(message.getText()).contains("raw-activation-token");
        assertThat(message.getText()).contains("http://localhost:3000/set-password?token=raw-activation-token");
        assertThat(message.getText()).contains("60 minutes");
    }
}
