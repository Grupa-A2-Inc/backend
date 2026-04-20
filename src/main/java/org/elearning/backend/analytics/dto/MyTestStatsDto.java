package org.elearning.backend.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class MyTestStatsDto {
    private UUID testId;
    private String testTitle;
    private Integer totalAttemptCount;
    private BigDecimal bestScore;
    private BigDecimal lowestScore;
    private Double averageScore;

    private BigDecimal lastScore;

    private Integer totalStudentCount;
    private Double classAverage;

    private BigDecimal classMedian;

    private Integer rank;
    private BigDecimal percentile;

    public MyTestStatsDto(MyPersonalTestStatsDto myPersonalTestStatsDto,
                          BigDecimal lastScore,
                          MyClassTestAverageDto myClassTestAverageDto,
                          BigDecimal classMedian,
                          Integer rank,
                          BigDecimal percentile){
        this.testId = myPersonalTestStatsDto.getTestId();
        this.testTitle = myPersonalTestStatsDto.getTestTitle();
        this.totalAttemptCount = myPersonalTestStatsDto.getTotalAttemptCount();
        this.bestScore = myPersonalTestStatsDto.getBestScore();
        this.lowestScore = myPersonalTestStatsDto.getLowestScore();
        this.averageScore = myPersonalTestStatsDto.getAverageScore();
        this.lastScore = lastScore;
        this.totalStudentCount = myClassTestAverageDto.getTotalStudentCount();
        this.classAverage = myClassTestAverageDto.getClassAverage();
        this.classMedian = classMedian;
        this.rank = rank;
        this.percentile = percentile;

    }

}
