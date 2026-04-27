package org.elearning.backend.auth.service;

import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.auth.dto.request.LoginRequest;
import org.elearning.backend.auth.dto.request.RegisterRequest;
import org.elearning.backend.auth.dto.response.AuthResponse;
import org.elearning.backend.auth.dto.response.UserDataResponse;
import org.elearning.backend.auth.exception.*;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrganizationRepository organizationRepository;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private static final DateTimeFormatter LOCK_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int LOCK_OUT_TIME_IN_MINUTES = 10;
    private static final int NUMBER_OF_MAXIMUM_FAILED_ATTEMPTS = 5;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthConflictException("Email already in use: " + request.getEmail());
        }

        if (organizationRepository.existsByName(request.getOrganizationName())) {
            throw new AuthConflictException("Organization name already exists: " + request.getOrganizationName());
        }

        if (!Objects.equals(request.getConfirmPassword(), request.getPassword())) {
            throw new AuthBadRequestException("Passwords do not match");
        }

        Role role = roleRepository.findByName(RoleName.ORGANIZATION_ADMIN)
                .orElseThrow(() -> new AuthResourceNotFoundException("Role ORGANIZATION_ADMIN not found"));

        User user = new User();
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);

        User savedUser = userRepository.save(user);

        Organization organization = new Organization();
        organization.setName(request.getOrganizationName());
        organization.setCountry(request.getCountry());
        organization.setCity(request.getCity());
        organization.setOrganizationType(request.getOrganizationType());
        organization.setAddress(request.getAddress());
        organization.setPhoneNumber(request.getPhoneNumber());
        organization.setOwner(savedUser);
        Organization savedOrganization = organizationRepository.save(organization);

        savedUser.setOrganization(savedOrganization);
        userRepository.save(savedUser);

        String accessToken = jwtUtil.generateAccessToken(savedUser.getId(), RoleName.ORGANIZATION_ADMIN);
        String refreshToken = jwtUtil.generateRefreshToken(savedUser.getId());
        refreshTokenService.storeRefreshToken(savedUser, refreshToken);

        UserDataResponse userData = new UserDataResponse(
                savedUser.getId(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getEmail(),
                RoleName.ORGANIZATION_ADMIN,
                savedUser.getStatus(),
                savedOrganization.getId(),
                savedOrganization.getName(),
                savedOrganization.getOrganizationType(),
                savedOrganization.getCountry(),
                savedOrganization.getCity(),
                savedOrganization.getPhoneNumber(),
                savedOrganization.getAddress()
        );

        return new AuthResponse("User registered successfully", accessToken, refreshToken, userData);
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        validateUserCanLogin(user);

        handleExistingLock(user);

        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User authenticatedUser = userDetails.getUser();

            authenticatedUser.setLockedUntil(null);
            authenticatedUser.setFailedLoginAttempts(0);
            authenticatedUser.setUpdatedAt(LocalDateTime.now());
            userRepository.save(authenticatedUser);

            String accessToken = jwtUtil.generateAccessToken(authenticatedUser.getId(), authenticatedUser.getRole().getName());
            String refreshToken = jwtUtil.generateRefreshToken(authenticatedUser.getId());
            refreshTokenService.storeRefreshToken(authenticatedUser, refreshToken);

            UserDataResponse userData = new UserDataResponse(
                    authenticatedUser.getId(),
                    authenticatedUser.getFirstName(),
                    authenticatedUser.getLastName(),
                    authenticatedUser.getEmail(),
                    authenticatedUser.getRole().getName(),
                    authenticatedUser.getStatus(),
                    authenticatedUser.getOrganization() != null ? authenticatedUser.getOrganization().getId() : null,
                    authenticatedUser.getOrganization() != null ? authenticatedUser.getOrganization().getName() : null,
                    authenticatedUser.getOrganization() != null ? authenticatedUser.getOrganization().getOrganizationType() : null,
                    authenticatedUser.getOrganization() != null ? authenticatedUser.getOrganization().getCountry() : null,
                    authenticatedUser.getOrganization() != null ? authenticatedUser.getOrganization().getCity() : null,
                    authenticatedUser.getOrganization() != null ? authenticatedUser.getOrganization().getPhoneNumber() : null,
                    authenticatedUser.getOrganization() != null ? authenticatedUser.getOrganization().getAddress() : null
            );

            return new AuthResponse("Login successful", accessToken, refreshToken, userData);

        } catch (AuthenticationException ex) {

            checkIfTooManyLoginAttempts(user);

            throw new InvalidCredentialsException("Invalid credentials");
        }
    }

    private void handleExistingLock(User user) {
        LocalDateTime now = LocalDateTime.now();

        if (user.getLockedUntil() != null) {
            if (user.getLockedUntil().isAfter(now)) {

                String lockedFormat = user.getLockedUntil().format(LOCK_TIME_FORMAT);

                throw new AuthLockedAccount("Too many login attempts. Please try again after " +
                        lockedFormat);
            } else {
                user.setLockedUntil(null);
                user.setFailedLoginAttempts(0);
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
            }
        }
    }

    private void checkIfTooManyLoginAttempts(User user) {
        int failedAttempts = user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts();
        failedAttempts++;
        user.setFailedLoginAttempts(failedAttempts);

        if (failedAttempts >= NUMBER_OF_MAXIMUM_FAILED_ATTEMPTS) {
            //pragul e de 5 failed attempts
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_OUT_TIME_IN_MINUTES));
        }

        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {

            String lockedFormat = user.getLockedUntil().format(LOCK_TIME_FORMAT);
            throw new AuthLockedAccount("Too many login attempts. Please try again after " +
                    lockedFormat);
        }
    }

    private void validateUserCanLogin(User user) {
        if (user.getStatus() == UserStatus.ACTIVE) {
            return;
        }

        throw new InvalidCredentialsException("Account is " + user.getStatus() + ". Login is not allowed.");
    }
}
