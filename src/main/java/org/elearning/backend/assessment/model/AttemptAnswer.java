package org.elearning.backend.assessment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing an answer provided by a student for a specific question during a test attempt.
 * This entity captures the selected options, correctness, time spent, and the timestamp of when the answer was saved.
 */
@Entity
@Table(name = "attempt_answers")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class AttemptAnswer {

    /**
     * Primary key for the AttemptAnswer entity, generated as a UUID.
     */
    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /**
     * Foreign key to TestAttempt entity, representing the attempt to which this answer belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private TestAttempt attempt;

    /**
     * Foreign key to Question entity, representing the question that was answered.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    /**
     * List of selected option IDs for the answered question, stored as JSONB in PostgreSQL.
     * Example: [1, 3]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "selected_option_ids", columnDefinition = "jsonb", nullable = false)
    private List<Long> selectedOptionIds;

    /**
     * Boolean indicating whether the student's answer is correct or not.
     * This is determined by comparing the selected options with the correct options for the question.
     */
    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect;

    /**
     * Time spent on this question in seconds, represented as a decimal value.
     * This value is provided by the frontend and is not calculated by the backend.
     */
    @Column(name = "time_spent")
    private BigDecimal timeSpent;

    /**
     * Timestamp indicating when the answer was saved.
     * This is automatically set to the current time when the answer is created.
     */
    @Column(name = "answered_at", nullable = false)
    private LocalDateTime answeredAt;

    /**
     * Bidirectional relationship with TestAttempt entity.
     * An attempt can have multiple answers, and if an attempt is deleted, all associated answers will also be deleted (cascade = CascadeType.ALL).
     * orphanRemoval = true ensures that if an answer is removed from the attempt's list of answers, it will also be deleted from the database.
     */
    /*@OneToMany(mappedBy = "attempt_answers", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AttemptAnswer> answers = new ArrayList<>();*/
}
