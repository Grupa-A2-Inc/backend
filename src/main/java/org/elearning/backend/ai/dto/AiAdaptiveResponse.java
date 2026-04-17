package org.elearning.backend.ai.dto;

import lombok.Data;
import java.util.List;

@Data
public class AiAdaptiveResponse {
    private List<AiAdaptiveExerciseDto> exercises;
}