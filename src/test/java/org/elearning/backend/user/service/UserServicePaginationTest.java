package org.elearning.backend.user.service;

import org.elearning.backend.common.dto.response.PaginatedResponse;
import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.role.repository.RoleRepository;
import org.elearning.backend.user.dto.response.UserResponse;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.entity.UserStatus;
import org.elearning.backend.user.exception.UserNotFoundException;
import org.elearning.backend.user.repository.UserRepository;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.auth.service.ActivationTokenService;
import org.elearning.backend.auth.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServicePaginationTest {

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
    void getAllUsersPaginated_shouldReturnPaginatedResponseWithMetadata() {
        User first = buildUser("ana@example.com", "Ana", "Ionescu", RoleName.STUDENT, UserStatus.ACTIVE, UUID.randomUUID());
        User second = buildUser("dan@example.com", "Dan", "Popescu", RoleName.TEACHER, UserStatus.INACTIVE, UUID.randomUUID());

        Page<User> page = new PageImpl<>(
                List.of(first, second),
                PageRequest.of(0, 2, Sort.by("firstName").ascending()),
                5
        );

        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PaginatedResponse<UserResponse> response =
                userService.getAllUsersPaginated(0, 2, null, null, null, "firstName", "asc");

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(2);
        assertThat(response.getTotalElements()).isEqualTo(5L);
        assertThat(response.getContent().get(0).getEmail()).isEqualTo("ana@example.com");
        assertThat(response.getContent().get(1).getEmail()).isEqualTo("dan@example.com");
    }

    @Test
    void getAllUsersPaginated_shouldUseDefaultPaginationAndSorting_whenParamsAreNull() {
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        userService.getAllUsersPaginated(null, null, null, null, null, null, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(any(Specification.class), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("firstName")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("firstName").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void getAllUsersPaginated_shouldUseRequestedSortFieldAndDirection() {
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        userService.getAllUsersPaginated(1, 5, "ana", "STUDENT", UserStatus.ACTIVE, "createdAt", "desc");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(any(Specification.class), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getAllUsersPaginated_shouldThrowWhenSortFieldIsInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.getAllUsersPaginated(0, 10, null, null, null, "organizationId", "asc"));

        verify(userRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getAllUsersPaginated_shouldThrowWhenRoleFilterIsInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.getAllUsersPaginated(0, 10, null, "BAD_ROLE", null, "firstName", "asc"));

        verify(userRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getCurrentOrganizationUsersPaginated_shouldReturnPaginatedResponse() {
        UUID organizationId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        User currentUser = buildUser("admin@org.com", "Org", "Admin", RoleName.ORGANIZATION_ADMIN, UserStatus.ACTIVE, organizationId);
        currentUser.setId(currentUserId);

        User first = buildUser("student1@org.com", "Ana", "Ionescu", RoleName.STUDENT, UserStatus.ACTIVE, organizationId);
        User second = buildUser("teacher1@org.com", "Dan", "Popescu", RoleName.TEACHER, UserStatus.INACTIVE, organizationId);

        Page<User> page = new PageImpl<>(
                List.of(first, second),
                PageRequest.of(0, 2, Sort.by("email").ascending()),
                2
        );

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PaginatedResponse<UserResponse> response =
                userService.getCurrentOrganizationUsersPaginated(
                        currentUserId, 0, 2, "org", "TEACHER", UserStatus.INACTIVE, "email", "asc"
                );

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(2);
        assertThat(response.getTotalElements()).isEqualTo(2L);
    }

    @Test
    void getCurrentOrganizationUsersPaginated_shouldThrowWhenCurrentUserDoesNotExist() {
        UUID currentUserId = UUID.randomUUID();
        when(userRepository.findById(currentUserId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.getCurrentOrganizationUsersPaginated(
                        currentUserId, 0, 10, null, null, null, null, null
                ));
    }

    @Test
    void getCurrentOrganizationUsersPaginated_shouldThrowWhenCurrentUserHasNoOrganization() {
        UUID currentUserId = UUID.randomUUID();
        User currentUser = buildUserWithoutOrganization("admin@platform.com", "Platform", "Admin", RoleName.ORGANIZATION_ADMIN, UserStatus.ACTIVE);
        currentUser.setId(currentUserId);

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));

        assertThrows(IllegalArgumentException.class,
                () -> userService.getCurrentOrganizationUsersPaginated(
                        currentUserId, 0, 10, null, null, null, null, null
                ));

        verify(userRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getCurrentOrganizationUsersPaginated_shouldThrowWhenSortFieldIsInvalid() {
        UUID organizationId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        User currentUser = buildUser("admin@org.com", "Org", "Admin", RoleName.ORGANIZATION_ADMIN, UserStatus.ACTIVE, organizationId);
        currentUser.setId(currentUserId);

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));

        assertThrows(IllegalArgumentException.class,
                () -> userService.getCurrentOrganizationUsersPaginated(
                        currentUserId, 0, 10, null, null, null, "createdAt", "asc"
                ));

        verify(userRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getCurrentOrganizationUsersPaginated_shouldThrowWhenRoleFilterIsInvalid() {
        UUID organizationId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        User currentUser = buildUser("admin@org.com", "Org", "Admin", RoleName.ORGANIZATION_ADMIN, UserStatus.ACTIVE, organizationId);
        currentUser.setId(currentUserId);

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));

        assertThrows(IllegalArgumentException.class,
                () -> userService.getCurrentOrganizationUsersPaginated(
                        currentUserId, 0, 10, null, "NOT_A_ROLE", null, "firstName", "asc"
                ));

        verify(userRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    private User buildUser(String email, String firstName, String lastName, RoleName roleName, UserStatus status, UUID organizationId) {
        User user = new User();
        user.setId(UUID.randomUUID());
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

    private User buildUserWithoutOrganization(String email, String firstName, String lastName, RoleName roleName, UserStatus status) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setStatus(status);

        Role role = new Role();
        role.setName(roleName);
        user.setRole(role);

        return user;
    }
}