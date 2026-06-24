package org.elearning.backend.reward;

import com.testifyai.crypto.dto.RewardStatsResponse;
import com.testifyai.crypto.service.TaiRewardBlockchainService;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.reward.dto.StudentRewardCandidate;
import org.elearning.backend.reward.entity.RewardCycle;
import org.elearning.backend.reward.entity.RewardCycleStatus;
import org.elearning.backend.reward.entity.StudentReward;
import org.elearning.backend.reward.entity.StudentRewardStatus;
import org.elearning.backend.reward.exception.RewardConflictException;
import org.elearning.backend.reward.repository.RewardCycleRepository;
import org.elearning.backend.reward.repository.StudentRewardRepository;
import org.elearning.backend.reward.service.RewardCandidateService;
import org.elearning.backend.reward.service.RewardConfigService;
import org.elearning.backend.reward.service.RewardDistributionService;
import org.elearning.backend.subscription.repository.OrganizationSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardDistributionServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationSubscriptionRepository organizationSubscriptionRepository;

    @Mock
    private RewardConfigService rewardConfigService;

    @Mock
    private RewardCandidateService rewardCandidateService;

    @Mock
    private RewardCycleRepository rewardCycleRepository;

    @Mock
    private StudentRewardRepository studentRewardRepository;

    @Mock
    private ObjectProvider<TaiRewardBlockchainService> taiRewardBlockchainServiceProvider;

    @Mock
    private TaiRewardBlockchainService taiRewardBlockchainService;

    private RewardDistributionService service;

    @BeforeEach
    void setUp() {
        service = new RewardDistributionService(
                organizationRepository,
                organizationSubscriptionRepository,
                rewardConfigService,
                rewardCandidateService,
                rewardCycleRepository,
                studentRewardRepository,
                taiRewardBlockchainServiceProvider
        );
    }

    @Test
    void calculateRewardPoolAmount_appliesFixedTenPercent() {
        BigDecimal pool = service.calculateRewardPoolAmount(BigDecimal.valueOf(250));

        assertThat(pool).isEqualByComparingTo("25.000000");
    }

    @Test
    void distributeRewards_splitsPoolProportionallyAndRoundsToSixDecimals() {
        List<StudentRewardCandidate> candidates = List.of(
                candidate(UUID.randomUUID(), "0x1111111111111111111111111111111111111111", "2.0000", 1),
                candidate(UUID.randomUUID(), "0x2222222222222222222222222222222222222222", "1.0000", 2)
        );

        List<StudentRewardCandidate> distributed = service.distributeRewards(
                candidates,
                new BigDecimal("10.000000"),
                new BigDecimal("10.000000")
        );

        assertThat(distributed).hasSize(2);
        assertThat(distributed.get(0).getCalculatedRewardAmount()).isEqualByComparingTo("6.666667");
        assertThat(distributed.get(1).getCalculatedRewardAmount()).isEqualByComparingTo("3.333333");
        BigDecimal total = distributed.stream()
                .map(StudentRewardCandidate::getCalculatedRewardAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("10.000000");
    }

    @Test
    void distributeRewards_doesNotExceedDepositedAmount() {
        List<StudentRewardCandidate> candidates = List.of(
                candidate(UUID.randomUUID(), "0x1111111111111111111111111111111111111111", "50.0000", 1),
                candidate(UUID.randomUUID(), "0x2222222222222222222222222222222222222222", "50.0000", 2)
        );

        List<StudentRewardCandidate> distributed = service.distributeRewards(
                candidates,
                new BigDecimal("10.000000"),
                new BigDecimal("7.500000")
        );

        BigDecimal total = distributed.stream()
                .map(StudentRewardCandidate::getCalculatedRewardAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("7.500000");
    }

    @Test
    void mintCycle_insufficientBackingDoesNotCallMintRewards() {
        UUID cycleId = UUID.randomUUID();
        RewardCycle cycle = cycle(cycleId, RewardCycleStatus.CALCULATED);
        List<StudentReward> rewards = List.of(reward(cycleId, new BigDecimal("10.000000")));

        when(rewardCycleRepository.findById(cycleId)).thenReturn(Optional.of(cycle));
        when(studentRewardRepository.findAllByRewardCycleIdOrderByRankAsc(cycleId)).thenReturn(rewards);
        when(taiRewardBlockchainServiceProvider.getIfAvailable()).thenReturn(taiRewardBlockchainService);
        when(taiRewardBlockchainService.getRewardStats())
                .thenReturn(new RewardStatsResponse(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        new BigDecimal("5.000000"),
                        true
                ));

        service.mintCycle(cycleId);

        verify(taiRewardBlockchainService, never()).mintRewards(org.mockito.ArgumentMatchers.any());
        assertThat(cycle.getStatus()).isEqualTo(RewardCycleStatus.FAILED);
        assertThat(rewards.get(0).getStatus()).isEqualTo(StudentRewardStatus.FAILED);
    }

    @Test
    void mintCycle_rejectsAlreadyMintedCycle() {
        UUID cycleId = UUID.randomUUID();
        RewardCycle cycle = cycle(cycleId, RewardCycleStatus.MINTED);
        when(rewardCycleRepository.findById(cycleId)).thenReturn(Optional.of(cycle));

        assertThatThrownBy(() -> service.mintCycle(cycleId))
                .isInstanceOf(RewardConflictException.class)
                .hasMessageContaining("already minted");
    }

    private StudentRewardCandidate candidate(UUID studentId, String walletAddress, String score, int rank) {
        return new StudentRewardCandidate(
                studentId,
                walletAddress,
                new BigDecimal(score),
                rank,
                BigDecimal.ZERO
        );
    }

    private RewardCycle cycle(UUID cycleId, RewardCycleStatus status) {
        RewardCycle cycle = new RewardCycle();
        cycle.setId(cycleId);
        cycle.setOrganizationId(UUID.randomUUID());
        cycle.setPeriodStart(LocalDateTime.of(2026, 5, 1, 0, 0));
        cycle.setPeriodEnd(LocalDateTime.of(2026, 6, 1, 0, 0));
        cycle.setSubscriptionAmount(new BigDecimal("100.00"));
        cycle.setRewardPoolAmount(new BigDecimal("10.000000"));
        cycle.setEurcDepositedAmount(new BigDecimal("10.000000"));
        cycle.setStatus(status);
        return cycle;
    }

    private StudentReward reward(UUID cycleId, BigDecimal amount) {
        StudentReward reward = new StudentReward();
        reward.setId(UUID.randomUUID());
        reward.setRewardCycleId(cycleId);
        reward.setStudentId(UUID.randomUUID());
        reward.setStudentWalletAddress("0x1111111111111111111111111111111111111111");
        reward.setRank(1);
        reward.setScore(new BigDecimal("95.0000"));
        reward.setRewardAmount(amount);
        reward.setStatus(StudentRewardStatus.CALCULATED);
        return reward;
    }
}
