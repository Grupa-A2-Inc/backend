package org.elearning.backend.assessment.repository;

import org.elearning.backend.assessment.model.AttemptStatus;
import org.elearning.backend.assessment.model.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, UUID> {

    List<TestResult> findByStudentIdAndTestIdOrderByAttemptStartedAtDesc(UUID studentId, UUID testId);

    Optional<TestResult> findTopByStudentIdAndTestIdAndAttemptStatusOrderByScorePercentDesc(UUID studentId, UUID testId, AttemptStatus status);

    @Query("""
        SELECT t.lessonId FROM TestResult tr
        JOIN tr.attempt ta
        JOIN ta.test t
        WHERE ta.studentId = :studentId
          AND t.lessonId IN :lessonIds
          AND tr.passed = true
    """)
    Set<UUID> findPassedLessonIds(@Param("studentId") UUID studentId,
                                  @Param("lessonIds") List<UUID> lessonIds);
}