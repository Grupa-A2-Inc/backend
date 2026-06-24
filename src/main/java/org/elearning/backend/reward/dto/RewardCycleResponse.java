package org.elearning.backend.reward.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.elearning.backend.reward.entity.RewardCycleStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class RewardCycleResponse {
    private UUID id;
    private UUID organizationId;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private BigDecimal subscriptionAmount;
    private BigDecimal rewardPoolAmount;
    private BigDecimal eurcDepositedAmount;
    private RewardCycleStatus status;
    private String depositTxHash;
    private String mintTxHash;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<StudentRewardResponse> rewards;
}
