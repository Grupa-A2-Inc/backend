package org.elearning.backend.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elearning.backend.auth.entity.RevokedAccessToken;
import org.elearning.backend.auth.repository.RevokedAccessTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.function.ThrowingSupplier;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlackListService {

    private final RevokedAccessTokenRepository revokedAccessTokenRepository;

    @Transactional
    public void revokeAccessToken(String rawToken, LocalDateTime expiresAt) {
        String tokenHash = hashToken(rawToken);

        if (revokedAccessTokenRepository.existsByTokenHash(tokenHash)) {
            return;
        }

        RevokedAccessToken entity = new RevokedAccessToken();
        entity.setTokenHash(tokenHash);
        entity.setRevokedAt(LocalDateTime.now());
        entity.setExpiresAt(expiresAt);

        revokedAccessTokenRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String rawToken) {
        return revokedAccessTokenRepository.existsByTokenHash(hashToken(rawToken));
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        revokedAccessTokenRepository.deleteAllExpiredBefore(LocalDateTime.now());
        log.info("Blacklist cleanup: tokene expirate șterse.");
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