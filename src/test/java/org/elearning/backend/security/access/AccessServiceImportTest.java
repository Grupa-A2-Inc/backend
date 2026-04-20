package org.elearning.backend.security.access;

import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.assessment.repository.QuestionRepository;
import org.elearning.backend.assessment.repository.TestAttemptRepository;
import org.elearning.backend.assessment.repository.TestRepository;
import org.elearning.backend.content.repository.ChapterRepository;
import org.elearning.backend.content.repository.CourseRepository;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.content.repository.LessonResourceRepository;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.elearning.backend.user.dto.request.CreateUserBulkRequest;
import org.elearning.backend.user.dto.request.CreateUserRequest;
import org.elearning.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessServiceImportTest {

    @Mock private UserRepository userRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private ChapterRepository chapterRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private LessonResourceRepository lessonResourceRepository;
    @Mock private TestRepository testRepository;
    @Mock private TestAttemptRepository testAttemptRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private CourseRepository courseRepository;

    @InjectMocks
    private AccessService accessService;

    private Authentication authWith(RoleName role, UUID organizationId) {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getRoleName()).thenReturn(role);
        if (organizationId != null) {
            when(userDetails.getOrganizationId()).thenReturn(organizationId);
        }
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        return auth;
    }

    private CreateUserBulkRequest bulkRequestWithOrgId(UUID... orgIds) {
        List<CreateUserRequest> users = java.util.Arrays.stream(orgIds)
                .map(orgId -> CreateUserRequest.builder()
                        .email("user@test.ro")
                        .password("parola123")
                        .firstName("Ion")
                        .lastName("Pop")
                        .roleName(RoleName.STUDENT)
                        .organizationId(orgId)
                        .build())
                .toList();
        return new CreateUserBulkRequest(users);
    }

    @Test
    void canImportUsers_nullAuthentication_returnsFalse() {
        assertFalse(accessService.canImportUsers(null, bulkRequestWithOrgId(UUID.randomUUID())));
    }

    @Test
    void canImportUsers_principalNotCustomUserDetails_returnsFalse() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("not-a-user-details");

        assertFalse(accessService.canImportUsers(auth, bulkRequestWithOrgId(UUID.randomUUID())));
    }

    @Test
    void canImportUsers_adminRole_returnsTrue() {
        Authentication auth = authWith(RoleName.ADMIN, null);

        assertTrue(accessService.canImportUsers(auth, bulkRequestWithOrgId(UUID.randomUUID())));
    }

    @Test
    void canImportUsers_nonAdminNonOrgAdmin_returnsFalse() {
        Authentication auth = authWith(RoleName.TEACHER, null);

        assertFalse(accessService.canImportUsers(auth, bulkRequestWithOrgId(UUID.randomUUID())));
    }

    @Test
    void canImportUsers_orgAdmin_nullOwnOrgId_returnsFalse() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getRoleName()).thenReturn(RoleName.ORGANIZATION_ADMIN);
        when(userDetails.getOrganizationId()).thenReturn(null);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);

        assertFalse(accessService.canImportUsers(auth, bulkRequestWithOrgId(UUID.randomUUID())));
    }

    @Test
    void canImportUsers_orgAdmin_allUsersMatchOwnOrg_returnsTrue() {
        UUID orgId = UUID.randomUUID();
        Authentication auth = authWith(RoleName.ORGANIZATION_ADMIN, orgId);

        assertTrue(accessService.canImportUsers(auth, bulkRequestWithOrgId(orgId, orgId)));
    }

    @Test
    void canImportUsers_orgAdmin_oneUserHasDifferentOrg_returnsFalse() {
        UUID orgId = UUID.randomUUID();
        UUID otherOrgId = UUID.randomUUID();
        Authentication auth = authWith(RoleName.ORGANIZATION_ADMIN, orgId);

        assertFalse(accessService.canImportUsers(auth, bulkRequestWithOrgId(orgId, otherOrgId)));
    }

    @Test
    void canImportUsers_orgAdmin_oneUserHasNullOrgId_returnsFalse() {
        UUID orgId = UUID.randomUUID();
        Authentication auth = authWith(RoleName.ORGANIZATION_ADMIN, orgId);

        assertFalse(accessService.canImportUsers(auth, bulkRequestWithOrgId(orgId, null)));
    }
}
