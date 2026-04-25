package org.elearning.backend.analytics.repository;

import org.elearning.backend.analytics.model.AdaptiveSessionExercise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdaptiveSessionExerciseRepository extends JpaRepository<AdaptiveSessionExercise, UUID> {
}
