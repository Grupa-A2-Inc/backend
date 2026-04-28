package org.elearning.backend.classroom.service;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.classroom.dto.request.AssignCoursesToClassroomRequest;
import org.elearning.backend.classroom.dto.response.ClassroomCourseDetailsResponse;
import org.elearning.backend.classroom.dto.response.ClassroomCourseResponse;
import org.elearning.backend.classroom.entity.Classroom;
import org.elearning.backend.classroom.entity.ClassroomCourse;
import org.elearning.backend.classroom.exception.ClassroomBadRequestException;
import org.elearning.backend.classroom.exception.ClassroomNotFoundException;
import org.elearning.backend.classroom.exception.CourseNotEligibleException;
import org.elearning.backend.classroom.repository.ClassroomCourseRepository;
import org.elearning.backend.classroom.repository.ClassroomRepository;
import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.repository.CourseRepository;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.exception.UserNotFoundException;
import org.elearning.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClassroomCourseService {
    private final ClassroomRepository classroomRepository;
    private final ClassroomCourseRepository classroomCourseRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

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

        return saved.stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ClassroomCourseDetailsResponse> getClassroomCourses(UUID classroomId) {
        if (!classroomRepository.existsById(classroomId)) {
            throw new ClassroomNotFoundException("Classroom not found: " + classroomId);
        }

        return classroomCourseRepository.findAllByClassroomIdOrderByAssignedAtAsc(classroomId)
                .stream()
                .map(cc -> {
                    Course course = courseRepository.findById(cc.getCourseId())
                            .orElseThrow(() -> new ClassroomBadRequestException(
                                    "Course not found: " + cc.getCourseId()));
                    return toCourseDetailsResponse(cc, course);
                })
                .toList();
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