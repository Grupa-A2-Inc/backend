package org.elearning.backend.feedback.repository;

import org.elearning.backend.feedback.dto.CommentDto;
import org.elearning.backend.feedback.dto.LessonRatingStatsDto;
import org.elearning.backend.feedback.model.LessonRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LessonRatingRepository extends JpaRepository<LessonRating, UUID> {
    @Query("SELECT new org.elearning.backend.feedback.dto.LessonRatingStatsDto(AVG(r.rating), COUNT(r)) " +
            "FROM LessonRating r WHERE r.lessonId = :lessonId")
    LessonRatingStatsDto getAverageAndCountByLessonId(@Param("lessonId") UUID lessonId);


    @Modifying
    @Query(value = """
            INSERT INTO lesson_ratings (lesson_id, student_id, rating, comment) 
            VALUES (:lessonId, :studentId, :rating, :comment) 
            ON CONFLICT (lesson_id, student_id) 
            DO UPDATE SET 
                rating = EXCLUDED.rating, 
                comment = EXCLUDED.comment, 
                updated_at = NOW()
            """,
            nativeQuery = true)
    void saveOrUpdate(
            @Param("lessonId") UUID lessonId,
            @Param("studentId") UUID studentId,
            @Param("rating") int rating,
            @Param("comment") String comment
    );

    @Query("SELECT AVG(rating) FROM LessonRating WHERE lessonId = :lessonId")
    Double findAverageRatingByLessonId(@Param("lessonId") UUID lessonId);

    @Query("SELECT COUNT(r) FROM LessonRating r WHERE r.lessonId = :lessonId")
    int countRatingsByLessonId(@Param("lessonId") UUID lessonId);

    Optional<LessonRating> findByLessonIdAndStudentId(UUID lessonId, UUID studentId);

    @Query("SELECT r.rating, COUNT(r) FROM LessonRating r WHERE r.lessonId = :lessonId GROUP BY r.rating")
    List<Object[]> getRatingDistribution(@Param("lessonId") UUID lessonId);

    List<LessonRating> findTop5ByLessonIdAndCommentIsNotNullAndCommentNotOrderByUpdatedAtDesc(UUID lessonId, String emptyString);
}
