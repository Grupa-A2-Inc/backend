package org.elearning.backend.feedback;

import org.elearning.backend.auth.service.EmailService;
import org.elearning.backend.content.model.CourseVisibility;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.feedback.dto.LessonRatingStatsDto;
import org.elearning.backend.feedback.dto.LessonVisibilityAndOwnerDto;
import org.elearning.backend.feedback.repository.LessonRatingRepository;
import org.elearning.backend.feedback.service.RatingAlertService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class RatingAlertServiceTest {

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private LessonRatingRepository lessonRatingRepository;

    @MockitoBean
    private LessonRepository lessonRepository;

    @Autowired
    private RatingAlertService ratingAlertService;

    @Test
    void shouldLogWarningWhenAvgUnder3AndCountAtLeast5(CapturedOutput output) {
        UUID lessonId = UUID.randomUUID();

        LessonRatingStatsDto stats = mock(LessonRatingStatsDto.class);
        when(stats.getCount()).thenReturn(5L);
        when(stats.getRating()).thenReturn(2.5);

        LessonVisibilityAndOwnerDto visibilityDto = mock(LessonVisibilityAndOwnerDto.class);
        when(visibilityDto.getTitle()).thenReturn("Test Lesson");
        when(visibilityDto.getCourseVisibility()).thenReturn(CourseVisibility.PUBLIC);

        when(lessonRatingRepository.getAverageAndCountByLessonId(lessonId)).thenReturn(stats);
        when(lessonRepository.getLessonVisibilityAndOwner(lessonId)).thenReturn(visibilityDto);

        ratingAlertService.checkLessonRating(lessonId);

        assertThat(output.getOut()).contains("WARN");
        assertThat(output.getOut()).contains("Lectia publica 'Test Lesson'");
        assertThat(output.getOut()).contains(lessonId.toString());
        assertThat(output.getOut()).contains("2.5");
    }

    @Test
    void shouldSkipLoggingWhenCountUnder3(CapturedOutput output) {
        UUID lessonId = UUID.randomUUID();

        LessonRatingStatsDto stats = mock(LessonRatingStatsDto.class);
        when(stats.getCount()).thenReturn(2L);

        when(stats.getRating()).thenReturn(1.5);

        when(lessonRatingRepository.getAverageAndCountByLessonId(lessonId)).thenReturn(stats);

        ratingAlertService.checkLessonRating(lessonId);

        assertThat(output.getOut()).doesNotContain("[RATING ALERT]");
    }

    @Test
    void shouldLogPrivateLessonWarningWhenAverageIsLow(CapturedOutput output) {
        UUID lessonId = UUID.randomUUID();

        LessonRatingStatsDto stats = new LessonRatingStatsDto(2.2, 3L);
        LessonVisibilityAndOwnerDto visibilityDto = new LessonVisibilityAndOwnerDto(
                CourseVisibility.PRIVATE,
                UUID.randomUUID(),
                "Private Lesson"
        );

        when(lessonRatingRepository.getAverageAndCountByLessonId(lessonId)).thenReturn(stats);
        when(lessonRepository.getLessonVisibilityAndOwner(lessonId)).thenReturn(visibilityDto);

        ratingAlertService.checkLessonRating(lessonId);

        assertThat(output.getOut()).contains("Private Lesson");
        assertThat(output.getOut()).contains("Profesor:");
    }

    @Test
    void shouldNotQueryLessonWhenRatingIsNotLow() {
        UUID lessonId = UUID.randomUUID();
        when(lessonRatingRepository.getAverageAndCountByLessonId(lessonId))
                .thenReturn(new LessonRatingStatsDto(4.5, 10L));

        ratingAlertService.checkLessonRating(lessonId);

        verify(lessonRepository, never()).getLessonVisibilityAndOwner(lessonId);
    }

    @Test
    void shouldCatchRuntimeException(CapturedOutput output) {
        UUID lessonId = UUID.randomUUID();
        when(lessonRatingRepository.getAverageAndCountByLessonId(lessonId)).thenThrow(new RuntimeException("Database timeout"));

        assertDoesNotThrow(() -> ratingAlertService.checkLessonRating(lessonId));

        assertThat(output.getOut()).contains("ERROR");
        assertThat(output.getOut()).contains("Eroare la verificarea ratingului");
    }

    @Test
    void shouldReturnImmediatelyWhenStatsAreNull() {
        UUID lessonId = UUID.randomUUID();
        when(lessonRatingRepository.getAverageAndCountByLessonId(lessonId)).thenReturn(null);

        ratingAlertService.checkLessonRating(lessonId);

        verify(lessonRepository, never()).getLessonVisibilityAndOwner(lessonId);
    }

    @Test
    void shouldReturnImmediatelyWhenCountIsNull() {
        UUID lessonId = UUID.randomUUID();
        LessonRatingStatsDto stats = mock(LessonRatingStatsDto.class);
        when(stats.getCount()).thenReturn(null);
        when(lessonRatingRepository.getAverageAndCountByLessonId(lessonId)).thenReturn(stats);

        ratingAlertService.checkLessonRating(lessonId);

        verify(lessonRepository, never()).getLessonVisibilityAndOwner(lessonId);
    }

    @Test
    void shouldNotQueryLessonWhenRatingIsNull() {
        UUID lessonId = UUID.randomUUID();
        LessonRatingStatsDto stats = mock(LessonRatingStatsDto.class);
        when(stats.getCount()).thenReturn(5L);
        when(stats.getRating()).thenReturn(null);
        when(lessonRatingRepository.getAverageAndCountByLessonId(lessonId)).thenReturn(stats);

        ratingAlertService.checkLessonRating(lessonId);

        verify(lessonRepository, never()).getLessonVisibilityAndOwner(lessonId);
    }
}
