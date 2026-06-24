package org.elearning.backend.reward.service;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.reward.dto.RewardConfigRequest;
import org.elearning.backend.reward.dto.RewardConfigResponse;
import org.elearning.backend.reward.entity.OrganizationRewardConfig;
import org.elearning.backend.reward.exception.RewardBadRequestException;
import org.elearning.backend.reward.exception.RewardNotFoundException;
import org.elearning.backend.reward.repository.OrganizationRewardConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RewardConfigService {

    public static final BigDecimal FIXED_REWARD_PERCENT = BigDecimal.valueOf(10).setScale(2);

    private final OrganizationRewardConfigRepository rewardConfigRepository;
    private final OrganizationRepository organizationRepository;

    @Transactional
    public RewardConfigResponse upsertConfig(UUID organizationId, RewardConfigRequest request) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new RewardNotFoundException("Organization not found: " + organizationId);
        }
        validateRequest(request);

        OrganizationRewardConfig config = rewardConfigRepository.findByOrganizationId(organizationId)
                .orElseGet(OrganizationRewardConfig::new);
        config.setOrganizationId(organizationId);
        config.setRewardPercent(FIXED_REWARD_PERCENT);
        config.setMinimumScore(request.getMinimumScore());
        config.setMaximumWinners(request.getMaximumWinners());
        config.setDistributionPeriod(request.getDistributionPeriod());
        config.setEnabled(request.getEnabled());

        return toResponse(rewardConfigRepository.save(config));
    }

    @Transactional(readOnly = true)
    public OrganizationRewardConfig getEnabledConfigEntity(UUID organizationId) {
        OrganizationRewardConfig config = rewardConfigRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new RewardNotFoundException("Reward config not found for organization: " + organizationId));
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            throw new RewardBadRequestException("Reward config is disabled for organization: " + organizationId);
        }
        validateConfig(config);
        return config;
    }

    @Transactional(readOnly = true)
    public RewardConfigResponse getConfig(UUID organizationId) {
        return rewardConfigRepository.findByOrganizationId(organizationId)
                .map(this::toResponse)
                .orElseThrow(() -> new RewardNotFoundException("Reward config not found for organization: " + organizationId));
    }

    private void validateRequest(RewardConfigRequest request) {
        if (request.getMinimumScore() == null
                || request.getMinimumScore().compareTo(BigDecimal.ZERO) < 0
                || request.getMinimumScore().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new RewardBadRequestException("Minimum score must be between 0 and 100");
        }
        if (request.getMaximumWinners() == null || request.getMaximumWinners() <= 0) {
            throw new RewardBadRequestException("Maximum winners must be greater than zero");
        }
        if (request.getDistributionPeriod() == null) {
            throw new RewardBadRequestException("Distribution period is required");
        }
        if (request.getEnabled() == null) {
            throw new RewardBadRequestException("Enabled flag is required");
        }
    }

    private void validateConfig(OrganizationRewardConfig config) {
        if (config.getRewardPercent() == null
                || config.getRewardPercent().compareTo(FIXED_REWARD_PERCENT) != 0) {
            throw new RewardBadRequestException("Reward percent must be fixed at 10");
        }
        if (config.getMaximumWinners() == null || config.getMaximumWinners() <= 0) {
            throw new RewardBadRequestException("Maximum winners must be greater than zero");
        }
    }

    private RewardConfigResponse toResponse(OrganizationRewardConfig config) {
        return new RewardConfigResponse(
                config.getId(),
                config.getOrganizationId(),
                config.getRewardPercent(),
                config.getMinimumScore(),
                config.getMaximumWinners(),
                config.getDistributionPeriod(),
                config.getEnabled(),
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }
}
