package org.elearning.backend.security.jwt;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.ServletException;
import org.elearning.backend.role.entity.RoleName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private final StubJwtUtil jwtUtil = new StubJwtUtil();
    private final JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtUtil);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotFilter_returnsTrueForAuthEndpoints() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/auth/login");

        assertThat(jwtAuthenticationFilter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilter_returnsFalseForProtectedEndpoints() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/users");

        assertThat(jwtAuthenticationFilter.shouldNotFilter(request)).isFalse();
    }

    @Test
    void doFilterInternal_withoutAuthorizationHeader_leavesSecurityContextEmpty() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TrackingFilterChain filterChain = new TrackingFilterChain();

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.wasInvoked()).isTrue();
    }

    @Test
    void doFilterInternal_withNonBearerAuthorizationHeader_leavesSecurityContextEmpty() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/users");
        request.addHeader("Authorization", "Basic abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TrackingFilterChain filterChain = new TrackingFilterChain();

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.wasInvoked()).isTrue();
    }

    @Test
    void doFilterInternal_withValidBearerToken_setsAuthentication() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/users");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TrackingFilterChain filterChain = new TrackingFilterChain();

        jwtUtil.username = "user@test.com";
        jwtUtil.role = RoleName.ADMIN;

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo("user@test.com");
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
        assertThat(filterChain.wasInvoked()).isTrue();
    }

    @Test
    void doFilterInternal_withInvalidBearerToken_clearsExistingAuthentication() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/users");
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TrackingFilterChain filterChain = new TrackingFilterChain();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "existing-user",
                        null
                )
        );
        jwtUtil.throwJwtException = true;

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.wasInvoked()).isTrue();
    }

    private static final class TrackingFilterChain implements jakarta.servlet.FilterChain {

        private boolean invoked;

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
            this.invoked = true;
        }

        boolean wasInvoked() {
            return invoked;
        }
    }

    private static final class StubJwtUtil extends JwtUtil {

        private String username;
        private RoleName role;
        private boolean throwJwtException;

        @Override
        public String extractUsername(String token) {
            if (throwJwtException) {
                throw new JwtException("bad token");
            }
            return username;
        }

        @Override
        public RoleName extractRole(String token) {
            if (throwJwtException) {
                throw new JwtException("bad token");
            }
            return role;
        }
    }
}
