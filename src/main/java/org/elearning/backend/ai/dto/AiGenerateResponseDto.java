package org.elearning.backend.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.elearning.backend.analytics.model.AiRequestStatus;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class AiGenerateResponseDto {
    private UUID requestId;
    private AiRequestStatus status;
    private UUID lessonId;
}
