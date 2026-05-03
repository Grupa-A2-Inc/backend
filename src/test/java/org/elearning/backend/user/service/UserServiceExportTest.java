package org.elearning.backend.user.service;

import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.role.repository.RoleRepository;
import org.elearning.backend.auth.service.ActivationTokenService;
import org.elearning.backend.auth.service.EmailService;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.entity.UserStatus;
import org.elearning.backend.user.exception.UserNotFoundException;
import org.elearning.backend.user.exception.UserOrganizationNotFoundException;
import org.elearning.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceExportTest {

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

    @InjectMocks
    private UserService userService;

    @Test
    void exportOrganizationUsersCsv_shouldReturnHeaderAndRows() {
        UUID organizationId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        User currentUser = buildUser(
                currentUserId,
                "orgadmin@test.com",
                "Org",
                "Admin",
                RoleName.ORGANIZATION_ADMIN,
                UserStatus.ACTIVE,
                organizationId
        );

        User first = buildUser(
                UUID.randomUUID(),
                "ana@test.com",
                "Ana",
                "Ionescu",
                RoleName.STUDENT,
                UserStatus.ACTIVE,
                organizationId
        );

        User second = buildUser(
                UUID.randomUUID(),
                "dan@test.com",
                "Dan",
                "Popescu",
                RoleName.TEACHER,
                UserStatus.BLOCKED,
                organizationId
        );

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(userRepository.findAll(org.mockito.ArgumentMatchers.<Specification<User>>any(), any(Sort.class)))
                .thenReturn(List.of(first, second));
        String csv = userService.exportOrganizationUsersCsv(null, null, null, currentUserId);

        assertThat(csv)
                .contains("id,email,firstName,lastName,role,status,organizationId")
                .contains("ana@test.com")
                .contains("dan@test.com")
                .contains("STUDENT")
                .contains("TEACHER")
                .contains("ACTIVE")
                .contains("BLOCKED");

        verify(userRepository).findById(currentUserId);
        verify(userRepository).findAll(org.mockito.ArgumentMatchers.<Specification<User>>any(), eq(Sort.by("firstName").ascending()));
    }

    @Test
    void exportOrganizationUsersCsv_shouldUseAscendingSortByFirstName() {
        UUID organizationId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        User currentUser = buildUser(
                currentUserId,
                "orgadmin@test.com",
                "Org",
                "Admin",
                RoleName.ORGANIZATION_ADMIN,
                UserStatus.ACTIVE,
                organizationId
        );

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(userRepository.findAll(org.mockito.ArgumentMatchers.<Specification<User>>any(), any(Sort.class)))
                .thenReturn(List.of());

        userService.exportOrganizationUsersCsv("ana", "STUDENT", UserStatus.ACTIVE, currentUserId);

        verify(userRepository).findAll(org.mockito.ArgumentMatchers.<Specification<User>>any(), eq(Sort.by("firstName").ascending()));
    }

    @Test
    void exportOrganizationUsersCsv_shouldHandleNullAndQuotedFields() {
        UUID organizationId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        User currentUser = buildUser(
                currentUserId,
                "orgadmin@test.com",
                "Org",
                "Admin",
                RoleName.ORGANIZATION_ADMIN,
                UserStatus.ACTIVE,
                organizationId
        );

        User exportedUser = new User();
        exportedUser.setId(null);
        exportedUser.setEmail(null);
        exportedUser.setFirstName("An\"a");
        exportedUser.setLastName(null);
        exportedUser.setRole(null);
        exportedUser.setStatus(null);
        exportedUser.setOrganization(null);

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(userRepository.findAll(org.mockito.ArgumentMatchers.<Specification<User>>any(), any(Sort.class)))
                .thenReturn(List.of(exportedUser));

        String csv = userService.exportOrganizationUsersCsv(null, null, null, currentUserId);

        assertThat(csv).contains("\"\",\"\",\"An\"\"a\",\"\",\"\",\"\",\"\"");
    }

    @Test
    void exportOrganizationUsersCsv_shouldHandleMissingRoleNameAndOrganizationId() {
        UUID organizationId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        User currentUser = buildUser(
                currentUserId,
                "orgadmin@test.com",
                "Org",
                "Admin",
                RoleName.ORGANIZATION_ADMIN,
                UserStatus.ACTIVE,
                organizationId
        );

        User exportedUser = new User();
        exportedUser.setId(UUID.randomUUID());
        exportedUser.setEmail("partial@test.com");
        exportedUser.setFirstName("Partial");
        exportedUser.setLastName("User");

        Role role = new Role();
        exportedUser.setRole(role);

        Organization organization = new Organization();
        exportedUser.setOrganization(organization);

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(userRepository.findAll(org.mockito.ArgumentMatchers.<Specification<User>>any(), any(Sort.class)))
                .thenReturn(List.of(exportedUser));

        String csv = userService.exportOrganizationUsersCsv(null, null, null, currentUserId);

        assertThat(csv).contains("\"partial@test.com\",\"Partial\",\"User\",\"\",\"ACTIVE\",\"\"");
    }

    @Test
    void exportOrganizationUsersCsv_shouldThrowWhenCurrentUserDoesNotExist() {
        UUID currentUserId = UUID.randomUUID();
        when(userRepository.findById(currentUserId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.exportOrganizationUsersCsv(null, null, null, currentUserId));

        verify(userRepository, never()).findAll(org.mockito.ArgumentMatchers.<Specification<User>>any(), any(Sort.class));
    }

    @Test
    void exportOrganizationUsersCsv_shouldThrowWhenCurrentUserHasNoOrganization() {
        UUID currentUserId = UUID.randomUUID();

        User currentUser = new User();
        currentUser.setId(currentUserId);
        currentUser.setEmail("orgadmin@test.com");
        currentUser.setFirstName("Org");
        currentUser.setLastName("Admin");
        currentUser.setStatus(UserStatus.ACTIVE);

        Role role = new Role();
        role.setName(RoleName.ORGANIZATION_ADMIN);
        currentUser.setRole(role);

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));

        assertThrows(UserOrganizationNotFoundException.class,
                () -> userService.exportOrganizationUsersCsv(null, null, null, currentUserId));

        verify(userRepository, never()).findAll(org.mockito.ArgumentMatchers.<Specification<User>>any(), any(Sort.class));
    }

    @Test
    void exportOrganizationUsersCsv_shouldThrowWhenRoleFilterIsInvalid() {
        UUID organizationId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        User currentUser = buildUser(
                currentUserId,
                "orgadmin@test.com",
                "Org",
                "Admin",
                RoleName.ORGANIZATION_ADMIN,
                UserStatus.ACTIVE,
                organizationId
        );

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));

        assertThrows(IllegalArgumentException.class,
                () -> userService.exportOrganizationUsersCsv(null, "NOT_A_ROLE", null, currentUserId));

        verify(userRepository, never()).findAll(org.mockito.ArgumentMatchers.<Specification<User>>any(), any(Sort.class));
    }

    private User buildUser(
            UUID id,
            String email,
            String firstName,
            String lastName,
            RoleName roleName,
            UserStatus status,
            UUID organizationId
    ) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setStatus(status);

        Role role = new Role();
        role.setName(roleName);
        user.setRole(role);

        Organization organization = new Organization();
        organization.setId(organizationId);
        user.setOrganization(organization);

        return user;
    }
}
