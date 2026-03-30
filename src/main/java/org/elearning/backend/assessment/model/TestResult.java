package org.elearning.backend.assessment.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "test_results")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class TestResult {

    @Id
    private UUID attemptId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "attempt_id")
    private TestAttempt attempt;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @Column(name = "score", nullable = false, precision = 5, scale = 4)
    private BigDecimal score;

    @Column(name = "score_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal scorePercent;

    @Column(name = "passed", nullable = false)
    private boolean passed;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;
}