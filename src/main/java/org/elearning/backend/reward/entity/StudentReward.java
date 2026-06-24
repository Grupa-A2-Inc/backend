package org.elearning.backend.reward.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "student_rewards")
public class StudentReward {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reward_cycle_id", nullable = false)
    private UUID rewardCycleId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "student_wallet_address", nullable = false, length = 128)
    private String studentWalletAddress;

    @Column(name = "reward_rank", nullable = false)
    private Integer rank;

    @Column(nullable = false, precision = 8, scale = 4)
    private BigDecimal score;

    @Column(name = "reward_amount", nullable = false, precision = 12, scale = 6)
    private BigDecimal rewardAmount;

    @Column(name = "tx_hash")
    private String txHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StudentRewardStatus status = StudentRewardStatus.CALCULATED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
