package org.elearning.backend.ai.dto;

import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
public class AdaptiveStartDto {
    private UUID sessionId;
    private LocalDateTime expiresAt;
    private List<ClientExerciseDto> exercises;
}
