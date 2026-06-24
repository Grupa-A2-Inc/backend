package org.elearning.backend.reward.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class StudentRedeemResponse {
    private String walletAddress;
    private BigDecimal amount;
    private String transactionHash;
    private String status;
}
