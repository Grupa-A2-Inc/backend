package org.elearning.backend.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.frontend.url}")
    private String frontendUrl;

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

    public void sendActivationEmail(String email, String firstName, String rawToken) {
        SimpleMailMessage msg = new SimpleMailMessage();

        msg.setFrom(from);
        msg.setTo(email);
        msg.setSubject("Activate your account");
        msg.setText(
                "Hello " + firstName + ",\n\n" +
                        "An account has been created for you.\n" +
                        "Click the link below to set your password and activate your account:\n\n" +
                        "   " + frontendUrl + "/set-password?token=" + rawToken + "\n\n" +
                        "This link expires in 60 minutes.\n\n" +
                        "If you did not expect this email, please ignore it.");

        javaMailSender.send(msg);
    }

}
