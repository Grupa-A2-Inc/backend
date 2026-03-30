package org.elearning.backend.auth.service;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.auth.dto.request.LoginRequest;
import org.elearning.backend.auth.dto.request.RefreshRequest;
import org.elearning.backend.auth.dto.request.RegisterRequest;
import org.elearning.backend.auth.dto.response.AuthResponse;
import org.elearning.backend.common.exception.DuplicateResourceException;
import org.elearning.backend.common.exception.InvalidCredentials;
import org.elearning.backend.common.exception.ResourceNotFoundException;
import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.role.repository.RoleRepository;
import org.elearning.backend.security.jwt.JwtUtil;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.entity.UserStatus;
import org.elearning.backend.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrganizationRepository organizationRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already in use: " + request.getEmail());
        }

        if (organizationRepository.existsByName(request.getOrganizationName())) {
            throw new DuplicateResourceException("Organization name already exists: " + request.getOrganizationName());
        }

        Role role = roleRepository.findByName(RoleName.ORGANIZATION_ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Role ORGANIZATION_ADMIN not found"));

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
        organization.setOwner(savedUser);
        Organization savedOrganization = organizationRepository.save(organization);

        savedUser.setOrganization(savedOrganization);
        userRepository.save(savedUser);

        return new AuthResponse("User registered successfully");
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentials("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentials("Invalid credentials");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole().getName());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        return new AuthResponse("Login successful", accessToken, refreshToken);
    }
}
