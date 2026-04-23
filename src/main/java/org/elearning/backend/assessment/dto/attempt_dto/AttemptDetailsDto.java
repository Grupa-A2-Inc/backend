package org.elearning.backend.assessment.dto.attempt_dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AttemptDetailsDto {
    private UUID attemptId;
    private UUID testId;
    private String testTitle;
    private BigDecimal score;
    private BigDecimal scorePercent;
    private boolean passed;
    private LocalDateTime completedAt;
}
