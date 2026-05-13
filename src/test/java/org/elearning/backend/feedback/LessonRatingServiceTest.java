package org.elearning.backend.feedback;

import org.elearning.backend.content.model.Chapter;
import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.enrollment.repository.CourseEnrollmentRepository;
import org.elearning.backend.feedback.dto.RateLessonResponseDto;
import org.elearning.backend.feedback.repository.LessonRatingRepository;
import org.elearning.backend.feedback.service.LessonRatingService;
import org.elearning.backend.feedback.service.RatingAlertService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonRatingServiceTest {

    @Mock private LessonRepository lessonRepository;
    @Mock private CourseEnrollmentRepository courseEnrollmentRepository;
    @Mock private LessonRatingRepository lessonRatingRepository;
    @Mock private RatingAlertService ratingAlertService;

    @InjectMocks
    private LessonRatingService lessonRatingService;

    @Test
    void rateLesson_shouldReturnResponseWhenAlertCheckFails() {
        UUID lessonId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        Course course = new Course();
        course.setId(courseId);
        Chapter chapter = new Chapter();
        chapter.setCourse(course);
        Lesson lesson = new Lesson();
        lesson.setId(lessonId);
        lesson.setTitle("Lesson 1");
        lesson.setChapter(chapter);

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(courseEnrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)).thenReturn(true);
        when(lessonRatingRepository.findAverageRatingByLessonId(lessonId)).thenReturn(4.2);
        when(lessonRatingRepository.countRatingsByLessonId(lessonId)).thenReturn(8);
        doThrow(new RuntimeException("alert failure")).when(ratingAlertService).checkLessonRating(lessonId);

        RateLessonResponseDto result = lessonRatingService.rateLesson(lessonId, studentId, 5, "great");

        assertThat(result.getLessonId()).isEqualTo(lessonId);
        assertThat(result.getAvgRating()).isEqualTo(4.2);
        assertThat(result.getTotalRatings()).isEqualTo(8);
    }
}
