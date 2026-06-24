package org.elearning.backend.reward.repository;

import java.math.BigDecimal;
import java.util.UUID;

public interface RewardCandidateProjection {
    UUID getStudentId();

    String getWalletAddress();

    Integer getCompletedLessons();

    Integer getSolvedExercises();

    BigDecimal getAverageScore();

    BigDecimal getCompletionRate();

    Integer getActivityCount();
}
