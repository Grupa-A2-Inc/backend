package org.elearning.backend.enrollment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor @AllArgsConstructor
@Getter @Setter
public class EnrolledCourseDto {
    private UUID unrollmentId;
    private UUID courseId;
    private String courseTitle;
    private String courseCategory;
    private LocalDateTime enrolledAt;
    private BigDecimal progressPercent;
    private LocalDateTime completedAt;
}
