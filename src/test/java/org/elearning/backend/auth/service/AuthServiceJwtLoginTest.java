package org.elearning.backend.auth.service;

import org.elearning.backend.auth.dto.request.LoginRequest;
import org.elearning.backend.auth.dto.response.AuthResponse;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.jwt.JwtUtil;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AuthServiceJwtLoginTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User makeUser(String email, String hashedPassword, RoleName roleName) {
        Role role = new Role(roleName);
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setPasswordHash(hashedPassword);
        user.setRole(role);
        return user;
    }

    @Test
    void login_success_returnsAccessToken() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("parola123");

        User user = makeUser("test@test.com", "hashed_parola", RoleName.ORGANIZATION_ADMIN);

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("parola123", "hashed_parola")).thenReturn(true);
        when(jwtUtil.generateAccessToken(user.getId(), RoleName.ORGANIZATION_ADMIN))
                .thenReturn("mocked.access.token");
        when(jwtUtil.generateRefreshToken(user.getId()))
                .thenReturn("mocked.refresh.token");

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("mocked.access.token");
    }

    @Test
    void login_success_returnsRefreshToken() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("parola123");

        User user = makeUser("test@test.com", "hashed_parola", RoleName.ORGANIZATION_ADMIN);

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("parola123", "hashed_parola")).thenReturn(true);
        when(jwtUtil.generateAccessToken(user.getId(), RoleName.ORGANIZATION_ADMIN))
                .thenReturn("mocked.access.token");
        when(jwtUtil.generateRefreshToken(user.getId()))
                .thenReturn("mocked.refresh.token");

        AuthResponse response = authService.login(request);

        assertThat(response.getRefreshToken()).isEqualTo("mocked.refresh.token");
    }

    @Test
    void login_success_callsGenerateTokenWithCorrectArgs() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("parola123");

        User user = makeUser("test@test.com", "hashed_parola", RoleName.ORGANIZATION_ADMIN);

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("parola123", "hashed_parola")).thenReturn(true);
        when(jwtUtil.generateAccessToken(user.getId(), RoleName.ORGANIZATION_ADMIN))
                .thenReturn("mocked.access.token");
        when(jwtUtil.generateRefreshToken(user.getId()))
                .thenReturn("mocked.refresh.token");

        authService.login(request);

        verify(jwtUtil, times(1))
                .generateAccessToken(user.getId(), RoleName.ORGANIZATION_ADMIN);
        verify(jwtUtil, times(1))
                .generateRefreshToken(user.getId());
    }

    @Test
    void login_wrongPassword_doesNotCallGenerateToken() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("parolaGresita");

        User user = makeUser("test@test.com", "hashed_parola", RoleName.ORGANIZATION_ADMIN);

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("parolaGresita", "hashed_parola")).thenReturn(false);

        try { authService.login(request); } catch (Exception ignored) {}

        verify(jwtUtil, never()).generateAccessToken(any(), any());
        verify(jwtUtil, never()).generateRefreshToken(any());
    }

    @Test
    void login_emailNotFound_doesNotCallGenerateToken() {
        LoginRequest request = new LoginRequest();
        request.setEmail("inexistent@test.com");
        request.setPassword("parola123");

        when(userRepository.findByEmail("inexistent@test.com")).thenReturn(Optional.empty());

        try { authService.login(request); } catch (Exception ignored) {}

        verify(jwtUtil, never()).generateAccessToken(any(), any());
        verify(jwtUtil, never()).generateRefreshToken(any());
    }
}
