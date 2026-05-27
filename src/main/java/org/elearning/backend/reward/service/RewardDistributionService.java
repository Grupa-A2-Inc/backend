package org.elearning.backend.reward.service;

import com.testifyai.crypto.dto.MintRewardsRequest;
import com.testifyai.crypto.dto.RewardStatsResponse;
import com.testifyai.crypto.dto.StudentRewardRequest;
import com.testifyai.crypto.service.TaiRewardBlockchainService;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.reward.dto.CalculateRewardCycleRequest;
import org.elearning.backend.reward.dto.RewardCycleResponse;
import org.elearning.backend.reward.dto.StudentRewardCandidate;
import org.elearning.backend.reward.dto.StudentRewardResponse;
import org.elearning.backend.reward.entity.OrganizationRewardConfig;
import org.elearning.backend.reward.entity.RewardCycle;
import org.elearning.backend.reward.entity.RewardCycleStatus;
import org.elearning.backend.reward.entity.StudentReward;
import org.elearning.backend.reward.entity.StudentRewardStatus;
import org.elearning.backend.reward.exception.RewardBadRequestException;
import org.elearning.backend.reward.exception.RewardConflictException;
import org.elearning.backend.reward.exception.RewardNotFoundException;
import org.elearning.backend.reward.repository.RewardCycleRepository;
import org.elearning.backend.reward.repository.StudentRewardRepository;
import org.elearning.backend.subscription.entity.OrganizationSubscription;
import org.elearning.backend.subscription.entity.OrganizationSubscriptionStatus;
import org.elearning.backend.subscription.repository.OrganizationSubscriptionRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RewardDistributionService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final List<OrganizationSubscriptionStatus> ACTIVE_STATUSES = List.of(
            OrganizationSubscriptionStatus.ACTIVE,
            OrganizationSubscriptionStatus.TRIALING
    );

    private final OrganizationRepository organizationRepository;
    private final OrganizationSubscriptionRepository organizationSubscriptionRepository;
    private final RewardConfigService rewardConfigService;
    private final RewardCandidateService rewardCandidateService;
    private final RewardCycleRepository rewardCycleRepository;
    private final StudentRewardRepository studentRewardRepository;
    private final ObjectProvider<TaiRewardBlockchainService> taiRewardBlockchainServiceProvider;

    @Transactional
    public RewardCycleResponse calculateCycle(UUID organizationId, CalculateRewardCycleRequest request) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new RewardNotFoundException("Organization not found: " + organizationId);
        }

        OrganizationRewardConfig config = rewardConfigService.getEnabledConfigEntity(organizationId);
        OrganizationSubscription subscription = getActiveSubscription(organizationId);

        LocalDateTime periodStart = request.getPeriodStart() != null
                ? request.getPeriodStart()
                : subscription.getCurrentPeriodStart();
        LocalDateTime periodEnd = request.getPeriodEnd() != null
                ? request.getPeriodEnd()
                : subscription.getCurrentPeriodEnd();
        validatePeriod(periodStart, periodEnd);

        RewardCycle cycle = rewardCycleRepository.findByOrganizationIdAndPeriodStartAndPeriodEnd(
                        organizationId,
                        periodStart,
                        periodEnd
                )
                .orElseGet(RewardCycle::new);

        BigDecimal subscriptionAmount = request.getSubscriptionAmount() != null
                ? request.getSubscriptionAmount()
                : cycle.getSubscriptionAmount() != null
                ? cycle.getSubscriptionAmount()
                : subscription.getSubscriptionPlan().getPriceMonthly();
        if (subscriptionAmount == null) {
            throw new RewardBadRequestException("Subscription amount is required when the plan has no monthly price");
        }

        BigDecimal rewardPoolAmount = calculateRewardPoolAmount(subscriptionAmount);
        BigDecimal eurcDepositedAmount = request.getEurcDepositedAmount() != null
                ? request.getEurcDepositedAmount().setScale(6, RoundingMode.DOWN)
                : cycle.getEurcDepositedAmount() != null
                ? cycle.getEurcDepositedAmount().setScale(6, RoundingMode.DOWN)
                : rewardPoolAmount;

        if (cycle.getId() != null && RewardCycleStatus.MINTED.equals(cycle.getStatus())) {
            throw new RewardConflictException("Reward cycle was already minted");
        }
        if (cycle.getId() != null && studentRewardRepository.existsByRewardCycleIdAndStatus(cycle.getId(), StudentRewardStatus.MINTED)) {
            throw new RewardConflictException("Reward cycle has minted student rewards");
        }

        cycle.setOrganizationId(organizationId);
        cycle.setPeriodStart(periodStart);
        cycle.setPeriodEnd(periodEnd);
        cycle.setSubscriptionAmount(subscriptionAmount.setScale(2, RoundingMode.HALF_UP));
        cycle.setRewardPoolAmount(rewardPoolAmount);
        cycle.setEurcDepositedAmount(eurcDepositedAmount);
        cycle.setStatus(RewardCycleStatus.CALCULATED);
        cycle.setFailureReason(null);
        cycle = rewardCycleRepository.save(cycle);

        studentRewardRepository.deleteAllByRewardCycleId(cycle.getId());

        List<StudentRewardCandidate> candidates = rewardCandidateService.calculateRankedCandidates(
                organizationId,
                periodStart,
                periodEnd,
                config.getMinimumScore(),
                config.getMaximumWinners()
        );
        List<StudentRewardCandidate> distributedCandidates = distributeRewards(candidates, rewardPoolAmount, eurcDepositedAmount);

        UUID rewardCycleId = cycle.getId();
        List<StudentReward> rewards = distributedCandidates.stream()
                .filter(candidate -> candidate.getCalculatedRewardAmount().compareTo(BigDecimal.ZERO) > 0)
                .map(candidate -> toStudentReward(rewardCycleId, candidate))
                .toList();
        studentRewardRepository.saveAll(rewards);

        return toCycleResponse(cycle, rewards);
    }

    @Transactional
    public RewardCycleResponse mintCycle(UUID cycleId) {
        RewardCycle cycle = rewardCycleRepository.findById(cycleId)
                .orElseThrow(() -> new RewardNotFoundException("Reward cycle not found: " + cycleId));
        if (RewardCycleStatus.MINTED.equals(cycle.getStatus())) {
            throw new RewardConflictException("Reward cycle was already minted");
        }

        List<StudentReward> rewards = studentRewardRepository.findAllByRewardCycleIdOrderByRankAsc(cycleId);
        if (rewards.isEmpty()) {
            throw new RewardBadRequestException("Reward cycle has no calculated student rewards");
        }

        BigDecimal totalRewards = rewards.stream()
                .map(StudentReward::getRewardAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(6, RoundingMode.DOWN);

        TaiRewardBlockchainService blockchainService = taiRewardBlockchainServiceProvider.getIfAvailable();
        if (blockchainService == null) {
            failCycle(cycle, rewards, "Crypto reward blockchain service is not configured");
            return toCycleResponse(cycle, rewards);
        }

        RewardStatsResponse stats = blockchainService.getRewardStats();
        if (stats.getAvailableToMint().compareTo(totalRewards) < 0) {
            failCycle(cycle, rewards, "Insufficient EURC backing available to mint rewards");
            return toCycleResponse(cycle, rewards);
        }

        MintRewardsRequest request = new MintRewardsRequest();
        request.setRewards(rewards.stream()
                .map(this::toStudentRewardRequest)
                .toList());

        try {
            TransactionReceipt receipt = blockchainService.mintRewards(request);
            String txHash = receipt.getTransactionHash();
            rewards.forEach(reward -> {
                reward.setStatus(StudentRewardStatus.MINTED);
                reward.setTxHash(txHash);
            });
            cycle.setStatus(RewardCycleStatus.MINTED);
            cycle.setMintTxHash(txHash);
            cycle.setFailureReason(null);
            studentRewardRepository.saveAll(rewards);
            rewardCycleRepository.save(cycle);
            return toCycleResponse(cycle, rewards);
        } catch (RuntimeException exception) {
            failCycle(cycle, rewards, exception.getMessage());
            return toCycleResponse(cycle, rewards);
        }
    }

    @Transactional(readOnly = true)
    public RewardCycleResponse getCycle(UUID cycleId) {
        RewardCycle cycle = rewardCycleRepository.findById(cycleId)
                .orElseThrow(() -> new RewardNotFoundException("Reward cycle not found: " + cycleId));
        return toCycleResponse(cycle, studentRewardRepository.findAllByRewardCycleIdOrderByRankAsc(cycleId));
    }

    @Transactional(readOnly = true)
    public RewardCycleResponse getLatestCycleForOrganization(UUID organizationId) {
        RewardCycle cycle = rewardCycleRepository.findFirstByOrganizationIdOrderByPeriodEndDesc(organizationId)
                .orElseThrow(() -> new RewardNotFoundException("Reward cycle not found for organization: " + organizationId));
        return toCycleResponse(cycle, studentRewardRepository.findAllByRewardCycleIdOrderByRankAsc(cycle.getId()));
    }

    @Transactional(readOnly = true)
    public List<StudentRewardResponse> getStudentRewardHistory(UUID studentId) {
        return studentRewardRepository.findAllByStudentIdOrderByCreatedAtDesc(studentId)
                .stream()
                .map(this::toStudentRewardResponse)
                .toList();
    }

    public BigDecimal calculateRewardPoolAmount(BigDecimal subscriptionAmount) {
        if (subscriptionAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new RewardBadRequestException("Subscription amount must not be negative");
        }
        return subscriptionAmount
                .multiply(RewardConfigService.FIXED_REWARD_PERCENT)
                .divide(ONE_HUNDRED, 6, RoundingMode.DOWN);
    }

    public List<StudentRewardCandidate> distributeRewards(
            List<StudentRewardCandidate> candidates,
            BigDecimal rewardPoolAmount,
            BigDecimal eurcDepositedAmount
    ) {
        BigDecimal distributableAmount = rewardPoolAmount.min(eurcDepositedAmount).setScale(6, RoundingMode.DOWN);
        if (distributableAmount.compareTo(BigDecimal.ZERO) <= 0 || candidates.isEmpty()) {
            return List.of();
        }

        BigDecimal totalScore = candidates.stream()
                .map(StudentRewardCandidate::getFinalScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalScore.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }

        List<StudentRewardCandidate> distributed = new ArrayList<>();
        BigDecimal distributedAmount = BigDecimal.ZERO.setScale(6, RoundingMode.DOWN);

        for (StudentRewardCandidate candidate : candidates) {
            BigDecimal amount = distributableAmount
                    .multiply(candidate.getFinalScore())
                    .divide(totalScore, 6, RoundingMode.DOWN);
            distributedAmount = distributedAmount.add(amount);
            distributed.add(new StudentRewardCandidate(
                    candidate.getStudentId(),
                    candidate.getWalletAddress(),
                    candidate.getFinalScore(),
                    candidate.getRank(),
                    amount
            ));
        }

        BigDecimal remainder = distributableAmount.subtract(distributedAmount).setScale(6, RoundingMode.DOWN);
        if (remainder.compareTo(BigDecimal.ZERO) > 0 && !distributed.isEmpty()) {
            StudentRewardCandidate first = distributed.get(0);
            distributed.set(0, new StudentRewardCandidate(
                    first.getStudentId(),
                    first.getWalletAddress(),
                    first.getFinalScore(),
                    first.getRank(),
                    first.getCalculatedRewardAmount().add(remainder).setScale(6, RoundingMode.DOWN)
            ));
        }

        return distributed;
    }

    private OrganizationSubscription getActiveSubscription(UUID organizationId) {
        return organizationSubscriptionRepository
                .findFirstByOrganizationIdAndStatusInOrderByCurrentPeriodEndDesc(organizationId, ACTIVE_STATUSES)
                .orElseThrow(() -> new RewardBadRequestException("Organization does not have an active subscription"));
    }

    private void validatePeriod(LocalDateTime periodStart, LocalDateTime periodEnd) {
        if (periodStart == null || periodEnd == null) {
            throw new RewardBadRequestException("Reward period start and end are required");
        }
        if (!periodEnd.isAfter(periodStart)) {
            throw new RewardBadRequestException("Reward period end must be after start");
        }
    }

    private StudentReward toStudentReward(UUID cycleId, StudentRewardCandidate candidate) {
        StudentReward reward = new StudentReward();
        reward.setRewardCycleId(cycleId);
        reward.setStudentId(candidate.getStudentId());
        reward.setStudentWalletAddress(candidate.getWalletAddress());
        reward.setRank(candidate.getRank());
        reward.setScore(candidate.getFinalScore());
        reward.setRewardAmount(candidate.getCalculatedRewardAmount());
        reward.setStatus(StudentRewardStatus.CALCULATED);
        return reward;
    }

    private StudentRewardRequest toStudentRewardRequest(StudentReward reward) {
        StudentRewardRequest request = new StudentRewardRequest();
        request.setStudentWalletAddress(reward.getStudentWalletAddress());
        request.setAmount(reward.getRewardAmount());
        return request;
    }

    private void failCycle(RewardCycle cycle, List<StudentReward> rewards, String reason) {
        cycle.setStatus(RewardCycleStatus.FAILED);
        cycle.setFailureReason(reason);
        rewards.forEach(reward -> reward.setStatus(StudentRewardStatus.FAILED));
        studentRewardRepository.saveAll(rewards);
        rewardCycleRepository.save(cycle);
    }

    private RewardCycleResponse toCycleResponse(RewardCycle cycle, List<StudentReward> rewards) {
        return new RewardCycleResponse(
                cycle.getId(),
                cycle.getOrganizationId(),
                cycle.getPeriodStart(),
                cycle.getPeriodEnd(),
                cycle.getSubscriptionAmount(),
                cycle.getRewardPoolAmount(),
                cycle.getEurcDepositedAmount(),
                cycle.getStatus(),
                cycle.getDepositTxHash(),
                cycle.getMintTxHash(),
                cycle.getFailureReason(),
                cycle.getCreatedAt(),
                cycle.getUpdatedAt(),
                rewards.stream().map(this::toStudentRewardResponse).toList()
        );
    }

    private StudentRewardResponse toStudentRewardResponse(StudentReward reward) {
        return new StudentRewardResponse(
                reward.getId(),
                reward.getRewardCycleId(),
                reward.getStudentId(),
                reward.getStudentWalletAddress(),
                reward.getRank(),
                reward.getScore(),
                reward.getRewardAmount(),
                reward.getTxHash(),
                reward.getStatus(),
                reward.getCreatedAt(),
                reward.getUpdatedAt()
        );
    }
}
