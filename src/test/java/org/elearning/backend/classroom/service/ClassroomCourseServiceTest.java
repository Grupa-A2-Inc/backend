package org.elearning.backend.classroom.service;

import org.elearning.backend.classroom.dto.request.AssignCoursesToClassroomRequest;
import org.elearning.backend.classroom.dto.response.ClassroomCourseDetailsResponse;
import org.elearning.backend.classroom.dto.response.ClassroomCourseResponse;
import org.elearning.backend.classroom.entity.Classroom;
import org.elearning.backend.classroom.entity.ClassroomCourse;
import org.elearning.backend.classroom.exception.ClassroomBadRequestException;
import org.elearning.backend.classroom.exception.ClassroomNotFoundException;
import org.elearning.backend.classroom.exception.CourseNotEligibleException;
import org.elearning.backend.classroom.repository.ClassroomCourseRepository;
import org.elearning.backend.classroom.repository.ClassroomMembershipRepository;
import org.elearning.backend.classroom.repository.ClassroomRepository;
import org.elearning.backend.common.dto.response.PaginatedResponse;
import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.model.CourseStatus;
import org.elearning.backend.content.model.CourseVisibility;
import org.elearning.backend.content.repository.CourseRepository;
import org.elearning.backend.enrollment.model.CourseEnrollment;
import org.elearning.backend.enrollment.repository.CourseEnrollmentRepository;
import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.exception.UserNotFoundException;
import org.elearning.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@org.springframework.test.context.ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class ClassroomCourseServiceTest {

    @Mock private ClassroomRepository classroomRepository;
    @Mock private ClassroomCourseRepository classroomCourseRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClassroomMembershipRepository classroomMembershipRepository;
    @Mock private CourseEnrollmentRepository courseEnrollmentRepository;

    @InjectMocks
    private ClassroomCourseService classroomCourseService;

    private UUID userId;
    private UUID orgId;
    private UUID classroomId;
    private UUID courseId;
    private User requester;
    private Organization organization;
    private Classroom classroom;
    private Course course;
    private User courseCreator;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orgId = UUID.randomUUID();
        classroomId = UUID.randomUUID();
        courseId = UUID.randomUUID();

        organization = new Organization();
        organization.setId(orgId);

        requester = new User();
        requester.setId(userId);
        requester.setOrganization(organization);

        classroom = new Classroom();
        classroom.setId(classroomId);
        classroom.setOrganization(organization);

        courseCreator = new User();
        courseCreator.setId(UUID.randomUUID());
        courseCreator.setOrganization(organization);

        course = new Course();
        course.setId(courseId);
        course.setCreatedBy(courseCreator.getId());
        course.setStatus(CourseStatus.PUBLISHED);
        course.setVisibility(CourseVisibility.PUBLIC);
    }

    @Test
    void assignCourses_savesNewAssociation() {
        AssignCoursesToClassroomRequest request = new AssignCoursesToClassroomRequest();
        request.setCourseIds(List.of(courseId));

        when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        when(classroomRepository.findByIdAndOrganizationId(classroomId, orgId)).thenReturn(Optional.of(classroom));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(userRepository.findById(courseCreator.getId())).thenReturn(Optional.of(courseCreator));
        when(classroomCourseRepository.existsByClassroomIdAndCourseId(classroomId, courseId)).thenReturn(false);

        ClassroomCourse saved = new ClassroomCourse();
        saved.setId(UUID.randomUUID());
        saved.setClassroomId(classroomId);
        saved.setCourseId(courseId);
        saved.setAssignedAt(LocalDateTime.now());
        when(classroomCourseRepository.saveAll(any())).thenReturn(List.of(saved));

        List<ClassroomCourseResponse> result = classroomCourseService.assignCourses(classroomId, request, userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClassroomId()).isEqualTo(classroomId);
        assertThat(result.get(0).getCourseId()).isEqualTo(courseId);
    }

    @Test
    void assignCourses_skipsDuplicates() {
        AssignCoursesToClassroomRequest request = new AssignCoursesToClassroomRequest();
        request.setCourseIds(List.of(courseId));

        when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        when(classroomRepository.findByIdAndOrganizationId(classroomId, orgId)).thenReturn(Optional.of(classroom));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(userRepository.findById(courseCreator.getId())).thenReturn(Optional.of(courseCreator));
        when(classroomCourseRepository.existsByClassroomIdAndCourseId(classroomId, courseId)).thenReturn(true);
        when(classroomCourseRepository.saveAll(any())).thenReturn(List.of());

        List<ClassroomCourseResponse> result = classroomCourseService.assignCourses(classroomId, request, userId);

        assertThat(result).isEmpty();
        verify(classroomCourseRepository, never()).save(any());
    }

    @Test
    void assignCourses_throwsUserNotFound_whenUserMissing() {
        AssignCoursesToClassroomRequest request = new AssignCoursesToClassroomRequest();
        request.setCourseIds(List.of(courseId));

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> classroomCourseService.assignCourses(classroomId, request, userId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void assignCourses_throwsBadRequest_whenUserHasNoOrganization() {
        requester.setOrganization(null);
        AssignCoursesToClassroomRequest request = new AssignCoursesToClassroomRequest();
        request.setCourseIds(List.of(courseId));

        when(userRepository.findById(userId)).thenReturn(Optional.of(requester));

        assertThatThrownBy(() -> classroomCourseService.assignCourses(classroomId, request, userId))
                .isInstanceOf(ClassroomBadRequestException.class);
    }

    @Test
    void assignCourses_throwsClassroomNotFound_whenClassroomMissing() {
        AssignCoursesToClassroomRequest request = new AssignCoursesToClassroomRequest();
        request.setCourseIds(List.of(courseId));

        when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        when(classroomRepository.findByIdAndOrganizationId(classroomId, orgId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> classroomCourseService.assignCourses(classroomId, request, userId))
                .isInstanceOf(ClassroomNotFoundException.class);
    }

    @Test
    void assignCourses_throwsBadRequest_whenCourseNotFound() {
        AssignCoursesToClassroomRequest request = new AssignCoursesToClassroomRequest();
        request.setCourseIds(List.of(courseId));

        when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        when(classroomRepository.findByIdAndOrganizationId(classroomId, orgId)).thenReturn(Optional.of(classroom));
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> classroomCourseService.assignCourses(classroomId, request, userId))
                .isInstanceOf(ClassroomBadRequestException.class);
    }

    @Test
    void assignCourses_throwsCourseNotEligible_whenCourseHasNoCreator() {
        course.setCreatedBy(null);
        AssignCoursesToClassroomRequest request = new AssignCoursesToClassroomRequest();
        request.setCourseIds(List.of(courseId));

        when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        when(classroomRepository.findByIdAndOrganizationId(classroomId, orgId)).thenReturn(Optional.of(classroom));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> classroomCourseService.assignCourses(classroomId, request, userId))
                .isInstanceOf(CourseNotEligibleException.class)
                .hasMessageContaining("no creator");
    }

    @Test
    void assignCourses_throwsCourseNotEligible_whenCreatorNotFound() {
        AssignCoursesToClassroomRequest request = new AssignCoursesToClassroomRequest();
        request.setCourseIds(List.of(courseId));

        when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        when(classroomRepository.findByIdAndOrganizationId(classroomId, orgId)).thenReturn(Optional.of(classroom));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(userRepository.findById(courseCreator.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> classroomCourseService.assignCourses(classroomId, request, userId))
                .isInstanceOf(CourseNotEligibleException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void assignCourses_throwsCourseNotEligible_whenCreatorHasNoOrganization() {
        courseCreator.setOrganization(null);
        AssignCoursesToClassroomRequest request = new AssignCoursesToClassroomRequest();
        request.setCourseIds(List.of(courseId));

        when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        when(classroomRepository.findByIdAndOrganizationId(classroomId, orgId)).thenReturn(Optional.of(classroom));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(userRepository.findById(courseCreator.getId())).thenReturn(Optional.of(courseCreator));

        assertThatThrownBy(() -> classroomCourseService.assignCourses(classroomId, request, userId))
                .isInstanceOf(CourseNotEligibleException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void assignCourses_throwsCourseNotEligible_whenCreatorBelongsToDifferentOrg() {
        Organization otherOrg = new Organization();
        otherOrg.setId(UUID.randomUUID());
        courseCreator.setOrganization(otherOrg);

        AssignCoursesToClassroomRequest request = new AssignCoursesToClassroomRequest();
        request.setCourseIds(List.of(courseId));

        when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        when(classroomRepository.findByIdAndOrganizationId(classroomId, orgId)).thenReturn(Optional.of(classroom));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(userRepository.findById(courseCreator.getId())).thenReturn(Optional.of(courseCreator));

        assertThatThrownBy(() -> classroomCourseService.assignCourses(classroomId, request, userId))
                .isInstanceOf(CourseNotEligibleException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void getClassroomCourses_throwsNotFound_whenClassroomDoesNotExist() {
        when(classroomRepository.existsById(classroomId)).thenReturn(false);

        assertThatThrownBy(() -> classroomCourseService.getClassroomCourses(
                classroomId, 0, 10, null, null, null, null))
                .isInstanceOf(ClassroomNotFoundException.class)
                .hasMessageContaining(classroomId.toString());
    }


    @Test
    void getClassroomCourses_returnsEmptyList_whenNoCoursesAssigned() {
        when(classroomRepository.existsById(classroomId)).thenReturn(true);
        when(classroomCourseRepository.findAllByClassroomIdOrderByAssignedAtAsc(classroomId))
                .thenReturn(List.of());

        PaginatedResponse<ClassroomCourseDetailsResponse> result =
                classroomCourseService.getClassroomCourses(classroomId, 0, 10, null, null, null, null);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void getClassroomCourses_returnsMappedDetails_whenCoursesExist() {
        ClassroomCourse cc = new ClassroomCourse();
        cc.setClassroomId(classroomId);
        cc.setCourseId(courseId);
        cc.setAssignedAt(LocalDateTime.of(2026, 4, 28, 10, 0));

        when(classroomRepository.existsById(classroomId)).thenReturn(true);
        when(classroomCourseRepository.findAllByClassroomIdOrderByAssignedAtAsc(classroomId))
                .thenReturn(List.of(cc));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        PaginatedResponse<ClassroomCourseDetailsResponse> result =
                classroomCourseService.getClassroomCourses(classroomId, 0, 10, null, null, null, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCourseId()).isEqualTo(courseId);
        assertThat(result.getContent().get(0).getAssignedAt()).isEqualTo(cc.getAssignedAt());
        assertThat(result.getTotalElements()).isEqualTo(1L);
    }

    @Test
    void getClassroomCourses_excludesDraftCourses() {
        ClassroomCourse cc = new ClassroomCourse();
        cc.setClassroomId(classroomId);
        cc.setCourseId(courseId);
        cc.setAssignedAt(LocalDateTime.of(2026, 4, 28, 10, 0));
        course.setStatus(CourseStatus.DRAFT);

        when(classroomRepository.existsById(classroomId)).thenReturn(true);
        when(classroomCourseRepository.findAllByClassroomIdOrderByAssignedAtAsc(classroomId))
                .thenReturn(List.of(cc));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        PaginatedResponse<ClassroomCourseDetailsResponse> result =
                classroomCourseService.getClassroomCourses(classroomId, 0, 10, null, null, null, null);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void getClassroomCourses_withSearch_filtersOnTitle() {
        UUID mathId = UUID.randomUUID();
        UUID historyId = UUID.randomUUID();

        Course math = buildCourse(mathId, "Math 101", null, CourseStatus.PUBLISHED);
        Course history = buildCourse(historyId, "History", null, CourseStatus.PUBLISHED);

        ClassroomCourse cc1 = buildCc(mathId);
        ClassroomCourse cc2 = buildCc(historyId);

        when(classroomRepository.existsById(classroomId)).thenReturn(true);
        when(classroomCourseRepository.findAllByClassroomIdOrderByAssignedAtAsc(classroomId))
                .thenReturn(List.of(cc1, cc2));
        when(courseRepository.findById(mathId)).thenReturn(Optional.of(math));
        when(courseRepository.findById(historyId)).thenReturn(Optional.of(history));

        PaginatedResponse<ClassroomCourseDetailsResponse> result =
                classroomCourseService.getClassroomCourses(classroomId, 0, 10, "math", null, null, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Math 101");
        assertThat(result.getTotalElements()).isEqualTo(1L);
    }

    @Test
    void getClassroomCourses_withCategory_filtersOnCategory() {
        UUID scienceId = UUID.randomUUID();
        UUID humanitiesId = UUID.randomUUID();

        Course science = buildCourse(scienceId, "Math 101", "Science", CourseStatus.PUBLISHED);
        Course humanities = buildCourse(humanitiesId, "History", "Humanities", CourseStatus.PUBLISHED);

        ClassroomCourse cc1 = buildCc(scienceId);
        ClassroomCourse cc2 = buildCc(humanitiesId);

        when(classroomRepository.existsById(classroomId)).thenReturn(true);
        when(classroomCourseRepository.findAllByClassroomIdOrderByAssignedAtAsc(classroomId))
                .thenReturn(List.of(cc1, cc2));
        when(courseRepository.findById(scienceId)).thenReturn(Optional.of(science));
        when(courseRepository.findById(humanitiesId)).thenReturn(Optional.of(humanities));

        PaginatedResponse<ClassroomCourseDetailsResponse> result =
                classroomCourseService.getClassroomCourses(classroomId, 0, 10, null, "Science", null, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Math 101");
        assertThat(result.getTotalElements()).isEqualTo(1L);
    }

    @Test
    void getClassroomCourses_sortByTitle_returnsAscendingOrder() {
        UUID aId = UUID.randomUUID();
        UUID zId = UUID.randomUUID();

        Course courseA = buildCourse(aId, "Algebra", null, CourseStatus.PUBLISHED);
        Course courseZ = buildCourse(zId, "Zoology", null, CourseStatus.PUBLISHED);

        ClassroomCourse cc1 = buildCc(zId);
        ClassroomCourse cc2 = buildCc(aId);

        when(classroomRepository.existsById(classroomId)).thenReturn(true);
        when(classroomCourseRepository.findAllByClassroomIdOrderByAssignedAtAsc(classroomId))
                .thenReturn(List.of(cc1, cc2));
        when(courseRepository.findById(zId)).thenReturn(Optional.of(courseZ));
        when(courseRepository.findById(aId)).thenReturn(Optional.of(courseA));

        PaginatedResponse<ClassroomCourseDetailsResponse> result =
                classroomCourseService.getClassroomCourses(classroomId, 0, 10, null, null, "title", "asc");

        assertThat(result.getContent())
                .extracting(ClassroomCourseDetailsResponse::getTitle)
                .containsExactly("Algebra", "Zoology");
    }

    @Test
    void getClassroomCourses_paginationSlicesCorrectly() {
        UUID aId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();
        UUID cId = UUID.randomUUID();

        Course courseA = buildCourse(aId, "A", null, CourseStatus.PUBLISHED);
        Course courseB = buildCourse(bId, "B", null, CourseStatus.PUBLISHED);
        Course courseC = buildCourse(cId, "C", null, CourseStatus.PUBLISHED);

        when(classroomRepository.existsById(classroomId)).thenReturn(true);
        when(classroomCourseRepository.findAllByClassroomIdOrderByAssignedAtAsc(classroomId))
                .thenReturn(List.of(buildCc(aId), buildCc(bId), buildCc(cId)));
        when(courseRepository.findById(aId)).thenReturn(Optional.of(courseA));
        when(courseRepository.findById(bId)).thenReturn(Optional.of(courseB));
        when(courseRepository.findById(cId)).thenReturn(Optional.of(courseC));

        PaginatedResponse<ClassroomCourseDetailsResponse> result =
                classroomCourseService.getClassroomCourses(classroomId, 0, 2, null, null, "title", "asc");

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3L);
        assertThat(result.getContent())
                .extracting(ClassroomCourseDetailsResponse::getTitle)
                .containsExactly("A", "B");
    }

    @Test
    void getClassroomCourses_sortByAssignedAtDescending_returnsDescendingOrder() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();

        ClassroomCourse earlier = buildCc(firstId);
        earlier.setAssignedAt(LocalDateTime.of(2026, 4, 28, 8, 0));
        ClassroomCourse later = buildCc(secondId);
        later.setAssignedAt(LocalDateTime.of(2026, 4, 28, 10, 0));

        when(classroomRepository.existsById(classroomId)).thenReturn(true);
        when(classroomCourseRepository.findAllByClassroomIdOrderByAssignedAtAsc(classroomId))
                .thenReturn(List.of(earlier, later));
        when(courseRepository.findById(firstId)).thenReturn(Optional.of(buildCourse(firstId, "A", null, CourseStatus.PUBLISHED)));
        when(courseRepository.findById(secondId)).thenReturn(Optional.of(buildCourse(secondId, "B", null, CourseStatus.PUBLISHED)));

        PaginatedResponse<ClassroomCourseDetailsResponse> result =
                classroomCourseService.getClassroomCourses(classroomId, 0, 10, null, null, "assignedAt", "desc");

        assertThat(result.getContent())
                .extracting(ClassroomCourseDetailsResponse::getCourseId)
                .containsExactly(secondId, firstId);
    }

    @Test
    void getClassroomCourses_withInvalidSortAndBlankFilters_usesDefaults() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();

        ClassroomCourse earlier = buildCc(firstId);
        earlier.setAssignedAt(LocalDateTime.of(2026, 4, 28, 8, 0));
        ClassroomCourse later = buildCc(secondId);
        later.setAssignedAt(LocalDateTime.of(2026, 4, 28, 10, 0));

        when(classroomRepository.existsById(classroomId)).thenReturn(true);
        when(classroomCourseRepository.findAllByClassroomIdOrderByAssignedAtAsc(classroomId))
                .thenReturn(List.of(earlier, later));
        when(courseRepository.findById(firstId)).thenReturn(Optional.of(buildCourse(firstId, "A", "Science", CourseStatus.PUBLISHED)));
        when(courseRepository.findById(secondId)).thenReturn(Optional.of(buildCourse(secondId, "B", "Math", CourseStatus.PUBLISHED)));

        PaginatedResponse<ClassroomCourseDetailsResponse> result =
                classroomCourseService.getClassroomCourses(classroomId, -1, 0, " ", " ", "invalid", " ");

        assertThat(result.getPage()).isZero();
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getContent())
                .extracting(ClassroomCourseDetailsResponse::getCourseId)
                .containsExactly(firstId, secondId);
    }

    @Test
    void getClassroomCourses_withNullPageAndSize_usesDefaults() {
        UUID singleCourseId = UUID.randomUUID();
        ClassroomCourse cc = buildCc(singleCourseId);

        when(classroomRepository.existsById(classroomId)).thenReturn(true);
        when(classroomCourseRepository.findAllByClassroomIdOrderByAssignedAtAsc(classroomId))
                .thenReturn(List.of(cc));
        when(courseRepository.findById(singleCourseId))
                .thenReturn(Optional.of(buildCourse(singleCourseId, "Math", null, CourseStatus.PUBLISHED)));

        PaginatedResponse<ClassroomCourseDetailsResponse> result =
                classroomCourseService.getClassroomCourses(classroomId, null, null, null, null, null, null);

        assertThat(result.getPage()).isZero();
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getClassroomCourses_ignoresCourse_whenCourseNotFoundDuringMapping() {
        ClassroomCourse cc = new ClassroomCourse();
        cc.setClassroomId(classroomId);
        cc.setCourseId(courseId);
        cc.setAssignedAt(LocalDateTime.now());

        when(classroomRepository.existsById(classroomId)).thenReturn(true);
        when(classroomCourseRepository.findAllByClassroomIdOrderByAssignedAtAsc(classroomId))
                .thenReturn(List.of(cc));
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        PaginatedResponse<ClassroomCourseDetailsResponse> result =
                classroomCourseService.getClassroomCourses(classroomId, 0, 10, null, null, null, null);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void assignCourses_shouldAutoEnrollExistingStudents_whenCourseAssigned() {
        UUID studentId = UUID.randomUUID();

        AssignCoursesToClassroomRequest request = new AssignCoursesToClassroomRequest();
        request.setCourseIds(List.of(courseId));

        org.elearning.backend.classroom.entity.ClassroomMembership membership =
                new org.elearning.backend.classroom.entity.ClassroomMembership();
        User student = new User();
        student.setId(studentId);
        membership.setUser(student);

        ClassroomCourse saved = new ClassroomCourse();
        saved.setClassroomId(classroomId);
        saved.setCourseId(courseId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        when(classroomRepository.findByIdAndOrganizationId(classroomId, orgId)).thenReturn(Optional.of(classroom));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(userRepository.findById(courseCreator.getId())).thenReturn(Optional.of(courseCreator));
        when(classroomCourseRepository.existsByClassroomIdAndCourseId(classroomId, courseId)).thenReturn(false);
        when(classroomCourseRepository.saveAll(any())).thenReturn(List.of(saved));
        when(classroomMembershipRepository.findAllByClassroomIdAndMembershipType(
                classroomId, org.elearning.backend.classroom.entity.MembershipType.STUDENT))
                .thenReturn(List.of(membership));
        when(courseEnrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId))
                .thenReturn(false);

        classroomCourseService.assignCourses(classroomId, request, userId);

        verify(courseEnrollmentRepository).save(any(org.elearning.backend.enrollment.model.CourseEnrollment.class));
    }

    @Test
    void assignCourses_shouldNotEnrollStudent_whenAlreadyEnrolled() {
        UUID studentId = UUID.randomUUID();

        AssignCoursesToClassroomRequest request = new AssignCoursesToClassroomRequest();
        request.setCourseIds(List.of(courseId));

        org.elearning.backend.classroom.entity.ClassroomMembership membership =
                new org.elearning.backend.classroom.entity.ClassroomMembership();
        User student = new User();
        student.setId(studentId);
        membership.setUser(student);

        ClassroomCourse saved = new ClassroomCourse();
        saved.setClassroomId(classroomId);
        saved.setCourseId(courseId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        when(classroomRepository.findByIdAndOrganizationId(classroomId, orgId)).thenReturn(Optional.of(classroom));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(userRepository.findById(courseCreator.getId())).thenReturn(Optional.of(courseCreator));
        when(classroomCourseRepository.existsByClassroomIdAndCourseId(classroomId, courseId)).thenReturn(false);
        when(classroomCourseRepository.saveAll(any())).thenReturn(List.of(saved));
        when(classroomMembershipRepository.findAllByClassroomIdAndMembershipType(
                classroomId, org.elearning.backend.classroom.entity.MembershipType.STUDENT))
                .thenReturn(List.of(membership));
        when(courseEnrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId))
                .thenReturn(true);

        classroomCourseService.assignCourses(classroomId, request, userId);

        verify(courseEnrollmentRepository, never()).save(any());
    }

    @Test
    void assignCourses_shouldNotEnrollAnyone_whenNoStudentsInClassroom() {
        AssignCoursesToClassroomRequest request = new AssignCoursesToClassroomRequest();
        request.setCourseIds(List.of(courseId));

        ClassroomCourse saved = new ClassroomCourse();
        saved.setClassroomId(classroomId);
        saved.setCourseId(courseId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        when(classroomRepository.findByIdAndOrganizationId(classroomId, orgId)).thenReturn(Optional.of(classroom));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(userRepository.findById(courseCreator.getId())).thenReturn(Optional.of(courseCreator));
        when(classroomCourseRepository.existsByClassroomIdAndCourseId(classroomId, courseId)).thenReturn(false);
        when(classroomCourseRepository.saveAll(any())).thenReturn(List.of(saved));
        when(classroomMembershipRepository.findAllByClassroomIdAndMembershipType(
                classroomId, org.elearning.backend.classroom.entity.MembershipType.STUDENT))
                .thenReturn(List.of());

        classroomCourseService.assignCourses(classroomId, request, userId);

        verify(courseEnrollmentRepository, never()).save(any());
    }

    @Test
    void assignCourses_shouldSaveEnrollmentWithExactStudentAndCourseIds() {
        UUID studentId = UUID.randomUUID();

        AssignCoursesToClassroomRequest request = new AssignCoursesToClassroomRequest();
        request.setCourseIds(List.of(courseId));

        org.elearning.backend.classroom.entity.ClassroomMembership membership =
                new org.elearning.backend.classroom.entity.ClassroomMembership();
        User student = new User();
        student.setId(studentId);
        membership.setUser(student);

        ClassroomCourse saved = new ClassroomCourse();
        saved.setClassroomId(classroomId);
        saved.setCourseId(courseId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        when(classroomRepository.findByIdAndOrganizationId(classroomId, orgId)).thenReturn(Optional.of(classroom));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(userRepository.findById(courseCreator.getId())).thenReturn(Optional.of(courseCreator));
        when(classroomCourseRepository.existsByClassroomIdAndCourseId(classroomId, courseId)).thenReturn(false);
        when(classroomCourseRepository.saveAll(any())).thenReturn(List.of(saved));
        when(classroomMembershipRepository.findAllByClassroomIdAndMembershipType(
                classroomId, org.elearning.backend.classroom.entity.MembershipType.STUDENT))
                .thenReturn(List.of(membership));
        when(courseEnrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)).thenReturn(false);

        classroomCourseService.assignCourses(classroomId, request, userId);

        ArgumentCaptor<CourseEnrollment> captor = ArgumentCaptor.forClass(CourseEnrollment.class);
        verify(courseEnrollmentRepository).save(captor.capture());

        CourseEnrollment enrollment = captor.getValue();
        assertThat(enrollment.getStudentId()).isEqualTo(studentId);
        assertThat(enrollment.getCourseId()).isEqualTo(courseId);
    }

    @Test
    void assignCourses_shouldSaveOneEnrollmentPerStudentCoursePair() {
        UUID firstStudentId = UUID.randomUUID();
        UUID secondStudentId = UUID.randomUUID();
        UUID firstCourseId = UUID.randomUUID();
        UUID secondCourseId = UUID.randomUUID();

        Course secondCourse = new Course();
        secondCourse.setId(secondCourseId);
        secondCourse.setCreatedBy(courseCreator.getId());
        secondCourse.setStatus(CourseStatus.PUBLISHED);
        secondCourse.setVisibility(CourseVisibility.PUBLIC);

        AssignCoursesToClassroomRequest request = new AssignCoursesToClassroomRequest();
        request.setCourseIds(List.of(firstCourseId, secondCourseId));

        org.elearning.backend.classroom.entity.ClassroomMembership firstMembership =
                new org.elearning.backend.classroom.entity.ClassroomMembership();
        User firstStudent = new User();
        firstStudent.setId(firstStudentId);
        firstMembership.setUser(firstStudent);

        org.elearning.backend.classroom.entity.ClassroomMembership secondMembership =
                new org.elearning.backend.classroom.entity.ClassroomMembership();
        User secondStudent = new User();
        secondStudent.setId(secondStudentId);
        secondMembership.setUser(secondStudent);

        ClassroomCourse firstSaved = new ClassroomCourse();
        firstSaved.setClassroomId(classroomId);
        firstSaved.setCourseId(firstCourseId);

        ClassroomCourse secondSaved = new ClassroomCourse();
        secondSaved.setClassroomId(classroomId);
        secondSaved.setCourseId(secondCourseId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        when(classroomRepository.findByIdAndOrganizationId(classroomId, orgId)).thenReturn(Optional.of(classroom));
        when(courseRepository.findById(firstCourseId)).thenReturn(Optional.of(course));
        when(courseRepository.findById(secondCourseId)).thenReturn(Optional.of(secondCourse));
        when(userRepository.findById(courseCreator.getId())).thenReturn(Optional.of(courseCreator));
        when(classroomCourseRepository.existsByClassroomIdAndCourseId(classroomId, firstCourseId)).thenReturn(false);
        when(classroomCourseRepository.existsByClassroomIdAndCourseId(classroomId, secondCourseId)).thenReturn(false);
        when(classroomCourseRepository.saveAll(any())).thenReturn(List.of(firstSaved, secondSaved));
        when(classroomMembershipRepository.findAllByClassroomIdAndMembershipType(
                classroomId, org.elearning.backend.classroom.entity.MembershipType.STUDENT))
                .thenReturn(List.of(firstMembership, secondMembership));
        when(courseEnrollmentRepository.existsByStudentIdAndCourseId(firstStudentId, firstCourseId)).thenReturn(false);
        when(courseEnrollmentRepository.existsByStudentIdAndCourseId(firstStudentId, secondCourseId)).thenReturn(false);
        when(courseEnrollmentRepository.existsByStudentIdAndCourseId(secondStudentId, firstCourseId)).thenReturn(false);
        when(courseEnrollmentRepository.existsByStudentIdAndCourseId(secondStudentId, secondCourseId)).thenReturn(false);

        classroomCourseService.assignCourses(classroomId, request, userId);

        ArgumentCaptor<CourseEnrollment> captor = ArgumentCaptor.forClass(CourseEnrollment.class);
        verify(courseEnrollmentRepository, times(4)).save(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(CourseEnrollment::getStudentId, CourseEnrollment::getCourseId)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(firstStudentId, firstCourseId),
                        org.assertj.core.groups.Tuple.tuple(firstStudentId, secondCourseId),
                        org.assertj.core.groups.Tuple.tuple(secondStudentId, firstCourseId),
                        org.assertj.core.groups.Tuple.tuple(secondStudentId, secondCourseId)
                );
    }

    private ClassroomCourse buildCc(UUID courseId) {
        ClassroomCourse cc = new ClassroomCourse();
        cc.setClassroomId(classroomId);
        cc.setCourseId(courseId);
        cc.setAssignedAt(LocalDateTime.now());
        return cc;
    }

    private Course buildCourse(UUID id, String title, String category, CourseStatus status) {
        Course c = new Course();
        c.setId(id);
        c.setTitle(title);
        c.setCategory(category);
        c.setStatus(status);
        c.setCreatedBy(courseCreator.getId());
        return c;
    }
}
