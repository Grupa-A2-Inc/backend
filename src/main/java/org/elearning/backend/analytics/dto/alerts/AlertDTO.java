package org.elearning.backend.analytics.dto.alerts;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class AlertDTO {
    private UUID alertId;
    private UUID testId;
    private UUID professorId;
    private BigDecimal failureThreshold;
    private BigDecimal currentFailureRate;
    private Boolean isActive;
}
