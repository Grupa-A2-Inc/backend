package org.elearning.backend.enrollment.service;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.enrollment.dto.ProgressDto;
import org.elearning.backend.enrollment.model.CourseEnrollment;
import org.elearning.backend.enrollment.repository.CourseEnrollmentRepository;
import org.elearning.backend.enrollment.repository.LessonProgressRepository;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.assessment.repository.TestRepository;
import org.elearning.backend.assessment.repository.TestResultRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProgressCalculatorService {

    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final TestRepository testRepository;
    private final TestResultRepository testResultRepository;
    private final CourseEnrollmentRepository enrollmentRepository;

    public ProgressDto calculateCourseProgress(UUID courseId, UUID studentId, UUID enrollmentId) {

        List<UUID> allLessonIds = lessonRepository.findAllLessonIdsByCourseId(courseId);
        int total = allLessonIds.size();

        if (total == 0) return ProgressDto.empty(courseId, studentId);

        Set<UUID> visited          = lessonProgressRepository.findVisitedLessonIds(enrollmentId);
        Set<UUID> lessonsWithTest  = testRepository.findLessonIdsWithPublishedTest(allLessonIds);
        Set<UUID> lessonTestPassed = testResultRepository.findPassedLessonIds(studentId, allLessonIds);

        long completed = allLessonIds.stream()
                .filter(lessonId -> {
                    boolean wasVisited = visited.contains(lessonId);
                    boolean hasTest    = lessonsWithTest.contains(lessonId);
                    boolean testPassed = lessonTestPassed.contains(lessonId);
                    return wasVisited && (!hasTest || testPassed);
                })
                .count();

        double percentage = (double) completed / total;

        return ProgressDto.builder()
                .courseId(courseId)
                .studentId(studentId)
                .totalLessons(total)
                .completedLessons((int) completed)
                .percentage(Math.round(percentage * 10000.0) / 10000.0)
                .percentageDisplay(Math.round(percentage * 10000.0) / 100.0)
                .isCompleted(completed == total)
                .build();
    }

    public double calculateProgressPercent(UUID enrollmentId) {
        CourseEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + enrollmentId));

        return calculateCourseProgress(
                enrollment.getCourseId(),
                enrollment.getStudentId(),
                enrollmentId
        ).getPercentageDisplay();
    }

    public boolean checkAndMarkCompletion(UUID enrollmentId) {
        CourseEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + enrollmentId));

        if (enrollment.getCompletedAt() != null) {
            return false;
        }

        if (calculateProgressPercent(enrollmentId) < 100.0) {
            return false;
        }

        enrollment.setCompletedAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);
        return true;
    }
}