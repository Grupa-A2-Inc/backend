package org.elearning.backend.assessment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // SERIAL în PostgreSQL
    @Column(name = "question_id")
    private Long id;

    @Column(name = "test_id", nullable = false)
    private UUID testId;

    @Column(name = "subject_id")
    private Integer subjectId;

    @Column(name = "topic_id")
    private Integer topicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    @Column(columnDefinition = "TEXT")
    private String content;

    // Precizie pentru note/dificultate (ex: 2.50)
    @Column(precision = 4, scale = 2)
    private BigDecimal difficulty;

    @Column(name = "is_active")
    private Boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Relația către Opțiuni (O Întrebare are mai multe Opțiuni)
    // CascadeType.ALL: Dacă ștergi întrebarea, se șterg automat și opțiunile din DB
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionOption> options = new ArrayList<>();
}