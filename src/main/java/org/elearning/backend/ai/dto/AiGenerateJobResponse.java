package org.elearning.backend.ai.dto;

import lombok.Data;
import org.elearning.backend.ai.model.AiRequestStatus;

@Data
public class AiGenerateJobResponse {
    private String jobId;
    private AiRequestStatus status;
}
