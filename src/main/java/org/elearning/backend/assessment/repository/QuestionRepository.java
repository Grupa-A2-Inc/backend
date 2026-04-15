package org.elearning.backend.assessment.repository;

import org.elearning.backend.assessment.model.Question;
import org.elearning.backend.assessment.model.QuestionType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, Integer> {

    @Query("SELECT q FROM Question q LEFT JOIN FETCH q.options WHERE q.test.id = :testId AND q.isActive = true")
    List<Question> findByTestIdWithOptions(@Param("testId") UUID testId);

    List<Question> findByTestIdAndIsActiveTrue(UUID testId);

    List<Question> findByTestId(UUID testId);

    @Query("SELECT q FROM Question q WHERE q.test.id = :testId " +
            "AND (:questionType IS NULL OR q.questionType = :questionType) " +
            "AND (:difficulty IS NULL OR q.difficulty = :difficulty)")
    List<Question> findFilteredQuestions(
            @Param("testId") UUID testId,
            @Param("questionType") QuestionType questionType,
            @Param("difficulty") BigDecimal difficulty,
            Sort sort // spring transforma asta in order by pentru query
    );
}
