package org.elearning.backend.assessment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a Test, which is associated with a specific lesson and created by a user.
 * A Test can have multiple questions and is used to assess students' understanding of the lesson material.
 */
@Entity
@Table(name = "tests")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Test {

    /**
     * Primary key for the Test entity, generated automatically as a UUID.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The ID of the lesson to which this test belongs.
     * This is stored as a UUID and is not nullable, as every test must be associated with a lesson.
     */
    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    /**
     * The ID of the user who created the test.
     * This is stored as a UUID and is not nullable, as every test must have a creator.
     */
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "previous_version_id")
    private UUID previousVersionId;

    /**
     * The title of the test, which will be displayed to students when they take the test.
     * This is stored as a string and is not nullable, as every test must have a title.
     */
    @Column(nullable = false)
    private String title;

    /**
     * A detailed description of the test, which can include instructions for students or any other relevant information.
     * This is stored as TEXT in the database to allow for longer descriptions if needed.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * The time limit for the test in seconds.
     * This is stored as an Integer and can be null if there is no time limit for the test.
     */
    @Column(name = "time_limit_sec")
    private Integer timeLimitSec;

    /**
     * The status of the test, represented as an enum (e.g., DRAFT, PUBLISHED).
     * This is stored as a string in the database for better readability and maintainability.
     * It is not nullable, as every test must have a status.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private TestStatus status;

    /**
     * Boolean indicating whether the data is sent to the AI agent.
     */
    @Column(name = "ai_enabled", columnDefinition = "boolean default false")
    private Boolean aiEnabled = false;

    /**
     * Timestamp indicating when the test was created.
     * This is automatically set to the current time when the test is created and is not updatable.
     */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp indicating when the test was last updated.
     * This is automatically updated to the current time whenever the test is updated.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * This is a one-to-many relationship, as a test can have multiple questions.
     * The mappedBy attribute indicates that the "test" field in the Question entity owns the relationship.
     * CascadeType.ALL means that any changes to the Test entity (like persist, merge, remove) will be cascaded to the associated Question entities.
     * orphanRemoval = true means that if a Question is removed from the questions list, it will also be removed from the database.
     */
    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions = new ArrayList<>();

    /**
     * This is a one-to-many relationship, as a test can have multiple test results.
     * The mappedBy attribute indicates that the "test" field in the TestResult entity owns the relationship.
     * CascadeType.ALL means that any changes to the Test entity (like persist, merge, remove) will be cascaded to the associated TestResult entities.
     * orphanRemoval = true means that if a TestResult is removed from the testResults list, it will also be removed from the database.
     */
    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestResult> testResults = new ArrayList<>();

    /**
     * This method is called before the entity is persisted (saved for the first time) to the database.
     * It sets the createdAt and updatedAt timestamps to the current time.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * This method is called before the entity is updated in the database.
     * It updates the updatedAt timestamp to the current time.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
