package org.elearning.backend.user.service;

import lombok.AllArgsConstructor;
import org.elearning.backend.auth.service.ActivationTokenService;
import org.elearning.backend.auth.service.EmailService;
import org.elearning.backend.common.dto.response.PaginatedResponse;
import org.elearning.backend.organization.dto.response.OrganizationResponse;
import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.parent.entity.Parent;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.role.repository.RoleRepository;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.elearning.backend.student.entity.Student;
import org.elearning.backend.user.dto.request.ChangePasswordRequest;
import org.elearning.backend.user.dto.request.CreateUserRequest;
import org.elearning.backend.user.dto.request.UpdateUserRequest;
import org.elearning.backend.user.dto.request.UpdateUserStatusRequest;
import org.elearning.backend.user.dto.response.UserResponse;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.entity.UserStatus;
import org.elearning.backend.user.exception.*;
import org.elearning.backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@AllArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final ActivationTokenService activationTokenService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private static final String USER_NO_EXIST = "User does not exist: ";

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        Role role = roleRepository.findByName(request.getRoleName())
                .orElseThrow(() -> new UserRoleNotFoundException("Role does not exist: " + request.getRoleName()));

        User user = createUserEntityForRole(request.getRoleName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(null);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(role);
        user.setStatus(UserStatus.PENDING);

        if (request.getOrganizationId() != null) {
            Organization org = organizationRepository.findById(request.getOrganizationId())
                    .orElseThrow(() -> new UserOrganizationNotFoundException("Organization not found: " + request.getOrganizationId()));
            user.setOrganization(org);
        }

        User saved = userRepository.save(user);

        String rawToken = activationTokenService.generateActivationToken(saved);
        emailService.sendActivationEmail(saved.getEmail(), saved.getFirstName(), rawToken);

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

    public List<UserResponse> getCurrentOrganizationUsers() {
        CustomUserDetails userDetails =
                (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        UUID currentUserId = userDetails.getUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException(USER_NO_EXIST + currentUserId));

        Organization organization = currentUser.getOrganization();
        if (organization == null) {
            throw new UserOrganizationNotFoundException("Organization not found.");
        }

        UUID organizationId = organization.getId();

        return userRepository.findByOrganizationId(organizationId)
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

    public void updateUserStatus(UUID userId, UpdateUserStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(USER_NO_EXIST + userId));

        user.setStatus(request.getStatus());
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
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

    public List<UserResponse> getUsersByOrganizationId(UUID organizationId) {
        return userRepository.findByOrganizationId(organizationId)
                .stream()
                .map(this::toResponse)
                .toList();
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

    private User createUserEntityForRole(RoleName roleName) {
        return switch (roleName) {
            case PARENT -> new Parent();
            case STUDENT -> new Student();
            default -> new User();
        };
    }

    public PaginatedResponse<UserResponse> getCurrentOrganizationUsersPaginated(UUID currentUserId, Integer page, Integer size, String search, String role, UserStatus status, String sortBy, String sortDir){

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + currentUserId));

        if (currentUser.getOrganization() == null) {
            throw new IllegalArgumentException("Authenticated user is not assigned to an organization.");
        }

        UUID organizationId = currentUser.getOrganization().getId();

        //daca nu se da page ul sau size ul
        int pageValue = (page == null || page < 0) ? 0 : page;
        int sizeValue = (size == null || size <= 0) ? 10 : size;

        //sortare dupa firstName, lastName si email
        String sortField = (sortBy == null || sortBy.isBlank()) ? "firstName" : sortBy;
        //ascendent
        String direction = (sortDir == null || sortDir.isBlank()) ? "asc" : sortDir.toLowerCase();

        Set<String> allowedSortFields = Set.of("firstName", "lastName", "email");
        if (!allowedSortFields.contains(sortField)) {
            throw new IllegalArgumentException("Invalid sortBy field: " + sortField);
        }

        Sort sort = direction.equals("desc")
                ? Sort.by(sortField).descending()
                : Sort.by(sortField).ascending();

        Pageable pageable = PageRequest.of(pageValue, sizeValue, sort);

        Specification<User> spec = Specification.where(
                (root, query, cb) -> cb.equal(root.get("organization").get("id"), organizationId)
        );

        if (search != null && !search.isBlank()) {
            String likeValue = "%" + search.toLowerCase().trim() + "%";

            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("firstName")), likeValue),
                    cb.like(cb.lower(root.get("lastName")), likeValue),
                    cb.like(cb.lower(root.get("email")), likeValue)
            ));
        }

        if (role != null && !role.isBlank()) {
            RoleName roleName;
            try {
                roleName = RoleName.valueOf(role.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid role filter: " + role);
            }

            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("role").get("name"), roleName)
            );
        }

        if (status != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("status"), status)
            );
        }

        Page<User> userPage = userRepository.findAll(spec, pageable);

        List<UserResponse> content = userPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return new PaginatedResponse<>(
                content,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements()
        );

    }

    public PaginatedResponse<UserResponse> getAllUsersPaginated(Integer page, Integer size, String search, String role, UserStatus status, String sortBy, String sortDir){

        int pageValue = (page == null || page < 0) ? 0 : page;
        int sizeValue = (size == null || size <= 0) ? 10 : size;

        String sortField = (sortBy == null || sortBy.isBlank()) ? "firstName" : sortBy;
        String direction = (sortDir == null || sortDir.isBlank()) ? "asc" : sortDir.toLowerCase();

        Set<String> allowedSortFields = Set.of("firstName", "lastName", "email", "createdAt");
        if (!allowedSortFields.contains(sortField)) {
            throw new IllegalArgumentException("Invalid sortBy field: " + sortField);
        }

        Sort sort = direction.equals("desc")
                ? Sort.by(sortField).descending()
                : Sort.by(sortField).ascending();

        Pageable pageable = PageRequest.of(pageValue, sizeValue, sort);

        Specification<User> spec = Specification.where(null);

        if (search != null && !search.isBlank()) {
            String likeValue = "%" + search.toLowerCase().trim() + "%";

            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("firstName")), likeValue),
                    cb.like(cb.lower(root.get("lastName")), likeValue),
                    cb.like(cb.lower(root.get("email")), likeValue)
            ));
        }

        if (role != null && !role.isBlank()) {
            RoleName roleName;
            try {
                roleName = RoleName.valueOf(role.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid role filter: " + role);
            }

            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("role").get("name"), roleName)
            );
        }

        if (status != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("status"), status)
            );
        }

        Page<User> userPage = userRepository.findAll(spec, pageable);

        List<UserResponse> content = userPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return new PaginatedResponse<>(
                content,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements()
        );

    }
}