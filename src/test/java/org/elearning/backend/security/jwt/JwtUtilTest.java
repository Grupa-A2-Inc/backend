package org.elearning.backend.security.jwt;

import org.elearning.backend.security.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.elearning.backend.role.entity.RoleName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String TEST_SECRET = "test-secret-key-that-is-long-enough-for-hmac";
    private static final String TEST_USERNAME = "test@test.com";
    private static final RoleName TEST_ROLE = RoleName.ORGANIZATION_ADMIN;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        // manually call @PostConstruct since Spring isn't running
        jwtUtil.init();
    }

    // -------------------------
    // ACCESS TOKEN TESTS
    // -------------------------

    @Test
    void generateAccessToken_shouldReturnNonNullToken() {
        String token = jwtUtil.generateAccessToken(TEST_USERNAME, TEST_ROLE);
        assertThat(token).isNotNull();
    }

    @Test
    void generateAccessToken_shouldHaveThreeParts() {
        String token = jwtUtil.generateAccessToken(TEST_USERNAME, TEST_ROLE);
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void validateAccessToken_shouldReturnCorrectUsername() {
        String token = jwtUtil.generateAccessToken(TEST_USERNAME, TEST_ROLE);
        Claims claims = jwtUtil.validateToken(token);
        assertThat(claims.getSubject()).isEqualTo(TEST_USERNAME);
    }

    @Test
    void validateAccessToken_shouldReturnCorrectRole() {
        String token = jwtUtil.generateAccessToken(TEST_USERNAME, TEST_ROLE);
        Claims claims = jwtUtil.validateToken(token);
        assertThat(claims.get("role", String.class)).isEqualTo(TEST_ROLE.name());
    }

    @Test
    void validateAccessToken_withTamperedToken_shouldThrowException() {
        String token = jwtUtil.generateAccessToken(TEST_USERNAME, TEST_ROLE);
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
        String token = jwtUtil.generateRefreshToken(TEST_USERNAME);
        assertThat(token).isNotNull();
    }

    @Test
    void validateRefreshToken_shouldReturnCorrectUsername() {
        String token = jwtUtil.generateRefreshToken(TEST_USERNAME);
        Claims claims = jwtUtil.validateToken(token);
        assertThat(claims.getSubject()).isEqualTo(TEST_USERNAME);
    }

    @Test
    void validateRefreshToken_withTamperedToken_shouldThrowException() {
        String token = jwtUtil.generateRefreshToken(TEST_USERNAME);
        String tampered = token + "tampered";
        assertThatThrownBy(() -> jwtUtil.validateToken(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void accessAndRefreshTokens_shouldBeDifferent() {
        String accessToken = jwtUtil.generateAccessToken(TEST_USERNAME, TEST_ROLE);
        String refreshToken = jwtUtil.generateRefreshToken(TEST_USERNAME);
        assertThat(accessToken).isNotEqualTo(refreshToken);
    }

    // -------------------------
    // EXTRACT HELPER TESTS
    // -------------------------

    @Test
    void extractUsername_shouldReturnCorrectUsername() {
        String token = jwtUtil.generateAccessToken(TEST_USERNAME, TEST_ROLE);
        assertThat(jwtUtil.extractUsername(token)).isEqualTo(TEST_USERNAME);
    }

    @Test
    void extractRole_shouldReturnCorrectRole() {
        String token = jwtUtil.generateAccessToken(TEST_USERNAME, TEST_ROLE);
        assertThat(jwtUtil.extractRole(token)).isEqualTo(TEST_ROLE);
    }
}