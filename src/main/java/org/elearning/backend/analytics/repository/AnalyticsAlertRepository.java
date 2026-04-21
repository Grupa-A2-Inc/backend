package org.elearning.backend.analytics.repository;

import org.elearning.backend.analytics.model.AnalyticsAlert;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnalyticsAlertRepository {
    Optional<AnalyticsAlert> findByTestId(UUID testId);
    Optional<AnalyticsAlert> findByTestIdAndIsActiveTrue(UUID testId);

     AnalyticsAlert save(AnalyticsAlert analyticsAlert);
}
