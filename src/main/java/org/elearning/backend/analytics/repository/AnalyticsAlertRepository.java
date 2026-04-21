package org.elearning.backend.analytics.repository;

import org.elearning.backend.analytics.model.AnalyticsAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnalyticsAlertRepository extends JpaRepository<AnalyticsAlert, UUID> {
    Optional<AnalyticsAlert> findByTestIdAndIsActiveTrue(UUID testId);
}
