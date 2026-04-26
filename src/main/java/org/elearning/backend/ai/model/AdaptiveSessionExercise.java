package org.elearning.backend.ai.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "adaptive_session_exercises")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdaptiveSessionExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "ml_exercise_id", nullable = false, length = 100)
    private String mlExerciseId;

    @Column(name = "exercise_text", nullable = false, columnDefinition = "TEXT")
    private String exerciseText;

    @Column(name = "exercise_type", nullable = false, length = 30)
    private String exerciseType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answers_raw", nullable = false, columnDefinition = "jsonb")
    private String answersRaw;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "correct_answers_raw", nullable = false, columnDefinition = "jsonb")
    private String correctAnswersRaw;

    @Column(precision = 4, scale = 2)
    private BigDecimal difficulty;
}

