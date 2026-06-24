package org.elearning.backend.reward.service;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.reward.dto.StudentRewardCandidate;
import org.elearning.backend.reward.repository.RewardCandidateProjection;
import org.elearning.backend.reward.repository.StudentRewardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class RewardCandidateService {

    private static final BigDecimal EXERCISE_WEIGHT = BigDecimal.valueOf(0.6);
    private static final BigDecimal COMPLETION_WEIGHT = BigDecimal.valueOf(0.3);
    private static final BigDecimal ACTIVITY_WEIGHT = BigDecimal.valueOf(0.1);

    private final StudentRewardRepository studentRewardRepository;

    @Transactional(readOnly = true)
    public List<StudentRewardCandidate> calculateRankedCandidates(
            UUID organizationId,
            LocalDateTime periodStart,
            LocalDateTime periodEnd,
            BigDecimal minimumScore,
            int maximumWinners
    ) {
        AtomicInteger rank = new AtomicInteger(1);
        return studentRewardRepository.findRewardCandidateMetrics(organizationId, periodStart, periodEnd)
                .stream()
                .filter(candidate -> EvmAddressValidator.isValid(candidate.getWalletAddress()))
                .map(this::toCandidateWithoutReward)
                .filter(candidate -> candidate.getFinalScore().compareTo(minimumScore) >= 0)
                .sorted(Comparator.comparing(StudentRewardCandidate::getFinalScore).reversed())
                .limit(maximumWinners)
                .map(candidate -> new StudentRewardCandidate(
                        candidate.getStudentId(),
                        candidate.getWalletAddress(),
                        candidate.getFinalScore(),
                        rank.getAndIncrement(),
                        BigDecimal.ZERO.setScale(6, RoundingMode.DOWN)
                ))
                .toList();
    }

    public BigDecimal calculateFinalScore(BigDecimal averageScore, BigDecimal completionRate, int activityCount) {
        BigDecimal activityBonus = BigDecimal.valueOf(Math.min(100, activityCount * 10L));
        return averageScore.multiply(EXERCISE_WEIGHT)
                .add(completionRate.multiply(COMPLETION_WEIGHT))
                .add(activityBonus.multiply(ACTIVITY_WEIGHT))
                .setScale(4, RoundingMode.HALF_UP);
    }

    private StudentRewardCandidate toCandidateWithoutReward(RewardCandidateProjection projection) {
        BigDecimal finalScore = calculateFinalScore(
                defaultZero(projection.getAverageScore()),
                defaultZero(projection.getCompletionRate()),
                projection.getActivityCount() == null ? 0 : projection.getActivityCount()
        );
        return new StudentRewardCandidate(
                projection.getStudentId(),
                projection.getWalletAddress(),
                finalScore,
                0,
                BigDecimal.ZERO.setScale(6, RoundingMode.DOWN)
        );
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
