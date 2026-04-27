package org.elearning.backend.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.elearning.backend.ai.model.AiRequestStatus;

import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
public class AiRequestStatusDto {
    private UUID requestId;
    private AiRequestStatus status;
}
