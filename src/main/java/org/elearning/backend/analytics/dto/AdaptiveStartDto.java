package org.elearning.backend.analytics.dto;

import lombok.AllArgsConstructor;
import org.elearning.backend.ai.dto.AiAdaptiveExerciseDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
public class AdaptiveStartDto {
    private UUID sessionId;
    private LocalDateTime expiresAt;
    private List<ClientExerciseDto> exercises;
}
