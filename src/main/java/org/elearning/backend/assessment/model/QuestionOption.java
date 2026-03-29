package org.elearning.backend.assessment.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "question_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id")
    private Long id;

    // Relația inversă: Mai multe Opțiuni aparțin unei singure Întrebări
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "is_correct")
    private Boolean isCorrect;
}