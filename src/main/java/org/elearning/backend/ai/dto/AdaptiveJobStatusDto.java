package org.elearning.backend.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.elearning.backend.ai.model.AiRequestStatus;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Schema(name = "AdaptiveJobStatus", description = "Polling response for an adaptive generation job. `session` is null until the job reaches DONE.")
public class AdaptiveJobStatusDto {
    @Schema(description = "Local backend job identifier.", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID jobId;

    @Schema(description = "Current local job status.", example = "RUNNING", allowableValues = {"PENDING", "RUNNING", "DONE", "FAILED"})
    private AiRequestStatus status;

    @Schema(description = "Error message returned when the job failed. Null for non-failed jobs.", example = "Adaptive AI returned an invalid response.")
    private String error;

    @Schema(description = "Generated adaptive session payload. Present only when status is DONE.")
    private AdaptiveStartDto session;
}
