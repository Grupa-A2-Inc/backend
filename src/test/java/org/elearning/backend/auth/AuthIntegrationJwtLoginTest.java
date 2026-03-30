package org.elearning.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elearning.backend.auth.controller.AuthController;
import org.elearning.backend.auth.dto.request.LoginRequest;
import org.elearning.backend.auth.dto.response.AuthResponse;
import org.elearning.backend.auth.dto.response.UserDataResponse;
import org.elearning.backend.auth.service.AuthService;
import org.elearning.backend.common.exception.GlobalExceptionHandler;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.jwt.JwtAuthenticationFilter;
import org.elearning.backend.user.entity.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthIntegrationJwtLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private AuthResponse mockAuthResponse() {
        UserDataResponse mockUser = new UserDataResponse(
                UUID.randomUUID(),
                "Test",
                "User",
                "test@test.com",
                RoleName.STUDENT,
                UserStatus.ACTIVE,
                "Test Organization"
        );

        return new AuthResponse(
                "Login successful",
                "header.payload.signature",
                "refresh-token",
                mockUser
        );
    }

    private LoginRequest mockLoginRequest() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@test.com");
        loginRequest.setPassword("parola123");
        return loginRequest;
    }

    @Test
    void login_success_returnsAccessToken() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockLoginRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void login_success_returnsRefreshToken() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockLoginRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void login_success_accessTokenIsValidJwtFormat() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse());

        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockLoginRequest())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = objectMapper.readTree(response).get("accessToken").asText();
        assertThat(accessToken.split("\\.")).hasSize(3);
    }
}