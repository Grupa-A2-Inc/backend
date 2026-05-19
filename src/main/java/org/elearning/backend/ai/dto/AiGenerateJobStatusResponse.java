package org.elearning.backend.ai.dto;

import lombok.Data;
import org.elearning.backend.ai.model.AiRequestStatus;

import java.util.List;

@Data
public class AiGenerateJobStatusResponse {
    private String jobId;
    private AiRequestStatus status;
    private List<AiQuestionDto> questions;
    private String error;
}
