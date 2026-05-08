package org.elearning.backend.security.access;

import jdk.jfr.Description;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.assessment.model.Question;
import org.elearning.backend.assessment.model.Test;
import org.elearning.backend.assessment.model.TestAttempt;
import org.elearning.backend.classroom.dto.request.AssignCoursesToClassroomRequest;
import org.elearning.backend.assessment.repository.QuestionRepository;
import org.elearning.backend.assessment.repository.TestAttemptRepository;
import org.elearning.backend.assessment.repository.TestRepository;
import org.elearning.backend.classroom.entity.Classroom;
import org.elearning.backend.classroom.entity.MembershipType;
import org.elearning.backend.classroom.repository.ClassroomMembershipRepository;
import org.elearning.backend.classroom.repository.ClassroomRepository;
import org.elearning.backend.content.model.Chapter;
import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.content.model.LessonResource;
import org.elearning.backend.content.repository.ChapterRepository;
import org.elearning.backend.content.repository.CourseRepository;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.content.repository.LessonResourceRepository;
import org.elearning.backend.enrollment.repository.CourseEnrollmentRepository;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.parent.entity.Parent;
import org.elearning.backend.parent.repository.ParentRepository;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.elearning.backend.student.entity.Student;
import org.elearning.backend.student.repository.StudentRepository;
import org.elearning.backend.user.dto.request.CreateUserBulkRequest;
import org.elearning.backend.user.dto.request.CreateUserRequest;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.function.BiPredicate;

@Service("accessService")
@RequiredArgsConstructor
public class AccessService {
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final LessonResourceRepository lessonResourceRepository;
    private final TestRepository testRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final QuestionRepository questionRepository;
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassroomMembershipRepository classroomMembershipRepository;

    public boolean canCreateUser(Authentication authentication, CreateUserRequest request) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        if (currentUser == null || request == null) {
            return false;
        }

        if (isAdmin(currentUser)) {
            return true;
        }

