package org.elearning.backend.security.access;

import org.elearning.backend.assessment.model.Question;
import org.elearning.backend.assessment.model.TestAttempt;
import org.elearning.backend.classroom.dto.request.AssignCoursesToClassroomRequest;
import org.elearning.backend.classroom.repository.ClassroomMembershipRepository;
import org.elearning.backend.classroom.repository.ClassroomRepository;
import org.elearning.backend.assessment.repository.QuestionRepository;
import org.elearning.backend.assessment.repository.TestAttemptRepository;
import org.elearning.backend.assessment.repository.TestRepository;
import org.elearning.backend.auth.service.EmailService;
import org.elearning.backend.content.model.Chapter;
import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.content.model.LessonResource;
import org.elearning.backend.content.repository.ChapterRepository;
import org.elearning.backend.content.repository.CourseRepository;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.content.repository.LessonResourceRepository;
import org.elearning.backend.enrollment.repository.CourseEnrollmentRepository;
import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.parent.entity.Parent;
import org.elearning.backend.parent.repository.ParentRepository;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.elearning.backend.student.entity.Student;
import org.elearning.backend.student.repository.StudentRepository;
import org.elearning.backend.user.dto.request.CreateUserBulkRequest;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@org.springframework.test.context.ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class AccessServiceCoverageTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @MockitoBean
    private EmailService emailService;
    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LessonResourceRepository lessonResourceRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Mock
    private ParentRepository parentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ClassroomMembershipRepository classroomMembershipRepository;

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private TestRepository testRepository;

    @Mock
    private TestAttemptRepository testAttemptRepository;

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private AccessService accessService;

    @Test
    void canChangePassword_allowsAdminsAndSameUserOnly() {
        User admin = user(RoleName.ADMIN, UUID.randomUUID());
        User student = user(RoleName.STUDENT, UUID.randomUUID());

        assertThat(accessService.canChangePassword(null, UUID.randomUUID())).isFalse();
        assertThat(accessService.canChangePassword(authenticationFor(admin), UUID.randomUUID())).isTrue();
        assertThat(accessService.canChangePassword(authenticationFor(student), student.getId())).isTrue();
        assertThat(accessService.canChangePassword(authenticationFor(student), UUID.randomUUID())).isFalse();
    }

    @Test
    void userStatusPermissions_coverMissingAdminAndOrganizationPaths() {
        UUID organizationId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        User admin = user(RoleName.ADMIN, UUID.randomUUID());
        User orgAdmin = user(RoleName.ORGANIZATION_ADMIN, organizationId);
        User targetUser = user(RoleName.STUDENT, organizationId);
        User targetWithoutOrganization = user(RoleName.STUDENT, null);
        UUID targetWithoutOrganizationId = UUID.randomUUID();
        UUID missingTargetUserId = UUID.randomUUID();
        targetUser.setId(targetUserId);
        targetWithoutOrganization.setId(targetWithoutOrganizationId);

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.findById(targetWithoutOrganizationId)).thenReturn(Optional.of(targetWithoutOrganization));
        when(userRepository.findById(missingTargetUserId)).thenReturn(Optional.empty());

        assertThat(accessService.canUpdateUserStatus(null, targetUserId)).isFalse();
        assertThat(accessService.canUpdateUserStatus(authenticationFor(admin), targetUserId)).isTrue();
        assertThat(accessService.canUpdateUserStatus(authenticationFor(orgAdmin), targetUserId)).isTrue();
        assertThat(accessService.canUpdateUserStatus(authenticationFor(user(RoleName.STUDENT, organizationId)), targetUserId)).isFalse();
        assertThat(accessService.canUpdateUserStatus(authenticationFor(user(RoleName.ORGANIZATION_ADMIN, null)), targetUserId)).isFalse();
        assertThat(accessService.canUpdateUserStatus(authenticationFor(orgAdmin), targetWithoutOrganizationId)).isFalse();
        assertThat(accessService.canUpdateUserStatus(authenticationFor(orgAdmin), missingTargetUserId)).isFalse();
    }

    @Test
    void createUserPermission_allowsOrganizationAdminOnlyInsideOwnOrganization() {
        UUID organizationId = UUID.randomUUID();
        UUID otherOrganizationId = UUID.randomUUID();
        CreateUserRequest sameOrganizationRequest = CreateUserRequest.builder()
                .email("student@example.com")
                .firstName("Student")
                .lastName("One")
                .roleName(RoleName.STUDENT)
                .organizationId(organizationId)
                .build();
        CreateUserRequest otherOrganizationRequest = CreateUserRequest.builder()
                .email("student2@example.com")
                .firstName("Student")
                .lastName("Two")
                .roleName(RoleName.STUDENT)
                .organizationId(otherOrganizationId)
                .build();
        Authentication orgAdmin = authenticationFor(user(RoleName.ORGANIZATION_ADMIN, organizationId));

        assertThat(accessService.canCreateUser(orgAdmin, sameOrganizationRequest)).isTrue();
        assertThat(accessService.canCreateUser(orgAdmin, otherOrganizationRequest)).isFalse();
    }

    @Test
    void createAndImportPermissions_returnFalseForNullRequests() {
        UUID organizationId = UUID.randomUUID();
        Authentication orgAdmin = authenticationFor(user(RoleName.ORGANIZATION_ADMIN, organizationId));

        assertThat(accessService.canCreateUser(orgAdmin, null)).isFalse();
        assertThat(accessService.canImportUsers(orgAdmin, null)).isFalse();
        assertThat(accessService.canImportUsers(orgAdmin, new CreateUserBulkRequest(null))).isFalse();
    }

    @Test
    void userPermissions_returnFalseWhenOrganizationAdminReceivesNullTargetUserId() {
        Authentication orgAdmin = authenticationFor(user(RoleName.ORGANIZATION_ADMIN, UUID.randomUUID()));

        assertThat(accessService.canViewUser(orgAdmin, null)).isFalse();
        assertThat(accessService.canUpdateUserStatus(orgAdmin, null)).isFalse();
    }

    @Test
    void parentPermissions_coverAdminOrgAdminAndParentAccess() {
        UUID organizationId = UUID.randomUUID();
        UUID otherOrganizationId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Parent parent = parent(parentId, organizationId);
        Student student = student(studentId, organizationId);
        Parent otherOrganizationParent = parent(UUID.randomUUID(), otherOrganizationId);

        when(userRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(parentRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(parentRepository.findById(otherOrganizationParent.getId())).thenReturn(Optional.of(otherOrganizationParent));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));

        Authentication admin = authenticationFor(user(RoleName.ADMIN, UUID.randomUUID()));
        Authentication orgAdmin = authenticationFor(user(RoleName.ORGANIZATION_ADMIN, organizationId));
        Authentication otherOrgAdmin = authenticationFor(user(RoleName.ORGANIZATION_ADMIN, otherOrganizationId));
        Authentication parentAuthentication = authenticationFor(parent);
        Authentication otherParent = authenticationFor(user(RoleName.PARENT, organizationId));

        assertThat(accessService.canViewAllParents(admin)).isTrue();
        assertThat(accessService.canViewAllParents(orgAdmin)).isFalse();
        assertThat(accessService.canViewParent(admin, parentId)).isTrue();
        assertThat(accessService.canViewParent(orgAdmin, parentId)).isTrue();
        assertThat(accessService.canViewParent(otherOrgAdmin, parentId)).isFalse();
        assertThat(accessService.canManageParentStudent(orgAdmin, parentId, studentId)).isTrue();
        assertThat(accessService.canManageParentStudent(otherOrgAdmin, parentId, studentId)).isFalse();
        assertThat(accessService.canViewParentStudents(parentAuthentication, parentId)).isTrue();
        assertThat(accessService.canViewParentStudents(otherParent, parentId)).isFalse();
        assertThat(accessService.canViewParentStudents(orgAdmin, otherOrganizationParent.getId())).isFalse();
    }

    @Test
    void parentStudentManagement_rejectsMissingAndCrossOrganizationUsers() {
        UUID organizationId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID otherStudentId = UUID.randomUUID();
        Parent parent = parent(parentId, organizationId);
        Student otherOrganizationStudent = student(otherStudentId, UUID.randomUUID());
        Authentication orgAdmin = authenticationFor(user(RoleName.ORGANIZATION_ADMIN, organizationId));

        when(parentRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());
        when(studentRepository.findById(otherStudentId)).thenReturn(Optional.of(otherOrganizationStudent));

        assertThat(accessService.canManageParentStudent(null, parentId, studentId)).isFalse();
        assertThat(accessService.canManageParentStudent(orgAdmin, parentId, studentId)).isFalse();
        assertThat(accessService.canManageParentStudent(orgAdmin, parentId, otherStudentId)).isFalse();
        assertThat(accessService.canManageParentStudent(authenticationFor(user(RoleName.PARENT, organizationId)), parentId, otherStudentId)).isFalse();
    }

    @Test
    void parentPermissions_returnFalseWhenIdsAreNull() {
        Authentication orgAdmin = authenticationFor(user(RoleName.ORGANIZATION_ADMIN, UUID.randomUUID()));

        assertThat(accessService.canManageParentStudent(orgAdmin, null, UUID.randomUUID())).isFalse();
        assertThat(accessService.canManageParentStudent(orgAdmin, UUID.randomUUID(), null)).isFalse();
        assertThat(accessService.canViewParentStudents(orgAdmin, null)).isFalse();
    }

    @Test
    void parentPermissions_coverAdminMissingOrganizationAndMissingTargetBranches() {
        UUID organizationId = UUID.randomUUID();
        UUID targetWithoutOrganizationId = UUID.randomUUID();
        UUID missingParentId = UUID.randomUUID();
        UUID parentWithoutOrganizationId = UUID.randomUUID();
        UUID studentWithoutOrganizationId = UUID.randomUUID();
        UUID validStudentId = UUID.randomUUID();
        Parent targetWithoutOrganization = parent(targetWithoutOrganizationId, null);
        Parent parentWithoutOrganization = parent(parentWithoutOrganizationId, null);
        Student studentWithoutOrganization = student(studentWithoutOrganizationId, null);
        Student validStudent = student(validStudentId, organizationId);

        when(userRepository.findById(targetWithoutOrganizationId)).thenReturn(Optional.of(targetWithoutOrganization));
        when(userRepository.findById(missingParentId)).thenReturn(Optional.empty());
        when(parentRepository.findById(parentWithoutOrganizationId)).thenReturn(Optional.of(parentWithoutOrganization));
        when(studentRepository.findById(validStudentId)).thenReturn(Optional.of(validStudent));
        when(studentRepository.findById(studentWithoutOrganizationId)).thenReturn(Optional.of(studentWithoutOrganization));

        Authentication admin = authenticationFor(user(RoleName.ADMIN, UUID.randomUUID()));
        Authentication orgAdmin = authenticationFor(user(RoleName.ORGANIZATION_ADMIN, organizationId));
        Authentication orgAdminWithoutOrganization = authenticationFor(user(RoleName.ORGANIZATION_ADMIN, null));

        assertThat(accessService.canViewAllParents(null)).isFalse();
        assertThat(accessService.canViewParent(null, missingParentId)).isFalse();
        assertThat(accessService.canViewParent(orgAdminWithoutOrganization, missingParentId)).isFalse();
        assertThat(accessService.canViewParent(orgAdmin, missingParentId)).isFalse();
        assertThat(accessService.canViewParent(orgAdmin, targetWithoutOrganizationId)).isFalse();
        assertThat(accessService.canManageParentStudent(admin, missingParentId, validStudentId)).isTrue();
        assertThat(accessService.canManageParentStudent(orgAdminWithoutOrganization, parentWithoutOrganizationId, validStudentId)).isFalse();
        assertThat(accessService.canManageParentStudent(orgAdmin, parentWithoutOrganizationId, validStudentId)).isFalse();
        assertThat(accessService.canManageParentStudent(orgAdmin, parentWithoutOrganizationId, studentWithoutOrganizationId)).isFalse();
    }

    @Test
    void parentStudentViewPermissions_coverAdminMissingAndUnsupportedRoleBranches() {
        UUID organizationId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        UUID missingParentId = UUID.randomUUID();
        Parent parent = parent(parentId, organizationId);

        when(parentRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(parentRepository.findById(missingParentId)).thenReturn(Optional.empty());

        Authentication admin = authenticationFor(user(RoleName.ADMIN, UUID.randomUUID()));
        Authentication orgAdmin = authenticationFor(user(RoleName.ORGANIZATION_ADMIN, organizationId));
        Authentication orgAdminWithoutOrganization = authenticationFor(user(RoleName.ORGANIZATION_ADMIN, null));
        Authentication parentAuthentication = authenticationFor(parent);
        Authentication teacher = authenticationFor(user(RoleName.TEACHER, organizationId));
        Parent parentWithoutOrganization = parent(UUID.randomUUID(), null);
        Authentication parentWithoutOrganizationAuthentication = authenticationFor(parentWithoutOrganization);

        when(parentRepository.findById(parentWithoutOrganization.getId())).thenReturn(Optional.of(parentWithoutOrganization));

        assertThat(accessService.canViewParentStudents(null, parentId)).isFalse();
        assertThat(accessService.canViewParentStudents(admin, missingParentId)).isTrue();
        assertThat(accessService.canViewParentStudents(orgAdmin, parentId)).isTrue();
        assertThat(accessService.canViewParentStudents(orgAdminWithoutOrganization, parentId)).isFalse();
        assertThat(accessService.canViewParentStudents(orgAdmin, missingParentId)).isFalse();
        assertThat(accessService.canViewParentStudents(parentAuthentication, missingParentId)).isFalse();
        assertThat(accessService.canViewParentStudents(parentWithoutOrganizationAuthentication, parentWithoutOrganization.getId())).isFalse();
        assertThat(accessService.canViewParentStudents(teacher, parentId)).isFalse();
    }

    @Test
    void parentViewPermission_rejectsNonOrganizationAdminUsers() {
        UUID parentId = UUID.randomUUID();

        assertThat(accessService.canViewParent(authenticationFor(user(RoleName.TEACHER, UUID.randomUUID())), parentId)).isFalse();
    }

    @Test
    void parentStudentManagement_rejectsParentWhenStudentOrganizationIsNull() {
        UUID organizationId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Parent parent = parent(parentId, organizationId);
        Student studentWithoutOrganization = student(studentId, null);
        Authentication orgAdmin = authenticationFor(user(RoleName.ORGANIZATION_ADMIN, organizationId));

        when(parentRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(studentWithoutOrganization));

        assertThat(accessService.canManageParentStudent(orgAdmin, parentId, studentId)).isFalse();
    }

    @Test
    void chapterPermissions_coverMissingAuthenticatedAndManagedPaths() {
        UUID chapterId = UUID.randomUUID();
        UUID missingChapterId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        Chapter chapter = chapter(chapterId, courseId);

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
        when(chapterRepository.findById(missingChapterId)).thenReturn(Optional.empty());

        User teacherUser = user(RoleName.TEACHER, UUID.randomUUID());
        Authentication teacher = authenticationFor(teacherUser);
        stubManagedCourse(courseId, teacherUser.getId());

        assertThat(accessService.canViewChapterLessons(null, chapterId)).isFalse();
        assertThat(accessService.canViewChapterLessons(teacher, chapterId)).isTrue();
        assertThat(accessService.canViewChapterLessons(teacher, missingChapterId)).isFalse();
        assertThat(accessService.canCreateLessons(teacher, chapterId)).isTrue();
        assertThat(accessService.canCreateLessons(teacher, missingChapterId)).isFalse();
        assertThat(accessService.canEditChapter(teacher, chapterId)).isTrue();
        assertThat(accessService.canEditChapter(teacher, missingChapterId)).isFalse();
        assertThat(accessService.canDeleteChapter(teacher, chapterId)).isTrue();
        assertThat(accessService.canDeleteChapter(teacher, missingChapterId)).isFalse();
    }

    @Test
    void chapterAndLessonPermissions_returnFalseForBrokenRelationsAndNullIds() {
        UUID chapterId = UUID.randomUUID();
        UUID chapterWithCourseWithoutId = UUID.randomUUID();
        UUID chapterWithNullCourse = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID lessonWithNullCourseId = UUID.randomUUID();
        UUID lessonWithNullCourse = UUID.randomUUID();
        UUID lessonWithNullChapter = UUID.randomUUID();

        Chapter chapterWithoutCourse = new Chapter();
        chapterWithoutCourse.setId(chapterId);
        Chapter chapterWithoutCourseId = new Chapter();
        chapterWithoutCourseId.setId(chapterWithCourseWithoutId);
        chapterWithoutCourseId.setCourse(new Course());
        Chapter chapterWithCourseNull = new Chapter();
        chapterWithCourseNull.setId(chapterWithNullCourse);

        Lesson lessonWithoutChapter = new Lesson();
        lessonWithoutChapter.setId(lessonId);
        Lesson lessonWithCourseWithoutId = new Lesson();
        lessonWithCourseWithoutId.setId(lessonWithNullCourseId);
        Chapter lessonChapterWithCourseWithoutId = new Chapter();
        lessonChapterWithCourseWithoutId.setCourse(new Course());
        lessonWithCourseWithoutId.setChapter(lessonChapterWithCourseWithoutId);
        Lesson lessonWithCourseNull = new Lesson();
        lessonWithCourseNull.setId(lessonWithNullCourse);
        lessonWithCourseNull.setChapter(new Chapter());
        Lesson lessonWithMissingChapter = new Lesson();
        lessonWithMissingChapter.setId(lessonWithNullChapter);

        when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapterWithoutCourse));
        when(chapterRepository.findById(chapterWithCourseWithoutId)).thenReturn(Optional.of(chapterWithoutCourseId));
        when(chapterRepository.findById(chapterWithNullCourse)).thenReturn(Optional.of(chapterWithCourseNull));
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lessonWithoutChapter));
        when(lessonRepository.findById(lessonWithNullCourseId)).thenReturn(Optional.of(lessonWithCourseWithoutId));
        when(lessonRepository.findById(lessonWithNullCourse)).thenReturn(Optional.of(lessonWithCourseNull));
        when(lessonRepository.findById(lessonWithNullChapter)).thenReturn(Optional.of(lessonWithMissingChapter));

        Authentication teacher = authenticationFor(user(RoleName.TEACHER, UUID.randomUUID()));

        assertThat(accessService.canViewChapterLessons(teacher, null)).isFalse();
        assertThat(accessService.canViewChapterLessons(teacher, chapterId)).isFalse();
        assertThat(accessService.canViewChapterLessons(teacher, chapterWithCourseWithoutId)).isFalse();
        assertThat(accessService.canCreateLessons(teacher, chapterWithNullCourse)).isFalse();
        assertThat(accessService.canCreateLessons(teacher, null)).isFalse();
        assertThat(accessService.canEditLessonMetaData(teacher, null)).isFalse();
        assertThat(accessService.canEditLessonMetaData(teacher, lessonId)).isFalse();
        assertThat(accessService.canEditLessonMetaData(teacher, lessonWithNullCourseId)).isFalse();
        assertThat(accessService.canEditLessonMetaData(teacher, lessonWithNullCourse)).isFalse();
        assertThat(accessService.canViewLessonResources(teacher, null)).isFalse();
        assertThat(accessService.canViewLessonResources(teacher, lessonId)).isFalse();
        assertThat(accessService.canViewLessonResources(teacher, lessonWithNullCourseId)).isFalse();
        assertThat(accessService.canViewLessonResources(teacher, lessonWithNullChapter)).isFalse();
        assertThat(accessService.canViewLessonResources(teacher, lessonWithNullCourse)).isFalse();
    }

    @Test
    void lessonPermissions_coverEditAndDeleteDelegates() {
        UUID lessonId = UUID.randomUUID();
        UUID missingLessonId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        Lesson lesson = lesson(lessonId, courseId);

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(lessonRepository.findById(missingLessonId)).thenReturn(Optional.empty());

        User teacherUser = user(RoleName.TEACHER, UUID.randomUUID());
        Authentication teacher = authenticationFor(teacherUser);
        stubManagedCourse(courseId, teacherUser.getId());

        assertThat(accessService.canEditLessonMetaData(teacher, lessonId)).isTrue();
        assertThat(accessService.canEditLessonMetaData(teacher, missingLessonId)).isFalse();
        assertThat(accessService.canViewLessonContent(teacher, lessonId)).isTrue();
        assertThat(accessService.canEditLessonContent(teacher, lessonId)).isTrue();
        assertThat(accessService.canDeleteLesson(teacher, lessonId)).isTrue();
        assertThat(accessService.canDeleteLesson(teacher, missingLessonId)).isFalse();
    }

    @Test
    void lessonViewAndCreatePermissions_coverStudentAndTeacherPaths() {
        UUID lessonId = UUID.randomUUID();
        UUID missingLessonId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        Lesson lesson = lesson(lessonId, courseId);

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(lessonRepository.findById(missingLessonId)).thenReturn(Optional.empty());
        when(chapterRepository.findById(lesson.getChapter().getId())).thenReturn(Optional.of(lesson.getChapter()));

        User studentUser = user(RoleName.STUDENT, UUID.randomUUID());
        Authentication student = authenticationFor(studentUser);
        User teacherUser = user(RoleName.TEACHER, UUID.randomUUID());
        Authentication teacher = authenticationFor(teacherUser);
        stubManagedCourse(courseId, teacherUser.getId());
        stubEnrolledStudent(courseId, studentUser.getId());

        assertThat(accessService.canViewLessonResources(student, lessonId)).isTrue();
        assertThat(accessService.canViewLessonResources(student, missingLessonId)).isFalse();
        assertThat(accessService.canViewChapterLessons(student, lesson.getChapter().getId())).isFalse();
        assertThat(accessService.canViewChapterLessons(teacher, lesson.getChapter().getId())).isTrue();
        assertThat(accessService.canCreateLessonResource(teacher, lessonId)).isTrue();
        assertThat(accessService.canCreateLessonResource(teacher, missingLessonId)).isFalse();
        assertThat(accessService.canViewLessonTest(student, lessonId)).isTrue();
        assertThat(accessService.canViewLessonTest(student, missingLessonId)).isFalse();
        assertThat(accessService.canCreateLessonTest(teacher, lessonId)).isTrue();
        assertThat(accessService.canCreateLessonTest(teacher, missingLessonId)).isFalse();
    }

    @Test
    void lessonResourcePermissions_coverAdminMissingAndManagedResource() {
        UUID resourceId = UUID.randomUUID();
        UUID missingResourceId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        LessonResource resource = lessonResource(resourceId, courseId);

        when(lessonResourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
        when(lessonResourceRepository.findById(missingResourceId)).thenReturn(Optional.empty());

        Authentication admin = authenticationFor(user(RoleName.ADMIN, UUID.randomUUID()));
        User teacherUser = user(RoleName.TEACHER, UUID.randomUUID());
        Authentication teacher = authenticationFor(teacherUser);
        stubManagedCourse(courseId, teacherUser.getId());

        assertThat(accessService.canEditLessonResource(admin, resourceId)).isTrue();
        assertThat(accessService.canEditLessonResource(null, resourceId)).isFalse();
        assertThat(accessService.canDeleteLessonResource(teacher, resourceId)).isTrue();
        assertThat(accessService.canDeleteLessonResource(teacher, missingResourceId)).isFalse();
    }

    @Test
    void lessonResourcePermissions_returnFalseForNullIdAndBrokenRelations() {
        UUID resourceId = UUID.randomUUID();
        UUID resourceWithNullChapter = UUID.randomUUID();
        UUID resourceWithNullCourse = UUID.randomUUID();
        UUID resourceWithNullCourseId = UUID.randomUUID();
        LessonResource resourceWithoutLesson = new LessonResource();
        resourceWithoutLesson.setId(resourceId);
        LessonResource resourceWithBrokenChapter = new LessonResource();
        resourceWithBrokenChapter.setId(resourceWithNullChapter);
        resourceWithBrokenChapter.setLesson(new Lesson());
        LessonResource resourceWithBrokenCourse = new LessonResource();
        resourceWithBrokenCourse.setId(resourceWithNullCourse);
        Lesson lessonWithNullCourse = new Lesson();
        lessonWithNullCourse.setChapter(new Chapter());
        resourceWithBrokenCourse.setLesson(lessonWithNullCourse);
        LessonResource resourceWithBrokenCourseId = new LessonResource();
        resourceWithBrokenCourseId.setId(resourceWithNullCourseId);
        Lesson lesson = new Lesson();
        Chapter chapter = new Chapter();
        chapter.setCourse(new Course());
        lesson.setChapter(chapter);
        resourceWithBrokenCourseId.setLesson(lesson);
        when(lessonResourceRepository.findById(resourceId)).thenReturn(Optional.of(resourceWithoutLesson));
        when(lessonResourceRepository.findById(resourceWithNullChapter)).thenReturn(Optional.of(resourceWithBrokenChapter));
        when(lessonResourceRepository.findById(resourceWithNullCourse)).thenReturn(Optional.of(resourceWithBrokenCourse));
        when(lessonResourceRepository.findById(resourceWithNullCourseId)).thenReturn(Optional.of(resourceWithBrokenCourseId));

        Authentication teacher = authenticationFor(user(RoleName.TEACHER, UUID.randomUUID()));

        assertThat(accessService.canEditLessonResource(teacher, null)).isFalse();
        assertThat(accessService.canEditLessonResource(teacher, resourceId)).isFalse();
        assertThat(accessService.canEditLessonResource(teacher, resourceWithNullChapter)).isFalse();
        assertThat(accessService.canEditLessonResource(teacher, resourceWithNullCourse)).isFalse();
        assertThat(accessService.canEditLessonResource(teacher, resourceWithNullCourseId)).isFalse();
    }

    @Test
    void coursePermissions_coverCreateViewAndManageOperations() {
        UUID courseId = UUID.randomUUID();

        Authentication admin = authenticationFor(user(RoleName.ADMIN, UUID.randomUUID()));
        User teacherUser = user(RoleName.TEACHER, UUID.randomUUID());
        Authentication teacher = authenticationFor(teacherUser);
        User studentUser = user(RoleName.STUDENT, UUID.randomUUID());
        Authentication student = authenticationFor(studentUser);
        stubManagedCourse(courseId, teacherUser.getId());
        stubEnrolledStudent(courseId, studentUser.getId());

        assertThat(accessService.canCreateCourse(null)).isFalse();
        assertThat(accessService.canCreateCourse(admin)).isTrue();
        assertThat(accessService.canCreateCourse(teacher)).isTrue();
        assertThat(accessService.canCreateCourse(student)).isFalse();
        assertThat(accessService.canViewMyCourses(teacher)).isTrue();
        assertThat(accessService.canViewMyCourses(student)).isFalse();
        assertThat(accessService.canViewCourseChapters(admin, courseId)).isTrue();
        assertThat(accessService.canViewCourseFullView(student, courseId)).isTrue();
        assertThat(accessService.canCreateChapter(teacher, courseId)).isTrue();
        assertThat(accessService.canEditCourse(admin, courseId)).isTrue();
        assertThat(accessService.canEditCourse(teacher, courseId)).isTrue();
        assertThat(accessService.canDeleteCourse(teacher, courseId)).isTrue();
        assertThat(accessService.canReplaceCourse(teacher, courseId)).isTrue();
        assertThat(accessService.canEditCourse(null, courseId)).isFalse();
        assertThat(accessService.canViewPublicCourses(null)).isTrue();
    }

    @Test
    void canViewCourseFullView_returnsFalseForStudentWhenCourseIsDraftEvenIfEnrolled() {
        UUID courseId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        User studentUser = user(RoleName.STUDENT, UUID.randomUUID());
        studentUser.setId(studentId);

        Course draftCourse = course(courseId, UUID.randomUUID());
        draftCourse.setStatus(org.elearning.backend.content.model.CourseStatus.DRAFT);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(draftCourse));

        assertThat(accessService.canViewCourseFullView(authenticationFor(studentUser), courseId)).isFalse();
    }

    @Test
    void coursePermissions_returnFalseWhenCourseIdIsNullForNonAdmin() {
        Authentication teacher = authenticationFor(user(RoleName.TEACHER, UUID.randomUUID()));

        assertThat(accessService.canEditCourse(teacher, null)).isFalse();
    }

    @Test
    void testPermissions_coverMissingLessonAndSuccessfulDelegation() {
        UUID missingTestId = UUID.randomUUID();
        UUID orphanedTestId = UUID.randomUUID();
        UUID managedTestId = UUID.randomUUID();
        UUID missingLessonId = UUID.randomUUID();
        UUID managedLessonId = UUID.randomUUID();

        org.elearning.backend.assessment.model.Test orphanedTest = test(orphanedTestId, missingLessonId);
        org.elearning.backend.assessment.model.Test managedTest = test(managedTestId, managedLessonId);
        UUID courseId = UUID.randomUUID();
        Lesson managedLesson = lesson(managedLessonId, courseId);

        when(testRepository.findById(missingTestId)).thenReturn(Optional.empty());
        when(testRepository.findById(orphanedTestId)).thenReturn(Optional.of(orphanedTest));
        when(testRepository.findById(managedTestId)).thenReturn(Optional.of(managedTest));
        when(lessonRepository.findById(missingLessonId)).thenReturn(Optional.empty());
        when(lessonRepository.findById(managedLessonId)).thenReturn(Optional.of(managedLesson));

        User teacherUser = user(RoleName.TEACHER, UUID.randomUUID());
        Authentication teacher = authenticationFor(teacherUser);
        User studentUser = user(RoleName.STUDENT, UUID.randomUUID());
        Authentication student = authenticationFor(studentUser);
        stubManagedCourse(courseId, teacherUser.getId());
        stubEnrolledStudent(courseId, studentUser.getId());

        assertThat(accessService.canViewTest(teacher, missingTestId)).isFalse();
        assertThat(accessService.canDeleteTest(teacher, orphanedTestId)).isFalse();
        assertThat(accessService.canEditTest(teacher, managedTestId)).isTrue();
        assertThat(accessService.canPublishTest(teacher, managedTestId)).isTrue();
        assertThat(accessService.canCreateTestQuestion(teacher, managedTestId)).isTrue();
        assertThat(accessService.canViewTestQuestions(teacher, managedTestId)).isTrue();
        assertThat(accessService.canStartTest(student, missingTestId)).isFalse();
        assertThat(accessService.canStartTest(student, orphanedTestId)).isFalse();
        assertThat(accessService.canStartTest(student, managedTestId)).isTrue();
    }

    @Test
    void testPermissions_returnFalseForNullAndBrokenCourseChains() {
        UUID testId = UUID.randomUUID();
        UUID testWithNullCourseId = UUID.randomUUID();
        UUID testWithNullCourse = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID lessonWithNullCourseId = UUID.randomUUID();
        UUID lessonWithNullCourse = UUID.randomUUID();
        org.elearning.backend.assessment.model.Test test = test(testId, lessonId);
        org.elearning.backend.assessment.model.Test testBrokenAtCourseId = test(testWithNullCourseId, lessonWithNullCourseId);
        org.elearning.backend.assessment.model.Test testBrokenAtCourse = test(testWithNullCourse, lessonWithNullCourse);
        Lesson lessonWithoutChapter = new Lesson();
        lessonWithoutChapter.setId(lessonId);
        Lesson lessonWithCourseWithoutId = new Lesson();
        lessonWithCourseWithoutId.setId(lessonWithNullCourseId);
        Chapter chapter = new Chapter();
        chapter.setCourse(new Course());
        lessonWithCourseWithoutId.setChapter(chapter);
        Lesson lessonWithMissingCourse = new Lesson();
        lessonWithMissingCourse.setId(lessonWithNullCourse);
        lessonWithMissingCourse.setChapter(new Chapter());

        when(testRepository.findById(testId)).thenReturn(Optional.of(test));
        when(testRepository.findById(testWithNullCourseId)).thenReturn(Optional.of(testBrokenAtCourseId));
        when(testRepository.findById(testWithNullCourse)).thenReturn(Optional.of(testBrokenAtCourse));
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lessonWithoutChapter));
        when(lessonRepository.findById(lessonWithNullCourseId)).thenReturn(Optional.of(lessonWithCourseWithoutId));
        when(lessonRepository.findById(lessonWithNullCourse)).thenReturn(Optional.of(lessonWithMissingCourse));

        Authentication teacher = authenticationFor(user(RoleName.TEACHER, UUID.randomUUID()));
        Authentication student = authenticationFor(user(RoleName.STUDENT, UUID.randomUUID()));

        assertThat(accessService.canStartTest(student, null)).isFalse();
        assertThat(accessService.canStartTest(student, testId)).isFalse();
        assertThat(accessService.canStartTest(student, testWithNullCourseId)).isFalse();
        assertThat(accessService.canStartTest(student, testWithNullCourse)).isFalse();
        assertThat(accessService.canViewTest(teacher, null)).isFalse();
        assertThat(accessService.canViewMyBestTestResult(student, null)).isFalse();
        assertThat(accessService.canViewMyBestTestResult(student, testId)).isFalse();
        assertThat(accessService.canViewMyBestTestResult(student, testWithNullCourseId)).isFalse();
    }

    @Test
    void bestResultPermissions_returnFalseWhenCourseExistsButStudentHasNoAccess() {
        UUID testId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        org.elearning.backend.assessment.model.Test test = test(testId, lessonId);
        Lesson lesson = lesson(lessonId, courseId);
        User studentUser = user(RoleName.STUDENT, UUID.randomUUID());

        when(testRepository.findById(testId)).thenReturn(Optional.of(test));
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course(courseId, UUID.randomUUID())));
        when(courseEnrollmentRepository.existsByStudentIdAndCourseId(studentUser.getId(), courseId)).thenReturn(false);

        assertThat(accessService.canViewMyBestTestResult(authenticationFor(studentUser), testId)).isFalse();
    }

    @Test
    void startTestPermissions_returnFalseWhenCourseExistsButUserCannotAccessIt() {
        UUID testId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        org.elearning.backend.assessment.model.Test test = test(testId, lessonId);
        Lesson lesson = lesson(lessonId, courseId);
        User studentUser = user(RoleName.STUDENT, UUID.randomUUID());

        when(testRepository.findById(testId)).thenReturn(Optional.of(test));
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course(courseId, UUID.randomUUID())));
        when(courseEnrollmentRepository.existsByStudentIdAndCourseId(studentUser.getId(), courseId)).thenReturn(false);

        assertThat(accessService.canStartTest(authenticationFor(studentUser), testId)).isFalse();
    }

    @Test
    void testQuestionPermissions_coverValidationBranches() {
        UUID targetTestId = UUID.randomUUID();
        UUID otherTestId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        int missingQuestionId = 10;
        int nullTestQuestionId = 11;
        int nullTestIdQuestionId = 12;
        int mismatchedQuestionId = 13;
        int validQuestionId = 14;

        org.elearning.backend.assessment.model.Test targetTest = test(targetTestId, lessonId);
        org.elearning.backend.assessment.model.Test otherTest = test(otherTestId, lessonId);
        org.elearning.backend.assessment.model.Test noIdTest = new org.elearning.backend.assessment.model.Test();
        Lesson lesson = lesson(lessonId, courseId);

        Question nullTestQuestion = new Question();
        nullTestQuestion.setId(nullTestQuestionId);

        Question nullTestIdQuestion = new Question();
        nullTestIdQuestion.setId(nullTestIdQuestionId);
        nullTestIdQuestion.setTest(noIdTest);

        Question mismatchedQuestion = new Question();
        mismatchedQuestion.setId(mismatchedQuestionId);
        mismatchedQuestion.setTest(otherTest);

        Question validQuestion = new Question();
        validQuestion.setId(validQuestionId);
        validQuestion.setTest(targetTest);

        when(testRepository.findById(targetTestId)).thenReturn(Optional.of(targetTest));
        when(testRepository.findById(otherTestId)).thenReturn(Optional.empty());
        when(questionRepository.findById(missingQuestionId)).thenReturn(Optional.empty());
        when(questionRepository.findById(nullTestQuestionId)).thenReturn(Optional.of(nullTestQuestion));
        when(questionRepository.findById(nullTestIdQuestionId)).thenReturn(Optional.of(nullTestIdQuestion));
        when(questionRepository.findById(mismatchedQuestionId)).thenReturn(Optional.of(mismatchedQuestion));
        when(questionRepository.findById(validQuestionId)).thenReturn(Optional.of(validQuestion));
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));

        Authentication admin = authenticationFor(user(RoleName.ADMIN, UUID.randomUUID()));
        User teacherUser = user(RoleName.TEACHER, UUID.randomUUID());
        Authentication teacher = authenticationFor(teacherUser);
        stubManagedCourse(courseId, teacherUser.getId());

        assertThat(accessService.canViewTestQuestion(null, targetTestId, validQuestionId)).isFalse();
        assertThat(accessService.canEditTestQuestion(admin, targetTestId, validQuestionId)).isTrue();
        assertThat(accessService.canViewTestQuestion(teacher, otherTestId, validQuestionId)).isFalse();
        assertThat(accessService.canViewTestQuestion(teacher, targetTestId, missingQuestionId)).isFalse();
        assertThat(accessService.canViewTestQuestion(teacher, targetTestId, nullTestQuestionId)).isFalse();
        assertThat(accessService.canViewTestQuestion(teacher, targetTestId, nullTestIdQuestionId)).isFalse();
        assertThat(accessService.canViewTestQuestion(teacher, targetTestId, mismatchedQuestionId)).isFalse();
        assertThat(accessService.canDeleteTestQuestion(teacher, targetTestId, validQuestionId)).isTrue();
    }

    @Test
    void testQuestionPermissions_returnFalseWhenLessonForTestIsMissing() {
        UUID targetTestId = UUID.randomUUID();
        UUID missingLessonId = UUID.randomUUID();
        int questionId = 77;

        org.elearning.backend.assessment.model.Test targetTest = test(targetTestId, missingLessonId);
        Question validQuestion = new Question();
        validQuestion.setId(questionId);
        validQuestion.setTest(targetTest);

        when(testRepository.findById(targetTestId)).thenReturn(Optional.of(targetTest));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(validQuestion));
        when(lessonRepository.findById(missingLessonId)).thenReturn(Optional.empty());

        Authentication teacher = authenticationFor(user(RoleName.TEACHER, UUID.randomUUID()));

        assertThat(accessService.canDeleteTestQuestion(teacher, targetTestId, questionId)).isFalse();
    }

    @Test
    void testQuestionPermissions_returnFalseForNullIdsAndBrokenLessonChain() {
        UUID targetTestId = UUID.randomUUID();
        UUID targetTestWithNullCourseId = UUID.randomUUID();
        UUID targetTestWithNullCourse = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID lessonWithNullCourseId = UUID.randomUUID();
        UUID lessonWithNullCourse = UUID.randomUUID();
        int questionId = 88;

        org.elearning.backend.assessment.model.Test targetTest = test(targetTestId, lessonId);
        org.elearning.backend.assessment.model.Test targetTestBrokenAtCourseId = test(targetTestWithNullCourseId, lessonWithNullCourseId);
        org.elearning.backend.assessment.model.Test targetTestBrokenAtCourse = test(targetTestWithNullCourse, lessonWithNullCourse);
        Question validQuestion = new Question();
        validQuestion.setId(questionId);
        validQuestion.setTest(targetTest);
        Question validQuestionForBrokenCourseId = new Question();
        validQuestionForBrokenCourseId.setId(questionId + 1);
        validQuestionForBrokenCourseId.setTest(targetTestBrokenAtCourseId);
        Question validQuestionForBrokenCourse = new Question();
        validQuestionForBrokenCourse.setId(questionId + 2);
        validQuestionForBrokenCourse.setTest(targetTestBrokenAtCourse);
        Lesson lessonWithoutChapter = new Lesson();
        lessonWithoutChapter.setId(lessonId);
        Lesson lessonWithBrokenCourseId = new Lesson();
        lessonWithBrokenCourseId.setId(lessonWithNullCourseId);
        Chapter chapter = new Chapter();
        chapter.setCourse(new Course());
        lessonWithBrokenCourseId.setChapter(chapter);
        Lesson lessonWithBrokenCourse = new Lesson();
        lessonWithBrokenCourse.setId(lessonWithNullCourse);
        lessonWithBrokenCourse.setChapter(new Chapter());

        when(testRepository.findById(targetTestId)).thenReturn(Optional.of(targetTest));
        when(testRepository.findById(targetTestWithNullCourseId)).thenReturn(Optional.of(targetTestBrokenAtCourseId));
        when(testRepository.findById(targetTestWithNullCourse)).thenReturn(Optional.of(targetTestBrokenAtCourse));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(validQuestion));
        when(questionRepository.findById(questionId + 1)).thenReturn(Optional.of(validQuestionForBrokenCourseId));
        when(questionRepository.findById(questionId + 2)).thenReturn(Optional.of(validQuestionForBrokenCourse));
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lessonWithoutChapter));
        when(lessonRepository.findById(lessonWithNullCourseId)).thenReturn(Optional.of(lessonWithBrokenCourseId));
        when(lessonRepository.findById(lessonWithNullCourse)).thenReturn(Optional.of(lessonWithBrokenCourse));

        Authentication teacher = authenticationFor(user(RoleName.TEACHER, UUID.randomUUID()));

        assertThat(accessService.canViewTestQuestion(teacher, null, questionId)).isFalse();
        assertThat(accessService.canViewTestQuestion(teacher, targetTestId, null)).isFalse();
        assertThat(accessService.canViewTestQuestion(teacher, targetTestId, questionId)).isFalse();
        assertThat(accessService.canViewTestQuestion(teacher, targetTestWithNullCourseId, questionId + 1)).isFalse();
        assertThat(accessService.canViewTestQuestion(teacher, targetTestWithNullCourse, questionId + 2)).isFalse();
    }

    @Test
    void attemptPermissions_coverSubmitAndViewResultChecks() {
        UUID ownAttemptId = UUID.randomUUID();
        UUID otherAttemptId = UUID.randomUUID();
        UUID missingAttemptId = UUID.randomUUID();
        User student = user(RoleName.STUDENT, UUID.randomUUID());

        TestAttempt ownAttempt = attempt(ownAttemptId, student.getId());
        TestAttempt otherAttempt = attempt(otherAttemptId, UUID.randomUUID());

        when(testAttemptRepository.findById(ownAttemptId)).thenReturn(Optional.of(ownAttempt));
        when(testAttemptRepository.findById(otherAttemptId)).thenReturn(Optional.of(otherAttempt));
        when(testAttemptRepository.findById(missingAttemptId)).thenReturn(Optional.empty());

        Authentication authentication = authenticationFor(student);

        assertThat(accessService.canSubmitAttempt(null, ownAttemptId)).isFalse();
        assertThat(accessService.canSubmitAttempt(authentication, missingAttemptId)).isFalse();
        assertThat(accessService.canSubmitAttempt(authentication, ownAttemptId)).isTrue();
        assertThat(accessService.canSubmitAttempt(authentication, otherAttemptId)).isFalse();
        assertThat(accessService.canViewAttemptResult(null, ownAttemptId)).isFalse();
        assertThat(accessService.canViewAttemptResult(authentication, missingAttemptId)).isFalse();
        assertThat(accessService.canViewAttemptResult(authentication, ownAttemptId)).isTrue();
        assertThat(accessService.canViewAttemptResult(authentication, otherAttemptId)).isFalse();
    }

    @Test
    void attemptPermissions_returnFalseForNullAttemptIds() {
        Authentication authentication = authenticationFor(user(RoleName.STUDENT, UUID.randomUUID()));

        assertThat(accessService.canSubmitAttempt(authentication, null)).isFalse();
        assertThat(accessService.canViewAttemptResult(authentication, null)).isFalse();
    }

    @Test
    void enrollmentPermissions_onlyAllowStudents() {
        UUID courseId = UUID.randomUUID();

        Authentication student = authenticationFor(user(RoleName.STUDENT, UUID.randomUUID()));
        Authentication teacher = authenticationFor(user(RoleName.TEACHER, UUID.randomUUID()));

        assertThat(accessService.canEnrollInCourse(null, courseId)).isFalse();
        assertThat(accessService.canEnrollInCourse(student, courseId)).isTrue();
        assertThat(accessService.canViewEnrolledCourses(student)).isTrue();
        assertThat(accessService.canUnenrollFromCourse(student, courseId)).isTrue();
        assertThat(accessService.canEnrollInCourse(teacher, courseId)).isFalse();
        assertThat(accessService.canViewEnrolledCourses(teacher)).isFalse();
        assertThat(accessService.canUnenrollFromCourse(teacher, courseId)).isFalse();
    }

    @Test
    void bestResultPermissions_coverStudentValidationAndDelegation() {
        UUID missingTestId = UUID.randomUUID();
        UUID orphanedTestId = UUID.randomUUID();
        UUID validTestId = UUID.randomUUID();
        UUID missingLessonId = UUID.randomUUID();
        UUID validLessonId = UUID.randomUUID();

        org.elearning.backend.assessment.model.Test orphanedTest = test(orphanedTestId, missingLessonId);
        org.elearning.backend.assessment.model.Test validTest = test(validTestId, validLessonId);
        UUID courseId = UUID.randomUUID();
        Lesson validLesson = lesson(validLessonId, courseId);

        when(testRepository.findById(missingTestId)).thenReturn(Optional.empty());
        when(testRepository.findById(orphanedTestId)).thenReturn(Optional.of(orphanedTest));
        when(testRepository.findById(validTestId)).thenReturn(Optional.of(validTest));
        when(lessonRepository.findById(missingLessonId)).thenReturn(Optional.empty());
        when(lessonRepository.findById(validLessonId)).thenReturn(Optional.of(validLesson));

        User studentUser = user(RoleName.STUDENT, UUID.randomUUID());
        Authentication student = authenticationFor(studentUser);
        Authentication teacher = authenticationFor(user(RoleName.TEACHER, UUID.randomUUID()));
        stubAccessibleCourse(courseId);
        stubEnrolledStudent(courseId, studentUser.getId());

        assertThat(accessService.canViewMyBestTestResult(null, validTestId)).isFalse();
        assertThat(accessService.canViewMyBestTestResult(teacher, validTestId)).isFalse();
        assertThat(accessService.canViewMyBestTestResult(student, missingTestId)).isFalse();
        assertThat(accessService.canViewMyBestTestResult(student, orphanedTestId)).isFalse();
        assertThat(accessService.canViewMyBestTestResult(student, validTestId)).isTrue();
        assertThat(accessService.canViewMyTestAttempts(student, validTestId)).isTrue();
    }

    @Test
    void markViewedPermission_requiresAuthenticationAndEnrolledStudent() {
        UUID lessonId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        User studentUser = user(RoleName.STUDENT, UUID.randomUUID());
        User teacherUser = user(RoleName.TEACHER, UUID.randomUUID());
        Lesson lesson = lesson(lessonId, courseId);

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        stubAccessibleCourse(courseId);
        stubEnrolledStudent(courseId, studentUser.getId());
        stubManagedCourse(courseId, teacherUser.getId());

        assertThat(accessService.canMarkViewedLesson(null, lessonId)).isFalse();
        assertThat(accessService.canMarkViewedLesson(authenticationFor(studentUser), lessonId)).isTrue();
    }

    @Test
    void courseAccessPermissions_coverOrganizationAdminAndUnsupportedRoles() {
        UUID organizationId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID organizationCourseId = UUID.randomUUID();
        UUID parentCourseId = UUID.randomUUID();
        UUID otherOrganizationCourseId = UUID.randomUUID();
        UUID otherOrganizationId = UUID.randomUUID();
        UUID otherCreatorId = UUID.randomUUID();
        User creator = user(RoleName.TEACHER, organizationId);
        creator.setId(creatorId);
        User otherCreator = user(RoleName.TEACHER, otherOrganizationId);
        otherCreator.setId(otherCreatorId);
        Course organizationCourse = course(organizationCourseId, creatorId);
        Course parentCourse = course(parentCourseId, creatorId);
        Course otherOrganizationCourse = course(otherOrganizationCourseId, otherCreatorId);

        when(courseRepository.findById(organizationCourseId)).thenReturn(Optional.of(organizationCourse));
        when(courseRepository.findById(parentCourseId)).thenReturn(Optional.of(parentCourse));
        when(courseRepository.findById(otherOrganizationCourseId)).thenReturn(Optional.of(otherOrganizationCourse));
        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
        when(userRepository.findById(otherCreatorId)).thenReturn(Optional.of(otherCreator));

        Authentication orgAdmin = authenticationFor(user(RoleName.ORGANIZATION_ADMIN, organizationId));
        Authentication parentAuthentication = authenticationFor(user(RoleName.PARENT, organizationId));

        assertThat(accessService.canViewCourseChapters(null, organizationCourseId)).isFalse();
        assertThat(accessService.canViewCourseChapters(orgAdmin, organizationCourseId)).isTrue();
        assertThat(accessService.canViewCourseChapters(orgAdmin, otherOrganizationCourseId)).isFalse();
        assertThat(accessService.canCreateChapter(orgAdmin, organizationCourseId)).isTrue();
        assertThat(accessService.canViewCourseChapters(parentAuthentication, parentCourseId)).isFalse();
        assertThat(accessService.canViewCourseChapters(authenticationFor(user(RoleName.STUDENT, organizationId)), parentCourseId)).isFalse();
    }

    @Test
    void courseAccessPermissions_restrictStudentsAndTeachersToTheirCourses() {
        UUID courseId = UUID.randomUUID();
        User creator = user(RoleName.TEACHER, UUID.randomUUID());
        creator.setId(UUID.randomUUID());
        Course course = course(courseId, creator.getId());
        User enrolledStudent = user(RoleName.STUDENT, UUID.randomUUID());
        User otherStudent = user(RoleName.STUDENT, UUID.randomUUID());
        User owningTeacher = user(RoleName.TEACHER, UUID.randomUUID());
        owningTeacher.setId(creator.getId());
        User otherTeacher = user(RoleName.TEACHER, UUID.randomUUID());

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseEnrollmentRepository.existsByStudentIdAndCourseId(enrolledStudent.getId(), courseId)).thenReturn(true);
        when(courseEnrollmentRepository.existsByStudentIdAndCourseId(otherStudent.getId(), courseId)).thenReturn(false);

        assertThat(accessService.canViewCourseFullView(authenticationFor(enrolledStudent), courseId)).isTrue();
        assertThat(accessService.canViewCourseFullView(authenticationFor(otherStudent), courseId)).isFalse();
        assertThat(accessService.canViewCourseFullView(authenticationFor(owningTeacher), courseId)).isTrue();
        assertThat(accessService.canViewCourseFullView(authenticationFor(otherTeacher), courseId)).isFalse();
        assertThat(accessService.canViewCourseFullView(authenticationFor(user(RoleName.PARENT, UUID.randomUUID())), courseId)).isFalse();
    }

    @Test
    void courseManagementPermissions_coverOrganizationCourseCreatorFailures() {
        UUID organizationId = UUID.randomUUID();
        UUID nullCreatorCourseId = UUID.randomUUID();
        UUID missingCreatorCourseId = UUID.randomUUID();
        UUID creatorWithoutOrganizationCourseId = UUID.randomUUID();
        UUID nullOrganizationAdminCourseId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID creatorWithoutOrganizationId = UUID.randomUUID();

        Course nullCreatorCourse = course(nullCreatorCourseId, null);
        Course missingCreatorCourse = course(missingCreatorCourseId, creatorId);
        Course creatorWithoutOrganizationCourse = course(creatorWithoutOrganizationCourseId, creatorWithoutOrganizationId);
        Course nullOrganizationAdminCourse = course(nullOrganizationAdminCourseId, creatorId);
        User creatorWithoutOrganization = user(RoleName.TEACHER, null);
        creatorWithoutOrganization.setId(creatorWithoutOrganizationId);

        when(courseRepository.findById(nullCreatorCourseId)).thenReturn(Optional.of(nullCreatorCourse));
        when(courseRepository.findById(missingCreatorCourseId)).thenReturn(Optional.of(missingCreatorCourse));
        when(courseRepository.findById(creatorWithoutOrganizationCourseId)).thenReturn(Optional.of(creatorWithoutOrganizationCourse));
        when(courseRepository.findById(nullOrganizationAdminCourseId)).thenReturn(Optional.of(nullOrganizationAdminCourse));
        when(userRepository.findById(creatorId)).thenReturn(Optional.empty());
        when(userRepository.findById(creatorWithoutOrganizationId)).thenReturn(Optional.of(creatorWithoutOrganization));

        Authentication orgAdmin = authenticationFor(user(RoleName.ORGANIZATION_ADMIN, organizationId));
        Authentication orgAdminWithoutOrganization = authenticationFor(user(RoleName.ORGANIZATION_ADMIN, null));
        Authentication otherTeacher = authenticationFor(user(RoleName.TEACHER, UUID.randomUUID()));

        assertThat(accessService.canCreateChapter(orgAdmin, nullCreatorCourseId)).isFalse();
        assertThat(accessService.canCreateChapter(orgAdmin, missingCreatorCourseId)).isFalse();
        assertThat(accessService.canCreateChapter(orgAdmin, creatorWithoutOrganizationCourseId)).isFalse();
        assertThat(accessService.canCreateChapter(orgAdminWithoutOrganization, nullOrganizationAdminCourseId)).isFalse();
        assertThat(accessService.canCreateChapter(otherTeacher, nullOrganizationAdminCourseId)).isFalse();
        assertThat(accessService.canCreateChapter(authenticationFor(user(RoleName.PARENT, organizationId)), nullOrganizationAdminCourseId)).isFalse();
    }

    @Test
    void classroomPermissions_returnFalseForNullIdsAndRequests() {
        UUID organizationId = UUID.randomUUID();
        Authentication teacher = authenticationFor(user(RoleName.TEACHER, organizationId));
        Authentication orgAdmin = authenticationFor(user(RoleName.ORGANIZATION_ADMIN, organizationId));
        AssignCoursesToClassroomRequest request = new AssignCoursesToClassroomRequest();
        request.setCourseIds(java.util.List.of(UUID.randomUUID()));

        assertThat(accessService.canAssignCoursesToClassroom(teacher, null, request)).isFalse();
        assertThat(accessService.canManageClassroom(orgAdmin, null)).isFalse();
        assertThat(accessService.canListClassroomMembers(orgAdmin, null)).isFalse();
    }

    private Authentication authenticationFor(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    private User user(RoleName roleName, UUID organizationId) {
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

    private Parent parent(UUID parentId, UUID organizationId) {
        Parent parent = new Parent();
        parent.setId(parentId);
        parent.setEmail("parent-" + parentId + "@example.com");
        parent.setPasswordHash("hashed");
        parent.setRole(new Role(RoleName.PARENT));
        parent.setStatus(UserStatus.ACTIVE);

        if (organizationId != null) {
            Organization organization = new Organization();
            organization.setId(organizationId);
            parent.setOrganization(organization);
        }

        return parent;
    }

    private Student student(UUID studentId, UUID organizationId) {
        Student student = new Student();
        student.setId(studentId);
        student.setEmail("student-" + studentId + "@example.com");
        student.setPasswordHash("hashed");
        student.setRole(new Role(RoleName.STUDENT));
        student.setStatus(UserStatus.ACTIVE);

        if (organizationId != null) {
            Organization organization = new Organization();
            organization.setId(organizationId);
            student.setOrganization(organization);
        }

        return student;
    }

    private Chapter chapter(UUID chapterId, UUID courseId) {
        Course course = new Course();
        course.setId(courseId);

        Chapter chapter = new Chapter();
        chapter.setId(chapterId);
        chapter.setCourse(course);
        return chapter;
    }

    private Lesson lesson(UUID lessonId, UUID courseId) {
        Chapter chapter = chapter(UUID.randomUUID(), courseId);

        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setChapter(chapter);
        return lesson;
    }

    private LessonResource lessonResource(UUID resourceId, UUID courseId) {
        LessonResource resource = new LessonResource();
        resource.setId(resourceId);
        resource.setLesson(lesson(UUID.randomUUID(), courseId));
        return resource;
    }

    private org.elearning.backend.assessment.model.Test test(UUID testId, UUID lessonId) {
        org.elearning.backend.assessment.model.Test test = new org.elearning.backend.assessment.model.Test();
        test.setId(testId);
        test.setLessonId(lessonId);
        return test;
    }

    private void stubManagedCourse(UUID courseId, UUID teacherId) {
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course(courseId, teacherId)));
    }

    private void stubAccessibleCourse(UUID courseId) {
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course(courseId, UUID.randomUUID())));
    }

    private void stubEnrolledStudent(UUID courseId, UUID studentId) {
        when(courseEnrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)).thenReturn(true);
    }

    private Course course(UUID courseId, UUID teacherId) {
        Course course = new Course();
        course.setId(courseId);
        course.setCreatedBy(teacherId);
        course.setStatus(org.elearning.backend.content.model.CourseStatus.PUBLISHED);
        return course;
    }

    private TestAttempt attempt(UUID attemptId, UUID studentId) {
        TestAttempt attempt = new TestAttempt();
        attempt.setId(attemptId);
        attempt.setStudentId(studentId);
        return attempt;
    }
}
