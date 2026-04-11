package org.elearning.backend.enrollment.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class StudentProgressDto {
    private UUID studentId;
    private LocalDateTime enrolledAt;
    private Double progressPercent;
    private LocalDateTime completedAt;
}
