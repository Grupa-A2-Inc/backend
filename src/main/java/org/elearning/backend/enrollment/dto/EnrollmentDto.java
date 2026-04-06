package org.elearning.backend.enrollment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@NoArgsConstructor @AllArgsConstructor
@Getter @Setter
public class EnrollmentDto {
    private UUID enrollmentId;
    private UUID courseId;
    private UUID studentId;
    private Date enrolledAt;
    private BigDecimal progressPercent;
}
