package org.elearning.backend.assessment.repository;

import org.elearning.backend.assessment.model.TestAttempt;
import org.elearning.backend.assessment.model.AttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface TestAttemptRepository extends JpaRepository<TestAttempt, UUID> {

    /** Numără câte attempts are elevul pe un test — pentru attemptNumber */
    int countByTestIdAndStudentId(UUID testId, UUID studentId);

    /** Toate attempts ale elevului pe un test, ordonate descendent */
    List<TestAttempt> findByTestIdAndStudentIdOrderByStartedAtDesc(
            UUID testId, UUID studentId);

    /** Verifică dacă există attempt IN_PROGRESS pentru elev pe test */
    boolean existsByTestIdAndStudentIdAndStatus(
            UUID testId, UUID studentId, AttemptStatus status);
}
