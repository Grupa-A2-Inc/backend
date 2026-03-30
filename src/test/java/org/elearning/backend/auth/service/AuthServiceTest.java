package org.elearning.backend.auth.service;

import org.elearning.backend.auth.dto.response.AuthResponse;
import org.elearning.backend.auth.dto.request.RegisterRequest;
import org.elearning.backend.auth.dto.request.LoginRequest;
import org.elearning.backend.common.exception.DuplicateResourceException;
import org.elearning.backend.common.exception.InvalidCredentials;
import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.role.repository.RoleRepository;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private AuthService authService;

    // REGISTER

    @Test
    void register_emailAlreadyExists_throwsException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existent@test.com");
        request.setPassword("parola123");
        request.setFirstName("Ion");
        request.setLastName("Popescu");
        request.setOrganizationName("Scoala Ion");

        when(userRepository.existsByEmail("existent@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Email already in use: existent@test.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_passwordIsHashed() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@test.com");
        request.setPassword("parola123");
        request.setFirstName("Ion");
        request.setLastName("Popescu");
        request.setOrganizationName("Scoala Ion");

        Role role = new Role(RoleName.ORGANIZATION_ADMIN);
        User savedUser = new User();

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(roleRepository.findByName(any())).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("parola123")).thenReturn("hashed_parola");
        when(organizationRepository.existsByName(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        authService.register(request);

        verify(passwordEncoder, times(1)).encode("parola123");
    }

    // teste noi pentru organizatie

    @Test
    void register_organizationAdmin_createsOrganization() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("admin@test.com");
        request.setPassword("parola123");
        request.setFirstName("Ion");
        request.setLastName("Popescu");
        request.setOrganizationName("Scoala Ion");

        Role role = new Role(RoleName.ORGANIZATION_ADMIN);
        User savedUser = new User();

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(roleRepository.findByName(RoleName.ORGANIZATION_ADMIN)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(organizationRepository.existsByName("Scoala Ion")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        AuthResponse response = authService.register(request);

        assertThat(response.getMessage()).isEqualTo("User registered successfully");
        verify(organizationRepository, times(1)).save(any(Organization.class));
    }

    @Test
    void register_organizationAdmin_duplicateOrgName_throwsException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("admin@test.com");
        request.setPassword("parola123");
        request.setFirstName("Ion");
        request.setLastName("Popescu");
        request.setOrganizationName("Scoala Ion");

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(organizationRepository.existsByName("Scoala Ion")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Organization name already exists: Scoala Ion");

        verify(organizationRepository, never()).save(any());
    }

    // LOGIN

    @Test
    void login_success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("parola123");

        User user = new User();
        user.setEmail("test@test.com");
        user.setPasswordHash("hashed_parola");

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("parola123", "hashed_parola")).thenReturn(true);

        AuthResponse response = authService.login(request);

        assertThat(response.getMessage()).isEqualTo("Login successful");
    }

    @Test
    void login_emailNotFound_throwsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("inexistent@test.com");
        request.setPassword("parola123");

        when(userRepository.findByEmail("inexistent@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentials.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void login_wrongPassword_throwsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("parolaGresita");

        User user = new User();
        user.setEmail("test@test.com");
        user.setPasswordHash("hashed_parola");

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("parolaGresita", "hashed_parola")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentials.class)
                .hasMessage("Invalid credentials");
    }
}
