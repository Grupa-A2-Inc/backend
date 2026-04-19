package org.elearning.backend.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MyPersonalTestStatsDto {
    private UUID testId;
    private String testTitle;
    private Integer totalAttemptCount;
    private BigDecimal bestScore;
    private BigDecimal lowestScore;
    private Double averageScore;
}
