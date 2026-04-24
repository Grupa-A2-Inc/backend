package org.elearning.backend.analytics.repository;

import org.elearning.backend.analytics.model.AnalyticsAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnalyticsAlertRepository extends JpaRepository<AnalyticsAlert, UUID> {
    Optional<AnalyticsAlert> findByTestIdAndIsActiveTrue(UUID testId);

    @Modifying
    @Transactional
    @Query(value = """
    INSERT INTO analytics_alerts (test_id, professor_id, failure_threshold)
    VALUES (:testId, :professorId, :threshold)
    ON CONFLICT (test_id, professor_id) 
    DO UPDATE SET failure_threshold = EXCLUDED.failure_threshold
    """, nativeQuery = true)
    void upsertAlertThreshold(
            @Param("testId") UUID testId,
            @Param("professorId") UUID professorId,
            @Param("threshold") BigDecimal threshold
    );

    @Query("""
    SELECT a FROM AnalyticsAlert a 
    WHERE a.professorId = :professorId 
    AND a.isActive = true 
    ORDER BY a.triggeredAt DESC NULLS LAST
    """)
    List<AnalyticsAlert> getActiveAlertsForProfessor(@Param("professorId") UUID professorId);
}
