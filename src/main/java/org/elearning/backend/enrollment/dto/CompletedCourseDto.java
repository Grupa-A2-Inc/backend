package org.elearning.backend.enrollment.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CompletedCourseDto {
    private UUID courseId;
    private String courseTitle;
    private LocalDateTime enrolledAt;
    private LocalDateTime completedAt;
}
