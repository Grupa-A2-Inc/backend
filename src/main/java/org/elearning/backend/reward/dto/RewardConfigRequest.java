package org.elearning.backend.reward.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.elearning.backend.reward.entity.DistributionPeriod;

import java.math.BigDecimal;

@Getter
@Setter
public class RewardConfigRequest {
    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal minimumScore;

    @NotNull
    @Min(1)
    private Integer maximumWinners;

    @NotNull
    private DistributionPeriod distributionPeriod;

    @NotNull
    private Boolean enabled;
}
