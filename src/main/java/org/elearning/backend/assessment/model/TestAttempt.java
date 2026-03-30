package org.elearning.backend.assessment.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity which represents an attempt of a student to take a test.
 * It tracks the start and end times, the status of the attempt, and which test and student it belongs to.
 */
@Entity
@Table(name = "test_attempts")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class TestAttempt {

    /**
     * Primary key for the TestAttempt entity, generated automatically as a UUID.
     */
    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Many-to-one relationship with the Test entity, as multiple attempts can be made for a single test.
     * Lazy fetching is used to optimize performance by loading the associated test only when needed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    @JsonIgnore
    private Test test;

    /**
     * Id of the student who made the attempt.
     * Not nullable, as every attempt must be associated with a student.
     */
    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    /**
     * The number of the attempt for this student and test.
     * For example, if this is the student's first attempt at this test, attemptNumber would be 1.
     * If it's their second attempt, it would be 2, and so on.
     * This allows us to track multiple attempts by the same student for the same test.
     */
    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    /**
     * The time when the attempt was started.
     * Not nullable, as every attempt must have a start time.
     */
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    /**
     * The time when the attempt was ended.
     * This can be null if the attempt is still in progress.
     */
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    /**
     * The current status of the attempt, represented as an enum.
     * Not nullable, as every attempt must have a status.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private AttemptStatus status;
}
