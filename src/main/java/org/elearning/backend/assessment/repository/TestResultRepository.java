package org.elearning.backend.assessment.repository;

import org.elearning.backend.assessment.model.AttemptStatus;
import org.elearning.backend.assessment.model.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, UUID> {

    // Aduce toate rezultatele unui student la un test (tot istoricul încercărilor)
    List<TestResult> findByStudentIdAndTestIdOrderByAttemptStartedAtDesc(UUID studentId, UUID testId);

    // Aduce doar CEL MAI BUN rezultat al studentului la acel test (după procentaj)
    Optional<TestResult> findTopByStudentIdAndTestIdAndAttemptStatusOrderByScorePercentDesc(UUID studentId, UUID testId, AttemptStatus status);
}