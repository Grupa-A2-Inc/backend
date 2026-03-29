package org.elearning.backend.assessment.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Stochează rezultatul final al unui attempt completat.
 * attempt_id e atât PK cât și FK către test_attempts.
 */
@Entity
@Table(name = "test_results")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class TestResult {

    /** PK și FK către test_attempts */
    @Id
    @Column(name = "attempt_id")
    private UUID attemptId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "test_id", nullable = false)
    private UUID testId;

    /** Scorul brut — între 0.0000 și 1.0000 */
    @Column(name = "score", nullable = false, precision = 5, scale = 4)
    private BigDecimal score;

    /** Scorul în procente — între 0.00 și 100.00 */
    @Column(name = "score_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal scorePercent;

    /** true dacă scorePercent >= 60 */
    @Column(name = "passed", nullable = false)
    private boolean passed;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;
}