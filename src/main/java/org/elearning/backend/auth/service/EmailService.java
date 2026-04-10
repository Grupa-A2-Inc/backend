package org.elearning.backend.auth.service;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${app.mail.from}")
    private String from;

    public void sendPasswordResetEmail(String email, String rawToken) {

        SimpleMailMessage msg = new SimpleMailMessage();

        msg.setFrom(from);
        msg.setTo(email);
        msg.setSubject("Reset password");
        msg.setText(
                "Use this reset token to change your password:\n" + "   "
                        + rawToken
                        + "\n\nThis token expires in 5 minutes.");

        javaMailSender.send(msg);
    }

}
