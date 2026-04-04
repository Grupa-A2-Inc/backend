package org.elearning.backend.assessment.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.elearning.backend.assessment.model.AttemptStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@Builder
public class AttemptStatusDTO {
    private UUID attemptID;
    private int attemptNumber;
    private BigDecimal score;
    private BigDecimal scorePercent;
    private boolean passed;
    private LocalDateTime startedAt;
    private AttemptStatus status;
}
