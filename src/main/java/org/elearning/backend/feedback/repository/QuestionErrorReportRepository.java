package org.elearning.backend.feedback.repository;

import org.elearning.backend.feedback.model.QuestionErrorReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface QuestionErrorReportRepository extends JpaRepository<QuestionErrorReport, UUID> {

}
