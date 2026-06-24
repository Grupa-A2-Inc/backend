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
@Table(name = "reward_cycles")
public class RewardCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "period_start", nullable = false)
    private LocalDateTime periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDateTime periodEnd;

    @Column(name = "subscription_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal subscriptionAmount;

    @Column(name = "reward_pool_amount", nullable = false, precision = 12, scale = 6)
    private BigDecimal rewardPoolAmount;

    @Column(name = "eurc_deposited_amount", nullable = false, precision = 12, scale = 6)
    private BigDecimal eurcDepositedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RewardCycleStatus status = RewardCycleStatus.CREATED;

    @Column(name = "deposit_tx_hash")
    private String depositTxHash;

    @Column(name = "mint_tx_hash")
    private String mintTxHash;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
