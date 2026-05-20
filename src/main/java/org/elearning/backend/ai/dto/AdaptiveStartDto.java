package org.elearning.backend.ai.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Schema(name = "AdaptiveSessionStart", description = "Adaptive session payload returned once the exercise set is ready.")
public class AdaptiveStartDto {
    @Schema(description = "Created adaptive session identifier. This value is later used on submit.", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID sessionId;

    @Schema(description = "Session expiration timestamp. The countdown starts when the session is materialized, not when the job is created.", example = "2026-05-20T10:45:00")
    private LocalDateTime expiresAt;

    @ArraySchema(schema = @Schema(implementation = ClientExerciseDto.class), arraySchema = @Schema(description = "Exercises included in the adaptive session."))
    private List<ClientExerciseDto> exercises;
}
