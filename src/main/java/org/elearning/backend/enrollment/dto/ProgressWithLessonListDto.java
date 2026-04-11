package org.elearning.backend.enrollment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class ProgressWithLessonListDto {
    private int totalLessons;
    private int visitedLessons;
    private double progressPercent;
    private LocalDateTime completedAt;
    private List<LessonStatusDto> lessons;
}
