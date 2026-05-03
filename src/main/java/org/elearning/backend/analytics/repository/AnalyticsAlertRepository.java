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
    /**
 * Finds the active AnalyticsAlert associated with the specified test.
 *
 * @param testId the UUID of the test to search alerts for
 * @return an {@link Optional} containing the active {@link AnalyticsAlert} for the given test ID, or empty if none exists
 */
Optional<AnalyticsAlert> findByTestIdAndIsActiveTrue(UUID testId);

    /**
     * Inserts or updates the failure threshold for the alert identified by the given test and professor.
     *
     * If an alert for the (testId, professorId) pair does not exist, a new row is inserted; if it does
     * exist, the row's failure threshold is updated to the provided value.
     *
     * @param testId      the UUID of the test the alert belongs to
     * @param professorId the UUID of the professor owning the alert
     * @param threshold   the failure threshold to set (as a BigDecimal percentage or absolute value as used by the schema)
     */
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

    /**
     * Retrieve active analytics alerts for the specified professor, ordered by most recent trigger time.
     *
     * @param professorId the professor's UUID whose active alerts to retrieve
     * @return a list of active {@code AnalyticsAlert} entities for the professor, ordered by {@code triggeredAt} descending (nulls last)
     */
    @Query("""
    SELECT a FROM AnalyticsAlert a 
    WHERE a.professorId = :professorId 
    AND a.isActive = true 
    ORDER BY a.triggeredAt DESC NULLS LAST
    """)
    List<AnalyticsAlert> getActiveAlertsForProfessor(@Param("professorId") UUID professorId);

    /**
     * Computes per-day failure percentages for the specified test.
     *
     * @param testId UUID of the test whose results will be aggregated by completion date.
     * @return a list of rows where each `Object[]` contains two elements:
     *         index 0 — the date for the grouped results (SQL date), index 1 — the daily failure rate as a percentage (`Double`).
     */
    @Query(value = """
    SELECT 
        DATE(tr.completed_at) AS date, 
        (COUNT(CASE WHEN tr.passed = false THEN 1 END) * 100.0 / NULLIF(COUNT(tr.attempt_id), 0)) AS dailyFailureRate
    FROM test_results tr
    WHERE tr.test_id = :testId
    GROUP BY DATE(tr.completed_at)
    ORDER BY date ASC
    """, nativeQuery = true)
    List<Object[]> getDailyFailureRatesForTest(@Param("testId") UUID testId);
}
