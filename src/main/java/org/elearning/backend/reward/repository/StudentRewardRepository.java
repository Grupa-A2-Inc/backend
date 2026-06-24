package org.elearning.backend.reward.repository;

import org.elearning.backend.reward.entity.StudentReward;
import org.elearning.backend.reward.entity.StudentRewardStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface StudentRewardRepository extends JpaRepository<StudentReward, UUID> {
    List<StudentReward> findAllByRewardCycleIdOrderByRankAsc(UUID rewardCycleId);

    List<StudentReward> findAllByStudentIdOrderByCreatedAtDesc(UUID studentId);

    boolean existsByRewardCycleIdAndStudentId(UUID rewardCycleId, UUID studentId);

    boolean existsByRewardCycleIdAndStatus(UUID rewardCycleId, StudentRewardStatus status);

    void deleteAllByRewardCycleId(UUID rewardCycleId);

    @Query(value = """
            WITH lesson_metrics AS (
                SELECT
                    u.id AS student_id,
                    COUNT(DISTINCT l.id) AS total_lessons,
                    COUNT(DISTINCT lp.lesson_id) AS completed_lessons
                FROM users u
                LEFT JOIN course_enrollments ce ON ce.student_id = u.id
                LEFT JOIN courses c ON c.id = ce.course_id
                LEFT JOIN chapters ch ON ch.course_id = c.id
                LEFT JOIN lessons l ON l.chapter_id = ch.id
                LEFT JOIN lesson_progress lp ON lp.lesson_id = l.id
                    AND lp.student_id = u.id
                    AND lp.visited_at >= :periodStart
                    AND lp.visited_at < :periodEnd
                WHERE u.organization_id = :organizationId
                  AND u.role_type = 'STUDENT'
                GROUP BY u.id
            ),
            scored_activities AS (
                SELECT
                    tr.student_id,
                    tr.attempt_id::text AS activity_id,
                    tr.score_percent AS score_percent
                FROM test_results tr
                JOIN users u ON u.id = tr.student_id
                WHERE u.organization_id = :organizationId
                  AND u.role_type = 'STUDENT'
                  AND tr.completed_at >= :periodStart
                  AND tr.completed_at < :periodEnd

                UNION ALL

                SELECT
                    s.student_id,
                    a.id::text AS activity_id,
                    COALESCE(a.score, 0) * 100 AS score_percent
                FROM adaptive_session_answers a
                JOIN adaptive_sessions s ON s.id = a.session_id
                JOIN users u ON u.id = s.student_id
                WHERE u.organization_id = :organizationId
                  AND u.role_type = 'STUDENT'
                  AND s.completed_at >= :periodStart
                  AND s.completed_at < :periodEnd
            ),
            score_metrics AS (
                SELECT
                    student_id,
                    COUNT(DISTINCT activity_id) AS solved_exercises,
                    AVG(score_percent) AS average_score
                FROM scored_activities
                GROUP BY student_id
            )
            SELECT
                u.id AS studentId,
                sw.wallet_address AS walletAddress,
                CAST(COALESCE(lm.completed_lessons, 0) AS integer) AS completedLessons,
                CAST(COALESCE(sm.solved_exercises, 0) AS integer) AS solvedExercises,
                CAST(COALESCE(sm.average_score, 0) AS numeric) AS averageScore,
                CAST(
                    CASE
                        WHEN COALESCE(lm.total_lessons, 0) = 0 THEN 0
                        ELSE COALESCE(lm.completed_lessons, 0) * 100.0 / lm.total_lessons
                    END AS numeric
                ) AS completionRate,
                CAST(COALESCE(lm.completed_lessons, 0) + COALESCE(sm.solved_exercises, 0) AS integer) AS activityCount
            FROM users u
            JOIN student_wallets sw ON sw.student_id = u.id AND sw.verified = true
            LEFT JOIN lesson_metrics lm ON lm.student_id = u.id
            LEFT JOIN score_metrics sm ON sm.student_id = u.id
            WHERE u.organization_id = :organizationId
              AND u.role_type = 'STUDENT'
            """, nativeQuery = true)
    List<RewardCandidateProjection> findRewardCandidateMetrics(
            @Param("organizationId") UUID organizationId,
            @Param("periodStart") LocalDateTime periodStart,
            @Param("periodEnd") LocalDateTime periodEnd
    );
}
