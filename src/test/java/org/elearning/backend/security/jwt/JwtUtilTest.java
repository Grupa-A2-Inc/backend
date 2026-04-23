package org.elearning.backend.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.elearning.backend.role.entity.RoleName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String TEST_SECRET = "test-secret-key-that-is-long-enough-for-hmac";
    private static final UUID TEST_ID = UUID.randomUUID();
    private static final RoleName TEST_ROLE = RoleName.ORGANIZATION_ADMIN;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        jwtUtil.init(); // simulate @PostConstruct
    }

    // -------------------------
    // ACCESS TOKEN TESTS
    // -------------------------

    @Test
    void generateAccessToken_shouldReturnNonNullToken() {
        String token = jwtUtil.generateAccessToken(TEST_ID, TEST_ROLE);
        assertThat(token).isNotNull();
    }

    @Test
    void generateAccessToken_shouldHaveThreeParts() {
        String token = jwtUtil.generateAccessToken(TEST_ID, TEST_ROLE);
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void validateAccessToken_shouldReturnCorrectId() {
        String token = jwtUtil.generateAccessToken(TEST_ID, TEST_ROLE);
        Claims claims = jwtUtil.validateToken(token);
        assertThat(claims.getSubject()).isEqualTo(TEST_ID.toString());
    }

    @Test
    void validateAccessToken_shouldReturnCorrectRole() {
        String token = jwtUtil.generateAccessToken(TEST_ID, TEST_ROLE);
        Claims claims = jwtUtil.validateToken(token);
        assertThat(claims.get("role", String.class)).isEqualTo(TEST_ROLE.name());
    }

    @Test
    void validateAccessToken_withTamperedToken_shouldThrowException() {
        String token = jwtUtil.generateAccessToken(TEST_ID, TEST_ROLE);
        String tampered = token + "tampered";

        assertThatThrownBy(() -> jwtUtil.validateToken(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void validateAccessToken_withRandomString_shouldThrowException() {
        assertThatThrownBy(() -> jwtUtil.validateToken("not.a.token"))
                .isInstanceOf(JwtException.class);
    }

    // -------------------------
    // REFRESH TOKEN TESTS
    // -------------------------

    @Test
    void generateRefreshToken_shouldReturnNonNullToken() {
        String token = jwtUtil.generateRefreshToken(TEST_ID);
        assertThat(token).isNotNull();
    }

    @Test
    void validateRefreshToken_shouldReturnCorrectId() {
        String token = jwtUtil.generateRefreshToken(TEST_ID);
        Claims claims = jwtUtil.validateToken(token);
        assertThat(claims.getSubject()).isEqualTo(TEST_ID.toString());
    }

    @Test
    void validateRefreshToken_withTamperedToken_shouldThrowException() {
        String token = jwtUtil.generateRefreshToken(TEST_ID);
        String tampered = token + "tampered";

        assertThatThrownBy(() -> jwtUtil.validateToken(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void accessAndRefreshTokens_shouldBeDifferent() {
        String accessToken = jwtUtil.generateAccessToken(TEST_ID, TEST_ROLE);
        String refreshToken = jwtUtil.generateRefreshToken(TEST_ID);

        assertThat(accessToken).isNotEqualTo(refreshToken);
    }

    // -------------------------
    // EXTRACT HELPER TESTS
    // -------------------------

    @Test
    void extractId_shouldReturnCorrectId() {
        String token = jwtUtil.generateAccessToken(TEST_ID, TEST_ROLE);
        assertThat(jwtUtil.extractId(token)).isEqualTo(TEST_ID);
    }

    @Test
    void extractRole_shouldReturnCorrectRole() {
        String token = jwtUtil.generateAccessToken(TEST_ID, TEST_ROLE);
        assertThat(jwtUtil.extractRole(token)).isEqualTo(TEST_ROLE);
    }

    @Test
    void extractExpiration_shouldReturnFutureExpiration() {
        String token = jwtUtil.generateAccessToken(TEST_ID, TEST_ROLE);

        assertThat(jwtUtil.extractExpiration(token)).isAfter(java.time.LocalDateTime.now());
    }
}
