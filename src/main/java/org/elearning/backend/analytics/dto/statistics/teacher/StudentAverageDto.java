package org.elearning.backend.analytics.dto.statistics.teacher;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
public class StudentAverageDto {
    private UUID studentId;
    private BigDecimal averageScore;
    private BigDecimal minScore;
    private BigDecimal maxScore;
    private Integer testCount;
    private Integer passedTests;
    private Integer failedTests;
    private LocalDateTime lastAttemptAt;
}
