package org.elearning.backend.auth.service;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.auth.dto.request.LoginRequest;
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

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already in use: " + request.getEmail());
        }

        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.getRole()));

        User user = new User();
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);

        if (request.getRole() == RoleName.ORGANIZATION_ADMIN) {
            if (organizationRepository.existsByName(request.getOrganizationName())) {
                throw new DuplicateResourceException("Organization name already exists: " + request.getOrganizationName());
            }

            User savedUser = userRepository.save(user);

            Organization organization = new Organization();
            organization.setName(request.getOrganizationName());
            organization.setOwner(savedUser);
            Organization savedOrganization = organizationRepository.save(organization);

            savedUser.setOrganization(savedOrganization);
            userRepository.save(savedUser);

        } else {
            Organization organization = organizationRepository.findByName(request.getOrganizationName())
                    .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + request.getOrganizationName()));

            user.setOrganization(organization);
            userRepository.save(user);
        }

        return new AuthResponse("User registered successfully");
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentials("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentials("Invalid credentials");
        }

        return new AuthResponse("Login successful", null);
    }
}