package org.elearning.backend.security.access;

import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.elearning.backend.user.dto.request.CreateUserRequest;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.entity.UserStatus;
import org.elearning.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private AccessService accessService;

    @Test
    void canCreateUser_returnsFalseWhenAuthenticationIsMissing() {
        CreateUserRequest request = CreateUserRequest.builder().organizationId(UUID.randomUUID()).build();

        assertThat(accessService.canCreateUser(null, request)).isFalse();
    }

    @Test
    void canCreateUser_returnsTrueForAdmin() {
        CreateUserRequest request = CreateUserRequest.builder().organizationId(UUID.randomUUID()).build();

        assertThat(accessService.canCreateUser(authenticationFor(makeUser(RoleName.ADMIN, UUID.randomUUID())), request)).isTrue();
    }

    @Test
    void canCreateUser_returnsTrueForOrganizationAdminFromSameOrganization() {
        UUID organizationId = UUID.randomUUID();
        CreateUserRequest request = CreateUserRequest.builder().organizationId(organizationId).build();

        assertThat(accessService.canCreateUser(authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, organizationId)), request)).isTrue();
    }

    @Test
    void canCreateUser_returnsFalseForOrganizationAdminFromDifferentOrganization() {
        CreateUserRequest request = CreateUserRequest.builder().organizationId(UUID.randomUUID()).build();

        assertThat(accessService.canCreateUser(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, UUID.randomUUID())),
                request
        )).isFalse();
    }

    @Test
    void canCreateUser_returnsFalseForOrganizationAdminWithoutOrganization() {
        CreateUserRequest request = CreateUserRequest.builder().organizationId(UUID.randomUUID()).build();

        assertThat(accessService.canCreateUser(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, null)),
                request
        )).isFalse();
    }

    @Test
    void canViewUser_returnsFalseWhenAuthenticationPrincipalIsUnsupported() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("plain-user", "secret");

        assertThat(accessService.canViewUser(authentication, UUID.randomUUID())).isFalse();
    }

    @Test
    void canViewUser_returnsTrueForAdmin() {
        assertThat(accessService.canViewUser(
                authenticationFor(makeUser(RoleName.ADMIN, UUID.randomUUID())),
                UUID.randomUUID()
        )).isTrue();
    }

    @Test
    void canViewUser_returnsTrueWhenUserRequestsOwnRecord() {
        User currentUser = makeUser(RoleName.STUDENT, UUID.randomUUID());

        assertThat(accessService.canViewUser(authenticationFor(currentUser), currentUser.getId())).isTrue();
        verifyNoInteractions(userRepository);
    }

    @Test
    void canViewUser_returnsFalseForNonOrganizationAdminLookingUpAnotherUser() {
        User currentUser = makeUser(RoleName.TEACHER, UUID.randomUUID());

        assertThat(accessService.canViewUser(authenticationFor(currentUser), UUID.randomUUID())).isFalse();
        verifyNoInteractions(userRepository);
    }

    @Test
    void canViewUser_returnsTrueForOrganizationAdminInSameOrganization() {
        UUID organizationId = UUID.randomUUID();
        User targetUser = makeUser(RoleName.STUDENT, organizationId);
        when(userRepository.findById(targetUser.getId())).thenReturn(Optional.of(targetUser));

        assertThat(accessService.canViewUser(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, organizationId)),
                targetUser.getId()
        )).isTrue();
    }

    @Test
    void canViewUser_returnsFalseForOrganizationAdminInDifferentOrganization() {
        UUID currentOrganizationId = UUID.randomUUID();
        User targetUser = makeUser(RoleName.STUDENT, UUID.randomUUID());
        when(userRepository.findById(targetUser.getId())).thenReturn(Optional.of(targetUser));

        assertThat(accessService.canViewUser(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, currentOrganizationId)),
                targetUser.getId()
        )).isFalse();
    }

    @Test
    void canViewUser_returnsFalseWhenTargetUserIsMissing() {
        UUID targetUserId = UUID.randomUUID();
        when(userRepository.findById(targetUserId)).thenReturn(Optional.empty());

        assertThat(accessService.canViewUser(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, UUID.randomUUID())),
                targetUserId
        )).isFalse();
    }

    @Test
    void canViewUser_returnsFalseForOrganizationAdminWithoutOrganization() {
        assertThat(accessService.canViewUser(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, null)),
                UUID.randomUUID()
        )).isFalse();
    }

    @Test
    void canViewUser_returnsFalseWhenTargetUserHasNoOrganization() {
        UUID targetUserId = UUID.randomUUID();
        User targetUser = makeUser(RoleName.STUDENT, null);
        targetUser.setId(targetUserId);
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        assertThat(accessService.canViewUser(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, UUID.randomUUID())),
                targetUserId
        )).isFalse();
    }

    @Test
    void canEditUser_reusesViewRules() {
        UUID organizationId = UUID.randomUUID();
        User targetUser = makeUser(RoleName.STUDENT, organizationId);
        when(userRepository.findById(targetUser.getId())).thenReturn(Optional.of(targetUser));

        assertThat(accessService.canEditUser(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, organizationId)),
                targetUser.getId()
        )).isTrue();
    }

    @Test
    void canViewOrganization_returnsTrueForAdmin() {
        assertThat(accessService.canViewOrganization(
                authenticationFor(makeUser(RoleName.ADMIN, UUID.randomUUID())),
                UUID.randomUUID()
        )).isTrue();
    }

    @Test
    void canViewOrganization_returnsFalseWhenAuthenticationIsMissing() {
        assertThat(accessService.canViewOrganization(null, UUID.randomUUID())).isFalse();
    }

    @Test
    void canViewOrganization_returnsTrueForOrganizationAdminWhenOrganizationExists() {
        UUID organizationId = UUID.randomUUID();
        when(organizationRepository.existsById(organizationId)).thenReturn(true);

        assertThat(accessService.canViewOrganization(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, organizationId)),
                organizationId
        )).isTrue();
    }

    @Test
    void canViewOrganization_returnsFalseWhenOrganizationDoesNotExist() {
        UUID organizationId = UUID.randomUUID();
        when(organizationRepository.existsById(organizationId)).thenReturn(false);

        assertThat(accessService.canViewOrganization(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, organizationId)),
                organizationId
        )).isFalse();
    }

    @Test
    void canViewOrganization_returnsFalseForNonOrganizationAdmin() {
        assertThat(accessService.canViewOrganization(
                authenticationFor(makeUser(RoleName.TEACHER, UUID.randomUUID())),
                UUID.randomUUID()
        )).isFalse();
    }

    @Test
    void canViewOrganization_returnsFalseForOrganizationAdminWithoutOrganization() {
        assertThat(accessService.canViewOrganization(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, null)),
                UUID.randomUUID()
        )).isFalse();
    }

    @Test
    void canEditOrganization_reusesViewRules() {
        UUID organizationId = UUID.randomUUID();
        when(organizationRepository.existsById(organizationId)).thenReturn(true);

        assertThat(accessService.canEditOrganization(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, organizationId)),
                organizationId
        )).isTrue();
    }

    @Test
    void canDeleteUser_returnsFalseWhenAuthenticationIsMissing() {
        assertThat(accessService.canDeleteUser(null, UUID.randomUUID())).isFalse();
    }

    @Test
    void canDeleteUser_returnsTrueForAdmin() {
        assertThat(accessService.canDeleteUser(
                authenticationFor(makeUser(RoleName.ADMIN, UUID.randomUUID())),
                UUID.randomUUID()
        )).isTrue();
    }

    @Test
    void canDeleteUser_returnsTrueForOrganizationAdminInSameOrganization() {
        UUID organizationId = UUID.randomUUID();
        User targetUser = makeUser(RoleName.STUDENT, organizationId);
        when(userRepository.findById(targetUser.getId())).thenReturn(Optional.of(targetUser));

        assertThat(accessService.canDeleteUser(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, organizationId)),
                targetUser.getId()
        )).isTrue();
    }

    @Test
    void canDeleteUser_returnsFalseForOrganizationAdminInDifferentOrganization() {
        UUID currentOrganizationId = UUID.randomUUID();
        User targetUser = makeUser(RoleName.STUDENT, UUID.randomUUID());
        when(userRepository.findById(targetUser.getId())).thenReturn(Optional.of(targetUser));

        assertThat(accessService.canDeleteUser(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, currentOrganizationId)),
                targetUser.getId()
        )).isFalse();
    }

    @Test
    void canDeleteUser_returnsFalseForOrganizationAdminWithoutOrganization() {
        User targetUser = makeUser(RoleName.STUDENT, UUID.randomUUID());
        when(userRepository.findById(targetUser.getId())).thenReturn(Optional.of(targetUser));

        assertThat(accessService.canDeleteUser(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, null)),
                targetUser.getId()
        )).isFalse();
    }

    @Test
    void canDeleteUser_returnsFalseForNonOrganizationAdmin() {
        User targetUser = makeUser(RoleName.STUDENT, UUID.randomUUID());
        when(userRepository.findById(targetUser.getId())).thenReturn(Optional.of(targetUser));

        assertThat(accessService.canDeleteUser(
                authenticationFor(makeUser(RoleName.TEACHER, UUID.randomUUID())),
                targetUser.getId()
        )).isFalse();
    }

    @Test
    void canDeleteUser_returnsFalseWhenTargetUserIsMissing() {
        UUID targetUserId = UUID.randomUUID();
        when(userRepository.findById(targetUserId)).thenReturn(Optional.empty());

        assertThat(accessService.canDeleteUser(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, UUID.randomUUID())),
                targetUserId
        )).isFalse();
    }

    @Test
    void canDeleteUser_returnsFalseWhenTargetUserHasNoOrganization() {
        UUID targetUserId = UUID.randomUUID();
        User targetUser = makeUser(RoleName.STUDENT, null);
        targetUser.setId(targetUserId);
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        assertThat(accessService.canDeleteUser(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, UUID.randomUUID())),
                targetUserId
        )).isFalse();
    }

    @Test
    void extractCurrentUser_returnsWrappedUserDetails() {
        User user = makeUser(RoleName.ADMIN, UUID.randomUUID());
        Authentication authentication = authenticationFor(user);

        assertThat(accessService.extractCurrentUser(authentication).getUser()).isSameAs(user);
    }

    private Authentication authenticationFor(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    private User makeUser(RoleName roleName, UUID organizationId) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(roleName.name().toLowerCase() + "@example.com");
        user.setPasswordHash("hashed");
        user.setRole(new Role(roleName));
        user.setStatus(UserStatus.ACTIVE);

        if (organizationId != null) {
            Organization organization = new Organization();
            organization.setId(organizationId);
            user.setOrganization(organization);
        }

        return user;
    }
}