        return isOrganizationAdmin(currentUser)
                && currentUser.getOrganizationId().equals(request.getOrganizationId());
    }

    public boolean canImportUsers(Authentication authentication, CreateUserBulkRequest request) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        if (currentUser == null || request == null || request.getUsers() == null) {
            return false;
        }

        if (isAdmin(currentUser)) {
            return true;
        }

        if (!isOrganizationAdmin(currentUser)) {
            return false;
        }

        return request.getUsers().stream()
                .allMatch(user -> currentUser.getOrganizationId().equals(user.getOrganizationId()));
    }

    public boolean canViewUser(Authentication authentication, UUID targetUserId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        if (currentUser == null) {
            return false;
        }

        if (isAdmin(currentUser)) {
            return true;
        }

        if (currentUser.getUserId().equals(targetUserId)) {
            return true;
        }

        return canOrganizationAdminAccessUser(currentUser, targetUserId);
    }

    public boolean canEditUser(Authentication authentication, UUID targetUserId) {
        return canViewUser(authentication, targetUserId);
    }

    public boolean canUpdateUserStatus(Authentication authentication, UUID targetUserId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        if (currentUser == null) {
            return false;
        }

        return canAdminOrOrganizationAdminAccessUser(authentication, targetUserId);
    }

    public boolean canViewOrganization(Authentication authentication, UUID organizationId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        if (currentUser == null) {
            return false;
        }

        if (isAdmin(currentUser)) {
            return true;
        }

        return isOrganizationAdmin(currentUser)
                && currentUser.getOrganizationId().equals(organizationId)
                && organizationRepository.existsById(organizationId);
    }

    public boolean canEditOrganization(Authentication authentication, UUID organizationId) {
        return canViewOrganization(authentication, organizationId);
    }

    public boolean canDeleteUser(Authentication authentication, UUID targetUserId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        if (currentUser == null || targetUserId == null) {
            return false;
        }

        if (isAdmin(currentUser)) {
            return true;
        }

        User targetUser = userRepository.findById(targetUserId).orElse(null);

        return targetUser != null
                && targetUser.getOrganization() != null
                && isOrganizationAdmin(currentUser)
                && currentUser.getOrganizationId().equals(targetUser.getOrganization().getId());
    }

    public boolean canChangePassword(Authentication authentication, UUID targetUserId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        if (currentUser == null) {
            return false;
        }

        if (isAdmin(currentUser)) {
            return true;
        }

        return currentUser.getUserId().equals(targetUserId);
    }

    public boolean canViewAllParents(Authentication authentication) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        return currentUser != null && isAdmin(currentUser);
    }

    public boolean canViewParent(Authentication authentication, UUID parentId) {
        return canAdminOrOrganizationAdminAccessUser(authentication, parentId);
    }

    public boolean canManageParentStudent(Authentication authentication, UUID parentId, UUID studentId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        if (currentUser == null || parentId == null || studentId == null) {
            return false;
        }

        if (isAdmin(currentUser)) {
            return true;
        }

        if (!isOrganizationAdmin(currentUser)) {
            return false;
        }

        Parent parent = parentRepository.findById(parentId).orElse(null);
        Student student = studentRepository.findById(studentId).orElse(null);

        return belongsToOrganization(parent, currentUser.getOrganizationId())
                && belongsToOrganization(student, currentUser.getOrganizationId());
    }

    public boolean canViewParentStudents(Authentication authentication, UUID parentId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        if (currentUser == null || parentId == null) {
            return false;
        }

        if (isAdmin(currentUser)) {
            return true;
        }

        Parent parent = parentRepository.findById(parentId).orElse(null);

        if (isOrganizationAdmin(currentUser)) {
            return belongsToOrganization(parent, currentUser.getOrganizationId());
        }

        return currentUser.getRoleName() == RoleName.PARENT
                && currentUser.getUserId().equals(parentId)
                && belongsToOrganization(parent, currentUser.getOrganizationId());
    }

    @Description("Returns true if the user can view the lessons from the given chapter, false otherwise")
    public boolean canViewChapterLessons(Authentication authentication, UUID targetChapterId) {
        if (targetChapterId == null) {
            return false;
        }

        Chapter chapter = chapterRepository.findById(targetChapterId).orElse(null);

        if (chapter == null || chapter.getCourse() == null || chapter.getCourse().getId() == null) {
            return false;
        }

        return canManageCourse(authentication, chapter.getCourse().getId());
    }

    @Description("Defines who can create lessons for a specific chapter. Returns true if the user can manage that course, false otherwise")
    public boolean canCreateLessons(Authentication authentication, UUID targetChapterId) {
        return canManageChapterCourse(authentication, targetChapterId);
    }

    @Description("Defines who can edit metadata for a specific lesson. Returns true if the user can manage that course, false otherwise")
    public boolean canEditLessonMetaData(Authentication authentication, UUID targetLessonId) {
        return canManageLessonCourse(authentication, targetLessonId);
    }

    @Description("Defines who can view the content of a specific lesson. Returns true if the user can access that course, false otherwise")
    public boolean canViewLessonContent(Authentication authentication, UUID targetLessonId) {
        return canEditLessonContent(authentication, targetLessonId);
    }

    @Description("Defines who can edit the content of a specific lesson. Returns true if the user can manage that course, false otherwise")
    public boolean canEditLessonContent(Authentication authentication, UUID targetLessonId) {
        return canEditLessonMetaData(authentication, targetLessonId);
    }

    @Description("Defines who can delete a specific lesson. Returns true if the user can manage that course, false otherwise")
    public boolean canDeleteLesson(Authentication authentication, UUID targetLessonId) {
        return canEditLessonMetaData(authentication, targetLessonId);
    }

    @Description("Defines who can view the resources of a specific lesson. Returns true if the user can access that course, false otherwise")
    public boolean canViewLessonResources(Authentication authentication, UUID lessonId) {
        return canAccessLessonCourse(authentication, lessonId);
    }

    @Description("Defines who can create a lesson resource for a specific lesson. Returns true if the user can manage that course, false otherwise")
    public boolean canCreateLessonResource(Authentication authentication, UUID lessonId) {
        return canManageLessonCourse(authentication, lessonId);
    }

    @Description("Defines who can edit a specific lesson resource. Returns true if the user can manage that resource, false otherwise")
    public boolean canEditLessonResource(Authentication authentication, UUID resourceId) {
        return canManageLessonResource(authentication, resourceId);
    }

    @Description("Defines who can delete a specific lesson resource. Returns true if the user can manage that resource, false otherwise")
    public boolean canDeleteLessonResource(Authentication authentication, UUID resourceId) {
        return canManageLessonResource(authentication, resourceId);
    }

    @Description("Defines who can view the chapters of a specific course. Returns true if the user can access that course, false otherwise")
    public boolean canViewCourseChapters(Authentication authentication, UUID targetCourseId) {
        return canAccessCourse(authentication, targetCourseId);
    }

    @Description("Defines who can create chapters for a specific course. Returns true if the user can manage that course, false otherwise")
    public boolean canCreateChapter(Authentication authentication, UUID targetCourseId) {
        return canManageCourse(authentication, targetCourseId);
    }

    @Description("Defines who can edit a specific chapter. Returns true if the user can manage that chapter's course, false otherwise")
    public boolean canEditChapter(Authentication authentication, UUID targetChapterId) {
        return canCreateLessons(authentication, targetChapterId);
    }

    @Description("Defines who can delete a specific chapter. Returns true if the user can manage that chapter's course, false otherwise")
    public boolean canDeleteChapter(Authentication authentication, UUID targetChapterId) {
        return canEditChapter(authentication, targetChapterId);
    }

    @Description("Defines who can create a course. Returns true if the user is allowed to create courses, false otherwise")
    public boolean canCreateCourse(Authentication authentication) {
        return canCreateOrViewOwnCourses(authentication);
    }

    @Description("Defines who can edit a specific course. Returns true if the user can manage that course, false otherwise")
    public boolean canEditCourse(Authentication authentication, UUID targetCourseId) {
        return canManageCourse(authentication, targetCourseId);
    }

    @Description("Defines who can delete a specific course. Returns true if the user can manage that course, false otherwise")
    public boolean canDeleteCourse(Authentication authentication, UUID targetCourseId) {
        return canManageCourse(authentication, targetCourseId);
    }

    @Description("Defines who can update a specific course through PUT. Returns true if the user can manage that course, false otherwise")
    public boolean canReplaceCourse(Authentication authentication, UUID targetCourseId) {
        return canManageCourse(authentication, targetCourseId);
    }

    @Description("Defines who can view the full view of a specific course. Returns true if the user can access that course, false otherwise")
    public boolean canViewCourseFullView(Authentication authentication, UUID targetCourseId) {
        return canAccessCourse(authentication, targetCourseId);
    }

    @Description("Defines who can view public courses. Returns true for everyone, even unauthenticated users")
    public boolean canViewPublicCourses(Authentication authentication) {
        return true;
    }

    @Description("Defines who can view their own courses. Returns true for authenticated teachers and students, false otherwise")
    public boolean canViewMyCourses(Authentication authentication) {
        return canCreateCourse(authentication);
    }

    @Description("Defines who can view a specific question from a test. Returns true if the user can manage that test, false otherwise")
    public boolean canViewTestQuestion(Authentication authentication, UUID targetTestId, Integer targetQuestionId) {
        return canManageTestQuestion(authentication, targetTestId, targetQuestionId);
    }

    @Description("Defines who can create a question for a specific test. Returns true if the user can manage that test, false otherwise")
    public boolean canCreateTestQuestion(Authentication authentication, UUID targetTestId) {
        return canManageTest(authentication, targetTestId);
    }

    @Description("Defines who can edit a specific question from a test. Returns true if the user can manage that test question, false otherwise")
    public boolean canEditTestQuestion(Authentication authentication, UUID targetTestId, Integer targetQuestionId) {
        return canManageTestQuestion(authentication, targetTestId, targetQuestionId);
    }

    @Description("Defines who can delete a specific question from a test. Returns true if the user can manage that test question, false otherwise")
    public boolean canDeleteTestQuestion(Authentication authentication, UUID targetTestId, Integer targetQuestionId) {
        return canManageTestQuestion(authentication, targetTestId, targetQuestionId);
    }

    @Description("Defines who can view all questions from a specific test. Returns true if the user can manage that test, false otherwise")
    public boolean canViewTestQuestions(Authentication authentication, UUID targetTestId) {
        return canManageTest(authentication, targetTestId);
    }

    @Description("Defines who can start a specific test. Returns true if the user can access the course of that test, false otherwise")
    public boolean canStartTest(Authentication authentication, UUID targetTestId) {
        UUID courseId = findCourseIdForTest(targetTestId);
        return courseId != null && canAccessCourse(authentication, courseId);
    }

    @Description("Defines who can submit a specific attempt. Returns true if the attempt belongs to the current user, false otherwise")
    public boolean canSubmitAttempt(Authentication authentication, UUID targetAttemptId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        if (currentUser == null || targetAttemptId == null) {
            return false;
        }

        TestAttempt attempt = testAttemptRepository.findById(targetAttemptId).orElse(null);

        if (attempt == null) {
            return false;
        }

        return currentUser.getUserId().equals(attempt.getStudentId());
    }

    @Description("Defines who can view the test of a lesson. Returns true if the user can access that course, false otherwise")
    public boolean canViewLessonTest(Authentication authentication, UUID lessonId) {
        return canViewLessonResources(authentication, lessonId);
    }

    @Description("Defines who can create a test for a lesson. Returns true if the user can manage that course, false otherwise")
    public boolean canCreateLessonTest(Authentication authentication, UUID lessonId) {
        return canCreateLessonResource(authentication, lessonId);
    }

    @Description("Defines who can view test details. Returns true if the user can manage that test, false otherwise")
    public boolean canViewTest(Authentication authentication, UUID testId) {
        return canManageTest(authentication, testId);
    }

    @Description("Defines who can delete a test. Returns true if the user can manage that test, false otherwise")
    public boolean canDeleteTest(Authentication authentication, UUID testId) {
        return canManageTest(authentication, testId);
    }

    @Description("Defines who can edit test metadata. Returns true if the user can manage that test, false otherwise")
    public boolean canEditTest(Authentication authentication, UUID testId) {
        return canManageTest(authentication, testId);
    }

    @Description("Defines who can publish a test. Returns true if the user can manage that test, false otherwise")
    public boolean canPublishTest(Authentication authentication, UUID testId) {
        return canManageTest(authentication, testId);
    }

    @Description("Defines who can enroll in a course. Returns true if the user is a student, false otherwise")
    public boolean canEnrollInCourse(Authentication authentication, UUID courseId) {
        return isStudent(authentication);
    }

    @Description("Defines who can view their enrolled courses. Returns true if the user is a student, false otherwise")
    public boolean canViewEnrolledCourses(Authentication authentication) {
        return canEnrollInCourse(authentication, null);
    }

    @Description("Defines who can unenroll from a course. Returns true if the user is a student, false otherwise")
    public boolean canUnenrollFromCourse(Authentication authentication, UUID courseId) {
        return canEnrollInCourse(authentication, courseId);
    }

    @Description("Defines who can view their best result for a test. Returns true if the user is a student and has access to the course")
    public boolean canViewMyBestTestResult(Authentication authentication, UUID testId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        if (currentUser == null || testId == null) {
            return false;
        }

        if (currentUser.getRoleName() != RoleName.STUDENT) {
            return false;
        }

        UUID courseId = findCourseIdForTest(testId);
        return courseId != null && canAccessCourse(authentication, courseId);
    }

    @Description("Defines who can view their attempts for a test. Returns true if the user is a student and has access to the course")
    public boolean canViewMyTestAttempts(Authentication authentication, UUID testId) {
        return canViewMyBestTestResult(authentication, testId);
    }

    @Description("Defines who can view a specific attempt result. Returns true if the attempt belongs to the user")
    public boolean canViewAttemptResult(Authentication authentication, UUID attemptId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        if (currentUser == null || attemptId == null) {
            return false;
        }

        TestAttempt attempt = testAttemptRepository.findById(attemptId).orElse(null);

        if (attempt == null) {
            return false;
        }

        return currentUser.getUserId().equals(attempt.getStudentId());
    }

    @Description("Defines who can submit an adaptive session. Returns true only for students")
    public boolean canSubmitAdaptiveSession(Authentication authentication, UUID sessionId) {
        return isStudent(authentication);
    }

    @Description("Defines who can inject AI-generated questions. Returns true only for teachers")
    public boolean canInjectAiQuestions(Authentication authentication, UUID requestId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        if (currentUser == null) {
            return false;
        }

        return isTeacher(currentUser);
    }

    public boolean canCreateClassroom(Authentication authentication) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        return currentUser != null && isOrganizationAdmin(currentUser);
    }

    public boolean canAssignCoursesToClassroom(Authentication authentication,
                                               UUID classroomId,
                                               AssignCoursesToClassroomRequest request) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        if (currentUser == null || classroomId == null) {
            return false;
        }

        if (!isTeacher(currentUser)
                || currentUser.getOrganizationId() == null
                || request == null
                || request.getCourseIds() == null
                || request.getCourseIds().isEmpty()) {
            return false;
        }

        Classroom classroom = classroomRepository.findById(classroomId).orElse(null);
        if (classroom == null || classroom.getOrganization() == null) {
            return false;
        }

        if (!currentUser.getOrganizationId().equals(classroom.getOrganization().getId())) {
            return false;
        }

        return request.getCourseIds().stream()
                .allMatch(courseId -> courseRepository.findById(courseId)
                        .map(course -> currentUser.getUserId().equals(course.getCreatedBy()))
                        .orElse(false));
    }

    public boolean canManageClassroom(Authentication authentication, UUID classroomId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        if (currentUser == null || classroomId == null) {
            return false;
        }

        Classroom classroom = classroomRepository.findById(classroomId).orElse(null);
        if (classroom == null || classroom.getOrganization() == null) {
            return false;
        }

        if (isAdmin(currentUser)) {
            return true;
        }

        if (isOrganizationAdmin(currentUser)) {
            return currentUser.getOrganizationId().equals(classroom.getOrganization().getId());
        }

        return false;
     }

    public boolean canListClassroomMembers(Authentication authentication, UUID classroomId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        if (currentUser == null || classroomId == null) {
            return false;
        }

        Classroom classroom = classroomRepository.findById(classroomId).orElse(null);

        if (classroom == null || classroom.getOrganization() == null) {
            return false;
        }

        UUID classroomOrganizationId = classroom.getOrganization().getId();

        if (isOrganizationAdmin(currentUser)) {
            return currentUser.getOrganizationId().equals(classroomOrganizationId);
        }

        if (isTeacher(currentUser)) {
            return classroomMembershipRepository.existsByClassroomIdAndUserIdAndMembershipType(
                    classroomId,
                    currentUser.getUserId(),
                    MembershipType.TEACHER
            );
        }

        return false;
    }

    public boolean canViewClassroomCourses(Authentication authentication, UUID classroomId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        if (currentUser == null) {
            return false;
        }

        if (canManageClassroom(authentication, classroomId)) {
            return true;
        }

        if (isTeacher(currentUser) || isStudent(currentUser)) {
            return classroomMembershipRepository
                    .existsByClassroomIdAndUserId(classroomId, currentUser.getUserId());
        }

        return false;
    }

    public boolean canMarkViewedLesson(Authentication authentication, UUID lessonId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        if (currentUser == null) {
            return false;
        }

        return isStudent(currentUser)
                && canAccessLessonCourse(authentication, lessonId);
    }

    private boolean canManageChapterCourse(Authentication authentication, UUID targetChapterId) {
        if (targetChapterId == null) {
            return false;
        }

        return chapterRepository.findById(targetChapterId)
                .map(chapter -> chapter.getCourse() != null ? chapter.getCourse().getId() : null)
                .map(courseId -> canManageCourse(authentication, courseId))
                .orElse(false);
    }

    private boolean canManageLessonCourse(Authentication authentication, UUID targetLessonId) {
        if (targetLessonId == null) {
            return false;
        }

        Lesson lesson = lessonRepository.findById(targetLessonId).orElse(null);

        if (lesson == null || lesson.getChapter() == null || lesson.getChapter().getCourse() == null
                || lesson.getChapter().getCourse().getId() == null) {
            return false;
        }

        return canManageCourse(authentication, lesson.getChapter().getCourse().getId());
    }

    private boolean canAccessLessonCourse(Authentication authentication, UUID lessonId) {
        if (lessonId == null) {
            return false;
        }

        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);

        if (lesson == null || lesson.getChapter() == null || lesson.getChapter().getCourse() == null
                || lesson.getChapter().getCourse().getId() == null) {
            return false;
        }

        return canAccessCourse(authentication, lesson.getChapter().getCourse().getId());
    }

    private boolean canCreateOrViewOwnCourses(Authentication authentication) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        if (currentUser == null) {
            return false;
        }

        if (isAdmin(currentUser)) {
            return true;
        }

        return isTeacher(currentUser);
    }

    private boolean isStudent(Authentication authentication) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        if (currentUser == null) {
            return false;
        }

        return isStudent(currentUser);
    }

    @Description("Returns true if the user can manage the given test, false otherwise")
    private boolean canManageTest(Authentication authentication, UUID targetTestId) {
        UUID courseId = findCourseIdForTest(targetTestId);
        return courseId != null && canManageCourse(authentication, courseId);
    }

    @Description("Returns true if the user can manage the given test question, false otherwise")
    private boolean canManageTestQuestion(Authentication authentication, UUID targetTestId, Integer targetQuestionId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        if (currentUser == null || targetTestId == null || targetQuestionId == null) {
            return false;
        }

        if (isAdmin(currentUser)) {
            return true;
        }

        Test test = testRepository.findById(targetTestId).orElse(null);

        if (test == null) {
            return false;
        }

        Question question = questionRepository.findById(targetQuestionId).orElse(null);

        if (question == null) {
            return false;
        }

        if (question.getTest() == null || question.getTest().getId() == null) {
            return false;
        }

        if (!question.getTest().getId().equals(targetTestId)) {
            return false;
        }

        Lesson lesson = lessonRepository.findById(test.getLessonId()).orElse(null);

        if (lesson == null) {
            return false;
        }

        if (lesson.getChapter() == null || lesson.getChapter().getCourse() == null || lesson.getChapter().getCourse().getId() == null) {
            return false;
        }

        return canManageCourse(authentication, lesson.getChapter().getCourse().getId());
    }

    @Description("Returns true if the user can access the given course, false otherwise")
    private boolean canAccessCourse(Authentication authentication, UUID courseId) {
        return evaluateCourseAccess(authentication, courseId, (currentUser, course) -> {
            if (isTeacher(currentUser)) {
                return currentUser.getUserId().equals(course.getCreatedBy());
            }

            if (isStudent(currentUser)) {
                return courseEnrollmentRepository.existsByStudentIdAndCourseId(currentUser.getUserId(), courseId);
            }

            return false;
        });
    }

    @Description("Returns true if the user can manage the given course, false otherwise")
    private boolean canManageCourse(Authentication authentication, UUID courseId) {
        return evaluateCourseAccess(authentication, courseId, (currentUser, course) ->
                isTeacher(currentUser) && currentUser.getUserId().equals(course.getCreatedBy())
        );
    }

    private Course findCourse(UUID courseId) {
        if (courseId == null) {
            return null;
        }

        return courseRepository.findById(courseId).orElse(null);
    }

    private UUID findCourseIdForTest(UUID testId) {
        if (testId == null) {
            return null;
        }

        Test test = testRepository.findById(testId).orElse(null);
        if (test == null) {
            return null;
        }

        Lesson lesson = lessonRepository.findById(test.getLessonId()).orElse(null);
        if (lesson == null || lesson.getChapter() == null || lesson.getChapter().getCourse() == null) {
            return null;
        }

        return lesson.getChapter().getCourse().getId();
    }

    private boolean evaluateCourseAccess(
            Authentication authentication,
            UUID courseId,
            BiPredicate<CustomUserDetails, Course> evaluator
    ) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        if (currentUser == null) {
            return false;
        }

        if (isAdmin(currentUser)) {
            return true;
        }

        Course course = findCourse(courseId);
        if (course == null) {
            return false;
        }

        if (isOrganizationAdmin(currentUser)) {
            return canOrganizationAdminAccessCourse(currentUser, course);
        }

        return evaluator.test(currentUser, course);
    }

    private boolean canOrganizationAdminAccessCourse(CustomUserDetails currentUser, Course course) {
        return isCourseCreatedInOrganization(course, currentUser.getOrganizationId());
    }

    private boolean isCourseCreatedInOrganization(Course course, UUID organizationId) {
        if (course.getCreatedBy() == null || organizationId == null) {
            return false;
        }

        User creator = userRepository.findById(course.getCreatedBy()).orElse(null);

        return creator != null
                && creator.getOrganization() != null
                && organizationId.equals(creator.getOrganization().getId());
    }

    private boolean canAdminOrOrganizationAdminAccessUser(Authentication authentication, UUID targetUserId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        if (currentUser == null) {
            return false;
        }

        if (isAdmin(currentUser)) {
            return true;
        }

        return canOrganizationAdminAccessUser(currentUser, targetUserId);
    }

    private boolean belongsToOrganization(User user, UUID organizationId) {
        return user != null
                && user.getOrganization() != null
                && organizationId != null
                && organizationId.equals(user.getOrganization().getId());
    }

    private boolean canOrganizationAdminAccessUser(CustomUserDetails currentUser, UUID targetUserId) {
        if (!isOrganizationAdmin(currentUser) || targetUserId == null) {
            return false;
        }

        User targetUser = userRepository.findById(targetUserId).orElse(null);
        return belongsToOrganization(targetUser, currentUser.getOrganizationId());
    }

    private boolean isAdmin(CustomUserDetails currentUser) {
        return currentUser.getRoleName() == RoleName.ADMIN;
    }

    private boolean isOrganizationAdmin(CustomUserDetails currentUser) {
        return currentUser.getRoleName() == RoleName.ORGANIZATION_ADMIN
                && currentUser.getOrganizationId() != null;
    }

    private boolean isTeacher(CustomUserDetails currentUser) {
        return currentUser.getRoleName() == RoleName.TEACHER;
    }

    private boolean isStudent(CustomUserDetails currentUser) {
        return currentUser.getRoleName() == RoleName.STUDENT;
    }

    @Description("Returns true if the user can manage the given lesson resource, false otherwise")
    private boolean canManageLessonResource(Authentication authentication, UUID targetResourceId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);

        if (currentUser == null || targetResourceId == null) {
            return false;
        }

        if (isAdmin(currentUser)) {
            return true;
        }

        LessonResource lessonResource = lessonResourceRepository.findById(targetResourceId).orElse(null);

        if (lessonResource == null) {
            return false;
        }

        if (lessonResource.getLesson() == null
                || lessonResource.getLesson().getChapter() == null
                || lessonResource.getLesson().getChapter().getCourse() == null
                || lessonResource.getLesson().getChapter().getCourse().getId() == null) {
            return false;
        }

        return canManageCourse(authentication, lessonResource.getLesson().getChapter().getCourse().getId());
    }

    public CustomUserDetails extractCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return null;
        }

        return userDetails;
    }
}
