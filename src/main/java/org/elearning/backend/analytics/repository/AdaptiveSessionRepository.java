package org.elearning.backend.analytics.repository;

import org.elearning.backend.analytics.model.AdaptiveSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdaptiveSessionRepository extends JpaRepository<AdaptiveSession, UUID> {
}
