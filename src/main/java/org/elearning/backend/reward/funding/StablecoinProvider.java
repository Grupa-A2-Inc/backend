package org.elearning.backend.reward.funding;

import java.math.BigDecimal;
import java.util.UUID;

public interface StablecoinProvider {

    StablecoinFundingResult fundPlatformWallet(
            UUID organizationId,
            BigDecimal paymentAmount,
            BigDecimal eurcAmount,
            String idempotencyKey
    );
}
