package org.elearning.backend.enrollment.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ProgressDto {
    private UUID courseId;
    private UUID studentId;
    private int totalLessons;
    private int completedLessons;
    private double percentage;
    private double percentageDisplay;
    private boolean isCompleted;

    public static ProgressDto empty(UUID courseId, UUID studentId) {
        return ProgressDto.builder()
                .courseId(courseId)
                .studentId(studentId)
                .totalLessons(0)
                .completedLessons(0)
                .percentage(0.0)
                .percentageDisplay(0.0)
                .isCompleted(false)
                .build();
    }
}