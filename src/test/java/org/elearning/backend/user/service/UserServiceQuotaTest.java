package org.elearning.backend.user.service;

import org.elearning.backend.ai.service.AiStudentRegistrationService;
import org.elearning.backend.ai.exception.AiApiException;
import org.elearning.backend.auth.service.ActivationTokenService;
import org.elearning.backend.auth.service.EmailService;
import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.role.repository.RoleRepository;
import org.elearning.backend.subscription.exception.UserLimitExceededException;
import org.elearning.backend.subscription.service.EntitlementService;
import org.elearning.backend.user.dto.request.CreateUserBulkRequest;
import org.elearning.backend.user.dto.request.CreateUserRequest;
import org.elearning.backend.user.dto.response.BulkImportResponse;
import org.elearning.backend.user.dto.response.UserResponse;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceQuotaTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private ActivationTokenService activationTokenService;
    @Mock private EmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AiStudentRegistrationService aiStudentRegistrationService;
    @Mock private EntitlementService entitlementService;

    private UserService userService;
    private UserImportService userImportService;

    private UUID organizationId;
    private Organization organization;
    private Role studentRole;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                roleRepository,
                organizationRepository,
                activationTokenService,
                emailService,
                passwordEncoder,
                aiStudentRegistrationService,
                entitlementService
        );

        userImportService = new UserImportService(userService);

        organizationId = UUID.randomUUID();

        organization = new Organization();
        organization.setId(organizationId);
        organization.setName("Test Org");
        organization.setCountry("Romania");
        organization.setCity("Iasi");
        organization.setOrganizationType("School");

        studentRole = new Role();
        studentRole.setName(RoleName.STUDENT);
    }

    // --- createUser sub limită ---

    @Test
    void createUser_whenBelowQuota_createsUserSuccessfully() {
        CreateUserRequest request = buildRequest("student@test.com", organizationId);

        when(userRepository.existsByEmail("student@test.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.STUDENT)).thenReturn(Optional.of(studentRole));
        doNothing().when(entitlementService).canCreateUser(organizationId);
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            u.setRole(studentRole);
            u.setOrganization(organization);
            return u;
        });
        when(activationTokenService.generateActivationToken(any())).thenReturn("token123");
        doNothing().when(emailService).sendActivationEmail(any(), any(), any());

        UserResponse response = userService.createUser(request);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("student@test.com");
        verify(entitlementService).canCreateUser(organizationId);
        verify(userRepository).save(any(User.class));
    }

    // --- createUser peste limită ---

    @Test
    void createUser_whenQuotaExceeded_throwsUserLimitExceededException() {
        CreateUserRequest request = buildRequest("student@test.com", organizationId);

        when(userRepository.existsByEmail("student@test.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.STUDENT)).thenReturn(Optional.of(studentRole));
        doThrow(new UserLimitExceededException(organizationId, 10))
                .when(entitlementService).canCreateUser(organizationId);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(UserLimitExceededException.class)
                .hasMessageContaining("10");

        verify(userRepository, never()).save(any());
    }

    // --- createUser fără organizație — quota skip ---

    @Test
    void createUser_whenNoOrganization_skipsQuotaCheck() {
        CreateUserRequest request = buildRequest("admin@test.com", null);

        when(userRepository.existsByEmail("admin@test.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.STUDENT)).thenReturn(Optional.of(studentRole));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            u.setRole(studentRole);
            return u;
        });
        when(activationTokenService.generateActivationToken(any())).thenReturn("token123");
        doNothing().when(emailService).sendActivationEmail(any(), any(), any());

        userService.createUser(request);

        verify(entitlementService, never()).canCreateUser(any());
    }

    // --- import bulk sub limită ---

    @Test
    void importUsers_whenAllBelowQuota_allSucceed() {
        List<CreateUserRequest> requests = List.of(
                buildRequest("u1@test.com", organizationId),
                buildRequest("u2@test.com", organizationId),
                buildRequest("u3@test.com", organizationId)
        );

        for (CreateUserRequest req : requests) {
            when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
        }
        when(roleRepository.findByName(RoleName.STUDENT)).thenReturn(Optional.of(studentRole));
        doNothing().when(entitlementService).canCreateUser(organizationId);
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            u.setRole(studentRole);
            u.setOrganization(organization);
            return u;
        });
        when(activationTokenService.generateActivationToken(any())).thenReturn("token123");
        doNothing().when(emailService).sendActivationEmail(any(), any(), any());

        BulkImportResponse response = userImportService.importUsers(new CreateUserBulkRequest(requests));

        assertThat(response.getTotal()).isEqualTo(3);
        assertThat(response.getSucceeded()).isEqualTo(3);
        assertThat(response.getFailed()).isZero();
    }

    // --- import bulk peste limită — import parțial ---

    @Test
    void importUsers_whenQuotaExceededMidImport_partialSuccessWithFailedMarked() {
        List<CreateUserRequest> requests = List.of(
                buildRequest("u1@test.com", organizationId),
                buildRequest("u2@test.com", organizationId),
                buildRequest("u3@test.com", organizationId)
        );

        // primii 2 reușesc
        when(userRepository.existsByEmail("u1@test.com")).thenReturn(false);
        when(userRepository.existsByEmail("u2@test.com")).thenReturn(false);
        when(userRepository.existsByEmail("u3@test.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.STUDENT)).thenReturn(Optional.of(studentRole));
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(activationTokenService.generateActivationToken(any())).thenReturn("token123");
        doNothing().when(emailService).sendActivationEmail(any(), any(), any());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            u.setRole(studentRole);
            u.setOrganization(organization);
            return u;
        });

        // al 3-lea apel la canCreateUser aruncă excepție
        doNothing()
                .doNothing()
                .doThrow(new UserLimitExceededException(organizationId, 2))
                .when(entitlementService).canCreateUser(organizationId);

        BulkImportResponse response = userImportService.importUsers(new CreateUserBulkRequest(requests));

        assertThat(response.getTotal()).isEqualTo(3);
        assertThat(response.getSucceeded()).isEqualTo(2);
        assertThat(response.getFailed()).isEqualTo(1);
        assertThat(response.getResults().get(2).isSuccess()).isFalse();
        assertThat(response.getResults().get(2).getEmail()).isEqualTo("u3@test.com");
        assertThat(response.getResults().get(2).getErrorMessage()).contains("2");
    }

    // --- import bulk — toți peste limită ---

    @Test
    void importUsers_whenAllExceedQuota_allFailed() {
        List<CreateUserRequest> requests = List.of(
                buildRequest("u1@test.com", organizationId),
                buildRequest("u2@test.com", organizationId)
        );

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(RoleName.STUDENT)).thenReturn(Optional.of(studentRole));
        doThrow(new UserLimitExceededException(organizationId, 0))
                .when(entitlementService).canCreateUser(organizationId);

        BulkImportResponse response = userImportService.importUsers(new CreateUserBulkRequest(requests));

        assertThat(response.getTotal()).isEqualTo(2);
        assertThat(response.getSucceeded()).isZero();
        assertThat(response.getFailed()).isEqualTo(2);
        response.getResults().forEach(r -> {
            assertThat(r.isSuccess()).isFalse();
            assertThat(r.getErrorMessage()).isNotBlank();
        });
    }

    @Test
    void importUsers_whenAiStudentRegistrationFails_marksStudentAsFailed() {
        CreateUserRequest request = buildRequest("student-ai-fail@test.com", organizationId);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName(RoleName.STUDENT)).thenReturn(Optional.of(studentRole));
        doNothing().when(entitlementService).canCreateUser(organizationId);
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            u.setRole(studentRole);
            u.setOrganization(organization);
            return u;
        });
        doThrow(new AiApiException("AI sync failed"))
                .when(aiStudentRegistrationService)
                .registerStudent(any(UUID.class));

        BulkImportResponse response = userImportService.importUsers(new CreateUserBulkRequest(List.of(request)));

        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getSucceeded()).isZero();
        assertThat(response.getFailed()).isEqualTo(1);
        assertThat(response.getResults().get(0).isSuccess()).isFalse();
        assertThat(response.getResults().get(0).getEmail()).isEqualTo(request.getEmail());
        assertThat(response.getResults().get(0).getErrorMessage()).contains("AI sync failed");
        verify(activationTokenService, never()).generateActivationToken(any());
        verify(emailService, never()).sendActivationEmail(any(), any(), any());
    }

    // --- helpers ---

    private CreateUserRequest buildRequest(String email, UUID orgId) {
        return CreateUserRequest.builder()
                .email(email)
                .firstName("Ion")
                .lastName("Pop")
                .roleName(RoleName.STUDENT)
                .organizationId(orgId)
                .build();
    }
}
