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
            SELECT
                u.id AS studentId,
                sw.wallet_address AS walletAddress,
                CAST(COUNT(DISTINCT lp.lesson_id) AS integer) AS completedLessons,
                CAST(COUNT(DISTINCT tr.attempt_id) AS integer) AS solvedExercises,
                CAST(COALESCE(AVG(tr.score_percent), 0) AS numeric) AS averageScore,
                CAST(
                    CASE
                        WHEN COUNT(DISTINCT l.id) = 0 THEN 0
                        ELSE COUNT(DISTINCT lp.lesson_id) * 100.0 / COUNT(DISTINCT l.id)
                    END AS numeric
                ) AS completionRate,
                CAST(COUNT(DISTINCT lp.lesson_id) + COUNT(DISTINCT tr.attempt_id) AS integer) AS activityCount
            FROM users u
            JOIN student_wallets sw ON sw.student_id = u.id AND sw.verified = true
            LEFT JOIN course_enrollments ce ON ce.student_id = u.id
            LEFT JOIN courses c ON c.id = ce.course_id
            LEFT JOIN chapters ch ON ch.course_id = c.id
            LEFT JOIN lessons l ON l.chapter_id = ch.id
            LEFT JOIN lesson_progress lp ON lp.lesson_id = l.id
                AND lp.student_id = u.id
                AND lp.visited_at >= :periodStart
                AND lp.visited_at < :periodEnd
            LEFT JOIN test_results tr ON tr.student_id = u.id
                AND tr.completed_at >= :periodStart
                AND tr.completed_at < :periodEnd
            WHERE u.organization_id = :organizationId
              AND u.role_type = 'STUDENT'
            GROUP BY u.id, sw.wallet_address
            """, nativeQuery = true)
    List<RewardCandidateProjection> findRewardCandidateMetrics(
            @Param("organizationId") UUID organizationId,
            @Param("periodStart") LocalDateTime periodStart,
            @Param("periodEnd") LocalDateTime periodEnd
    );
}
