package org.elearning.backend.ai.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "adaptive_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdaptiveSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "subject_id", nullable = false)
    private Integer subjectId;

    @Column(name = "topic_id", nullable = false)
    private Integer topicId;

    @Builder.Default
    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder.Default
    @Column(name = "ai_feedback_sent", nullable = false)
    private Boolean aiFeedbackSent = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}