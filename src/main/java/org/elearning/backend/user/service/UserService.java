package org.elearning.backend.user.service;

import lombok.AllArgsConstructor;
import org.elearning.backend.ai.service.AiStudentRegistrationService;
import org.elearning.backend.auth.service.ActivationTokenService;
import org.elearning.backend.auth.service.EmailService;
import org.elearning.backend.common.dto.response.PaginatedResponse;
import org.elearning.backend.organization.dto.response.OrganizationResponse;
import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.organization.service.OrganizationDeletionService;
import org.elearning.backend.parent.entity.Parent;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.role.repository.RoleRepository;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.elearning.backend.student.entity.Student;
import org.elearning.backend.subscription.service.EntitlementService;
import org.elearning.backend.user.dto.request.ChangePasswordRequest;
import org.elearning.backend.user.dto.request.CreateUserRequest;
import org.elearning.backend.user.dto.request.UserPaginationRequest;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.apache.commons.lang3.StringEscapeUtils.escapeCsv;

@AllArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final ActivationTokenService activationTokenService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AiStudentRegistrationService aiStudentRegistrationService;
    private final EntitlementService entitlementService;
    private final OrganizationDeletionService organizationDeletionService;

    private static final String USER_NO_EXIST = "User does not exist: ";
    private static final String DELIMITER = ",";
    private static final String LINE_SEPARATOR = "\n";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final String FIRST_NAME_FIELD = "firstName";
    private static final String LAST_NAME_FIELD = "lastName";
    private static final String EMAIL_FIELD = "email";
    private static final String CREATED_AT_FIELD = "createdAt";
    private static final String ASC_SORT_DIRECTION = "asc";
    private static final String DESC_SORT_DIRECTION = "desc";
    private static final Set<String> ORGANIZATION_ALLOWED_SORT_FIELDS = Set.of(
            FIRST_NAME_FIELD,
            LAST_NAME_FIELD,
            EMAIL_FIELD
    );
    private static final Set<String> ALL_USERS_ALLOWED_SORT_FIELDS = Set.of(
            FIRST_NAME_FIELD,
            LAST_NAME_FIELD,
            EMAIL_FIELD,
            CREATED_AT_FIELD
    );


    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        Role role = roleRepository.findByName(request.getRoleName())
                .orElseThrow(() -> new UserRoleNotFoundException("Role does not exist: " + request.getRoleName()));

        if (request.getOrganizationId() != null) {
            entitlementService.canCreateUser(request.getOrganizationId());
        }

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

        User saved;
        try {
            saved = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new UserAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        if (request.getRoleName() == RoleName.STUDENT) {
            aiStudentRegistrationService.registerStudent(saved.getId());
        }

        String rawToken = activationTokenService.generateActivationToken(saved);
        sendActivationEmailAfterCommit(saved, rawToken);

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
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(USER_NO_EXIST + id));

        if (user.getRole() != null
                && user.getRole().getName() == RoleName.ORGANIZATION_ADMIN
                && organizationRepository.findFirstByOwnerId(id).isPresent()) {
            organizationDeletionService.deleteOrganizationOwnedByAdmin(id);
            organizationDeletionService.deleteOrganizationOwnedByAdmin(id);
            return;
        }

        userRepository.delete(user);
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

    private void sendActivationEmailAfterCommit(User user, String rawToken) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emailService.sendActivationEmail(user.getEmail(), user.getFirstName(), rawToken);
                }
            });
            return;
        }

        emailService.sendActivationEmail(user.getEmail(), user.getFirstName(), rawToken);
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

    public PaginatedResponse<UserResponse> getCurrentOrganizationUsersPaginated(
            UUID currentUserId,
            UserPaginationRequest request
    ) {
        UUID organizationId = getRequiredOrganizationId(currentUserId);
        Pageable pageable = buildPageable(request, ORGANIZATION_ALLOWED_SORT_FIELDS);
        Specification<User> spec = buildOrganizationUsersSpecification(
                organizationId,
                request != null ? request.search() : null,
                request != null ? request.role() : null,
                request != null ? request.status() : null
        );

        return toPaginatedResponse(userRepository.findAll(spec, pageable));
    }

    public PaginatedResponse<UserResponse> getAllUsersPaginated(Integer page, Integer size, String search, String role, UserStatus status, String sortBy, String sortDir){
        UserPaginationRequest request = new UserPaginationRequest(page, size, search, role, status, sortBy, sortDir);
        Pageable pageable = buildPageable(request, ALL_USERS_ALLOWED_SORT_FIELDS);
        Specification<User> spec = applyUserFilters(Specification.where(null), search, role, status);

        return toPaginatedResponse(userRepository.findAll(spec, pageable));
    }

    private UUID getRequiredOrganizationId(UUID currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + currentUserId));

        if (currentUser.getOrganization() == null) {
            throw new IllegalArgumentException("Authenticated user is not assigned to an organization.");
        }

        return currentUser.getOrganization().getId();
    }

    private Pageable buildPageable(UserPaginationRequest request, Set<String> allowedSortFields) {
        int pageValue = normalizePage(request != null ? request.page() : null);
        int sizeValue = normalizeSize(request != null ? request.size() : null);
        String sortField = normalizeSortField(request != null ? request.sortBy() : null, allowedSortFields);

        return PageRequest.of(pageValue, sizeValue, buildSort(sortField, request != null ? request.sortDir() : null));
    }

    private int normalizePage(Integer page) {
        return page == null || page < 0 ? DEFAULT_PAGE : page;
    }

    private int normalizeSize(Integer size) {
        return size == null || size <= 0 ? DEFAULT_PAGE_SIZE : size;
    }

    private String normalizeSortField(String sortBy, Set<String> allowedSortFields) {
        String sortField = hasText(sortBy) ? sortBy : FIRST_NAME_FIELD;
        if (!allowedSortFields.contains(sortField)) {
            throw new IllegalArgumentException("Invalid sortBy field: " + sortField);
        }
        return sortField;
    }

    private Sort buildSort(String sortField, String sortDir) {
        String direction = hasText(sortDir) ? sortDir.toLowerCase(Locale.ROOT) : ASC_SORT_DIRECTION;
        return DESC_SORT_DIRECTION.equals(direction)
                ? Sort.by(sortField).descending()
                : Sort.by(sortField).ascending();
    }

    private Specification<User> buildOrganizationUsersSpecification(
            UUID organizationId,
            String search,
            String role,
            UserStatus status
    ) {
        return applyUserFilters(
                Specification.where((root, query, cb) -> cb.equal(root.get("organization").get("id"), organizationId)),
                search,
                role,
                status
        );
    }

    private Specification<User> applyUserFilters(
            Specification<User> specification,
            String search,
            String role,
            UserStatus status
    ) {
        Specification<User> spec = specification;

        if (hasText(search)) {
            spec = spec.and(buildSearchSpecification(search));
        }

        if (hasText(role)) {
            spec = spec.and(buildRoleSpecification(parseRoleName(role)));
        }

        if (status != null) {
            spec = spec.and(buildStatusSpecification(status));
        }

        return spec;
    }

    private Specification<User> buildSearchSpecification(String search) {
        String likeValue = "%" + search.toLowerCase(Locale.ROOT).trim() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get(FIRST_NAME_FIELD)), likeValue),
                cb.like(cb.lower(root.get(LAST_NAME_FIELD)), likeValue),
                cb.like(cb.lower(root.get(EMAIL_FIELD)), likeValue)
        );
    }

    private Specification<User> buildRoleSpecification(RoleName roleName) {
        return (root, query, cb) -> cb.equal(root.get("role").get("name"), roleName);
    }

    private Specification<User> buildStatusSpecification(UserStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private RoleName parseRoleName(String role) {
        try {
            return RoleName.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid role filter: " + role);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private PaginatedResponse<UserResponse> toPaginatedResponse(Page<User> userPage) {
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

    public String exportOrganizationUsersCsv(String search, String role, UserStatus status, UUID currentUserId){

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException(USER_NO_EXIST + currentUserId));

        if(currentUser.getOrganization() == null){
            throw new UserOrganizationNotFoundException("Organization not found.");
        }

        UUID organizationId = currentUser.getOrganization().getId();

        Specification<User> spec = buildOrganizationUsersSpecification(organizationId, search, role, status);

        List<User> users = userRepository.findAll(spec, Sort.by(FIRST_NAME_FIELD).ascending());

        StringBuilder csv = new StringBuilder();
        csv.append(writeHeader(List.of(
                "id", EMAIL_FIELD, FIRST_NAME_FIELD, LAST_NAME_FIELD, "role", "status", "organizationId"
        )));

        for (User user : users) {
            csv.append(writeBody(List.of(
                    escapeCsv(user.getId() != null ? user.getId().toString() : ""),
                    escapeCsv(user.getEmail()),
                    escapeCsv(user.getFirstName()),
                    escapeCsv(user.getLastName()),
                    escapeCsv(user.getRole() != null && user.getRole().getName() != null ? user.getRole().getName().name() : ""),
                    escapeCsv(user.getStatus() != null ? user.getStatus().name() : ""),
                    escapeCsv(user.getOrganization() != null && user.getOrganization().getId() != null
                            ? user.getOrganization().getId().toString()
                            : "")
            )));
        }

        return csv.toString();
    }

    public static String writeHeader(List<String> headers){
        return String.join(DELIMITER, headers) + LINE_SEPARATOR;
    }

    public static String writeBody(List<String> body){
        return String.join(DELIMITER, body) + LINE_SEPARATOR;
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "\"\"";
        }

        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
