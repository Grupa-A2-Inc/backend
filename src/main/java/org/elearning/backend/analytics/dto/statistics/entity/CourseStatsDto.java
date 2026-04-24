package org.elearning.backend.analytics.dto.statistics.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CourseStatsDto {
    private Integer totalTestDone;
    private Integer totalTestPassed;
    private BigDecimal bestScore;
    private BigDecimal lowestScore;
    private Double averageScore;
}
