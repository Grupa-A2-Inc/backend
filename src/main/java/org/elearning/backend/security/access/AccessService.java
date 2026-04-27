package org.elearning.backend.security.access;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.assessment.model.Question;
import org.elearning.backend.assessment.model.Test;
import org.elearning.backend.assessment.model.TestAttempt;
import org.elearning.backend.assessment.repository.QuestionRepository;
import org.elearning.backend.assessment.repository.TestAttemptRepository;
import org.elearning.backend.assessment.repository.TestRepository;
import org.elearning.backend.classroom.entity.MembershipType;
import org.elearning.backend.classroom.repository.ClassroomMembershipRepository;
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
import org.elearning.backend.classroom.repository.ClassroomRepository;
import org.elearning.backend.classroom.entity.Classroom;
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

    // -------------------------------------------------------------------------
    // User management
    // -------------------------------------------------------------------------

    public boolean canCreateUser(Authentication authentication, CreateUserRequest request) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);
        if (currentUser == null) return false;
        if (currentUser.getRoleName() == RoleName.ADMIN) return true;
        return currentUser.getRoleName() == RoleName.ORGANIZATION_ADMIN
                && currentUser.getOrganizationId() != null
                && currentUser.getOrganizationId().equals(request.getOrganizationId());
    }

    public boolean canImportUsers(Authentication authentication, CreateUserBulkRequest request) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);
        if (currentUser == null) return false;
        if (currentUser.getRoleName() == RoleName.ADMIN) return true;
        if (currentUser.getRoleName() != RoleName.ORGANIZATION_ADMIN || currentUser.getOrganizationId() == null) {
            return false;
        }
        return request.getUsers().stream()
                .allMatch(u -> currentUser.getOrganizationId().equals(u.getOrganizationId()));
    }

    public boolean canViewUser(Authentication authentication, UUID targetUserId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);
        if (currentUser == null) return false;
        if (currentUser.getRoleName() == RoleName.ADMIN) return true;
        if (currentUser.getUserId().equals(targetUserId)) return true;
        if (currentUser.getRoleName() != RoleName.ORGANIZATION_ADMIN || currentUser.getOrganizationId() == null) {
            return false;
        }
        User targetUser = userRepository.findById(targetUserId).orElse(null);
        return targetUser != null
                && targetUser.getOrganization() != null
                && currentUser.getOrganizationId().equals(targetUser.getOrganization().getId());
    }

    public boolean canEditUser(Authentication authentication, UUID targetUserId) {
        return canViewUser(authentication, targetUserId);
    }

    public boolean canUpdateUserStatus(Authentication authentication, UUID targetUserId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);
        if (currentUser == null) return false;
        if (currentUser.getRoleName() == RoleName.ADMIN) return true;
        if (currentUser.getRoleName() != RoleName.ORGANIZATION_ADMIN || currentUser.getOrganizationId() == null) {
            return false;
        }
        User targetUser = userRepository.findById(targetUserId).orElse(null);
        if (targetUser == null || targetUser.getOrganization() == null) return false;
        return currentUser.getOrganizationId().equals(targetUser.getOrganization().getId());
    }

    public boolean canDeleteUser(Authentication authentication, UUID targetUserId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);
        if (currentUser == null) return false;
        if (currentUser.getRoleName() == RoleName.ADMIN) return true;
        User targetUser = userRepository.findById(targetUserId).orElse(null);
        if (targetUser == null || targetUser.getOrganization() == null) return false;
        return currentUser.getRoleName() == RoleName.ORGANIZATION_ADMIN
                && currentUser.getOrganizationId() != null
                && currentUser.getOrganizationId().equals(targetUser.getOrganization().getId());
    }

    public boolean canChangePassword(Authentication authentication, UUID targetUserId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);
        if (currentUser == null) return false;
        if (currentUser.getRoleName() == RoleName.ADMIN) return true;
        return currentUser.getUserId().equals(targetUserId);
    }

    // -------------------------------------------------------------------------
    // Organization
    // -------------------------------------------------------------------------

    public boolean canViewOrganization(Authentication authentication, UUID organizationId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);
        if (currentUser == null) return false;
        if (currentUser.getRoleName() == RoleName.ADMIN) return true;
        return currentUser.getRoleName() == RoleName.ORGANIZATION_ADMIN
                && currentUser.getOrganizationId() != null
                && currentUser.getOrganizationId().equals(organizationId)
                && organizationRepository.existsById(organizationId);
    }

    public boolean canEditOrganization(Authentication authentication, UUID organizationId) {
        return canViewOrganization(authentication, organizationId);
    }

    // -------------------------------------------------------------------------
    // Parent
    // -------------------------------------------------------------------------

    public boolean canViewAllParents(Authentication authentication) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);
        return currentUser != null && currentUser.getRoleName() == RoleName.ADMIN;
    }

    public boolean canViewParent(Authentication authentication, UUID parentId) {
        return canAdminOrOrganizationAdminAccessUser(authentication, parentId);
    }

    public boolean canManageParentStudent(Authentication authentication, UUID parentId, UUID studentId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);
        if (currentUser == null) return false;
        if (currentUser.getRoleName() == RoleName.ADMIN) return true;
        if (currentUser.getRoleName() != RoleName.ORGANIZATION_ADMIN || currentUser.getOrganizationId() == null) {
            return false;
        }
        Parent parent = parentRepository.findById(parentId).orElse(null);
        Student student = studentRepository.findById(studentId).orElse(null);
        return belongsToOrganization(parent, currentUser.getOrganizationId())
                && belongsToOrganization(student, currentUser.getOrganizationId());
    }

    public boolean canViewParentStudents(Authentication authentication, UUID parentId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);
        if (currentUser == null) return false;
        if (currentUser.getRoleName() == RoleName.ADMIN) return true;
        Parent parent = parentRepository.findById(parentId).orElse(null);
        if (currentUser.getRoleName() == RoleName.ORGANIZATION_ADMIN) {
            return belongsToOrganization(parent, currentUser.getOrganizationId());
        }
        return currentUser.getRoleName() == RoleName.PARENT
                && currentUser.getUserId().equals(parentId)
                && belongsToOrganization(parent, currentUser.getOrganizationId());
    }

    // -------------------------------------------------------------------------
    // Classroom
    // -------------------------------------------------------------------------

    public boolean canCreateClassroom(Authentication authentication) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);
        return currentUser != null
                && currentUser.getRoleName() == RoleName.ORGANIZATION_ADMIN
                && currentUser.getOrganizationId() != null;
    }

    public boolean canManageClassroom(Authentication authentication, UUID classroomId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);
        if (currentUser == null
                || currentUser.getRoleName() != RoleName.ORGANIZATION_ADMIN
                || currentUser.getOrganizationId() == null) {
            return false;
        }
        Classroom classroom = classroomRepository.findById(classroomId).orElse(null);
        return classroom != null
                && classroom.getOrganization() != null
                && currentUser.getOrganizationId().equals(classroom.getOrganization().getId());
    }

    public boolean canListClassroomMembers(Authentication authentication, UUID classroomId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);
        if (currentUser == null) return false;

        Classroom classroom = classroomRepository.findById(classroomId).orElse(null);
        if (classroom == null || classroom.getOrganization() == null) return false;

        UUID classroomOrganizationId = classroom.getOrganization().getId();

        if (currentUser.getRoleName() == RoleName.ORGANIZATION_ADMIN) {
            return currentUser.getOrganizationId() != null
                    && currentUser.getOrganizationId().equals(classroomOrganizationId);
        }

        if (currentUser.getRoleName() == RoleName.TEACHER) {
            return classroomMembershipRepository.existsByClassroomIdAndUserIdAndMembershipType(
                    classroomId,
                    currentUser.getUserId(),
                    MembershipType.TEACHER
            );
        }

        return false;
    }

    // -------------------------------------------------------------------------
    // Course
    // -------------------------------------------------------------------------

    public boolean canCreateCourse(Authentication authentication) {
        return canCreateOrViewOwnCourses(authentication);
    }

    public boolean canEditCourse(Authentication authentication, UUID targetCourseId) {
        return canManageCourse(authentication, targetCourseId);
    }

    public boolean canDeleteCourse(Authentication authentication, UUID targetCourseId) {
        return canManageCourse(authentication, targetCourseId);
    }

    public boolean canReplaceCourse(Authentication authentication, UUID targetCourseId) {
        return canManageCourse(authentication, targetCourseId);
    }

    public boolean canViewCourseFullView(Authentication authentication, UUID targetCourseId) {
        return canAccessCourse(authentication, targetCourseId);
    }

    public boolean canViewPublicCourses(Authentication authentication) {
        return true;
    }

    public boolean canViewMyCourses(Authentication authentication) {
        return canCreateOrViewOwnCourses(authentication);
    }

    public boolean canViewCourseChapters(Authentication authentication, UUID targetCourseId) {
        return canAccessCourse(authentication, targetCourseId);
    }

    public boolean canCreateChapter(Authentication authentication, UUID targetCourseId) {
        return canManageCourse(authentication, targetCourseId);
    }

    public boolean canEditChapter(Authentication authentication, UUID targetChapterId) {
        return canManageChapterCourse(authentication, targetChapterId);
    }

    public boolean canDeleteChapter(Authentication authentication, UUID targetChapterId) {
        return canManageChapterCourse(authentication, targetChapterId);
    }

    // -------------------------------------------------------------------------
    // Lesson
    // -------------------------------------------------------------------------

    public boolean canViewChapterLessons(Authentication authentication, UUID targetChapterId) {
        return canManageChapterCourse(authentication, targetChapterId);
    }

    public boolean canCreateLessons(Authentication authentication, UUID targetChapterId) {
        return canManageChapterCourse(authentication, targetChapterId);
    }

    public boolean canEditLessonMetaData(Authentication authentication, UUID targetLessonId) {
        return canManageLessonCourse(authentication, targetLessonId);
    }

    public boolean canViewLessonContent(Authentication authentication, UUID targetLessonId) {
        return canAccessLessonCourse(authentication, targetLessonId);
    }

    public boolean canEditLessonContent(Authentication authentication, UUID targetLessonId) {
        return canManageLessonCourse(authentication, targetLessonId);
    }

    public boolean canDeleteLesson(Authentication authentication, UUID targetLessonId) {
        return canManageLessonCourse(authentication, targetLessonId);
    }

    public boolean canViewLessonResources(Authentication authentication, UUID lessonId) {
        return canAccessLessonCourse(authentication, lessonId);
    }

    public boolean canCreateLessonResource(Authentication authentication, UUID lessonId) {
        return canManageLessonCourse(authentication, lessonId);
    }

    public boolean canEditLessonResource(Authentication authentication, UUID resourceId) {
        return canManageLessonResource(authentication, resourceId);
    }

    public boolean canDeleteLessonResource(Authentication authentication, UUID resourceId) {
        return canManageLessonResource(authentication, resourceId);
    }

    public boolean canMarkViewedLesson(Authentication authentication, UUID lessonId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);
        if (currentUser == null) return false;
        return currentUser.getRoleName() == RoleName.STUDENT
                && canAccessLessonCourse(authentication, lessonId);
    }

    // -------------------------------------------------------------------------
    // Test
    // -------------------------------------------------------------------------

    public boolean canViewLessonTest(Authentication authentication, UUID lessonId) {
        return canAccessLessonCourse(authentication, lessonId);
    }

    public boolean canCreateLessonTest(Authentication authentication, UUID lessonId) {
        return canManageLessonCourse(authentication, lessonId);
    }

    public boolean canViewTest(Authentication authentication, UUID testId) {
        return canManageTest(authentication, testId);
    }

    public boolean canDeleteTest(Authentication authentication, UUID testId) {
        return canManageTest(authentication, testId);
    }

    public boolean canEditTest(Authentication authentication, UUID testId) {
        return canManageTest(authentication, testId);
    }

    public boolean canPublishTest(Authentication authentication, UUID testId) {
        return canManageTest(authentication, testId);
    }

    public boolean canViewTestQuestions(Authentication authentication, UUID targetTestId) {
        return canManageTest(authentication, targetTestId);
    }

    public boolean canViewTestQuestion(Authentication authentication, UUID targetTestId, Integer targetQuestionId) {
        return canManageTestQuestion(authentication, targetTestId, targetQuestionId);
    }

    public boolean canCreateTestQuestion(Authentication authentication, UUID targetTestId) {
        return canManageTest(authentication, targetTestId);
    }

    public boolean canEditTestQuestion(Authentication authentication, UUID targetTestId, Integer targetQuestionId) {
        return canManageTestQuestion(authentication, targetTestId, targetQuestionId);
    }

    public boolean canDeleteTestQuestion(Authentication authentication, UUID targetTestId, Integer targetQuestionId) {
        return canManageTestQuestion(authentication, targetTestId, targetQuestionId);
    }

    public boolean canStartTest(Authentication authentication, UUID targetTestId) {
        Test test = testRepository.findById(targetTestId).orElse(null);
        if (test == null) return false;
        Lesson lesson = lessonRepository.findById(test.getLessonId()).orElse(null);
        if (lesson == null) return false;
        return canAccessCourse(authentication, lesson.getChapter().getCourse().getId());
    }

    public boolean canSubmitAttempt(Authentication authentication, UUID targetAttemptId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);
        if (currentUser == null) return false;
        TestAttempt attempt = testAttemptRepository.findById(targetAttemptId).orElse(null);
        if (attempt == null) return false;
        return currentUser.getUserId().equals(attempt.getStudentId());
    }

    public boolean canViewMyBestTestResult(Authentication authentication, UUID testId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);
        if (currentUser == null) return false;
        if (currentUser.getRoleName() != RoleName.STUDENT) return false;
        Test test = testRepository.findById(testId).orElse(null);
        if (test == null) return false;
        Lesson lesson = lessonRepository.findById(test.getLessonId()).orElse(null);
        if (lesson == null) return false;
        return canAccessCourse(authentication, lesson.getChapter().getCourse().getId());
    }

    public boolean canViewMyTestAttempts(Authentication authentication, UUID testId) {
        return canViewMyBestTestResult(authentication, testId);
    }

    public boolean canViewAttemptResult(Authentication authentication, UUID attemptId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);
        if (currentUser == null) return false;
        TestAttempt attempt = testAttemptRepository.findById(attemptId).orElse(null);
        if (attempt == null) return false;
        return currentUser.getUserId().equals(attempt.getStudentId());
    }

    // -------------------------------------------------------------------------
    // Enrollment
    // -------------------------------------------------------------------------

    public boolean canEnrollInCourse(Authentication authentication, UUID courseId) {
        return isStudent(authentication);
    }

    public boolean canViewEnrolledCourses(Authentication authentication) {
        return isStudent(authentication);
    }

    public boolean canUnenrollFromCourse(Authentication authentication, UUID courseId) {
        return isStudent(authentication);
    }

    // -------------------------------------------------------------------------
    // AI / Adaptive
    // -------------------------------------------------------------------------

    public boolean canSubmitAdaptiveSession(Authentication authentication, UUID sessionId) {
        return isStudent(authentication);
    }

    public boolean canInjectAiQuestions(Authentication authentication, UUID requestId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);
        if (currentUser == null) return false;
        return currentUser.getRoleName() == RoleName.TEACHER;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private boolean canManageChapterCourse(Authentication authentication, UUID targetChapterId) {
        return chapterRepository.findById(targetChapterId)
                .map(chapter -> canManageCourse(authentication, chapter.getCourse().getId()))
                .orElse(false);
    }

    private boolean canManageLessonCourse(Authentication authentication, UUID targetLessonId) {
        Lesson lesson = lessonRepository.findById(targetLessonId).orElse(null);
        if (lesson == null) return false;
        return canManageCourse(authentication, lesson.getChapter().getCourse().getId());
    }

    private boolean canAccessLessonCourse(Authentication authentication, UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) return false;
        return canAccessCourse(authentication, lesson.getChapter().getCourse().getId());
    }

    private boolean canCreateOrViewOwnCourses(Authentication authentication) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);
        if (currentUser == null) return false;
        if (currentUser.getRoleName() == RoleName.ADMIN) return true;
        return currentUser.getRoleName() == RoleName.TEACHER;
    }

    private boolean isStudent(Authentication authentication) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);
        if (currentUser == null) return false;
        return currentUser.getRoleName() == RoleName.STUDENT;
    }

    private boolean canManageTest(Authentication authentication, UUID targetTestId) {
        Test test = testRepository.findById(targetTestId).orElse(null);
        if (test == null) return false;
        Lesson lesson = lessonRepository.findById(test.getLessonId()).orElse(null);
        if (lesson == null) return false;
        return canManageCourse(authentication, lesson.getChapter().getCourse().getId());
    }

    private boolean canManageTestQuestion(Authentication authentication, UUID targetTestId, Integer targetQuestionId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);
        if (currentUser == null) return false;
        if (currentUser.getRoleName() == RoleName.ADMIN) return true;

        Test test = testRepository.findById(targetTestId).orElse(null);
        if (test == null) return false;

        Question question = questionRepository.findById(targetQuestionId).orElse(null);
        if (question == null) return false;
        if (question.getTest() == null || question.getTest().getId() == null) return false;
        if (!question.getTest().getId().equals(targetTestId)) return false;

        Lesson lesson = lessonRepository.findById(test.getLessonId()).orElse(null);
        if (lesson == null) return false;

        return canManageCourse(authentication, lesson.getChapter().getCourse().getId());
    }

    private boolean canAccessCourse(Authentication authentication, UUID courseId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);
        if (currentUser == null) return false;
        if (currentUser.getRoleName() == RoleName.ADMIN) return true;

        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) return false;

        if (currentUser.getRoleName() == RoleName.ORGANIZATION_ADMIN) {
            return isCourseCreatedInOrganization(course, currentUser.getOrganizationId());
        }

        // TODO: pentru TEACHER verifica daca preda cursul
        // TODO: pentru STUDENT verifica daca e inscris
        return true;
    }

    private boolean canManageCourse(Authentication authentication, UUID courseId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);
        if (currentUser == null) return false;
        if (currentUser.getRoleName() == RoleName.ADMIN) return true;

        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) return false;

        if (currentUser.getRoleName() == RoleName.ORGANIZATION_ADMIN) {
            return isCourseCreatedInOrganization(course, currentUser.getOrganizationId());
        }

        return currentUser.getRoleName() == RoleName.TEACHER
                && currentUser.getUserId().equals(course.getCreatedBy());
    }

    private boolean isCourseCreatedInOrganization(Course course, UUID organizationId) {
        if (course.getCreatedBy() == null || organizationId == null) return false;
        User creator = userRepository.findById(course.getCreatedBy()).orElse(null);
        return creator != null
                && creator.getOrganization() != null
                && organizationId.equals(creator.getOrganization().getId());
    }

    private boolean canAdminOrOrganizationAdminAccessUser(Authentication authentication, UUID targetUserId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);
        if (currentUser == null) return false;
        if (currentUser.getRoleName() == RoleName.ADMIN) return true;
        if (currentUser.getRoleName() != RoleName.ORGANIZATION_ADMIN || currentUser.getOrganizationId() == null) {
            return false;
        }
        User targetUser = userRepository.findById(targetUserId).orElse(null);
        return belongsToOrganization(targetUser, currentUser.getOrganizationId());
    }

    private boolean belongsToOrganization(User user, UUID organizationId) {
        return user != null
                && user.getOrganization() != null
                && organizationId != null
                && organizationId.equals(user.getOrganization().getId());
    }

    private boolean canManageLessonResource(Authentication authentication, UUID targetResourceId) {
        CustomUserDetails currentUser = extractCurrentUser(authentication);
        if (currentUser == null) return false;
        if (currentUser.getRoleName() == RoleName.ADMIN) return true;
        LessonResource lessonResource = lessonResourceRepository.findById(targetResourceId).orElse(null);
        if (lessonResource == null) return false;
        return canManageCourse(authentication, lessonResource.getLesson().getChapter().getCourse().getId());
    }

    public CustomUserDetails extractCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return null;
        }
        return userDetails;
    }
}