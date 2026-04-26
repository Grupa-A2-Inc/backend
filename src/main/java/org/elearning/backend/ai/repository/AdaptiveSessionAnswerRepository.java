package org.elearning.backend.ai.repository;

import org.elearning.backend.ai.model.AdaptiveSessionAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdaptiveSessionAnswerRepository extends JpaRepository<AdaptiveSessionAnswer, UUID> {
}
