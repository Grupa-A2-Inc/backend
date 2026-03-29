// dto/TestResultDTO.java
package org.elearning.backend.assessment.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Rezultatul returnat elevului după submit */
@Getter @Setter @Builder
public class TestResultDTO {
    private UUID attemptId;
    private BigDecimal score;
    private BigDecimal scorePercent;
    private boolean passed;
    private LocalDateTime completedAt;
}