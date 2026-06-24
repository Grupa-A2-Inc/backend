package org.elearning.backend.reward.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.elearning.backend.reward.entity.StudentRewardStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class StudentRewardResponse {
    private UUID id;
    private UUID rewardCycleId;
    private UUID studentId;
    private String studentWalletAddress;
    private Integer rank;
    private BigDecimal score;
    private BigDecimal rewardAmount;
    private String txHash;
    private StudentRewardStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
