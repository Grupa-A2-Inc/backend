package org.elearning.backend.analytics.dto;

import lombok.Data;
import org.elearning.backend.ai.dto.AiAdaptiveExerciseDto;

import java.util.List;

@Data
public class AdaptiveStartRequestDto {
    private Integer subjectId;
    private Integer topicId;
    private int count;
}
