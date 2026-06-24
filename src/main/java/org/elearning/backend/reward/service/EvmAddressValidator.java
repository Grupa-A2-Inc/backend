package org.elearning.backend.reward.service;

import java.util.regex.Pattern;

public final class EvmAddressValidator {
    private static final Pattern EVM_ADDRESS_PATTERN = Pattern.compile("^0x[a-fA-F0-9]{40}$");

    private EvmAddressValidator() {
    }

    public static boolean isValid(String walletAddress) {
        return walletAddress != null && EVM_ADDRESS_PATTERN.matcher(walletAddress).matches();
    }
}
