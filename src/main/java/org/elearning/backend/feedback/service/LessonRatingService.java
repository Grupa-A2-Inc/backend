package org.elearning.backend.feedback.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.enrollment.repository.CourseEnrollmentRepository;
import org.elearning.backend.feedback.dto.CommentDto;
import org.elearning.backend.feedback.dto.LessonRatingSummaryDto;
import org.elearning.backend.feedback.dto.RateLessonResponseDto;
import org.elearning.backend.feedback.exception.DoesNotOwnTheCourseException;
import org.elearning.backend.feedback.exception.EnrolledInCourseException;
import org.elearning.backend.feedback.model.LessonRating;
import org.elearning.backend.feedback.repository.LessonRatingRepository;
import org.elearning.backend.role.entity.RoleName;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonRatingService {
    private final LessonRepository lessonRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final LessonRatingRepository lessonRatingRepository;
    private final RatingAlertService ratingAlertService;

    private static final double THRESHOLD = 3.0;

    @Transactional
    public RateLessonResponseDto rateLesson(UUID lessonId, UUID studentId, int rating, String comment) {
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow(
                () -> new DoesNotExistException("Lesson not found with id: " + lessonId)
        );

        if(!courseEnrollmentRepository.existsByStudentIdAndCourseId(studentId, lesson.getChapter().getCourse().getId())) {
            throw new EnrolledInCourseException("Student with id " + studentId + " is not enrolled in the course for lesson with id: " + lessonId);
        }

        lessonRatingRepository.saveOrUpdate(lessonId, studentId, rating, comment);

        double averageRating = lessonRatingRepository.findAverageRatingByLessonId(lessonId);
        int totalRatings = lessonRatingRepository.countRatingsByLessonId(lessonId);

        try {
            ratingAlertService.checkLessonRating(lessonId);
        }
        catch(Exception exception) {
            log.warn("Error while checking lesson rating for lesson with id {}: {}", lessonId, exception.getMessage());
        }

        return new RateLessonResponseDto(
                lessonId,
                lesson.getTitle(),
                rating,
                comment,
                averageRating,
                totalRatings
        );
    }

    public LessonRatingSummaryDto getLessonSummary(UUID lessonId, UUID userId, RoleName userRole) {
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow(
                () -> new DoesNotExistException("Lesson not found with id: " + lessonId)
        );

        Double avgRating = lessonRatingRepository.findAverageRatingByLessonId(lessonId);
        int totalRatings = lessonRatingRepository.countRatingsByLessonId(lessonId);

        LessonRatingSummaryDto lessonRatingSummaryDto = new LessonRatingSummaryDto();
        lessonRatingSummaryDto.setLessonId(lessonId);
        lessonRatingSummaryDto.setLessonTitle(lesson.getTitle());
        lessonRatingSummaryDto.setAvgRating(avgRating != null ? Math.round(avgRating * 100.0) / 100.0 : 0.0);
        lessonRatingSummaryDto.setTotalRatings(totalRatings);

        // For professor
        if (RoleName.TEACHER.equals(userRole)) {
            if(!lesson.getChapter().getCourse().getCreatedBy().equals(userId)) {
                throw new DoesNotOwnTheCourseException("User with id " + userId + " does not own the course for lesson with id: " + lessonId);
            }

            boolean isBelowThreshold = avgRating != null && avgRating < THRESHOLD;

            List<Object[]> distResults = lessonRatingRepository.getRatingDistribution(lessonId);
            Map<Integer, Long> distribution = new HashMap<>();
            for (int i = 1; i <= 5; i++) {
                distribution.put(i, 0L);
            }

            for (Object[] row : distResults) {
                distribution.put(((Short) row[0]).intValue(), (Long) row[1]);
            }

            List<LessonRating> top5Ratings = lessonRatingRepository
                    .findTop5ByLessonIdAndCommentIsNotNullAndCommentNotOrderByUpdatedAtDesc(lessonId, "");

            List<CommentDto> recentComments = top5Ratings.stream()
                    .map(r -> new CommentDto(r.getRating().intValue(), r.getComment(), r.getUpdatedAt()))
                    .toList();

            lessonRatingSummaryDto.setBelowThreshold(isBelowThreshold);
            lessonRatingSummaryDto.setDistribution(distribution);
            lessonRatingSummaryDto.setRecentComments(recentComments);
        }
        // For student
        else if (RoleName.STUDENT.equals(userRole)) {
            if (!courseEnrollmentRepository.existsByStudentIdAndCourseId(userId, lesson.getChapter().getCourse().getId())) {
                throw new EnrolledInCourseException(
                       "Student with id " + userId + " is not enrolled in the course for lesson with id: " + lessonId);
            }
            lessonRatingRepository.findByLessonIdAndStudentId(lessonId, userId)
                    .ifPresent(rating -> {
                        lessonRatingSummaryDto.setMyRating(rating.getRating().intValue());
                        lessonRatingSummaryDto.setMyComment(rating.getComment());
                    });
        }

        return lessonRatingSummaryDto;
    }
}
