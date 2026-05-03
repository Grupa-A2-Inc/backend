package org.elearning.backend.feedback.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elearning.backend.content.model.CourseVisibility;
import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.feedback.dto.LessonRatingStatsDto;
import org.elearning.backend.feedback.dto.LessonVisibilityAndOwnerDto;
import org.elearning.backend.feedback.repository.LessonRatingRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingAlertService {
    private final LessonRatingRepository lessonRatingRepository;
    private final LessonRepository lessonRepository;

    public void checkLessonRating(UUID lessonId) {
        try {
            LessonRatingStatsDto stats = lessonRatingRepository.getAverageAndCountByLessonId(lessonId);
            if (stats == null || stats.getCount() == null || stats.getCount() < 3) {
                return;
            }
            if (stats.getRating() != null && stats.getRating() < 3.0) {
                LessonVisibilityAndOwnerDto visibilityAndOwner = lessonRepository.getLessonVisibilityAndOwner(lessonId);
                if (visibilityAndOwner.getCourseVisibility() == CourseVisibility.PRIVATE) {
                    log.warn("[RATING ALERT] Lectia '{}' (id={}) are media {} stele. Profesor: {}.", visibilityAndOwner.getTitle(), lessonId, stats.getRating(), visibilityAndOwner.getCreatedBy());
                }
                else {
                    log.warn("[RATING ALERT] Lectia publica '{}' (id={}) are rating {}.", visibilityAndOwner.getTitle(), lessonId, stats.getRating());
                }
            }
        } catch (Exception e) {
            log.error("Eroare la verificarea ratingului pentru lectia cu id {}: {}", lessonId, e.getMessage());
        }
    }
}
