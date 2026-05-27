package org.elearning.backend.reward.funding;

import java.math.BigDecimal;

public record StablecoinFundingResult(
        BigDecimal eurcAmount,
        String provider,
        String transactionHash
) {
}
