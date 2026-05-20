package org.elearning.backend.ai.dto;

import lombok.Data;
import org.elearning.backend.ai.model.AiRequestStatus;

import java.util.List;

@Data
public class AiAdaptiveJobStatusResponse {
    private String jobId;
    private AiRequestStatus status;
    private List<AiAdaptiveExerciseDto> exercises;
    private String error;
}
