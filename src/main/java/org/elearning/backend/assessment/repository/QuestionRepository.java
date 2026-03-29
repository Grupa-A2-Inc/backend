package org.elearning.backend.assessment.repository;

import org.elearning.backend.assessment.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {

    @Query("SELECT q FROM Question q LEFT JOIN FETCH q.options WHERE q.testId = :testId AND q.isActive = true")
    List<Question> findByTestIdWithOptions(@Param("testId") UUID testId);

    List<Question> findByTestIdAndIsActiveTrue(UUID testId);
}
