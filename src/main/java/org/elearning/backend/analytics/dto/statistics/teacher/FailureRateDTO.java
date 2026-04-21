package org.elearning.backend.analytics.dto.statistics.teacher;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class FailureRateDTO {
    private BigDecimal failureRate;
    private BigDecimal threshold;
    private boolean alertTriggered;
}
