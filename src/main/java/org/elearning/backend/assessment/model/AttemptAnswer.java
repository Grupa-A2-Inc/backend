package org.elearning.backend.assessment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Reprezintă răspunsul unui elev la o întrebare în cadrul unui attempt.
 */
@Entity
@Table(name = "attempt_answers")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class AttemptAnswer {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    /** FK către attempt-ul curent */
    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;

    /** FK către întrebarea la care s-a răspuns */
    @Column(name = "question_id", nullable = false)
    private Long questionId;

    /**
     * ID-urile opțiunilor selectate de elev — stocat ca JSONB în PostgreSQL.
     * Exemplu: [1, 3]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "selected_option_ids", columnDefinition = "jsonb", nullable = false)
    private List<Long> selectedOptionIds;

    /** true dacă răspunsul elevului e corect */
    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect;

    /**
     * Timpul petrecut pe această întrebare — în secunde, float.
     * Primit de la frontend, NU calculat de backend.
     */
    @Column(name = "time_spent")
    private BigDecimal timeSpent;

    /** Momentul în care s-a salvat răspunsul */
    @Column(name = "answered_at", nullable = false)
    private LocalDateTime answeredAt;
}
