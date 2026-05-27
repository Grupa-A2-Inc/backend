package org.elearning.backend.reward;

import org.elearning.backend.reward.dto.StudentRewardCandidate;
import org.elearning.backend.reward.repository.RewardCandidateProjection;
import org.elearning.backend.reward.repository.StudentRewardRepository;
import org.elearning.backend.reward.service.RewardCandidateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardCandidateServiceTest {

    @Mock
    private StudentRewardRepository studentRewardRepository;

    @Test
    void calculateFinalScore_usesReadableWeightedFormula() {
        RewardCandidateService service = new RewardCandidateService(studentRewardRepository);

        BigDecimal score = service.calculateFinalScore(
                BigDecimal.valueOf(80),
                BigDecimal.valueOf(50),
                4
        );

        assertThat(score).isEqualByComparingTo("67.0000");
    }

    @Test
    void calculateRankedCandidates_excludesStudentsWithoutValidWalletAndBelowThreshold() {
        RewardCandidateService service = new RewardCandidateService(studentRewardRepository);
        UUID organizationId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 1, 0, 0);

        UUID firstStudent = UUID.randomUUID();
        UUID secondStudent = UUID.randomUUID();
        UUID thirdStudent = UUID.randomUUID();

        when(studentRewardRepository.findRewardCandidateMetrics(organizationId, start, end))
                .thenReturn(List.of(
                        projection(firstStudent, "0x1111111111111111111111111111111111111111", 90, 100, 10),
                        projection(secondStudent, "not-a-wallet", 100, 100, 10),
                        projection(thirdStudent, "0x3333333333333333333333333333333333333333", 10, 10, 1)
                ));

        List<StudentRewardCandidate> candidates = service.calculateRankedCandidates(
                organizationId,
                start,
                end,
                BigDecimal.valueOf(50),
                10
        );

        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).getStudentId()).isEqualTo(firstStudent);
        assertThat(candidates.get(0).getRank()).isEqualTo(1);
    }

    @Test
    void calculateRankedCandidates_ordersByFinalScoreAndLimitsWinners() {
        RewardCandidateService service = new RewardCandidateService(studentRewardRepository);
        UUID organizationId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 1, 0, 0);

        UUID lower = UUID.randomUUID();
        UUID higher = UUID.randomUUID();

        when(studentRewardRepository.findRewardCandidateMetrics(organizationId, start, end))
                .thenReturn(List.of(
                        projection(lower, "0x1111111111111111111111111111111111111111", 70, 70, 2),
                        projection(higher, "0x2222222222222222222222222222222222222222", 95, 95, 5)
                ));

        List<StudentRewardCandidate> candidates = service.calculateRankedCandidates(
                organizationId,
                start,
                end,
                BigDecimal.ZERO,
                1
        );

        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).getStudentId()).isEqualTo(higher);
        assertThat(candidates.get(0).getRank()).isEqualTo(1);
    }

    private RewardCandidateProjection projection(
            UUID studentId,
            String walletAddress,
            int averageScore,
            int completionRate,
            int activityCount
    ) {
        return new RewardCandidateProjection() {
            @Override
            public UUID getStudentId() {
                return studentId;
            }

            @Override
            public String getWalletAddress() {
                return walletAddress;
            }

            @Override
            public Integer getCompletedLessons() {
                return activityCount;
            }

            @Override
            public Integer getSolvedExercises() {
                return activityCount;
            }

            @Override
            public BigDecimal getAverageScore() {
                return BigDecimal.valueOf(averageScore);
            }

            @Override
            public BigDecimal getCompletionRate() {
                return BigDecimal.valueOf(completionRate);
            }

            @Override
            public Integer getActivityCount() {
                return activityCount;
            }
        };
    }
}
