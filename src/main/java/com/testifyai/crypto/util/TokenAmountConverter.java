package com.testifyai.crypto.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

public final class TokenAmountConverter {

    private static final int TOKEN_DECIMALS = 6;
    private static final BigDecimal TOKEN_MULTIPLIER = BigDecimal.TEN.pow(TOKEN_DECIMALS);

    private TokenAmountConverter() {
    }

    public static BigInteger toSmallestUnit(BigDecimal amount) {
        return amount
                .multiply(TOKEN_MULTIPLIER)
                .setScale(0, RoundingMode.UNNECESSARY)
                .toBigIntegerExact();
    }

    public static BigDecimal fromSmallestUnit(BigInteger amount) {
        return new BigDecimal(amount)
                .divide(TOKEN_MULTIPLIER, TOKEN_DECIMALS, RoundingMode.DOWN);
    }
}