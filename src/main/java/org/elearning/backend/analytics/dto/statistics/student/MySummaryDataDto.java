package org.elearning.backend.analytics.dto.statistics.student;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.elearning.backend.analytics.dto.statistics.entity.CourseDetailsDto;
import org.elearning.backend.analytics.dto.statistics.entity.CourseStatsDto;
import org.elearning.backend.analytics.dto.statistics.entity.DifficultyLessonDto;
import org.elearning.backend.assessment.dto.attempt_dto.AttemptDetailsDto;

import java.math.BigDecimal;
import java.util.List;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MySummaryDataDto {
    private String courseTitle;
    private Integer totalTestCount;
    private Integer totalTestDone;
    private Integer totalTestPassed;
    private BigDecimal bestScore;
    private BigDecimal lowestScore;
    private Double averageScore;
    private List<DifficultyLessonDto> difficultyLessons;
    private List<AttemptDetailsDto> lastAttempts;

    /**
     * Constructs a MySummaryDataDto by extracting course metadata and statistics and assigning the provided difficulty lessons and recent attempts.
     *
     * @param courseDetailsDto provides course title and total test count
     * @param courseStatsDto   provides total tests done, total tests passed, best/lowest scores, and average score
     * @param difficultyLessons list of difficulty breakdowns for lessons; assigned to the DTO's difficultyLessons field
     * @param lastAttempts     list of recent attempt details; assigned to the DTO's lastAttempts field
     */
    public MySummaryDataDto(CourseDetailsDto courseDetailsDto,
                            CourseStatsDto courseStatsDto,
                            List<DifficultyLessonDto> difficultyLessons,
                            List<AttemptDetailsDto> lastAttempts){
        this.courseTitle = courseDetailsDto.getCourseTitle();
        this.totalTestCount = courseDetailsDto.getTotalTestCount();
        this.totalTestDone = courseStatsDto.getTotalTestDone();
        this.totalTestPassed = courseStatsDto.getTotalTestPassed();
        this.bestScore = courseStatsDto.getBestScore();
        this.lowestScore = courseStatsDto.getLowestScore();
        this.averageScore = courseStatsDto.getAverageScore();
        this.difficultyLessons = difficultyLessons;
        this.lastAttempts = lastAttempts;
    }

}
