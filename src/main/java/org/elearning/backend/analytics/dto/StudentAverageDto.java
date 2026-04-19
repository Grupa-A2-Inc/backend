package org.elearning.backend.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
public class StudentAverageDto {
    private UUID studentId;
    private Double averageScore;
    private Double minScore;
    private Double maxScore;
    private Integer testCount;
    private Integer passedTests;
    private Integer failedTests;
    private LocalDateTime lastAttemptAt;
}
