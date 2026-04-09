package org.elearning.backend.enrollment.repository;

import org.elearning.backend.enrollment.model.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, UUID> {

    @Query("SELECT lp.lessonId FROM LessonProgress lp WHERE lp.courseEnrollment.id = :enrollmentId")
    Set<UUID> findVisitedLessonIds(@Param("enrollmentId") UUID enrollmentId);
}