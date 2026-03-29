package org.elearning.backend.assessment.repository;

import org.elearning.backend.assessment.model.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestRepository extends JpaRepository<Test, UUID> {

    // Returnează Optional pentru că un lessonId are MAXIM un test
    Optional<Test> findByLessonId(UUID lessonId);
}