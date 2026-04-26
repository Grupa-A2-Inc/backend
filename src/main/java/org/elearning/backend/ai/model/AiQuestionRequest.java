package org.elearning.backend.ai.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_question_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiQuestionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    @Column(name = "subject_id")
    private Integer subjectId;

    @Column(name = "topic_id")
    private Integer topicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AiRequestStatus status = AiRequestStatus.PENDING;

    @Column(name = "generated_questions", columnDefinition = "TEXT")
    private String generatedQuestions;

    @Column(name = "test_id")
    private UUID testId;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}