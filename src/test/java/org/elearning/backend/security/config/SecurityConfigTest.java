package org.elearning.backend.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elearning.backend.auth.service.TokenBlackListService;
import org.elearning.backend.security.auth.CustomUserDetailsService;
import org.elearning.backend.security.handler.JwtAccessDeniedHandler;
import org.elearning.backend.security.handler.JwtAuthenticationEntryPoint;
import org.elearning.backend.security.jwt.JwtAuthenticationFilter;
import org.elearning.backend.security.jwt.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockServletContext;
import jakarta.servlet.http.Cookie;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@org.springframework.test.context.ActiveProfiles("test")
class SecurityConfigTest {

    @Test
    void authEndpoints_areAccessibleWithoutAuthentication() throws Exception {
        try (AnnotationConfigWebApplicationContext context = createContext()) {
            MockMvc mockMvc = buildMockMvc(context);

            var result = mockMvc.perform(get("/api/v1/auth/ping"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getRequest().getSession(false)).isNull();
        }
    }

    @Test
    void nonAuthEndpoints_requireAuthentication() throws Exception {
        try (AnnotationConfigWebApplicationContext context = createContext()) {
            MockMvc mockMvc = buildMockMvc(context);

            var result = mockMvc.perform(get("/api/v1/secure/ping"))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            assertThat(result.getRequest().getSession(false)).isNull();
        }
    }

    @Test
    void methodSecurity_deniedRequest_returnsForbiddenJson() throws Exception {
        try (AnnotationConfigWebApplicationContext context = createContext()) {
            MockMvc mockMvc = buildMockMvc(context);

            mockMvc.perform(get("/api/v1/admin-only/ping").with(user("student").roles("STUDENT")))
                    .andExpect(status().isForbidden())
                    .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                            .isEqualTo("{\"message\":\"Access denied\"}"));
        }
    }

