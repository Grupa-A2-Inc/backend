package org.elearning.backend.reward.repository;

import org.elearning.backend.reward.entity.RewardCycle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RewardCycleRepository extends JpaRepository<RewardCycle, UUID> {
    Optional<RewardCycle> findByOrganizationIdAndPeriodStartAndPeriodEnd(
            UUID organizationId,
            LocalDateTime periodStart,
            LocalDateTime periodEnd
    );

    Optional<RewardCycle> findFirstByOrganizationIdOrderByPeriodEndDesc(UUID organizationId);
}
