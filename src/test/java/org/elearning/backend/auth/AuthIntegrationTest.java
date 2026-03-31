package org.elearning.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elearning.backend.auth.controller.AuthController;
import org.elearning.backend.auth.dto.request.LoginRequest;
import org.elearning.backend.auth.dto.request.RegisterRequest;
import org.elearning.backend.auth.dto.response.AuthResponse;
import org.elearning.backend.auth.dto.response.UserDataResponse;
import org.elearning.backend.auth.service.AuthService;
import org.elearning.backend.common.exception.DuplicateResourceException;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthIntegrationTest {

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

    // REGISTER

    @Test
    void register_organizationAdmin_success_returns200() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("admin@test.com");
        request.setPassword("parola123");
        request.setFirstName("Ion");
        request.setLastName("Popescu");
        request.setOrganizationName("Scoala Ion");
        request.setCountry("Romania");
        request.setCity("Bucharest");
        request.setOrganizationType("School");

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(new AuthResponse("User registered successfully", "access-token", "refresh-token", null));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully"));
    }

    @Test
    void register_organizationAdmin_delegatesToAuthService() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("admin@test.com");
        request.setPassword("parola123");
        request.setFirstName("Ion");
        request.setLastName("Popescu");
        request.setOrganizationName("Scoala Ion");
        request.setCountry("Romania");
        request.setCity("Bucharest");
        request.setOrganizationType("School");

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(new AuthResponse("User registered successfully", "access-token", "refresh-token", null));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("dup@test.com");
        request.setPassword("parola123");
        request.setFirstName("Ion");
        request.setLastName("Popescu");
        request.setOrganizationName("Scoala Ion");
        request.setCountry("Romania");
        request.setCity("Bucharest");
        request.setOrganizationType("School");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException("Email already in use: dup@test.com"));

        RegisterRequest request2 = new RegisterRequest();
        request2.setEmail("dup@test.com");
        request2.setPassword("parola123");
        request2.setFirstName("Ion");
        request2.setLastName("Popescu");
        request2.setOrganizationName("Scoala Ion 2");
        request2.setCountry("Romania");
        request2.setCity("Bucharest");
        request2.setOrganizationType("School");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_duplicateOrgName_returns409() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("admin@test.com");
        request.setPassword("parola123");
        request.setFirstName("Ion");
        request.setLastName("Popescu");
        request.setOrganizationName("Scoala Ion");
        request.setCountry("Romania");
        request.setCity("Bucharest");
        request.setOrganizationType("School");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException("Organization name already exists: Scoala Ion"));

        RegisterRequest request2 = new RegisterRequest();
        request2.setEmail("admin2@test.com");
        request2.setPassword("parola123");
        request2.setFirstName("Ana");
        request2.setLastName("Pop");
        request2.setOrganizationName("Scoala Ion");
        request2.setCountry("Romania");
        request2.setCity("Bucharest");
        request2.setOrganizationType("School");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_missingPassword_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@test.com");
        request.setFirstName("Ion");
        request.setLastName("Popescu");
        request.setOrganizationName("Scoala Ion");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // LOGIN

    @Test
    void login_success_returns200() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@test.com");
        loginRequest.setPassword("parola123");

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("Login successful", "access-token", "refresh-token", null));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@test.com");
        loginRequest.setPassword("parolaGresita");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new org.elearning.backend.common.exception.InvalidCredentials("Invalid credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_emailNotFound_returns401() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("inexistent@test.com");
        loginRequest.setPassword("parola123");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new org.elearning.backend.common.exception.InvalidCredentials("Invalid credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_missingEmail_returns400() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPassword("parola123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_success_returnsUserData() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockLoginRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("test@test.com"))
                .andExpect(jsonPath("$.user.firstName").value("Test"))
                .andExpect(jsonPath("$.user.lastName").value("User"))
                .andExpect(jsonPath("$.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.user.status").value("ACTIVE"))
                .andExpect(jsonPath("$.user.organizationName").value("Test Organization"));
    }
}
