package org.elearning.backend.ai.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Schema(name = "AdaptiveResult", description = "Submission result returned after the student submits an adaptive session.")
public class AdaptiveResultDto {
    @Schema(description = "Adaptive session identifier.", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID sessionId;

    @Schema(description = "Aggregated score for the whole session.", example = "8.5")
    private double totalScore;

    @ArraySchema(schema = @Schema(implementation = ClientResultDto.class), arraySchema = @Schema(description = "Per-exercise evaluation results."))
    private List<ClientResultDto> clientResults;

    @Schema(description = "True when the backend managed to send feedback data back to the AI service after submission.", example = "true")
    private boolean feedbackSent;
}
