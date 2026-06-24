package org.elearning.backend.reward.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class StudentRewardCandidate {
    private UUID studentId;
    private String walletAddress;
    private BigDecimal finalScore;
    private Integer rank;
    private BigDecimal calculatedRewardAmount;
}
