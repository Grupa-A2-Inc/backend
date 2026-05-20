package org.elearning.backend.security.access;

import org.elearning.backend.classroom.dto.request.AssignCoursesToClassroomRequest;
import org.elearning.backend.classroom.entity.Classroom;
import org.elearning.backend.classroom.entity.MembershipType;
import org.elearning.backend.classroom.repository.ClassroomMembershipRepository;
import org.elearning.backend.classroom.repository.ClassroomRepository;
import org.elearning.backend.content.model.Chapter;
import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.model.CourseStatus;
import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.content.repository.CourseRepository;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.enrollment.repository.CourseEnrollmentRepository;
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

@org.springframework.test.context.ActiveProfiles("test")
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
    private CourseRepository courseRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;

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
    void canEditCourse_returnsFalseForOrganizationAdminWhenCourseHasNoCreator() {
        UUID organizationId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        Course course = new Course();
        course.setId(courseId);
        course.setCreatedBy(null);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        assertThat(accessService.canEditCourse(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, organizationId)),
                courseId
        )).isFalse();
    }

    @Test
    void canDeleteUser_returnsFalseWhenOrganizationAdminHasNoOrganization() {
        UUID targetUserId = UUID.randomUUID();
        User targetUser = makeUser(RoleName.STUDENT, UUID.randomUUID());
        targetUser.setId(targetUserId);
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        assertThat(accessService.canDeleteUser(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, null)),
                targetUserId
        )).isFalse();
    }

    @Test
    void canSubmitAdaptiveSession_returnsTrueForStudent() {
        assertThat(accessService.canSubmitAdaptiveSession(
                authenticationFor(makeUser(RoleName.STUDENT, UUID.randomUUID())),
                UUID.randomUUID()
        )).isTrue();
    }

    @Test
    void canMarkViewedLesson_returnsTrueForStudentWithAccessibleLesson() {
        UUID lessonId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        Lesson lesson = new Lesson();
        Chapter chapter = new Chapter();
        Course course = new Course();
        course.setId(courseId);
        course.setStatus(CourseStatus.PUBLISHED);
        chapter.setCourse(course);
        lesson.setChapter(chapter);

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseEnrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)).thenReturn(true);

        User student = makeUser(RoleName.STUDENT, UUID.randomUUID());
        student.setId(studentId);

        assertThat(accessService.canMarkViewedLesson(authenticationFor(student), lessonId)).isTrue();
    }

    @Test
    void canEditCourse_returnsFalseWhenCourseDoesNotExist() {
        UUID courseId = UUID.randomUUID();
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        assertThat(accessService.canEditCourse(
                authenticationFor(makeUser(RoleName.TEACHER, UUID.randomUUID())),
                courseId
        )).isFalse();
    }

    @Test
    void canViewCourseChapters_returnsFalseForOrganizationAdminWhenCreatorUserIsMissing() {
        UUID organizationId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();

        Course course = new Course();
        course.setId(courseId);
        course.setCreatedBy(creatorId);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(userRepository.findById(creatorId)).thenReturn(Optional.empty());

        assertThat(accessService.canViewCourseChapters(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, organizationId)),
                courseId
        )).isFalse();
    }

    @Test
    void canViewCourseChapters_returnsFalseForOrganizationAdminWhenCreatorHasNoOrganization() {
        UUID organizationId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();

        Course course = new Course();
        course.setId(courseId);
        course.setCreatedBy(creatorId);

        User creator = makeUser(RoleName.TEACHER, null);
        creator.setId(creatorId);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));

        assertThat(accessService.canViewCourseChapters(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, organizationId)),
                courseId
        )).isFalse();
    }

    @Test
    void canViewParent_returnsFalseForOrganizationAdminWhenTargetParentIsMissing() {
        UUID parentId = UUID.randomUUID();
        when(userRepository.findById(parentId)).thenReturn(Optional.empty());

        assertThat(accessService.canViewParent(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, UUID.randomUUID())),
                parentId
        )).isFalse();
    }

    @Test
    void canMarkViewedLesson_returnsFalseForStudentWithoutCourseAccess() {
        UUID lessonId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        Lesson lesson = new Lesson();
        Chapter chapter = new Chapter();
        Course course = new Course();
        course.setId(courseId);
        chapter.setCourse(course);
        lesson.setChapter(chapter);

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));

        assertThat(accessService.canMarkViewedLesson(
                authenticationFor(makeUser(RoleName.STUDENT, UUID.randomUUID())),
                lessonId
        )).isFalse();
    }

    @Test
    void canMarkViewedLesson_returnsFalseForTeacher() {
        assertThat(accessService.canMarkViewedLesson(
                authenticationFor(makeUser(RoleName.TEACHER, UUID.randomUUID())),
                UUID.randomUUID()
        )).isFalse();
    }

    @Test
    void isCourseCreatedInOrganization_returnsFalseWhenOrganizationIdIsNull() throws Exception {
        Course course = new Course();
        course.setCreatedBy(UUID.randomUUID());

        var method = AccessService.class.getDeclaredMethod(
                "isCourseCreatedInOrganization",
                Course.class,
                UUID.class
        );
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(accessService, course, null);

        assertThat(result).isFalse();
    }

    @Test
    void belongsToOrganization_returnsFalseWhenOrganizationIdIsNull() throws Exception {
        User user = makeUser(RoleName.STUDENT, UUID.randomUUID());

        var method = AccessService.class.getDeclaredMethod(
                "belongsToOrganization",
                User.class,
                UUID.class
        );
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(accessService, user, null);

        assertThat(result).isFalse();
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
    void canDeleteUser_returnsFalseWhenTargetUserIdIsNull() {
        assertThat(accessService.canDeleteUser(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, UUID.randomUUID())),
                null
        )).isFalse();
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
    void canAssignCoursesToClassroom_returnsFalseWhenAuthenticationIsMissing() {
        AssignCoursesToClassroomRequest request = new AssignCoursesToClassroomRequest();
        request.setCourseIds(java.util.List.of(UUID.randomUUID()));

        assertFalse(accessService.canAssignCoursesToClassroom(null, UUID.randomUUID(), request));
    }

    @Test
    void canAssignCoursesToClassroom_returnsFalseForNonTeacher() {
        AssignCoursesToClassroomRequest request = new AssignCoursesToClassroomRequest();
        request.setCourseIds(java.util.List.of(UUID.randomUUID()));

        assertFalse(accessService.canAssignCoursesToClassroom(
                authenticationFor(makeUser(RoleName.ORGANIZATION_ADMIN, UUID.randomUUID())),
                UUID.randomUUID(),
                request
        ));
    }

    @Test
    void canAssignCoursesToClassroom_returnsFalseForTeacherWithoutOrganization() {
        AssignCoursesToClassroomRequest request = new AssignCoursesToClassroomRequest();
        request.setCourseIds(java.util.List.of(UUID.randomUUID()));

        assertFalse(accessService.canAssignCoursesToClassroom(
                authenticationFor(makeUser(RoleName.TEACHER, null)),
                UUID.randomUUID(),
                request
        ));
    }

    @Test
    void canAssignCoursesToClassroom_returnsFalseWhenRequestIsNull() {
        assertFalse(accessService.canAssignCoursesToClassroom(
                authenticationFor(makeUser(RoleName.TEACHER, UUID.randomUUID())),
                UUID.randomUUID(),
                null
        ));
    }

    @Test
    void canAssignCoursesToClassroom_returnsFalseWhenRequestCourseIdsAreNull() {
        AssignCoursesToClassroomRequest request = new AssignCoursesToClassroomRequest();

        assertFalse(accessService.canAssignCoursesToClassroom(
                authenticationFor(makeUser(RoleName.TEACHER, UUID.randomUUID())),
                UUID.randomUUID(),
                request
        ));
    }

    @Test
    void canAssignCoursesToClassroom_returnsFalseWhenRequestHasNoCourseIds() {
        AssignCoursesToClassroomRequest request = new AssignCoursesToClassroomRequest();
        request.setCourseIds(java.util.List.of());

        assertFalse(accessService.canAssignCoursesToClassroom(
                authenticationFor(makeUser(RoleName.TEACHER, UUID.randomUUID())),
                UUID.randomUUID(),
                request
        ));
    }

    @Test
    void canAssignCoursesToClassroom_returnsFalseWhenClassroomIsMissing() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        AssignCoursesToClassroomRequest request = new AssignCoursesToClassroomRequest();
        request.setCourseIds(java.util.List.of(UUID.randomUUID()));
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.empty());

        assertFalse(accessService.canAssignCoursesToClassroom(
                authenticationFor(makeUser(RoleName.TEACHER, orgId)),
                classroomId,
                request
        ));
    }

    @Test
    void canAssignCoursesToClassroom_returnsFalseWhenClassroomBelongsToDifferentOrganization() {
        UUID classroomId = UUID.randomUUID();
        UUID teacherOrgId = UUID.randomUUID();
        UUID classroomOrgId = UUID.randomUUID();
        AssignCoursesToClassroomRequest request = new AssignCoursesToClassroomRequest();
        request.setCourseIds(java.util.List.of(UUID.randomUUID()));
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(buildClassroom(classroomId, classroomOrgId)));

        assertFalse(accessService.canAssignCoursesToClassroom(
                authenticationFor(makeUser(RoleName.TEACHER, teacherOrgId)),
                classroomId,
                request
        ));
    }

    @Test
    void canAssignCoursesToClassroom_returnsFalseWhenClassroomHasNoOrganization() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        AssignCoursesToClassroomRequest request = new AssignCoursesToClassroomRequest();
        request.setCourseIds(java.util.List.of(UUID.randomUUID()));

        Classroom classroom = new Classroom();
        classroom.setId(classroomId);
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));

        assertFalse(accessService.canAssignCoursesToClassroom(
                authenticationFor(makeUser(RoleName.TEACHER, orgId)),
                classroomId,
                request
        ));
    }

    @Test
    void canAssignCoursesToClassroom_returnsFalseWhenAnyCourseWasNotCreatedByTeacher() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        User teacher = makeUser(RoleName.TEACHER, orgId);
        UUID otherTeacherId = UUID.randomUUID();
        UUID firstCourseId = UUID.randomUUID();
        UUID secondCourseId = UUID.randomUUID();

        Course ownCourse = new Course();
        ownCourse.setId(firstCourseId);
        ownCourse.setCreatedBy(teacher.getId());

        Course otherCourse = new Course();
        otherCourse.setId(secondCourseId);
        otherCourse.setCreatedBy(otherTeacherId);

        AssignCoursesToClassroomRequest request = new AssignCoursesToClassroomRequest();
        request.setCourseIds(java.util.List.of(firstCourseId, secondCourseId));

        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(buildClassroom(classroomId, orgId)));
        when(courseRepository.findById(firstCourseId)).thenReturn(Optional.of(ownCourse));
        when(courseRepository.findById(secondCourseId)).thenReturn(Optional.of(otherCourse));

        assertFalse(accessService.canAssignCoursesToClassroom(
                authenticationFor(teacher),
                classroomId,
                request
        ));
    }

    @Test
    void canAssignCoursesToClassroom_returnsTrueWhenTeacherCreatedAllCourses() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        User teacher = makeUser(RoleName.TEACHER, orgId);
        UUID firstCourseId = UUID.randomUUID();
        UUID secondCourseId = UUID.randomUUID();

        Course firstCourse = new Course();
        firstCourse.setId(firstCourseId);
        firstCourse.setCreatedBy(teacher.getId());

        Course secondCourse = new Course();
        secondCourse.setId(secondCourseId);
        secondCourse.setCreatedBy(teacher.getId());

        AssignCoursesToClassroomRequest request = new AssignCoursesToClassroomRequest();
        request.setCourseIds(java.util.List.of(firstCourseId, secondCourseId));

        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(buildClassroom(classroomId, orgId)));
        when(courseRepository.findById(firstCourseId)).thenReturn(Optional.of(firstCourse));
        when(courseRepository.findById(secondCourseId)).thenReturn(Optional.of(secondCourse));

        assertTrue(accessService.canAssignCoursesToClassroom(
                authenticationFor(teacher),
                classroomId,
                request
        ));
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
    void canInjectAiQuestions_returnsFalse_whenAuthenticationIsMissing() {
        assertFalse(accessService.canInjectAiQuestions(null, UUID.randomUUID()));
    }

    @Test
    void canInjectAiQuestions_returnsFalse_forNonTeacher() {
        assertFalse(accessService.canInjectAiQuestions(
                authenticationFor(makeUser(RoleName.STUDENT, UUID.randomUUID())),
                UUID.randomUUID()
        ));
    }

    @Test
    void canInjectAiQuestions_returnsTrue_forTeacher() {
        assertTrue(accessService.canInjectAiQuestions(
                authenticationFor(makeUser(RoleName.TEACHER, UUID.randomUUID())),
                UUID.randomUUID()
        ));
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
    void canListClassroomMembers_shouldReturnFalse_whenNotAuthenticated() {
        assertFalse(accessService.canListClassroomMembers(null, UUID.randomUUID()));
    }

    @Test
    void canListClassroomMembers_shouldReturnFalse_whenClassroomHasNoOrganization() {
        UUID classroomId = UUID.randomUUID();
        Classroom classroom = new Classroom();
        classroom.setId(classroomId);

        when(authentication.getPrincipal()).thenReturn(currentUser);
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));

        assertFalse(accessService.canListClassroomMembers(authentication, classroomId));
    }

    @Test
    void canListClassroomMembers_shouldReturnFalse_whenClassroomIsMissing() {
        UUID classroomId = UUID.randomUUID();

        when(authentication.getPrincipal()).thenReturn(currentUser);
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.empty());

        assertFalse(accessService.canListClassroomMembers(authentication, classroomId));
    }

    @Test
    void canListClassroomMembers_shouldReturnFalse_forOrganizationAdminWithoutOrganization() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);

        when(authentication.getPrincipal()).thenReturn(currentUser);
        when(currentUser.getRoleName()).thenReturn(RoleName.ORGANIZATION_ADMIN);
        when(currentUser.getOrganizationId()).thenReturn(null);
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));

        assertFalse(accessService.canListClassroomMembers(authentication, classroomId));
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
    void canListClassroomMembers_shouldReturnFalse_forOtherRole() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);

        when(authentication.getPrincipal()).thenReturn(currentUser);
        when(currentUser.getRoleName()).thenReturn(RoleName.PARENT);
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));

        assertFalse(accessService.canListClassroomMembers(authentication, classroomId));
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
    void canManageClassroom_shouldReturnFalse_whenOrganizationAdminHasNoOrganization() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);

        when(authentication.getPrincipal()).thenReturn(currentUser);
        when(currentUser.getRoleName()).thenReturn(RoleName.ORGANIZATION_ADMIN);
        when(currentUser.getOrganizationId()).thenReturn(null);
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));

        assertFalse(accessService.canManageClassroom(authentication, classroomId));
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

    @Test
    void canViewClassroomCourses_returnsFalse_whenNotAuthenticated() {
        assertThat(accessService.canViewClassroomCourses(null, UUID.randomUUID())).isFalse();
    }

    @Test
    void canViewClassroomCourses_returnsTrue_forAdmin() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);

        when(authentication.getPrincipal()).thenReturn(currentUser);
        when(currentUser.getRoleName()).thenReturn(RoleName.ADMIN);
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));

        assertTrue(accessService.canViewClassroomCourses(authentication, classroomId));
    }

    @Test
    void canViewClassroomCourses_returnsTrue_forOrganizationAdminInSameOrg() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);

        when(authentication.getPrincipal()).thenReturn(currentUser);
        when(currentUser.getRoleName()).thenReturn(RoleName.ORGANIZATION_ADMIN);
        when(currentUser.getOrganizationId()).thenReturn(orgId);
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));

        assertTrue(accessService.canViewClassroomCourses(authentication, classroomId));
    }

    @Test
    void canViewClassroomCourses_returnsTrue_forTeacherMemberOfClassroom() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);

        when(authentication.getPrincipal()).thenReturn(currentUser);
        when(currentUser.getRoleName()).thenReturn(RoleName.TEACHER);
        when(currentUser.getUserId()).thenReturn(teacherId);
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(classroomMembershipRepository.existsByClassroomIdAndUserId(classroomId, teacherId))
                .thenReturn(true);

        assertTrue(accessService.canViewClassroomCourses(authentication, classroomId));
    }

    @Test
    void canViewClassroomCourses_returnsFalse_forTeacherNotMemberOfClassroom() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);

        when(authentication.getPrincipal()).thenReturn(currentUser);
        when(currentUser.getRoleName()).thenReturn(RoleName.TEACHER);
        when(currentUser.getUserId()).thenReturn(teacherId);
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(classroomMembershipRepository.existsByClassroomIdAndUserId(classroomId, teacherId))
                .thenReturn(false);

        assertFalse(accessService.canViewClassroomCourses(authentication, classroomId));
    }

    @Test
    void canViewClassroomCourses_returnsTrue_forStudentMemberOfClassroom() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);

        when(authentication.getPrincipal()).thenReturn(currentUser);
        when(currentUser.getRoleName()).thenReturn(RoleName.STUDENT);
        when(currentUser.getUserId()).thenReturn(studentId);
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(classroomMembershipRepository.existsByClassroomIdAndUserId(classroomId, studentId))
                .thenReturn(true);

        assertTrue(accessService.canViewClassroomCourses(authentication, classroomId));
    }

    @Test
    void canViewClassroomCourses_returnsFalse_forStudentNotMemberOfClassroom() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);

        when(authentication.getPrincipal()).thenReturn(currentUser);
        when(currentUser.getRoleName()).thenReturn(RoleName.STUDENT);
        when(currentUser.getUserId()).thenReturn(studentId);
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));
        when(classroomMembershipRepository.existsByClassroomIdAndUserId(classroomId, studentId))
                .thenReturn(false);

        assertFalse(accessService.canViewClassroomCourses(authentication, classroomId));
    }

    @Test
    void canViewClassroomCourses_returnsFalse_forOtherRole() {
        UUID classroomId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        Classroom classroom = buildClassroom(classroomId, orgId);

        when(authentication.getPrincipal()).thenReturn(currentUser);
        when(currentUser.getRoleName()).thenReturn(RoleName.PARENT);
        when(classroomRepository.findById(classroomId)).thenReturn(Optional.of(classroom));

        assertFalse(accessService.canViewClassroomCourses(authentication, classroomId));
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
