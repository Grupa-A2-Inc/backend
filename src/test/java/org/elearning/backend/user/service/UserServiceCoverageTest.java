package org.elearning.backend.user.service;

import org.elearning.backend.ai.exception.AiApiException;
import org.elearning.backend.ai.service.AiStudentRegistrationService;
import org.elearning.backend.auth.service.ActivationTokenService;
import org.elearning.backend.auth.service.EmailService;
import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.parent.entity.Parent;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.role.repository.RoleRepository;
import org.elearning.backend.student.entity.Student;
import org.elearning.backend.user.dto.request.ChangePasswordRequest;
import org.elearning.backend.user.dto.request.CreateUserRequest;
import org.elearning.backend.user.dto.request.UpdateUserRequest;
import org.elearning.backend.user.dto.request.UpdateUserStatusRequest;
import org.elearning.backend.user.dto.response.UserResponse;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.entity.UserStatus;
import org.elearning.backend.user.exception.UserAlreadyExistsException;
import org.elearning.backend.user.exception.UserBadRequestException;
import org.elearning.backend.user.exception.UserNotFoundException;
import org.elearning.backend.user.exception.UserOrganizationNotFoundException;
import org.elearning.backend.user.exception.UserRoleNotFoundException;
import org.elearning.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@org.springframework.test.context.ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class UserServiceCoverageTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ActivationTokenService activationTokenService;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AiStudentRegistrationService aiStudentRegistrationService;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_duplicateEmail_throwsException() {
        CreateUserRequest request = createUserRequest(RoleName.STUDENT, null);
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("Email already exists: ana@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_missingRole_throwsException() {
        CreateUserRequest request = createUserRequest(RoleName.STUDENT, null);
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName(RoleName.STUDENT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(UserRoleNotFoundException.class)
                .hasMessage("Role does not exist: STUDENT");

        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_missingOrganization_throwsException() {
        UUID organizationId = UUID.randomUUID();
        CreateUserRequest request = createUserRequest(RoleName.TEACHER, organizationId);
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName(RoleName.TEACHER)).thenReturn(Optional.of(new Role(RoleName.TEACHER)));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(UserOrganizationNotFoundException.class)
                .hasMessage("Organization not found: " + organizationId);

        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_roleVariants_createCorrectPendingUserAndSendActivationEmail() {
        assertCreatedUserHasType(RoleName.STUDENT, Student.class);
        assertCreatedUserHasType(RoleName.PARENT, Parent.class);
        assertCreatedUserHasType(RoleName.TEACHER, User.class);
    }

    @Test
    void createUser_withoutOrganization_mapsNullOrganization() {
        CreateUserRequest request = createUserRequest(RoleName.ADMIN, null);
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.of(new Role(RoleName.ADMIN)));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(activationTokenService.generateActivationToken(any(User.class))).thenReturn("activation-token");

        UserResponse response = userService.createUser(request);

        assertThat(response.getOrganizationId()).isNull();
        assertThat(response.getStatus()).isEqualTo(UserStatus.PENDING);
        verify(emailService).sendActivationEmail("ana@example.com", "Ana", "activation-token");
    }

    @Test
    void createUser_studentRegistrationFailure_stopsBeforeActivationEmail() {
        UUID organizationId = UUID.randomUUID();
        CreateUserRequest request = createUserRequest(RoleName.STUDENT, organizationId);
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName(RoleName.STUDENT)).thenReturn(Optional.of(new Role(RoleName.STUDENT)));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization(organizationId)));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        doThrow(new AiApiException("AI sync failed"))
                .when(aiStudentRegistrationService)
                .registerStudent(any(UUID.class));

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(AiApiException.class)
                .hasMessage("AI sync failed");

        verify(aiStudentRegistrationService).registerStudent(any(UUID.class));
        verify(activationTokenService, never()).generateActivationToken(any(User.class));
        verify(emailService, never()).sendActivationEmail(any(), any(), any());
    }

    @Test
    void getUserById_foundAndMissingPaths() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, RoleName.ADMIN, null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserById(userId);

        assertThat(response.getId()).isEqualTo(userId);
        assertThat(response.getRoleName()).isEqualTo(RoleName.ADMIN);
        assertThat(response.getOrganizationId()).isNull();

        UUID missingId = UUID.randomUUID();
        when(userRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(missingId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User does not exist: " + missingId);
    }

    @Test
    void updateUser_updatesWithAndWithoutOrganizationAndHandlesMissingPaths() {
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        Organization organization = organization(organizationId);
        User user = user(userId, RoleName.TEACHER, null);
        UpdateUserRequest request = UpdateUserRequest.builder()
                .email("updated@example.com")
                .firstName("Updated")
                .lastName("Teacher")
                .organizationId(organizationId)
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(userRepository.save(user)).thenReturn(user);

        UserResponse response = userService.updateUser(userId, request);

        assertThat(response.getEmail()).isEqualTo("updated@example.com");
        assertThat(response.getOrganizationId()).isEqualTo(organizationId);
        assertThat(user.getUpdatedAt()).isNotNull();

        UUID noOrgUserId = UUID.randomUUID();
        User noOrgUser = user(noOrgUserId, RoleName.STUDENT, null);
        UpdateUserRequest noOrgRequest = UpdateUserRequest.builder()
                .email("no-org@example.com")
                .firstName("No")
                .lastName("Org")
                .build();
        when(userRepository.findById(noOrgUserId)).thenReturn(Optional.of(noOrgUser));
        when(userRepository.save(noOrgUser)).thenReturn(noOrgUser);

        UserResponse noOrgResponse = userService.updateUser(noOrgUserId, noOrgRequest);

        assertThat(noOrgResponse.getOrganizationId()).isNull();

        UUID missingUserId = UUID.randomUUID();
        when(userRepository.findById(missingUserId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.updateUser(missingUserId, noOrgRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User does not exist: " + missingUserId);

        UUID missingOrgUserId = UUID.randomUUID();
        UUID missingOrgId = UUID.randomUUID();
        User missingOrgUser = user(missingOrgUserId, RoleName.TEACHER, null);
        UpdateUserRequest missingOrgRequest = UpdateUserRequest.builder()
                .organizationId(missingOrgId)
                .build();
        when(userRepository.findById(missingOrgUserId)).thenReturn(Optional.of(missingOrgUser));
        when(organizationRepository.findById(missingOrgId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(missingOrgUserId, missingOrgRequest))
                .isInstanceOf(UserOrganizationNotFoundException.class)
                .hasMessage("Organization not found: " + missingOrgId);
    }

    @Test
    void updateUserStatus_updatesStatusAndHandlesMissingUser() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, RoleName.STUDENT, null);
        UpdateUserStatusRequest request = UpdateUserStatusRequest.builder()
                .status(UserStatus.BLOCKED)
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.updateUserStatus(userId, request);

        assertThat(user.getStatus()).isEqualTo(UserStatus.BLOCKED);
        assertThat(user.getUpdatedAt()).isNotNull();
        verify(userRepository).save(user);

        UUID missingUserId = UUID.randomUUID();
        when(userRepository.findById(missingUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserStatus(missingUserId, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User does not exist: " + missingUserId);
    }

    @Test
    void deleteUser_deletesExistingAndHandlesMissing() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(true);

        userService.deleteUser(userId);

        verify(userRepository).deleteById(userId);

        UUID missingUserId = UUID.randomUUID();
        when(userRepository.existsById(missingUserId)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(missingUserId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User does not exist: " + missingUserId);
    }

    @Test
    void changePassword_coversValidationAndSuccessPaths() {
        UUID missingUserId = UUID.randomUUID();
        ChangePasswordRequest request = changePasswordRequest("old", "new", "new");
        when(userRepository.findById(missingUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changePassword(missingUserId, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User does not exist: " + missingUserId);

        UUID userId = UUID.randomUUID();
        User user = user(userId, RoleName.STUDENT, null);
        user.setPasswordHash("old-hash");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "old-hash")).thenReturn(false);

        ChangePasswordRequest badCurrentPassword = changePasswordRequest("bad", "new", "new");
        assertThatThrownBy(() -> userService.changePassword(userId, badCurrentPassword))
                .isInstanceOf(UserBadRequestException.class)
                .hasMessage("Current password is incorrect");

        when(passwordEncoder.matches("old", "old-hash")).thenReturn(true);
        ChangePasswordRequest mismatchedConfirmation = changePasswordRequest("old", "new", "other");
        assertThatThrownBy(() -> userService.changePassword(userId, mismatchedConfirmation))
                .isInstanceOf(UserBadRequestException.class)
                .hasMessage("Passwords do not match");

        when(passwordEncoder.encode("new")).thenReturn("new-hash");
        userService.changePassword(userId, request);

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(user.getUpdatedAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void getUsersByOrganizationId_returnsMappedUsers() {
        UUID organizationId = UUID.randomUUID();
        Organization organization = organization(organizationId);
        User user = user(UUID.randomUUID(), RoleName.TEACHER, organization);
        when(userRepository.findByOrganizationId(organizationId)).thenReturn(List.of(user));

        List<UserResponse> response = userService.getUsersByOrganizationId(organizationId);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getOrganizationId()).isEqualTo(organizationId);
    }

    private void assertCreatedUserHasType(RoleName roleName, Class<? extends User> expectedType) {
        UUID organizationId = UUID.randomUUID();
        Organization organization = organization(organizationId);
        Role role = new Role(roleName);
        CreateUserRequest request = createUserRequest(roleName, organizationId);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName(roleName)).thenReturn(Optional.of(role));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(activationTokenService.generateActivationToken(any(User.class))).thenReturn("activation-token");

        UserResponse response = userService.createUser(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser).isInstanceOf(expectedType);
        assertThat(savedUser.getPasswordHash()).isNull();
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(response.getRoleName()).isEqualTo(roleName);
        if (roleName == RoleName.STUDENT) {
            verify(aiStudentRegistrationService).registerStudent(savedUser.getId());
        } else {
            verify(aiStudentRegistrationService, never()).registerStudent(any(UUID.class));
        }
        verify(emailService).sendActivationEmail(request.getEmail(), request.getFirstName(), "activation-token");
        clearInvocations(userRepository, emailService, aiStudentRegistrationService);
    }

    private CreateUserRequest createUserRequest(RoleName roleName, UUID organizationId) {
        return CreateUserRequest.builder()
                .email("ana@example.com")
                .firstName("Ana")
                .lastName("Pop")
                .roleName(roleName)
                .organizationId(organizationId)
                .build();
    }

    private ChangePasswordRequest changePasswordRequest(String currentPassword, String newPassword, String confirmPassword) {
        return ChangePasswordRequest.builder()
                .currentPassword(currentPassword)
                .newPassword(newPassword)
                .newPasswordConfirm(confirmPassword)
                .build();
    }

    private User user(UUID id, RoleName roleName, Organization organization) {
        User user = new User();
        user.setId(id);
        user.setEmail("user-" + id + "@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(new Role(roleName));
        user.setOrganization(organization);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private Organization organization(UUID id) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setName("Org");
        return organization;
    }
}
