package org.elearning.backend.security.access;

import org.elearning.backend.assessment.model.Question;
import org.elearning.backend.assessment.model.TestAttempt;
import org.elearning.backend.assessment.repository.QuestionRepository;
import org.elearning.backend.assessment.repository.TestAttemptRepository;
import org.elearning.backend.assessment.repository.TestRepository;
import org.elearning.backend.content.model.Chapter;
import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.content.model.LessonResource;
import org.elearning.backend.content.repository.ChapterRepository;
import org.elearning.backend.content.repository.CourseRepository;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.content.repository.LessonResourceRepository;
import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.auth.CustomUserDetails;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessServiceCoverageTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LessonResourceRepository lessonResourceRepository;

    @Mock
    private CourseRepository courseRepository;

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

        Authentication student = authenticationFor(user(RoleName.STUDENT, UUID.randomUUID()));
        User teacherUser = user(RoleName.TEACHER, UUID.randomUUID());
        Authentication teacher = authenticationFor(teacherUser);
        stubManagedCourse(courseId, teacherUser.getId());

        assertThat(accessService.canViewLessonResources(student, lessonId)).isTrue();
        assertThat(accessService.canViewLessonResources(student, missingLessonId)).isFalse();
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
    void coursePermissions_coverCreateViewAndManageOperations() {
        UUID courseId = UUID.randomUUID();

        Authentication admin = authenticationFor(user(RoleName.ADMIN, UUID.randomUUID()));
        User teacherUser = user(RoleName.TEACHER, UUID.randomUUID());
        Authentication teacher = authenticationFor(teacherUser);
        Authentication student = authenticationFor(user(RoleName.STUDENT, UUID.randomUUID()));
        stubManagedCourse(courseId, teacherUser.getId());

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
        Authentication student = authenticationFor(user(RoleName.STUDENT, UUID.randomUUID()));
        stubManagedCourse(courseId, teacherUser.getId());

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
        Lesson validLesson = lesson(validLessonId, UUID.randomUUID());

        when(testRepository.findById(missingTestId)).thenReturn(Optional.empty());
        when(testRepository.findById(orphanedTestId)).thenReturn(Optional.of(orphanedTest));
        when(testRepository.findById(validTestId)).thenReturn(Optional.of(validTest));
        when(lessonRepository.findById(missingLessonId)).thenReturn(Optional.empty());
        when(lessonRepository.findById(validLessonId)).thenReturn(Optional.of(validLesson));

        Authentication student = authenticationFor(user(RoleName.STUDENT, UUID.randomUUID()));
        Authentication teacher = authenticationFor(user(RoleName.TEACHER, UUID.randomUUID()));

        assertThat(accessService.canViewMyBestTestResult(null, validTestId)).isFalse();
        assertThat(accessService.canViewMyBestTestResult(teacher, validTestId)).isFalse();
        assertThat(accessService.canViewMyBestTestResult(student, missingTestId)).isFalse();
        assertThat(accessService.canViewMyBestTestResult(student, orphanedTestId)).isFalse();
        assertThat(accessService.canViewMyBestTestResult(student, validTestId)).isTrue();
        assertThat(accessService.canViewMyTestAttempts(student, validTestId)).isTrue();
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
        when(courseRepository.getReferenceById(courseId)).thenReturn(course(courseId, teacherId));
    }

    private Course course(UUID courseId, UUID teacherId) {
        Course course = new Course();
        course.setId(courseId);
        course.setCreatedBy(teacherId);
        return course;
    }

    private TestAttempt attempt(UUID attemptId, UUID studentId) {
        TestAttempt attempt = new TestAttempt();
        attempt.setId(attemptId);
        attempt.setStudentId(studentId);
        return attempt;
    }
}
