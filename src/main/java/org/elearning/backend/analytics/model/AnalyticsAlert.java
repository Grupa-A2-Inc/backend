package org.elearning.backend.analytics.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "analytics_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "test_id", nullable = false)
    private UUID testId;

    @Column(name = "professor_id", nullable = false)
    private UUID professorId;

    @Column(name = "failure_threshold", nullable = false, precision = 5, scale = 2)
    private BigDecimal failureThreshold;

    @Column(name = "current_failure_rate", precision = 5, scale = 2)
    private BigDecimal currentFailureRate;

    @Column(name = "triggered_at")
    private LocalDateTime triggeredAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}