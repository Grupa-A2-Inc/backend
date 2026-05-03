package org.elearning.backend.user.service;

import org.elearning.backend.common.dto.response.PaginatedResponse;
import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.role.repository.RoleRepository;
import org.elearning.backend.user.dto.request.UserPaginationRequest;
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
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.*;
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
        assertThat(response.getPage()).isZero();
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
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("firstName")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("firstName").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void getAllUsersPaginated_shouldUseDefaults_whenParamsAreBlankOrInvalid() {
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        userService.getAllUsersPaginated(-1, 0, " ", " ", null, " ", " ");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(any(Specification.class), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
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
                        currentUserId,
                        new UserPaginationRequest(0, 2, "org", "TEACHER", UserStatus.INACTIVE, "email", "asc")
                );

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getPage()).isZero();
        assertThat(response.getSize()).isEqualTo(2);
        assertThat(response.getTotalElements()).isEqualTo(2L);
    }

    @Test
    void getCurrentOrganizationUsersPaginated_shouldUseDefaults_whenRequestIsNull() {
        UUID organizationId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        User currentUser = buildUser("admin@org.com", "Org", "Admin", RoleName.ORGANIZATION_ADMIN, UserStatus.ACTIVE, organizationId);
        currentUser.setId(currentUserId);

        Page<User> page = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 10, Sort.by("firstName").ascending()),
                0
        );

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PaginatedResponse<UserResponse> response =
                userService.getCurrentOrganizationUsersPaginated(currentUserId, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(any(Specification.class), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("firstName")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("firstName").getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(response.getPage()).isZero();
        assertThat(response.getSize()).isEqualTo(10);
    }

    @Test
    void getCurrentOrganizationUsersPaginated_shouldBuildSpecificationForAllFilters() {
        UUID organizationId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        User currentUser = buildUser("admin@org.com", "Org", "Admin", RoleName.ORGANIZATION_ADMIN, UserStatus.ACTIVE, organizationId);
        currentUser.setId(currentUserId);

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        userService.getCurrentOrganizationUsersPaginated(
                currentUserId,
                new UserPaginationRequest(0, 10, " Ana ", "TEACHER", UserStatus.ACTIVE, "email", "asc")
        );

        ArgumentCaptor<Specification<User>> specificationCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(userRepository).findAll(specificationCaptor.capture(), any(Pageable.class));

        Specification<User> specification = specificationCaptor.getValue();

        @SuppressWarnings("unchecked")
        Root<User> root = mock(Root.class);
        @SuppressWarnings("unchecked")
        CriteriaQuery<Object> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);

        @SuppressWarnings("unchecked")
        Path<Object> organizationPath = mock(Path.class);
        @SuppressWarnings("unchecked")
        Path<Object> organizationIdPath = mock(Path.class);
        @SuppressWarnings("unchecked")
        Path<String> firstNamePath = mock(Path.class);
        @SuppressWarnings("unchecked")
        Path<String> lastNamePath = mock(Path.class);
        @SuppressWarnings("unchecked")
        Path<String> emailPath = mock(Path.class);
        @SuppressWarnings("unchecked")
        Expression<String> lowerFirstName = mock(Expression.class);
        @SuppressWarnings("unchecked")
        Expression<String> lowerLastName = mock(Expression.class);
        @SuppressWarnings("unchecked")
        Expression<String> lowerEmail = mock(Expression.class);
        @SuppressWarnings("unchecked")
        Path<Object> rolePath = mock(Path.class);
        @SuppressWarnings("unchecked")
        Path<Object> roleNamePath = mock(Path.class);
        @SuppressWarnings("unchecked")
        Path<Object> statusPath = mock(Path.class);

        Predicate organizationPredicate = mock(Predicate.class);
        Predicate firstNamePredicate = mock(Predicate.class);
        Predicate lastNamePredicate = mock(Predicate.class);
        Predicate emailPredicate = mock(Predicate.class);
        Predicate searchPredicate = mock(Predicate.class);
        Predicate rolePredicate = mock(Predicate.class);
        Predicate statusPredicate = mock(Predicate.class);
        Predicate organizationAndSearchPredicate = mock(Predicate.class);
        Predicate withRolePredicate = mock(Predicate.class);
        Predicate finalPredicate = mock(Predicate.class);

        when(root.<Object>get("organization")).thenReturn(organizationPath);
        when(organizationPath.get("id")).thenReturn(organizationIdPath);
        when(root.<String>get("firstName")).thenReturn(firstNamePath);
        when(root.<String>get("lastName")).thenReturn(lastNamePath);
        when(root.<String>get("email")).thenReturn(emailPath);
        when(root.<Object>get("role")).thenReturn(rolePath);
        when(rolePath.get("name")).thenReturn(roleNamePath);
        when(root.<Object>get("status")).thenReturn(statusPath);

        when(criteriaBuilder.equal(organizationIdPath, organizationId)).thenReturn(organizationPredicate);
        when(criteriaBuilder.lower(firstNamePath)).thenReturn(lowerFirstName);
        when(criteriaBuilder.lower(lastNamePath)).thenReturn(lowerLastName);
        when(criteriaBuilder.lower(emailPath)).thenReturn(lowerEmail);
        when(criteriaBuilder.like(lowerFirstName, "%ana%")).thenReturn(firstNamePredicate);
        when(criteriaBuilder.like(lowerLastName, "%ana%")).thenReturn(lastNamePredicate);
        when(criteriaBuilder.like(lowerEmail, "%ana%")).thenReturn(emailPredicate);
        when(criteriaBuilder.or(firstNamePredicate, lastNamePredicate, emailPredicate)).thenReturn(searchPredicate);
        when(criteriaBuilder.equal(roleNamePath, RoleName.TEACHER)).thenReturn(rolePredicate);
        when(criteriaBuilder.equal(statusPath, UserStatus.ACTIVE)).thenReturn(statusPredicate);
        when(criteriaBuilder.and(organizationPredicate, searchPredicate)).thenReturn(organizationAndSearchPredicate);
        when(criteriaBuilder.and(organizationAndSearchPredicate, rolePredicate)).thenReturn(withRolePredicate);
        when(criteriaBuilder.and(withRolePredicate, statusPredicate)).thenReturn(finalPredicate);

        Predicate predicate = specification.toPredicate(root, query, criteriaBuilder);

        assertThat(predicate).isSameAs(finalPredicate);
        verify(criteriaBuilder).equal(organizationIdPath, organizationId);
        verify(criteriaBuilder).or(firstNamePredicate, lastNamePredicate, emailPredicate);
        verify(criteriaBuilder).equal(roleNamePath, RoleName.TEACHER);
        verify(criteriaBuilder).equal(statusPath, UserStatus.ACTIVE);
    }

    @Test
    void getCurrentOrganizationUsersPaginated_shouldThrowWhenCurrentUserDoesNotExist() {
        UUID currentUserId = UUID.randomUUID();
        UserPaginationRequest request = new UserPaginationRequest(0, 10, null, null, null, null, null);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.getCurrentOrganizationUsersPaginated(currentUserId, request));
    }

    @Test
    void getCurrentOrganizationUsersPaginated_shouldThrowWhenCurrentUserHasNoOrganization() {
        UUID currentUserId = UUID.randomUUID();
        UserPaginationRequest request = new UserPaginationRequest(0, 10, null, null, null, null, null);
        User currentUser = buildUserWithoutOrganization("admin@platform.com", "Platform", "Admin", RoleName.ORGANIZATION_ADMIN, UserStatus.ACTIVE);
        currentUser.setId(currentUserId);

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));

        assertThrows(IllegalArgumentException.class,
                () -> userService.getCurrentOrganizationUsersPaginated(currentUserId, request));

        verify(userRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getCurrentOrganizationUsersPaginated_shouldThrowWhenSortFieldIsInvalid() {
        UUID organizationId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        UserPaginationRequest request = new UserPaginationRequest(0, 10, null, null, null, "createdAt", "asc");

        User currentUser = buildUser("admin@org.com", "Org", "Admin", RoleName.ORGANIZATION_ADMIN, UserStatus.ACTIVE, organizationId);
        currentUser.setId(currentUserId);

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));

        assertThrows(IllegalArgumentException.class,
                () -> userService.getCurrentOrganizationUsersPaginated(currentUserId, request));

        verify(userRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getCurrentOrganizationUsersPaginated_shouldThrowWhenRoleFilterIsInvalid() {
        UUID organizationId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        UserPaginationRequest request = new UserPaginationRequest(0, 10, null, "NOT_A_ROLE", null, "firstName", "asc");

        User currentUser = buildUser("admin@org.com", "Org", "Admin", RoleName.ORGANIZATION_ADMIN, UserStatus.ACTIVE, organizationId);
        currentUser.setId(currentUserId);

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));

        assertThrows(IllegalArgumentException.class,
                () -> userService.getCurrentOrganizationUsersPaginated(currentUserId, request));

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
