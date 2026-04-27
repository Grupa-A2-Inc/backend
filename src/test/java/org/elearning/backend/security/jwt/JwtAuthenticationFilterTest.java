package org.elearning.backend.security.jwt;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.ServletException;
import org.elearning.backend.auth.service.TokenBlackListService;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.elearning.backend.security.auth.CustomUserDetailsService;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.entity.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private final StubJwtUtil jwtUtil = new StubJwtUtil();
    private final StubCustomUserDetailsService customUserDetailsService = new StubCustomUserDetailsService();
    private final StubTokenBlackListService tokenBlacklistService = new StubTokenBlackListService();
    private final JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtUtil, customUserDetailsService, tokenBlacklistService);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotFilter_returnsTrueForAuthEndpoints() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/auth/login");

        assertThat(jwtAuthenticationFilter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilter_returnsFalseForProtectedEndpoints() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/users");

        assertThat(jwtAuthenticationFilter.shouldNotFilter(request)).isFalse();
    }

    @Test
    void doFilterInternal_withoutAuthorizationHeader_leavesSecurityContextEmpty() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TrackingFilterChain filterChain = new TrackingFilterChain();

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.wasInvoked()).isTrue();
    }

    @Test
    void doFilterInternal_withNonBearerAuthorizationHeader_leavesSecurityContextEmpty() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/users");
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
        request.setServletPath("/api/v1/users");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TrackingFilterChain filterChain = new TrackingFilterChain();

        jwtUtil.userId = UUID.randomUUID();
        customUserDetailsService.user = makeUser(jwtUtil.userId, "test@test.com", RoleName.ADMIN);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(CustomUserDetails.class);
        assertThat(((CustomUserDetails) authentication.getPrincipal()).getUserId()).isEqualTo(jwtUtil.userId);
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
        assertThat(filterChain.wasInvoked()).isTrue();
    }

    @Test
    void doFilterInternal_withInvalidBearerToken_clearsExistingAuthentication() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/users");
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

    @Test
    void doFilterInternal_withMissingUser_clearsExistingAuthentication() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/users");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TrackingFilterChain filterChain = new TrackingFilterChain();

        jwtUtil.userId = UUID.randomUUID();
        customUserDetailsService.throwUserNotFound = true;

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.wasInvoked()).isTrue();
    }

    @Test
    void doFilterInternal_withRevokedToken_leavesSecurityContextEmpty() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/users");
        request.addHeader("Authorization", "Bearer revoked-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TrackingFilterChain filterChain = new TrackingFilterChain();

        tokenBlacklistService.revoked = true;

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.wasInvoked()).isTrue();
    }

    @Test
    void doFilterInternal_withValidNonRevokedToken_setsAuthentication() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/users");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TrackingFilterChain filterChain = new TrackingFilterChain();

        tokenBlacklistService.revoked = false;
        jwtUtil.userId = UUID.randomUUID();
        customUserDetailsService.user = makeUser(jwtUtil.userId, "test@test.com", RoleName.ADMIN);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(filterChain.wasInvoked()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"INACTIVE", "BLOCKED", "PENDING"})
    void doFilterInternal_withValidTokenButNonActiveUser_leavesSecurityContextEmpty(UserStatus status)
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/users");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TrackingFilterChain filterChain = new TrackingFilterChain();

        tokenBlacklistService.revoked = false;
        jwtUtil.userId = UUID.randomUUID();
        User user = makeUser(jwtUtil.userId, status.name().toLowerCase() + "@test.com", RoleName.ADMIN);
        user.setStatus(status);
        customUserDetailsService.user = user;

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.wasInvoked()).isTrue();
    }

    private User makeUser(UUID id, String email, RoleName roleName) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPasswordHash("hashed");
        user.setRole(new Role(roleName));
        user.setStatus(UserStatus.ACTIVE);
        return user;
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

        private UUID userId;
        private boolean throwJwtException;

        @Override
        public UUID extractId(String token) {
            if (throwJwtException) {
                throw new JwtException("bad token");
            }
            return userId;
        }

    }

    private static final class StubCustomUserDetailsService extends CustomUserDetailsService {

        private User user;
        private boolean throwUserNotFound;

        private StubCustomUserDetailsService() {
            super(null);
        }

        @Override
        public CustomUserDetails loadUserById(UUID userId) {
            if (throwUserNotFound) {
                throw new org.springframework.security.core.userdetails.UsernameNotFoundException("missing");
            }
            return new CustomUserDetails(user);
        }
    }

    private static final class StubTokenBlackListService extends TokenBlackListService {

        private boolean revoked;

        private StubTokenBlackListService() {
            super(null);
        }

        @Override
        public boolean isRevoked(String rawToken) {
            return revoked;
        }
    }
}
