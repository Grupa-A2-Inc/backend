package org.elearning.backend.ai.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "adaptive_session_answers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdaptiveSessionAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "given_answers", nullable = false, columnDefinition = "jsonb")
    private String givenAnswers;

    @Column(precision = 3, scale = 2)
    private BigDecimal score;

    @Column(name = "time_spent")
    private Integer timeSpent;
}