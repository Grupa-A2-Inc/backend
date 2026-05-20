package org.elearning.backend.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.elearning.backend.role.entity.RoleName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    @Value("${JWT_SECRET}")
    private String secret;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    private SecretKey getKey() {
        return key;
    }

    private static final int ACCESS_TOKEN_VALIDITY_MILLISECONDS = 30*60*1000; // 20 min

    private static final long REFRESH_TOKEN_VALIDITY_MILLISECONDS = 7*24*60*60*1000L; // 7 days

    public String generateAccessToken(UUID id, RoleName role) {
        return Jwts.builder()
                .subject(id.toString())
                .claim("role", role.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_VALIDITY_MILLISECONDS))
                .signWith(getKey())
                .compact();
    }

    public String generateRefreshToken(UUID id) {
        return Jwts.builder()
                .subject(id.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_VALIDITY_MILLISECONDS))
                .signWith(getKey())
                .compact();
    }

    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractId(String token) {
        return UUID.fromString(validateToken(token).getSubject());
    }

    public RoleName extractRole(String token) {
        String roleName = validateToken(token).get("role", String.class);
        return RoleName.valueOf(roleName);
    }

    public LocalDateTime extractExpiration(String token) {
        Date expiration = validateToken(token).getExpiration();
        return expiration.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
