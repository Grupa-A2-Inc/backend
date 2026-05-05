package org.elearning.backend.classroom.service;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.classroom.dto.request.AssignCoursesToClassroomRequest;
import org.elearning.backend.classroom.dto.response.ClassroomCourseDetailsResponse;
import org.elearning.backend.classroom.dto.response.ClassroomCourseResponse;
import org.elearning.backend.classroom.entity.Classroom;
import org.elearning.backend.classroom.entity.ClassroomCourse;
import org.elearning.backend.classroom.entity.ClassroomMembership;
import org.elearning.backend.classroom.entity.MembershipType;
import org.elearning.backend.classroom.exception.ClassroomBadRequestException;
import org.elearning.backend.classroom.exception.ClassroomNotFoundException;
import org.elearning.backend.classroom.exception.CourseNotEligibleException;
import org.elearning.backend.classroom.repository.ClassroomCourseRepository;
import org.elearning.backend.classroom.repository.ClassroomMembershipRepository;
import org.elearning.backend.classroom.repository.ClassroomRepository;
import org.elearning.backend.common.dto.response.PaginatedResponse;
import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.model.CourseStatus;
import org.elearning.backend.content.repository.CourseRepository;
import org.elearning.backend.enrollment.model.CourseEnrollment;
import org.elearning.backend.enrollment.repository.CourseEnrollmentRepository;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.exception.UserNotFoundException;
import org.elearning.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ClassroomCourseService {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final String DEFAULT_SORT_FIELD = "assignedAt";
    private static final String TITLE_SORT_FIELD = "title";
    private static final String DESC_SORT_DIRECTION = "desc";
    private static final String ASC_SORT_DIRECTION = "asc";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(TITLE_SORT_FIELD, DEFAULT_SORT_FIELD);

    private final ClassroomRepository classroomRepository;
    private final ClassroomCourseRepository classroomCourseRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final ClassroomMembershipRepository classroomMembershipRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;

    @Transactional
    public List<ClassroomCourseResponse> assignCourses(UUID classroomId, AssignCoursesToClassroomRequest request, UUID requesterUserId) {
        User requester = userRepository.findById(requesterUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + requesterUserId));

        if (requester.getOrganization() == null) {
            throw new ClassroomBadRequestException("Authenticated user is not assigned to an organization");
        }

        UUID organizationId = requester.getOrganization().getId();

        Classroom classroom = classroomRepository.findByIdAndOrganizationId(classroomId, organizationId)
                .orElseThrow(() -> new ClassroomNotFoundException("Classroom not found: " + classroomId));

        List<ClassroomCourse> toSave = new ArrayList<>();

        for (UUID courseId : request.getCourseIds()) {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new ClassroomBadRequestException("Course not found: " + courseId));

            validateCourseEligibility(course, organizationId);

            if (classroomCourseRepository.existsByClassroomIdAndCourseId(classroomId, courseId)) {
                continue;
            }

            ClassroomCourse association = new ClassroomCourse();
            association.setClassroomId(classroom.getId());
            association.setCourseId(courseId);
            toSave.add(association);
        }

        List<ClassroomCourse> saved = classroomCourseRepository.saveAll(toSave);

        List<ClassroomMembership> studentMemberships = classroomMembershipRepository
                .findAllByClassroomIdAndMembershipType(classroomId, MembershipType.STUDENT);

        for (ClassroomCourse classroomCourse : saved) {
            for (ClassroomMembership membership : studentMemberships) {
                UUID studentId = membership.getUser().getId();
                UUID courseId = classroomCourse.getCourseId();

                if (!courseEnrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
                    CourseEnrollment enrollment = new CourseEnrollment();
                    enrollment.setCourseId(courseId);
                    enrollment.setStudentId(studentId);
                    courseEnrollmentRepository.save(enrollment);
                }
            }
        }

        return saved.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<ClassroomCourseDetailsResponse> getClassroomCourses(
            UUID classroomId,
            Integer page, Integer size,
            String search, String category,
            String sortBy, String sortDir) {

        if (!classroomRepository.existsById(classroomId)) {
            throw new ClassroomNotFoundException("Classroom not found: " + classroomId);
        }

        int pageVal = normalizePage(page);
        int sizeVal = normalizeSize(size);
        String field = normalizeSortField(sortBy);
        String dir = normalizeSortDirection(sortDir);

        List<ClassroomCourse> all = classroomCourseRepository.findAllByClassroomIdOrderByAssignedAtAsc(classroomId);

        List<ClassroomCourseDetailsResponse> filtered = all.stream()
                .map(this::toPublishedCourseEntry)
                .filter(Objects::nonNull)
                .filter(entry -> matchesSearch(entry, search))
                .filter(entry -> matchesCategory(entry, category))
                .sorted(buildCourseComparator(field, dir))
                .map(entry -> toCourseDetailsResponse(entry.getKey(), entry.getValue()))
                .toList();

        int total = filtered.size();
        int fromIndex = Math.min(pageVal * sizeVal, total);
        int toIndex   = Math.min(fromIndex + sizeVal, total);
        List<ClassroomCourseDetailsResponse> content = filtered.subList(fromIndex, toIndex);

        return new PaginatedResponse<>(content, pageVal, sizeVal, (long) total);
    }

    private Comparator<AbstractMap.SimpleEntry<ClassroomCourse, Course>> buildCourseComparator(String field, String dir) {
        Comparator<AbstractMap.SimpleEntry<ClassroomCourse, Course>> comp = field.equals(TITLE_SORT_FIELD)
                ? Comparator.comparing(e -> e.getValue().getTitle(), String.CASE_INSENSITIVE_ORDER)
                : Comparator.comparing(e -> e.getKey().getAssignedAt());

        return dir.equals(DESC_SORT_DIRECTION) ? comp.reversed() : comp;
    }

    private int normalizePage(Integer page) {
        return page == null || page < 0 ? DEFAULT_PAGE : page;
    }

    private int normalizeSize(Integer size) {
        return size == null || size <= 0 ? DEFAULT_PAGE_SIZE : size;
    }

    private String normalizeSortField(String sortBy) {
        return sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : DEFAULT_SORT_FIELD;
    }

    private String normalizeSortDirection(String sortDir) {
        return sortDir == null || sortDir.isBlank() ? ASC_SORT_DIRECTION : sortDir.toLowerCase(Locale.ROOT);
    }

    private AbstractMap.SimpleEntry<ClassroomCourse, Course> toPublishedCourseEntry(ClassroomCourse classroomCourse) {
        Course course = courseRepository.findById(classroomCourse.getCourseId()).orElse(null);
        if (course == null || course.getStatus() != CourseStatus.PUBLISHED) {
            return null;
        }

        return new AbstractMap.SimpleEntry<>(classroomCourse, course);
    }

    private boolean matchesSearch(AbstractMap.SimpleEntry<ClassroomCourse, Course> entry, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }

        String query = search.toLowerCase(Locale.ROOT).trim();
        return entry.getValue().getTitle().toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean matchesCategory(AbstractMap.SimpleEntry<ClassroomCourse, Course> entry, String category) {
        if (category == null || category.isBlank()) {
            return true;
        }

        return category.equalsIgnoreCase(entry.getValue().getCategory());
    }

    private void validateCourseEligibility(Course course, UUID organizationId) {
        if (course.getCreatedBy() == null) {
            throw new CourseNotEligibleException(
                    "Course " + course.getId() + " has no creator and cannot be assigned to a classroom"
            );
        }

        User creator = userRepository.findById(course.getCreatedBy())
                .orElseThrow(() -> new CourseNotEligibleException(
                        "Creator of course " + course.getId() + " not found"
                ));

        if (creator.getOrganization() == null
                || !organizationId.equals(creator.getOrganization().getId())) {
            throw new CourseNotEligibleException(
                    "Course " + course.getId() + " does not belong to the classroom's organization"
            );
        }
    }

    private ClassroomCourseResponse toResponse(ClassroomCourse cc) {
        ClassroomCourseResponse response = new ClassroomCourseResponse();
        response.setId(cc.getId());
        response.setClassroomId(cc.getClassroomId());
        response.setCourseId(cc.getCourseId());
        response.setAssignedAt(cc.getAssignedAt());
        return response;
    }

    private ClassroomCourseDetailsResponse toCourseDetailsResponse(ClassroomCourse cc, Course course) {
        ClassroomCourseDetailsResponse response = new ClassroomCourseDetailsResponse();
        response.setCourseId(course.getId());
        response.setTitle(course.getTitle());
        response.setDescription(course.getDescription());
        response.setCategory(course.getCategory());
        response.setStatus(course.getStatus());
        response.setVisibility(course.getVisibility());
        response.setCreatedBy(course.getCreatedBy());
        response.setAssignedAt(cc.getAssignedAt());
        return response;
    }
}
