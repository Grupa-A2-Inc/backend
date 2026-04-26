package org.elearning.backend.ai.repository;

import org.elearning.backend.ai.model.AdaptiveSessionExercise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdaptiveSessionExerciseRepository extends JpaRepository<AdaptiveSessionExercise, UUID> {
	List<AdaptiveSessionExercise> findAllBySessionId(UUID sessionId);
}
