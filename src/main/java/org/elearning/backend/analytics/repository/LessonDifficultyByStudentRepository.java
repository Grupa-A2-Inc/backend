package org.elearning.backend.analytics.repository;

import org.elearning.backend.analytics.dto.statistics.entity.DifficultyLessonDto;
import org.elearning.backend.analytics.model.LessonDifficultyByStudent;
import org.elearning.backend.analytics.model.LessonDifficultyKey;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface LessonDifficultyByStudentRepository extends JpaRepository<LessonDifficultyByStudent, LessonDifficultyKey> {

    /**
                                                       * Retrieve a paginated list of lessons annotated with difficulty metrics for a specific student in a course.
                                                       *
                                                       * Returns lessons where the student's best score is less than the provided passing grade or the lesson gap exceeds the provided problem gap; results are ordered by `gap` descending and constrained by the supplied `Pageable`.
                                                       *
                                                       * @param courseId     the course UUID to filter lessons by
                                                       * @param studentId    the student UUID whose metrics are requested
                                                       * @param passingGrade threshold score below which a lesson is considered not passed
                                                       * @param problemGap   threshold gap above which a lesson is considered problematic
                                                       * @param pageable     paging and sorting constraints to apply to the query
                                                       * @return             a list of DifficultyLessonDto matching the criteria, ordered by gap descending
                                                       */
                                                      @Query(value = """
            SELECT new org.elearning.backend.analytics.dto.statistics.entity.DifficultyLessonDto(
             ld.id.lessonId,
             ld.lessonTitle,
             ld.myBestScore,
             ld.classAverage,
             ld.gap
             )
            FROM LessonDifficultyByStudent ld
            WHERE ld.courseId = :courseId
            AND ld.id.studentId = :studentId
            AND (ld.myBestScore < :passingGrade
            OR ld.gap > :problemGap)
            ORDER BY ld.gap DESC
        """)
    List<DifficultyLessonDto> getLessonDifficultyList(@Param("courseId") UUID courseId,
                                                      @Param("studentId") UUID studentId,
                                                      @Param("passingGrade") BigDecimal passingGrade,
                                                      @Param("problemGap") BigDecimal problemGap,
                                                      Pageable pageable);
}
