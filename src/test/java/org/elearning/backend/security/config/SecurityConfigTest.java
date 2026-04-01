package org.elearning.backend.security.config;

import org.elearning.backend.security.auth.CustomUserDetailsService;
import org.elearning.backend.security.jwt.JwtAuthenticationFilter;
import org.elearning.backend.security.jwt.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityConfigTest {

    @Test
    void authEndpoints_areAccessibleWithoutAuthentication() throws Exception {
        try (AnnotationConfigWebApplicationContext context = createContext()) {
            MockMvc mockMvc = buildMockMvc(context);

            var result = mockMvc.perform(get("/api/auth/ping"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(result.getRequest().getSession(false)).isNull();
        }
    }

    @Test
    void nonAuthEndpoints_requireAuthentication() throws Exception {
        try (AnnotationConfigWebApplicationContext context = createContext()) {
            MockMvc mockMvc = buildMockMvc(context);

            var result = mockMvc.perform(get("/api/secure/ping"))
                    .andExpect(status().isForbidden())
                    .andReturn();

            assertThat(result.getRequest().getSession(false)).isNull();
        }
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
        JwtAuthenticationFilter jwtAuthenticationFilter(CustomUserDetailsService customUserDetailsService) {
            return new JwtAuthenticationFilter(new NoopJwtUtil(), customUserDetailsService);
        }
    }

    @RestController
    static class TestSecurityController {

        @GetMapping("/api/auth/ping")
        public ResponseEntity<String> authPing() {
            return ResponseEntity.ok("auth-ok");
        }

        @GetMapping("/api/secure/ping")
        public ResponseEntity<String> securePing() {
            return ResponseEntity.ok("secure-ok");
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
}