    @Test
    void refreshEndpoint_requiresCsrfToken() throws Exception {
        try (AnnotationConfigWebApplicationContext context = createContext()) {
            MockMvc mockMvc = buildMockMvc(context);

            mockMvc.perform(post("/api/v1/auth/refresh"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .cookie(new Cookie("refresh_token", "refresh-token"), new Cookie("XSRF-TOKEN", "csrf-token"))
                            .header("X-XSRF-TOKEN", "csrf-token"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void loginEndpoint_doesNotRequireCsrfToken() throws Exception {
        try (AnnotationConfigWebApplicationContext context = createContext()) {
            MockMvc mockMvc = buildMockMvc(context);

            var result = mockMvc.perform(post("/api/v1/auth/login"))
                    .andExpect(status().isOk())
                    .andReturn();

            String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
            assertThat(setCookieHeader).contains("XSRF-TOKEN=");
        }
    }

    @Test
    void csrfEndpoint_returnsRawTokenThatCanAuthorizeRefresh() throws Exception {
        try (AnnotationConfigWebApplicationContext context = createContext()) {
            MockMvc mockMvc = buildMockMvc(context);

            var csrfResult = mockMvc.perform(get("/api/v1/auth/csrf"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Set-Cookie", containsString("XSRF-TOKEN=")))
                    .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                    .andExpect(jsonPath("$.csrfToken").isNotEmpty())
                    .andReturn();

            String body = csrfResult.getResponse().getContentAsString();
            String csrfToken = new ObjectMapper().readTree(body).get("csrfToken").asText();
            Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");

            assertThat(csrfCookie).isNotNull();
            assertThat(csrfCookie.getValue()).isEqualTo(csrfToken);

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .cookie(new Cookie("refresh_token", "refresh-token"), csrfCookie)
                            .header("X-XSRF-TOKEN", csrfToken))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void corsConfigurationSource_allowsConfiguredOriginsMethodsAndHeaders() {
        SecurityConfig securityConfig = new SecurityConfig(
                new JwtAuthenticationFilter(
                        new NoopJwtUtil(),
                        new NoopCustomUserDetailsService(),
                        new NoopTokenBlackListService()
                ),
                new JwtAccessDeniedHandler(),
                new JwtAuthenticationEntryPoint()
        );

        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        var configuration = source.getCorsConfiguration(new org.springframework.mock.web.MockHttpServletRequest("OPTIONS", "/api/v1/secure/ping"));

        assertThat(configuration.getAllowedOrigins()).containsExactly(
                "http://localhost:3000",
                "https://frontend-teal-five-57.vercel.app",
                "https://frontend-z1g5f.vercel.app",
                "https://vibesvibesonlyvibes.vercel.app"
        );
        assertThat(configuration.getAllowedMethods()).containsExactly("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
        assertThat(configuration.getAllowedHeaders()).containsExactly(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "X-XSRF-TOKEN"
        );
    }

    private AnnotationConfigWebApplicationContext createContext() {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestWebSecurityConfig.class, SecurityConfig.class, SecurityBeansConfig.class);
        context.refresh();
        return context;
    }

    private MockMvc buildMockMvc(AnnotationConfigWebApplicationContext context) {
        DelegatingFilterProxy securityFilterChain = new DelegatingFilterProxy("springSecurityFilterChain", context);

        return MockMvcBuilders.webAppContextSetup(context)
                .addFilters(securityFilterChain)
                .build();
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import(TestSecurityController.class)
    static class TestWebSecurityConfig {

        @Bean
        CustomUserDetailsService customUserDetailsService() {
            return new NoopCustomUserDetailsService();
        }

        @Bean
        TokenBlackListService tokenBlacklistService() {
            return new NoopTokenBlackListService();
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(
                CustomUserDetailsService customUserDetailsService,
                TokenBlackListService tokenBlacklistService
        ) {
            return new JwtAuthenticationFilter(new NoopJwtUtil(), customUserDetailsService, tokenBlacklistService);
        }

        @Bean
        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
            return new JwtAuthenticationEntryPoint();
        }

        @Bean
        JwtAccessDeniedHandler jwtAccessDeniedHandler() {
            return new JwtAccessDeniedHandler();
        }
    }

    @RestController
    static class TestSecurityController {

        @GetMapping("/api/v1/auth/ping")
        public ResponseEntity<String> authPing() {
            return ResponseEntity.ok("auth-ok");
        }

        @org.springframework.web.bind.annotation.PostMapping("/api/v1/auth/login")
        public ResponseEntity<String> authLogin() {
            return ResponseEntity.ok("login-ok");
        }

        @GetMapping("/api/v1/auth/csrf")
        public ResponseEntity<Map<String, String>> csrf(
                @RequestAttribute(CsrfTokenAttributes.RAW_CSRF_TOKEN) CsrfToken csrfToken) {
            return ResponseEntity.ok(Map.of(
                    "csrfToken", csrfToken.getToken(),
                    "headerName", csrfToken.getHeaderName()
            ));
        }

        @org.springframework.web.bind.annotation.PostMapping("/api/v1/auth/refresh")
        public ResponseEntity<String> authRefresh() {
            return ResponseEntity.ok("refresh-ok");
        }

        @GetMapping("/api/v1/secure/ping")
        public ResponseEntity<String> securePing() {
            return ResponseEntity.ok("secure-ok");
        }

        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/api/v1/admin-only/ping")
        public ResponseEntity<String> adminPing() {
            return ResponseEntity.ok("admin-ok");
        }
    }

    static class NoopJwtUtil extends JwtUtil {

        @Override
        public UUID extractId(String token) {
            throw new UnsupportedOperationException("JWT parsing is not used in this MVC config test");
        }

        @Override
        public org.elearning.backend.role.entity.RoleName extractRole(String token) {
            throw new UnsupportedOperationException("JWT parsing is not used in this MVC config test");
        }
    }

    static class NoopCustomUserDetailsService extends CustomUserDetailsService {

        NoopCustomUserDetailsService() {
            super(null);
        }
    }

    static class NoopTokenBlackListService extends TokenBlackListService {

        NoopTokenBlackListService() {
            super(null);
        }

        @Override
        public boolean isRevoked(String rawToken) {
            return false;
        }
    }
}
