package org.elearning.backend.analytics.dto.statistics.student;

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
public class MySummaryDataDto {
    private UUID courseId;
    private String courseTitle;
    private Integer totalTestCount;
    private Integer totalTestDone;
    private Integer totalTestPassed;
    private BigDecimal bestScore;
    private BigDecimal lowestScore;
    private Double averageScore;

}
