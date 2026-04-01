package org.elearning.backend.security;

import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.elearning.backend.security.auth.CustomUserDetailsService;
import org.elearning.backend.security.config.SecurityConfig;
import org.elearning.backend.security.controller.ProtectedController;
import org.elearning.backend.security.handler.JwtAuthenticationEntryPoint;
import org.elearning.backend.security.jwt.JwtAuthenticationFilter;
import org.elearning.backend.security.jwt.JwtUtil;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.entity.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProtectedController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class})
class ProtectedEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private CustomUserDetails createUserDetails() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@test.com");
        user.setPasswordHash("hashed");
        user.setRole(new Role(RoleName.ADMIN));
        user.setStatus(UserStatus.ACTIVE);
        return new CustomUserDetails(user);
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/protected/ping"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withValidToken_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        CustomUserDetails userDetails = createUserDetails();

        when(jwtUtil.extractId("valid-token")).thenReturn(userId);
        when(customUserDetailsService.loadUserById(userId)).thenReturn(userDetails);

        mockMvc.perform(get("/api/protected/ping")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }

    @Test
    void protectedEndpoint_withInvalidToken_returns401() throws Exception {
        when(jwtUtil.extractId("invalid-token"))
                .thenThrow(new RuntimeException("Invalid token"));

        mockMvc.perform(get("/api/protected/ping")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withExpiredToken_returns401() throws Exception {
        UUID userId = UUID.randomUUID();

        when(jwtUtil.extractId("expired-token")).thenReturn(userId);
        when(customUserDetailsService.loadUserById(userId))
                .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found"));

        mockMvc.perform(get("/api/protected/ping")
                        .header("Authorization", "Bearer expired-token"))
                .andExpect(status().isUnauthorized());
    }
}