package org.elearning.backend.analytics.repository;

import org.elearning.backend.analytics.model.AiQuestionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AiQuestionRequestRepository extends JpaRepository<AiQuestionRequest, UUID> {

}
