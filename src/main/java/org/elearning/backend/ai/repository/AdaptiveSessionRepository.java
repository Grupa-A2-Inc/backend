package org.elearning.backend.ai.repository;

import org.elearning.backend.ai.model.AdaptiveSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdaptiveSessionRepository extends JpaRepository<AdaptiveSession, UUID> {
	Optional<AdaptiveSession> findByIdAndStudentId(UUID id, UUID studentId);
}
