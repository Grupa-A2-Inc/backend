package org.elearning.backend.enrollment.service;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.enrollment.dto.ProgressDto;
import org.elearning.backend.enrollment.dto.ProgressWithLessonListDto;
import org.elearning.backend.enrollment.dto.LessonStatusDto;
import org.elearning.backend.enrollment.exception.CourseNotFoundException;
import org.elearning.backend.enrollment.mapper.ProgressMapper;
import org.elearning.backend.enrollment.model.CourseEnrollment;
import org.elearning.backend.enrollment.model.LessonProgress;
import org.elearning.backend.enrollment.repository.CourseEnrollmentRepository;
import org.elearning.backend.enrollment.repository.LessonProgressRepository;
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
}
