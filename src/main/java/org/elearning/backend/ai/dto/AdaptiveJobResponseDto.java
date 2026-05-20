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
@Schema(name = "AdaptiveJobCreated", description = "Immediate response returned after creating an adaptive generation job.")
public class AdaptiveJobResponseDto {
    @Schema(description = "Local backend job identifier used for polling.", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID jobId;

    @Schema(description = "Current local job status right after creation.", example = "PENDING", allowableValues = {"PENDING", "RUNNING", "DONE", "FAILED"})
    private AiRequestStatus status;
}
