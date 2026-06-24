package org.elearning.backend.reward.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.elearning.backend.reward.entity.DistributionPeriod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class RewardConfigResponse {
    private UUID id;
    private UUID organizationId;
    private BigDecimal rewardPercent;
    private BigDecimal minimumScore;
    private Integer maximumWinners;
    private DistributionPeriod distributionPeriod;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
