package org.elearning.backend.user.service;

import lombok.AllArgsConstructor;
import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.parent.entity.Parent;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.role.repository.RoleRepository;
import org.elearning.backend.student.entity.Student;
import org.elearning.backend.user.dto.request.ChangePasswordRequest;
import org.elearning.backend.user.dto.request.CreateUserBulkRequest;
import org.elearning.backend.user.dto.request.CreateUserRequest;
import org.elearning.backend.user.dto.request.UpdateUserRequest;
import org.elearning.backend.user.dto.response.BulkImportResponse;
import org.elearning.backend.user.dto.response.UserImportResult;
import org.elearning.backend.user.dto.response.UserResponse;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.entity.UserStatus;
import org.elearning.backend.user.exception.*;
import org.elearning.backend.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private static final String USER_NO_EXIST = "User does not exist: ";

    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        Role role = roleRepository.findByName(request.getRoleName())
                .orElseThrow(() -> new UserRoleNotFoundException("Role does not exist: " + request.getRoleName()));

        User user = createUserEntityForRole(request.getRoleName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(role);

        if (request.getOrganizationId() != null) {
            Organization org = organizationRepository.findById(request.getOrganizationId())
                    .orElseThrow(() -> new UserOrganizationNotFoundException("Organization not found: " + request.getOrganizationId()));
            user.setOrganization(org);
        }
        user.setStatus(UserStatus.ACTIVE);

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(USER_NO_EXIST + id));
        return toResponse(user);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(USER_NO_EXIST + id));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setUpdatedAt(LocalDateTime.now());

        if (request.getOrganizationId() != null) {
            Organization org = organizationRepository.findById(request.getOrganizationId())
                    .orElseThrow(() -> new UserOrganizationNotFoundException("Organization not found: " + request.getOrganizationId()));
            user.setOrganization(org);
        }

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(USER_NO_EXIST + id);
        }
        userRepository.deleteById(id);
    }

    public void changePassword(UUID id, ChangePasswordRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(USER_NO_EXIST + id));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new UserBadRequestException("Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
            throw new UserBadRequestException("Passwords do not match");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().getName(),
                user.getOrganization() != null ? user.getOrganization().getId() : null,
                user.getStatus()
        );
    }

    public List<UserResponse> getUsersByOrganizationId(UUID organizationId) {
        return userRepository.findByOrganizationId(organizationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private final PasswordEncoder passwordEncoder;

    private User createUserEntityForRole(RoleName roleName) {
        return switch (roleName) {
            case PARENT -> new Parent();
            case STUDENT -> new Student();
            default -> new User();
        };
    }

    public BulkImportResponse importUsers(CreateUserBulkRequest request) {
        List<UserImportResult> results = request.getUsers().stream()
                .map(this::tryCreateSingleUser)
                .toList();

        return new BulkImportResponse(results);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UserImportResult tryCreateSingleUser(CreateUserRequest request) {
        try {
            UserResponse created = createUser(request);
            return UserImportResult.succeeded(created);
        } catch (Exception e) {
            return UserImportResult.failed(request.getEmail(), e.getMessage());
        }
    }
}
