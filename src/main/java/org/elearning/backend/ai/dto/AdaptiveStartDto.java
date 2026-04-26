package org.elearning.backend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdaptiveStartDto {
    private UUID sessionId;
    private LocalDateTime expiresAt;
    private List<ClientExerciseDto> exercises;
}
