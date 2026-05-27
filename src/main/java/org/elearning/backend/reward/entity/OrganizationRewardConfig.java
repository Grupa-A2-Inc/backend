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
@Table(name = "organization_reward_configs")
public class OrganizationRewardConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false, unique = true)
    private UUID organizationId;

    @Column(name = "reward_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal rewardPercent;

    @Column(name = "minimum_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal minimumScore;

    @Column(name = "maximum_winners", nullable = false)
    private Integer maximumWinners;

    @Enumerated(EnumType.STRING)
    @Column(name = "distribution_period", nullable = false, length = 30)
    private DistributionPeriod distributionPeriod = DistributionPeriod.MONTHLY;

    @Column(nullable = false)
    private Boolean enabled = Boolean.TRUE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
