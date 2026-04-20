package org.elearning.backend.auth.service;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.auth.entity.ActivationToken;
import org.elearning.backend.auth.exception.AuthBadRequestException;
import org.elearning.backend.auth.repository.ActivationTokenRepository;
import org.elearning.backend.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.function.ThrowingSupplier;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class ActivationTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long EXPIRATION_MINUTES = 60;

    private final ActivationTokenRepository activationTokenRepository;

    @Transactional
    public String generateActivationToken(User user) {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        String tokenHash = hashToken(rawToken);

        ActivationToken token = new ActivationToken();
        token.setUser(user);
        token.setTokenHash(tokenHash);
        token.setCreatedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES));
        token.setUsedAt(null);

        activationTokenRepository.save(token);

        return rawToken; // raw-ul se trimite prin email, hash-ul ramane in DB
    }

    @Transactional
    public User validateAndConsumeToken(String rawToken) {
        String tokenHash = hashToken(rawToken);

        ActivationToken token = activationTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AuthBadRequestException("Invalid activation token."));

        if (token.getUsedAt() != null) {
            throw new AuthBadRequestException("This activation token has already been used.");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AuthBadRequestException("Activation token has expired.");
        }

        token.setUsedAt(LocalDateTime.now());
        activationTokenRepository.save(token);

        return token.getUser();
    }

    private String hashToken(String token) {
        MessageDigest digest = ThrowingSupplier.of(() -> MessageDigest.getInstance("SHA-256")).get();
        byte[] hashedBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hashedBytes);
    }
}