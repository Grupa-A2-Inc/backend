package org.elearning.backend.security.access;

import org.elearning.backend.classroom.entity.Classroom;
import org.elearning.backend.classroom.entity.MembershipType;
import org.elearning.backend.classroom.repository.ClassroomMembershipRepository;
import org.elearning.backend.classroom.repository.ClassroomRepository;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private ClassroomMembershipRepository classroomMembershipRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private CustomUserDetails currentUser;

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
    void canCreateUser_returnsFalseForNonAdminRoles() {
        CreateUserRequest request = CreateUserRequest.builder().organizationId(UUID.randomUUID()).build();

        assertThat(accessService.canCreateUser(
                authenticationFor(makeUser(RoleName.TEACHER, UUID.randomUUID())),
                request
        )).isFalse();
    }

    @Test
    void canViewUser_returnsFalseWhenAuthenticationPrincipalIsUnsupported() {
        Authentication unsupportedAuthentication = new UsernamePasswordAuthenticationToken("plain-user", "secret");

        assertThat(accessService.canViewUser(unsupportedAuthentication, UUID.randomUUID())).isFalse();
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
        User requestingUser = makeUser(RoleName.STUDENT, UUID.randomUUID());

        assertThat(accessService.canViewUser(authenticationFor(requestingUser), requestingUser.getId())).isTrue();
        verifyNoInteractions(userRepository);
    }

    @Test
    void canViewUser_returnsFalseForNonOrganizationAdminLookingUpAnotherUser() {
        User requestingUser = makeUser(RoleName.TEACHER, UUID.randomUUID());

        assertThat(accessService.canViewUser(authenticationFor(requestingUser), UUID.randomUUID())).isFalse();
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
    void canViewOrganization_returnsFalseForOrganizationAdminInDifferentOrganization() {
        UUID currentOrganizationId = UUID.randomUUID();

        assertThat(accessService.canViewOrganization(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, currentOrganizationId)),
                UUID.randomUUID()
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
    void canEditOrganization_reusesViewRulesForDeniedRequests() {
        assertThat(accessService.canEditOrganization(
                authenticationFor(makeUser(RoleName.TEACHER, UUID.randomUUID())),
                UUID.randomUUID()
        )).isFalse();
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
        Authentication userAuthentication = authenticationFor(user);

        assertThat(accessService.extractCurrentUser(userAuthentication).getUser()).isSameAs(user);
    }

    @Test
    void extractCurrentUser_returnsNullWhenAuthenticationIsMissing() {
        assertThat(accessService.extractCurrentUser(null)).isNull();
    }

    @Test
    void extractCurrentUser_returnsNullWhenPrincipalIsUnsupported() {
        Authentication unsupportedAuthentication = new UsernamePasswordAuthenticationToken("plain-user", "secret");

        assertThat(accessService.extractCurrentUser(unsupportedAuthentication)).isNull();
    }

    @Test
    void canCreateClassroom_returnsFalseWhenAuthenticationIsMissing() {
        assertThat(accessService.canCreateClassroom(null)).isFalse();
    }

    @Test
    void canCreateClassroom_returnsTrueForOrganizationAdminWithOrganization() {
        assertThat(accessService.canCreateClassroom(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, UUID.randomUUID()))
        )).isTrue();
    }

    @Test
    void canCreateClassroom_returnsFalseForOrganizationAdminWithoutOrganization() {
        assertThat(accessService.canCreateClassroom(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, null))
        )).isFalse();
    }

    @Test
    void canCreateClassroom_returnsFalseForOtherRoles() {
        assertThat(accessService.canCreateClassroom(
                authenticationFor(makeUser(RoleName.TEACHER, UUID.randomUUID()))
        )).isFalse();
    }

    @Test
    void canManageClassroom_returnsFalseWhenAuthenticationIsMissing() {
        assertThat(accessService.canManageClassroom(null, UUID.randomUUID())).isFalse();
    }

    @Test
    void canManageClassroom_returnsFalseForNonOrganizationAdmin() {
        assertThat(accessService.canManageClassroom(
                authenticationFor(makeUser(RoleName.TEACHER, UUID.randomUUID())),
                UUID.randomUUID()
        )).isFalse();
    }

    @Test
    void canManageClassroom_returnsFalseForOrganizationAdminWithoutOrganization() {
        assertThat(accessService.canManageClassroom(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, null)),
                UUID.randomUUID()
        )).isFalse();
    }

    @Test
    void canManageClassroom_returnsFalseWhenClassroomIsMissing() {
        UUID classroomId = UUID.randomUUID();
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.empty());

        assertThat(accessService.canManageClassroom(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, UUID.randomUUID())),
                classroomId
        )).isFalse();
    }

    @Test
    void canManageClassroom_returnsFalseWhenClassroomHasNoOrganization() {
        UUID classroomId = UUID.randomUUID();
        Classroom classroom = new Classroom();
        classroom.setId(classroomId);
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));

        assertThat(accessService.canManageClassroom(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, UUID.randomUUID())),
                classroomId
        )).isFalse();
    }

    @Test
    void canManageClassroom_returnsFalseWhenClassroomBelongsToDifferentOrganization() {
        UUID classroomId = UUID.randomUUID();
        Classroom classroom = new Classroom();
        classroom.setId(classroomId);
        Organization organization = new Organization();
        organization.setId(UUID.randomUUID());
        classroom.setOrganization(organization);
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));

        assertThat(accessService.canManageClassroom(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, UUID.randomUUID())),
                classroomId
        )).isFalse();
    }

    @Test
    void canManageClassroom_returnsTrueForOrganizationAdminFromSameOrganization() {
        UUID organizationId = UUID.randomUUID();
        UUID classroomId = UUID.randomUUID();
        Classroom classroom = new Classroom();
        classroom.setId(classroomId);
        Organization organization = new Organization();
        organization.setId(organizationId);
        classroom.setOrganization(organization);
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));

        assertThat(accessService.canManageClassroom(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, organizationId)),
                classroomId
        )).isTrue();
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

    @Test
    void canUpdateUserStatus_shouldReturnTrue_forOrganizationAdminFromSameOrganization() {
        UUID orgId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        Authentication userAuthentication = mock(Authentication.class);
        CustomUserDetails authenticatedUser = mock(CustomUserDetails.class);

        when(userAuthentication.getPrincipal()).thenReturn(authenticatedUser);
        when(authenticatedUser.getRoleName()).thenReturn(RoleName.ORGANIZATION_ADMIN);
        when(authenticatedUser.getOrganizationId()).thenReturn(orgId);

        Organization organization = mock(Organization.class);
        when(organization.getId()).thenReturn(orgId);

        User targetUser = new User();
        targetUser.setId(targetUserId);
        targetUser.setOrganization(organization);

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        boolean result = accessService.canUpdateUserStatus(userAuthentication, targetUserId);

        assertTrue(result);
    }

    @Test
    void canUpdateUserStatus_shouldReturnFalse_forOrganizationAdminFromDifferentOrganization() {
        UUID currentOrgId = UUID.randomUUID();
        UUID targetOrgId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        Authentication userAuthentication = mock(Authentication.class);
        CustomUserDetails authenticatedUser = mock(CustomUserDetails.class);

        when(userAuthentication.getPrincipal()).thenReturn(authenticatedUser);
        when(authenticatedUser.getRoleName()).thenReturn(RoleName.ORGANIZATION_ADMIN);
        when(authenticatedUser.getOrganizationId()).thenReturn(currentOrgId);

        Organization organization = mock(Organization.class);
        when(organization.getId()).thenReturn(targetOrgId);

        User targetUser = new User();
        targetUser.setId(targetUserId);
        targetUser.setOrganization(organization);

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        boolean result = accessService.canUpdateUserStatus(userAuthentication, targetUserId);

        assertFalse(result);
    }

    @Test
    void canUpdateUserStatus_shouldReturnFalse_forRegularUser() {
        UUID targetUserId = UUID.randomUUID();

        Authentication userAuthentication = mock(Authentication.class);
        CustomUserDetails authenticatedUser = mock(CustomUserDetails.class);

        when(userAuthentication.getPrincipal()).thenReturn(authenticatedUser);
        when(authenticatedUser.getRoleName()).thenReturn(RoleName.STUDENT);

        boolean result = accessService.canUpdateUserStatus(userAuthentication, targetUserId);

        assertFalse(result);
    }

    @Test
    void canUpdateUserStatus_shouldReturnFalse_whenTargetUserDoesNotExist() {
        UUID orgId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        Authentication userAuthentication = mock(Authentication.class);
        CustomUserDetails authenticatedUser = mock(CustomUserDetails.class);

        when(userAuthentication.getPrincipal()).thenReturn(authenticatedUser);
        when(authenticatedUser.getRoleName()).thenReturn(RoleName.ORGANIZATION_ADMIN);
        when(authenticatedUser.getOrganizationId()).thenReturn(orgId);

        when(userRepository.findById(targetUserId)).thenReturn(Optional.empty());

        boolean result = accessService.canUpdateUserStatus(userAuthentication, targetUserId);

        assertFalse(result);
    }

    @Test
    void canUpdateUserStatus_shouldReturnFalse_whenTargetUserHasNoOrganization() {
        UUID orgId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        Authentication userAuthentication = mock(Authentication.class);
        CustomUserDetails authenticatedUser = mock(CustomUserDetails.class);

        when(userAuthentication.getPrincipal()).thenReturn(authenticatedUser);
        when(authenticatedUser.getRoleName()).thenReturn(RoleName.ORGANIZATION_ADMIN);
        when(authenticatedUser.getOrganizationId()).thenReturn(orgId);

        User targetUser = new User();
        targetUser.setId(targetUserId);
        targetUser.setOrganization(null);

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        boolean result = accessService.canUpdateUserStatus(userAuthentication, targetUserId);

        assertFalse(result);
    }

    @Test
    void canListClassroomMembers_shouldReturnTrue_forOrganizationAdminInSameOrganization() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);

        when(authentication.getPrincipal()).thenReturn(currentUser);
        when(currentUser.getRoleName()).thenReturn(RoleName.ORGANIZATION_ADMIN);
        when(currentUser.getOrganizationId()).thenReturn(orgId);
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));

        boolean result = accessService.canListClassroomMembers(authentication, classroomId);

        assertTrue(result);
    }

    @Test
    void canListClassroomMembers_shouldReturnFalse_forOrganizationAdminFromAnotherOrganization() {
        UUID classroomId = UUID.randomUUID();
        UUID classroomOrgId = UUID.randomUUID();
        UUID otherOrgId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, classroomOrgId);

        when(authentication.getPrincipal()).thenReturn(currentUser);
        when(currentUser.getRoleName()).thenReturn(RoleName.ORGANIZATION_ADMIN);
        when(currentUser.getOrganizationId()).thenReturn(otherOrgId);
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));

        boolean result = accessService.canListClassroomMembers(authentication, classroomId);

        assertFalse(result);
    }

    @Test
    void canListClassroomMembers_shouldReturnTrue_forTeacherMemberOfClassroom() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);

        when(authentication.getPrincipal()).thenReturn(currentUser);
        when(currentUser.getRoleName()).thenReturn(RoleName.TEACHER);
        when(currentUser.getUserId()).thenReturn(teacherId);
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(classroomMembershipRepository.existsByClassroomIdAndUserIdAndMembershipType(
                classroomId, teacherId, MembershipType.TEACHER))
                .thenReturn(true);

        boolean result = accessService.canListClassroomMembers(authentication, classroomId);

        assertTrue(result);
    }

    @Test
    void canListClassroomMembers_shouldReturnFalse_forTeacherWhoIsNotMemberOfClassroom() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);

        when(authentication.getPrincipal()).thenReturn(currentUser);
        when(currentUser.getRoleName()).thenReturn(RoleName.TEACHER);
        when(currentUser.getUserId()).thenReturn(teacherId);
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(classroomMembershipRepository.existsByClassroomIdAndUserIdAndMembershipType(
                classroomId, teacherId, MembershipType.TEACHER))
                .thenReturn(false);

        boolean result = accessService.canListClassroomMembers(authentication, classroomId);

        assertFalse(result);
    }

    @Test
    void canManageClassroom_shouldReturnTrue_forAdmin() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);

        when(authentication.getPrincipal()).thenReturn(currentUser);
        when(currentUser.getRoleName()).thenReturn(RoleName.ADMIN);
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));

        boolean result = accessService.canManageClassroom(authentication, classroomId);

        assertTrue(result);
    }

    @Test
    void canManageClassroom_shouldReturnTrue_forOrganizationAdminFromSameOrganization() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);

        when(authentication.getPrincipal()).thenReturn(currentUser);
        when(currentUser.getRoleName()).thenReturn(RoleName.ORGANIZATION_ADMIN);
        when(currentUser.getOrganizationId()).thenReturn(orgId);
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));

        boolean result = accessService.canManageClassroom(authentication, classroomId);

        assertTrue(result);
    }

    @Test
    void canManageClassroom_shouldReturnFalse_forOrganizationAdminFromAnotherOrganization() {
        UUID classroomId = UUID.randomUUID();
        UUID classroomOrgId = UUID.randomUUID();
        UUID otherOrgId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, classroomOrgId);

        when(authentication.getPrincipal()).thenReturn(currentUser);
        when(currentUser.getRoleName()).thenReturn(RoleName.ORGANIZATION_ADMIN);
        when(currentUser.getOrganizationId()).thenReturn(otherOrgId);
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));

        boolean result = accessService.canManageClassroom(authentication, classroomId);

        assertFalse(result);
    }

    @Test
    void canManageClassroom_shouldReturnFalse_forTeacher() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);

        when(authentication.getPrincipal()).thenReturn(currentUser);
        when(currentUser.getRoleName()).thenReturn(RoleName.TEACHER);
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));

        boolean result = accessService.canManageClassroom(authentication, classroomId);

        assertFalse(result);
    }

    private Classroom buildClassroom(UUID classroomId, UUID orgId) {
        Organization organization = new Organization();
        organization.setId(orgId);

        Classroom classroom = new Classroom();
        classroom.setId(classroomId);
        classroom.setOrganization(organization);

        return classroom;
    }
}
