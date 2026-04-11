package org.elearning.backend.auth.service;

import org.elearning.backend.auth.dto.response.AuthResponse;
import org.elearning.backend.auth.dto.request.RegisterRequest;
import org.elearning.backend.auth.dto.request.LoginRequest;
import org.elearning.backend.auth.exception.AuthBadRequestException;
import org.elearning.backend.auth.exception.AuthConflictException;
import org.elearning.backend.auth.exception.InvalidCredentialsException;
import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.role.repository.RoleRepository;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.elearning.backend.security.jwt.JwtUtil;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.entity.UserStatus;
import org.elearning.backend.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User saveUserWithGeneratedId(User user) {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID());
        }
        return user;
    }

    private UsernamePasswordAuthenticationToken authenticatedUser(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    private RegisterRequest validRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@test.com");
        request.setPassword("parola123");
        request.setConfirmPassword("parola123");
        request.setFirstName("Ion");
        request.setLastName("Popescu");
        request.setOrganizationName("Scoala Ion");
        request.setCountry("Romania");
        request.setCity("Bucharest");
        request.setOrganizationType("School");
        return request;
    }

    // REGISTER

    @Test
    void register_emailAlreadyExists_throwsException() {
        RegisterRequest request = validRegisterRequest();
        request.setEmail("existent@test.com");

        when(userRepository.existsByEmail("existent@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(AuthConflictException.class)
                .hasMessage("Email already in use: existent@test.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_passwordIsHashed() {
        RegisterRequest request = validRegisterRequest();

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
        RegisterRequest request = validRegisterRequest();
        request.setEmail("admin@test.com");

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
        RegisterRequest request = validRegisterRequest();
        request.setEmail("admin@test.com");

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(organizationRepository.existsByName("Scoala Ion")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(AuthConflictException.class)
                .hasMessage("Organization name already exists: Scoala Ion");

        verify(organizationRepository, never()).save(any());
    }

    @Test
    void register_passwordConfirmationMismatch_throwsBadRequestException() {
        RegisterRequest request = validRegisterRequest();
        request.setConfirmPassword("altaParola123");

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(organizationRepository.existsByName(any())).thenReturn(false);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(AuthBadRequestException.class)
                .hasMessage("Passwords do not match");

        verify(roleRepository, never()).findByName(any());
        verify(userRepository, never()).save(any());
        verify(organizationRepository, never()).save(any());
    }

    @Test
    void register_success_returnsAccessToken() {
        RegisterRequest request = validRegisterRequest();

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
        RegisterRequest request = validRegisterRequest();

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
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authenticatedUser(user));
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
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void login_wrongPassword_throwsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("parolaGresita");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@test.com");
        user.setPasswordHash("hashed_parola");
        user.setRole(new Role(RoleName.ORGANIZATION_ADMIN));

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
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
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authenticatedUser(user));
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
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authenticatedUser(user));
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
