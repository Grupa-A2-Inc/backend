package org.elearning.backend.assessment.repository;

import org.elearning.backend.assessment.model.AttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, UUID> {

    /** Toate răspunsurile dintr-un attempt */
    List<AttemptAnswer> findByAttemptId(UUID attemptId);
}
