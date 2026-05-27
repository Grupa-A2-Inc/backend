package org.elearning.backend.reward.dto.funding;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class StablecoinFundingResponse {
    private UUID cycleId;
    private UUID organizationId;
    private BigDecimal paymentAmount;
    private BigDecimal rewardPoolAmount;
    private BigDecimal eurcDepositedAmount;
    private String provider;
    private long chainId;
    private String transactionHash;
    private String status;
}
