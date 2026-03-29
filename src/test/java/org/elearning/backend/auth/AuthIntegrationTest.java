package org.elearning.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elearning.backend.auth.dto.request.LoginRequest;
import org.elearning.backend.auth.dto.request.RegisterRequest;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @BeforeEach
    void cleanup() {
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    // REGISTER

    @Test
    void register_organizationAdmin_success_returns200() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("admin@test.com");
        request.setPassword("parola123");
        request.setFirstName("Ion");
        request.setLastName("Popescu");
        request.setRole(RoleName.ORGANIZATION_ADMIN);
        request.setOrganizationName("Scoala Ion");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully"));
    }

    @Test
    void register_organizationAdmin_createsUserAndOrganizationInDB() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("admin@test.com");
        request.setPassword("parola123");
        request.setFirstName("Ion");
        request.setLastName("Popescu");
        request.setRole(RoleName.ORGANIZATION_ADMIN);
        request.setOrganizationName("Scoala Ion");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        assertThat(userRepository.existsByEmail("admin@test.com")).isTrue();
        assertThat(organizationRepository.existsByName("Scoala Ion")).isTrue();
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("dup@test.com");
        request.setPassword("parola123");
        request.setFirstName("Ion");
        request.setLastName("Popescu");
        request.setRole(RoleName.ORGANIZATION_ADMIN);
        request.setOrganizationName("Scoala Ion");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        RegisterRequest request2 = new RegisterRequest();
        request2.setEmail("dup@test.com");
        request2.setPassword("parola123");
        request2.setFirstName("Ion");
        request2.setLastName("Popescu");
        request2.setRole(RoleName.ORGANIZATION_ADMIN);
        request2.setOrganizationName("Scoala Ion 2");

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
        request.setRole(RoleName.ORGANIZATION_ADMIN);
        request.setOrganizationName("Scoala Ion");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        RegisterRequest request2 = new RegisterRequest();
        request2.setEmail("admin2@test.com");
        request2.setPassword("parola123");
        request2.setFirstName("Ana");
        request2.setLastName("Pop");
        request2.setRole(RoleName.ORGANIZATION_ADMIN);
        request2.setOrganizationName("Scoala Ion");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_teacher_existingOrganization_success() throws Exception {
        RegisterRequest adminRequest = new RegisterRequest();
        adminRequest.setEmail("admin@test.com");
        adminRequest.setPassword("parola123");
        adminRequest.setFirstName("Ion");
        adminRequest.setLastName("Popescu");
        adminRequest.setRole(RoleName.ORGANIZATION_ADMIN);
        adminRequest.setOrganizationName("Scoala Ion");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminRequest)));

        RegisterRequest teacherRequest = new RegisterRequest();
        teacherRequest.setEmail("teacher@test.com");
        teacherRequest.setPassword("parola123");
        teacherRequest.setFirstName("Maria");
        teacherRequest.setLastName("Pop");
        teacherRequest.setRole(RoleName.TEACHER);
        teacherRequest.setOrganizationName("Scoala Ion");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(teacherRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void register_teacher_organizationNotFound_returns404() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("teacher@test.com");
        request.setPassword("parola123");
        request.setFirstName("Maria");
        request.setLastName("Pop");
        request.setRole(RoleName.TEACHER);
        request.setOrganizationName("Scoala Inexistenta");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("nu-sunt-email");
        request.setPassword("parola123");
        request.setFirstName("Ion");
        request.setLastName("Popescu");
        request.setRole(RoleName.ORGANIZATION_ADMIN);
        request.setOrganizationName("Scoala Ion");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_missingPassword_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@test.com");
        request.setFirstName("Ion");
        request.setLastName("Popescu");
        request.setRole(RoleName.ORGANIZATION_ADMIN);
        request.setOrganizationName("Scoala Ion");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // LOGIN

    @Test
    void login_success_returns200() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@test.com");
        registerRequest.setPassword("parola123");
        registerRequest.setFirstName("Ion");
        registerRequest.setLastName("Popescu");
        registerRequest.setRole(RoleName.ORGANIZATION_ADMIN);
        registerRequest.setOrganizationName("Scoala Ion");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@test.com");
        loginRequest.setPassword("parola123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@test.com");
        registerRequest.setPassword("parola123");
        registerRequest.setFirstName("Ion");
        registerRequest.setLastName("Popescu");
        registerRequest.setRole(RoleName.ORGANIZATION_ADMIN);
        registerRequest.setOrganizationName("Scoala Ion");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@test.com");
        loginRequest.setPassword("parolaGresita");

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
}