package org.elearning.backend.assessment.repository;

import org.elearning.backend.assessment.model.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {

    // Aduce toate opțiunile unei întrebări
    List<QuestionOption> findByQuestionId(Long questionId);

    // Aduce DOAR opțiunile corecte pentru o întrebare (foarte util la submitAttempt!)
    List<QuestionOption> findByQuestionIdAndIsCorrectTrue(Long questionId);
}