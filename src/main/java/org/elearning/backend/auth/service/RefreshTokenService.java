package org.elearning.backend.auth.service;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.auth.entity.RefreshToken;
import org.elearning.backend.auth.exception.InvalidCredentialsException;
import org.elearning.backend.auth.repository.RefreshTokenRepository;
import org.elearning.backend.security.jwt.JwtUtil;
import org.elearning.backend.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.function.ThrowingSupplier;
import java.util.HexFormat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public void storeRefreshToken(User user, String rawToken) {
        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(hashToken(rawToken));
        entity.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(entity);
    }

    @Transactional
    public User validateAndGetUser(String rawToken) {
        String tokenHash = hashToken(rawToken);

        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

        if (stored.getRevokedAt() != null) {
            throw new InvalidCredentialsException("Refresh token has been revoked");
        }

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidCredentialsException("Refresh token has expired");
        }

        jwtUtil.validateToken(rawToken);

        return stored.getUser();
    }

    @Transactional
    public void revokeForToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(token -> {
                    token.setRevokedAt(LocalDateTime.now());
                    refreshTokenRepository.save(token);
                });
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