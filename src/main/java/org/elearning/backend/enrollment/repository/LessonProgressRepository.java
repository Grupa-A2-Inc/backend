package org.elearning.backend.enrollment.repository;

import org.elearning.backend.enrollment.model.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, UUID> {

    @Query("SELECT lp.lessonId FROM LessonProgress lp WHERE lp.courseEnrollment.id = :enrollmentId")
    Set<UUID> findVisitedLessonIds(@Param("enrollmentId") UUID enrollmentId);

    List<LessonProgress> findByStudentIdAndCourseEnrollmentId(UUID studentId, UUID enrollmentId);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO lesson_progress (lesson_id, student_id, enrollment_id)
            VALUES (:lessonId, :studentId, :enrollmentId)
            ON CONFLICT (lesson_id, student_id) DO NOTHING
            """, nativeQuery = true)
    void insertProgressIdempotent(
            @Param("lessonId") UUID lessonId,
            @Param("studentId") UUID studentId,
            @Param("enrollmentId") UUID enrollmentId
    );
}