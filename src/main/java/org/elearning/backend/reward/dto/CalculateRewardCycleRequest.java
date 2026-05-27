package org.elearning.backend.reward.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class CalculateRewardCycleRequest {
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    @DecimalMin("0.00")
    private BigDecimal subscriptionAmount;

    @DecimalMin("0.00")
    private BigDecimal eurcDepositedAmount;
}
