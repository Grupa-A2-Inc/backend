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
import org.elearning.backend.security.jwt.JwtUtil;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.entity.UserStatus;
import org.elearning.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User saveUserWithGeneratedId(User user) {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID());
        }
        return user;
    }

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

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(roleRepository.findByName(any())).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("parola123")).thenReturn("hashed_parola");
        when(organizationRepository.existsByName(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> saveUserWithGeneratedId(invocation.getArgument(0)));
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtUtil.generateAccessToken(any(UUID.class), eq(RoleName.ORGANIZATION_ADMIN))).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any(UUID.class))).thenReturn("refresh-token");

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

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(roleRepository.findByName(RoleName.ORGANIZATION_ADMIN)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(organizationRepository.existsByName("Scoala Ion")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> saveUserWithGeneratedId(invocation.getArgument(0)));
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtUtil.generateAccessToken(any(UUID.class), eq(RoleName.ORGANIZATION_ADMIN))).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any(UUID.class))).thenReturn("refresh-token");

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

    @Test
    void register_success_returnsAccessToken() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@test.com");
        request.setPassword("parola123");
        request.setFirstName("Ion");
        request.setLastName("Popescu");
        request.setOrganizationName("Scoala Ion");

        Role role = new Role(RoleName.ORGANIZATION_ADMIN);

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(organizationRepository.existsByName(any())).thenReturn(false);
        when(roleRepository.findByName(any())).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> saveUserWithGeneratedId(invocation.getArgument(0)));
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtUtil.generateAccessToken(any(UUID.class), eq(RoleName.ORGANIZATION_ADMIN))).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any(UUID.class))).thenReturn("refresh-token");

        AuthResponse response = authService.register(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void register_success_returnsUserData() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@test.com");
        request.setPassword("parola123");
        request.setFirstName("Ion");
        request.setLastName("Popescu");

        Role role = new Role(RoleName.ORGANIZATION_ADMIN);

        UUID organizationId = UUID.randomUUID();

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(organizationRepository.existsByName(any())).thenReturn(false);
        when(roleRepository.findByName(any())).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> {
                    Organization org = invocation.getArgument(0);
                    org.setId(organizationId);
                    return org;
                });
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> saveUserWithGeneratedId(invocation.getArgument(0)));
        when(jwtUtil.generateAccessToken(any(UUID.class), eq(RoleName.ORGANIZATION_ADMIN))).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any(UUID.class))).thenReturn("refresh-token");

        AuthResponse response = authService.register(request);

        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo("test@test.com");
        assertThat(response.getUser().getFirstName()).isEqualTo("Ion");
        assertThat(response.getUser().getLastName()).isEqualTo("Popescu");
        assertThat(response.getUser().getRole()).isEqualTo(RoleName.ORGANIZATION_ADMIN);

        assertThat(response.getUser().getOrganizationId())
                .isEqualTo(organizationId);
    }

    // LOGIN

    @Test
    void login_success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("parola123");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@test.com");
        user.setPasswordHash("hashed_parola");
        user.setRole(new Role(RoleName.ORGANIZATION_ADMIN));

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("parola123", "hashed_parola")).thenReturn(true);
        when(jwtUtil.generateAccessToken(user.getId(), RoleName.ORGANIZATION_ADMIN)).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(user.getId())).thenReturn("refresh-token");

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

    @Test
    void login_success_returnsAccessToken() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("parola123");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@test.com");
        user.setPasswordHash("hashed_parola");
        user.setRole(new Role(RoleName.ORGANIZATION_ADMIN));

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("parola123", "hashed_parola")).thenReturn(true);
        when(jwtUtil.generateAccessToken(user.getId(), RoleName.ORGANIZATION_ADMIN)).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(user.getId())).thenReturn("refresh-token");

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void login_success_returnsUserData() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("parola123");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@test.com");
        user.setFirstName("Ion");
        user.setLastName("Popescu");
        user.setPasswordHash("hashed_parola");
        user.setRole(new Role(RoleName.ORGANIZATION_ADMIN));
        user.setStatus(UserStatus.ACTIVE);

        UUID organizationId = UUID.randomUUID();

        Organization org = new Organization();
        org.setId(organizationId);
        user.setOrganization(org);

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("parola123", "hashed_parola")).thenReturn(true);
        when(jwtUtil.generateAccessToken(user.getId(), RoleName.ORGANIZATION_ADMIN)).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(user.getId())).thenReturn("refresh-token");

        AuthResponse response = authService.login(request);

        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo("test@test.com");
        assertThat(response.getUser().getFirstName()).isEqualTo("Ion");
        assertThat(response.getUser().getLastName()).isEqualTo("Popescu");
        assertThat(response.getUser().getRole()).isEqualTo(RoleName.ORGANIZATION_ADMIN);
        assertThat(response.getUser().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(response.getUser().getOrganizationId()).isEqualTo(organizationId);
    }
}
