package org.elearning.backend.analytics.dto.statistics.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DifficultyLessonDto {
    private UUID lessonId;
    private String lessonTitle;
    private Double myBestScore;
    private Double classAverage;
    private Double gap;
}
