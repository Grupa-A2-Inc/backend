package org.elearning.backend.auth.service;

import org.elearning.backend.auth.dto.request.LoginRequest;
import org.elearning.backend.auth.dto.request.RegisterRequest;
import org.elearning.backend.auth.exception.AuthLockedAccount;
import org.elearning.backend.auth.exception.AuthResourceNotFoundException;
import org.elearning.backend.auth.exception.InvalidCredentialsException;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.role.repository.RoleRepository;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.elearning.backend.security.jwt.JwtUtil;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceCoverageTest {

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

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_throwsWhenOrganizationAdminRoleIsMissing() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("admin@example.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");
        request.setFirstName("Admin");
        request.setLastName("User");
        request.setOrganizationName("Acme Academy");
        request.setCountry("Romania");
        request.setCity("Bucharest");
        request.setOrganizationType("School");

        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(organizationRepository.existsByName("Acme Academy")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ORGANIZATION_ADMIN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(AuthResourceNotFoundException.class)
                .hasMessage("Role ORGANIZATION_ADMIN not found");
    }

    @Test
    void login_throwsWhenAccountIsStillLocked() {
        User user = baseUser();
        user.setLockedUntil(LocalDateTime.now().plusMinutes(3));

        LoginRequest request = new LoginRequest();
        request.setEmail(user.getEmail());
        request.setPassword("password123");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthLockedAccount.class)
                .hasMessage("Too many login attempts. Please try again after " +
                        user.getLockedUntil().format(DateTimeFormatter.ofPattern("HH:mm")));

        verifyNoInteractions(authenticationManager, jwtUtil);
    }

    @Test
    void login_clearsExpiredLockBeforeAuthenticating() {
        User user = baseUser();
        user.setLockedUntil(LocalDateTime.now().minusMinutes(1));
        user.setFailedLoginAttempts(4);

        LoginRequest request = new LoginRequest();
        request.setEmail(user.getEmail());
        request.setPassword("password123");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authenticatedUser(user));
        when(jwtUtil.generateAccessToken(user.getId(), RoleName.ORGANIZATION_ADMIN)).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(user.getId())).thenReturn("refresh-token");

        var response = authService.login(request);

        assertThat(response.getMessage()).isEqualTo("Login successful");
        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.getFailedLoginAttempts()).isZero();
        verify(userRepository, times(2)).save(user);
    }

    @Test
    void login_usesZeroWhenFailedAttemptCounterIsNull() {
        User user = baseUser();
        user.setFailedLoginAttempts(null);

        LoginRequest request = new LoginRequest();
        request.setEmail(user.getEmail());
        request.setPassword("wrong-password");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");

        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        verify(userRepository).save(user);
        verify(jwtUtil, never()).generateAccessToken(any(), any());
    }

    @Test
    void login_locksUserAfterFifthFailedAttempt() {
        User user = baseUser();
        user.setFailedLoginAttempts(4);

        LoginRequest request = new LoginRequest();
        request.setEmail(user.getEmail());
        request.setPassword("wrong-password");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthLockedAccount.class)
                .hasMessageStartingWith("Too many login attempts. Please try again after ");

        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.getLockedUntil()).isAfter(LocalDateTime.now().plusMinutes(4));
        verify(userRepository).save(user);
    }

    @Test
    void checkIfTooManyLoginAttempts_doesNotThrowWhenExistingLockIsInThePast() {
        User user = baseUser();
        user.setFailedLoginAttempts(1);
        user.setLockedUntil(LocalDateTime.now().minusMinutes(1));

        ReflectionTestUtils.invokeMethod(authService, "checkIfTooManyLoginAttempts", user);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(2);
        assertThat(user.getLockedUntil()).isBefore(LocalDateTime.now());
        verify(userRepository).save(user);
    }

    private UsernamePasswordAuthenticationToken authenticatedUser(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    private User baseUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("admin@example.com");
        user.setPasswordHash("hashed-password");
        user.setRole(new Role(RoleName.ORGANIZATION_ADMIN));
        return user;
    }
}
