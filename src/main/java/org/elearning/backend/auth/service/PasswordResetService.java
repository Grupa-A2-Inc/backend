package org.elearning.backend.auth.service;


import lombok.RequiredArgsConstructor;
import org.elearning.backend.auth.dto.request.ForgotPasswordRequest;
import org.elearning.backend.auth.dto.request.ResetPasswordRequest;
import org.elearning.backend.auth.dto.response.ResetPasswordResponse;
import org.elearning.backend.auth.entity.PasswordResetToken;
import org.elearning.backend.auth.repository.PasswordResetTokenRepository;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.elearning.backend.auth.exception.AuthBadRequestException;
import org.springframework.util.function.ThrowingSupplier;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int PASSWORD_RESET_TOKEN_EXPIRATION_IN_MINUTES = 10;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailService emailService;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ResetPasswordResponse forgotPassword(ForgotPasswordRequest request) {

        String userEmail = request.getEmail();

        Optional<User> optionalUser = userRepository.findByEmail(userEmail);
        if (optionalUser.isEmpty()) {
            return new ResetPasswordResponse("If an account exists for this email, a reset token has been sent.");
        }

        User user = optionalUser.get();

        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(tokenHash);
        resetToken.setCreatedAt(LocalDateTime.now());
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(PASSWORD_RESET_TOKEN_EXPIRATION_IN_MINUTES));
        resetToken.setUsedAt(null);

        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user.getEmail(), rawToken);

        return new ResetPasswordResponse("If an account exists for this email, a reset token has been sent.");
    }

    @Transactional
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {

        String token = request.getToken();
        String newPassword = request.getNewPassword();
        String confirmPassword = request.getConfirmPassword();

        if (!newPassword.equals(confirmPassword)) {
            return new ResetPasswordResponse("Confirmed password does not match new password.");
        }

        String tokenHash = hashToken(token);

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AuthBadRequestException("Invalid or expired reset token."));

        if (resetToken.getUsedAt() != null) {
            throw new AuthBadRequestException("This reset token has already been used.");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AuthBadRequestException("Reset token has expired.");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);

        return new ResetPasswordResponse("Password changed successfully.");

    }

    private String hashToken(String token) {
        MessageDigest digest = createSha256Digest();
        byte[] hashedBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hashedBytes);
    }

    private MessageDigest createSha256Digest() {
        return ThrowingSupplier.of(() -> MessageDigest.getInstance("SHA-256")).get();
    }


}
