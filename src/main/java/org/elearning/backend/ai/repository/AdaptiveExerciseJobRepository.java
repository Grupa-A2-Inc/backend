package org.elearning.backend.ai.repository;

import org.elearning.backend.ai.model.AdaptiveExerciseJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdaptiveExerciseJobRepository extends JpaRepository<AdaptiveExerciseJob, UUID> {
    Optional<AdaptiveExerciseJob> findByIdAndStudentId(UUID id, UUID studentId);
}
