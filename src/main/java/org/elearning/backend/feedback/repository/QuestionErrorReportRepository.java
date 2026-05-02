package org.elearning.backend.feedback.repository;

import org.elearning.backend.feedback.dto.projections.GetErrorReportProjection;
import org.elearning.backend.feedback.model.QuestionErrorReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface QuestionErrorReportRepository extends JpaRepository<QuestionErrorReport, UUID> {


    //-------- Dev 4 --------
    /**
     * Retrieve error reports for a professor with optional filtering by status and course.
     * @param professorId
     * @param status
     * @param courseId
     * @param pageable
     * @return
     */
    @Query(
            value = "SELECT qer.*, q.content, q.source as question_source, l.title as lesson_title, co.title as course_title " +
                    "FROM question_error_reports qer " +
                    "JOIN questions q ON qer.question_id = q.id " +
                    "JOIN tests t ON q.test_id = t.id " +
                    "JOIN lessons l ON t.lesson_id = l.id " +
                    "JOIN chapters ch ON l.chapter_id = ch.id " +
                    "JOIN courses co ON ch.course_id = co.id " +
                    "WHERE co.created_by = :professorId " +
                    "AND (CAST(:status AS text) IS NULL OR qer.status = CAST(:status AS text)) " +
                    "AND (CAST(:courseId AS uuid) IS NULL OR co.id = CAST(:courseId AS uuid)) " +
                    "ORDER BY qer.created_at DESC",
            countQuery = "SELECT count(*) FROM question_error_reports qer " +
                    "JOIN questions q ON qer.question_id = q.id " +
                    "JOIN tests t ON q.test_id = t.id " +
                    "JOIN lessons l ON t.lesson_id = l.id " +
                    "JOIN chapters ch ON l.chapter_id = ch.id " +
                    "JOIN courses co ON ch.course_id = co.id " +
                    "WHERE co.created_by = :professorId " +
                    "AND (CAST(:status AS text) IS NULL OR qer.status = CAST(:status AS text)) " +
                    "AND (CAST(:courseId AS uuid) IS NULL OR co.id = CAST(:courseId AS uuid))",
            nativeQuery = true
    )
    Page<GetErrorReportProjection> findErrorReportsForProfessor(
            @Param("professorId") UUID professorId,
            @Param("status") String status,
            @Param("courseId") UUID courseId,
            Pageable pageable
    );
}
