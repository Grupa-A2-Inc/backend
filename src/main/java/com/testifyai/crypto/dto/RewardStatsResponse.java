package com.testifyai.crypto.dto;

import java.math.BigDecimal;

public class RewardStatsResponse {

    private BigDecimal vaultBalance;
    private BigDecimal taiTotalSupply;
    private BigDecimal availableToMint;
    private boolean fullyBacked;

    public RewardStatsResponse(
            BigDecimal vaultBalance,
            BigDecimal taiTotalSupply,
            BigDecimal availableToMint,
            boolean fullyBacked
    ) {
        this.vaultBalance = vaultBalance;
        this.taiTotalSupply = taiTotalSupply;
        this.availableToMint = availableToMint;
        this.fullyBacked = fullyBacked;
    }

    public BigDecimal getVaultBalance() {
        return vaultBalance;
    }

    public BigDecimal getTaiTotalSupply() {
        return taiTotalSupply;
    }

    public BigDecimal getAvailableToMint() {
        return availableToMint;
    }

    public boolean isFullyBacked() {
        return fullyBacked;
    }
}