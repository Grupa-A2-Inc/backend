package org.elearning.backend.ai.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "adaptive_exercise_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdaptiveExerciseJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "subject_id", nullable = false)
    private Integer subjectId;

    @Column(name = "topic_id", nullable = false)
    private Integer topicId;

    @Column(name = "question_count", nullable = false)
    private Integer questionCount;

    @Column(name = "ai_job_id", length = 100)
    private String aiJobId;

    @Column(name = "session_id")
    private UUID sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AiRequestStatus status = AiRequestStatus.PENDING;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
