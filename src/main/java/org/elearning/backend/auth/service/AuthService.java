package org.elearning.backend.auth.service;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.auth.dto.request.LoginRequest;
import org.elearning.backend.auth.dto.request.RegisterRequest;
import org.elearning.backend.auth.dto.response.AuthResponse;
import org.elearning.backend.auth.dto.response.UserDataResponse;
import org.elearning.backend.common.exception.DuplicateResourceException;
import org.elearning.backend.common.exception.InvalidCredentials;
import org.elearning.backend.common.exception.ResourceNotFoundException;
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

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrganizationRepository organizationRepository;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

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

        UserDataResponse userData = new UserDataResponse(
                savedUser.getId(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getEmail(),
                RoleName.ORGANIZATION_ADMIN,
                savedUser.getStatus(),
                savedOrganization.getId()
        );

        return new AuthResponse("User registered successfully", accessToken, refreshToken, userData);
    }

    public AuthResponse login(LoginRequest request) {
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();

            String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole().getName());
            String refreshToken = jwtUtil.generateRefreshToken(user.getId());

            UserDataResponse userData = new UserDataResponse(
                    user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    user.getRole().getName(),
                    user.getStatus(),
                    user.getOrganization() != null ? user.getOrganization().getId() : null
            );

            return new AuthResponse("Login successful", accessToken, refreshToken, userData);
        } catch (AuthenticationException ex) {
            throw new InvalidCredentials("Invalid credentials");
        }
    }
}
