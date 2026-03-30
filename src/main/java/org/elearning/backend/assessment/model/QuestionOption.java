package org.elearning.backend.assessment.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing an option for a question in a test.
 * Each question can have multiple options, and this entity captures the text of the option,
 * its display order, and whether it is the correct answer or not.
 */
@Entity
@Table(name = "question_options")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class QuestionOption {

    /**
     * Primary key for the QuestionOption entity, generated automatically as an auto-incrementing value.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Many-to-one relationship with the Question entity, as multiple options can belong to a single question.
     * Lazy fetching is used to optimize performance by loading the associated question only when needed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    @JsonIgnore
    private Question question;

    /**
     * The text of the option, which will be displayed to the student.
     * This is stored as TEXT in the database to allow for longer option texts if needed.
     */
    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    /**
     * The order in which the option should be displayed among other options for the same question.
     * This allows for custom ordering of options when presenting them to the student.
     */
    @Column(name = "display_order")
    private Integer displayOrder;

    /**
     * Boolean indicating whether this option is the correct answer for the question.
     * This is used to determine if the student's selected options are correct when grading their answers.
     */
    @Column(name = "is_correct")
    private Boolean isCorrect;
}