package org.elearning.backend.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassAverageDto {
    private UUID testId;
    private String testTitle;
    private Integer totalAttempts;
    private Integer passedCount;
    private Integer failedCount;
    private Double averageScore;
    private Double minScore;
    private Double maxScore;
    private Double failureRate;
}
