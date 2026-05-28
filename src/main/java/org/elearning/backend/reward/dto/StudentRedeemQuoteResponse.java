package org.elearning.backend.reward.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@AllArgsConstructor
public class StudentRedeemQuoteResponse {
    private String walletAddress;
    private String recipientAddress;
    private BigDecimal amount;
    private String amountSmallestUnit;
    private String nonce;
    private String deadline;
    private Map<String, Object> typedData;
}
