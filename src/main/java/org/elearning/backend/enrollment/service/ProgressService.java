package org.elearning.backend.enrollment.service;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.content.repository.CourseRepository;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.enrollment.dto.*;
import org.elearning.backend.enrollment.exception.CourseNotFoundException;
import org.elearning.backend.enrollment.mapper.ProgressMapper;
import org.elearning.backend.enrollment.model.CourseEnrollment;
import org.elearning.backend.enrollment.model.LessonProgress;
import org.elearning.backend.enrollment.repository.CourseEnrollmentRepository;
import org.elearning.backend.enrollment.repository.LessonProgressRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class ProgressService {
    private final CourseEnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final ProgressMapper progressMapper;
    private final ProgressCalculatorService progressCalculatorService;
    private final CourseRepository courseRepository;

    @Transactional
    public ProgressWithLessonListDto getMyCourseProgress(UUID courseId, UUID studentId) {
        CourseEnrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        List<Lesson> courseLessons = lessonRepository.findAllLessonIdsByCourseId(courseId)
                .stream()
                .map(lessonRepository::getReferenceById)
                .toList();
        List<LessonProgress> studentProgress = lessonProgressRepository.findByStudentIdAndCourseEnrollmentId(studentId, enrollment.getId());
        Map<UUID, LessonProgress> progressMap = studentProgress.stream().collect(Collectors.toMap(LessonProgress::getLessonId, p -> p));
        List<LessonStatusDto> lessonsList = courseLessons.stream()
                .map(lesson -> {
                    LessonProgress progressForThisLesson = progressMap.get(lesson.getId());
                    return progressMapper.toLessonStatusDto(lesson, progressForThisLesson);
                })
                .toList();

        ProgressDto calculation = progressCalculatorService.calculateCourseProgress(courseId, studentId, enrollment.getId());

        return new ProgressWithLessonListDto(
                calculation.getTotalLessons(),
                calculation.getCompletedLessons(),
                calculation.getPercentageDisplay(),
                enrollment.getCompletedAt(),
                lessonsList
        );
    }

    public Page<StudentProgressDto> getCourseProgressForProfessor(UUID courseId, UUID professorId, Pageable pageable) {
        var course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        if (!course.getCreatedBy().equals(professorId)) {
            throw new AccessDeniedException("You are not the owner of this course.");
        }

        Page<CourseEnrollment> enrollments = enrollmentRepository.findAllByCourseId(courseId, pageable);

        return enrollments.map(enrollment -> StudentProgressDto.builder()
                .studentId(enrollment.getStudentId())
                .enrolledAt(enrollment.getEnrolledAt())
                .completedAt(enrollment.getCompletedAt())
                .progressPercent(progressCalculatorService.calculateProgressPercent(enrollment.getId()))
                .build());
    }

    public List<EnrolledCourseDto> getStudentCoursesProgress(UUID requestedStudentId) {

        List<CourseEnrollment> enrollments = enrollmentRepository.findAllByStudentId(requestedStudentId);

        return enrollments.stream().map(enrollment -> {

            var course = courseRepository.findById(enrollment.getCourseId())
                    .orElseThrow(() -> new RuntimeException("Course was not found."));
            EnrolledCourseDto dto = new EnrolledCourseDto();
            dto.setUnrollmentId(enrollment.getId());
            dto.setCourseId(course.getId());
            dto.setCourseTitle(course.getTitle());
            dto.setCourseCategory(course.getCategory());
            dto.setEnrolledAt(enrollment.getEnrolledAt());
            dto.setCompletedAt(enrollment.getCompletedAt());
            double calculatedProgress = progressCalculatorService.calculateProgressPercent(enrollment.getId());
            dto.setProgressPercent(java.math.BigDecimal.valueOf(calculatedProgress));

            return dto;

        }).toList();
    }
}
