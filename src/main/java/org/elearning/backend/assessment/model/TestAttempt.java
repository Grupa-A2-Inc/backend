package org.elearning.backend.assessment.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Reprezintă o încercare a unui elev pe un test.
 * Un elev poate avea mai multe attempts pe același test.
 */
@Entity
@Table(name = "test_attempts")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class TestAttempt {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /** ID-ul testului pe care îl susține elevul */
    @Column(name = "test_id", nullable = false)
    private UUID testId;

    /** ID-ul elevului — extras din JWT, fără FK real până sosește modulul de useri */
    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    /** Al câtelea attempt e al acestui elev pe acest test */
    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    /** Momentul în care elevul a început testul */
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    /** Momentul în care elevul a terminat testul — null dacă e IN_PROGRESS */
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    /** Starea curentă a attempt-ului */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AttemptStatus status;
}
