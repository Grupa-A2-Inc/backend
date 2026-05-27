package org.elearning.backend.reward.repository;

import org.elearning.backend.reward.entity.OrganizationRewardConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationRewardConfigRepository extends JpaRepository<OrganizationRewardConfig, UUID> {
    Optional<OrganizationRewardConfig> findByOrganizationId(UUID organizationId);
}
