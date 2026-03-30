package org.elearning.backend.assessment.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a question that belongs to a test.
 * Each question has a type, content, difficulty level, and can have multiple options (for multiple-choice questions).
 * The question is associated with a specific test and can be categorized by subject and topic.
 */
@Entity
@Table(name = "questions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Question {

    /**
     * Primary key for the Question entity, generated automatically as an auto-incrementing value.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
    * Many-to-one relationship with the Test entity, as multiple questions can belong to a single test.
    * Lazy fetching is used to optimize performance by loading the associated test only when needed.
    */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    @JsonIgnore
    private Test test;

    /**
     * The ID of the subject to which this question belongs.
     * This is stored as an Integer and can be used to categorize questions by subject area.
     */
    @Column(name = "subject_id")
    private Integer subjectId;

    /**
     * The ID of the topic to which this question belongs.
     * This is stored as an Integer and can be used to further categorize questions within a subject.
     */
    @Column(name = "topic_id")
    private Integer topicId;

    /**
     * The type of the question, represented as an enum (e.g., MULTIPLE_CHOICE, TRUE_FALSE, SHORT_ANSWER).
     * This is stored as a string in the database for better readability and maintainability.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    /**
     * The content of the question, which will be displayed to the student.
     * This is stored as TEXT in the database to allow for longer question texts if needed.
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /**
     * The difficulty level of the question, represented as a decimal value.
     * This can be used to categorize questions by difficulty.
     * The precision is set to 4 and scale to 2, allowing for values like 1.00, 2.50, etc.
     */
    @Column(name = "difficulty", precision = 4, scale = 2)
    private BigDecimal difficulty;

    /**
     * Boolean indicating whether the question is active or not.
     * This can be used to soft-delete questions or to temporarily disable them without removing them from the database.
     */
    @Column(name = "is_active")
    private Boolean isActive;

    /**
     * Timestamp indicating when the question was created.
     * This is automatically set to the current time when the question is created and is not updatable.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * This is a one-to-many relationship, as a question can have multiple options.
     */
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionOption> options = new ArrayList<>();
}