package org.elearning.backend.feedback.repository;

import org.elearning.backend.feedback.dto.LessonRatingStatsDto;
import org.elearning.backend.feedback.model.LessonRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface LessonRatingRepository extends JpaRepository<LessonRating, UUID> {
    @Query("SELECT new org.elearning.backend.feedback.dto.LessonRatingStatsDto(AVG(r.rating), COUNT(r)) " +
            "FROM LessonRating r WHERE r.lessonId = :lessonId")
    LessonRatingStatsDto getAverageAndCountByLessonId(@Param("lessonId") UUID lessonId);

}
