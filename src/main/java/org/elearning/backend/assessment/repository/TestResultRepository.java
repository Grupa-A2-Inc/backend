package org.elearning.backend.assessment.repository;

import org.elearning.backend.analytics.dto.*;
import org.elearning.backend.assessment.model.AttemptStatus;
import org.elearning.backend.assessment.model.Test;
import org.elearning.backend.assessment.model.TestResult;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;



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

    @Query(value = """
            SELECT new org.elearning.backend.analytics.dto.ClassAverageDto (
            tr.test.id,
            tr.test.title,
            CAST(COUNT(tr) as integer),
            CAST(SUM(CASE WHEN tr.passed = true THEN 1 ELSE 0 END) as integer),
            CAST(SUM(CASE WHEN tr.passed = false THEN 1 ELSE 0 END) as integer),
            CAST(AVG(tr.scorePercent) AS bigdecimal),
            CAST(MIN(tr.scorePercent) AS bigdecimal),
            CAST(MAX(tr.scorePercent) AS bigdecimal),
            (CASE WHEN COUNT(tr) = 0 THEN CAST(null AS bigdecimal)
            ELSE (SUM(CASE WHEN tr.passed = false THEN 1 ELSE 0 END) * 100.0 / COUNT(tr))
            END))
            FROM TestResult tr
            WHERE tr.test = :test
            """)
    ClassAverageDto getClassAverages(@Param("test") Test test);

    @Query(value = """
            SELECT new org.elearning.backend.analytics.dto.StudentAverageDto(tr.studentId,
            CAST(AVG(tr.scorePercent) AS bigdecimal),
            CAST(MIN(tr.scorePercent) AS bigdecimal),
            CAST(MAX(tr.scorePercent) AS bigdecimal),
            CAST(COUNT(tr) AS integer),
            CAST(SUM(CASE WHEN tr.passed = true THEN 1 ELSE 0 END) AS INTEGER ),
            CAST(SUM(CASE WHEN tr.passed = false THEN 1 ELSE 0 END) AS integer),
            MIN(tr.completedAt))
            FROM TestResult tr
            JOIN Test t ON t.id = tr.test.id
            JOIN Lesson l ON l.id = t.lessonId
            JOIN Chapter ch ON l.chapter.id = ch.id
            JOIN Course c ON c.id = ch.course.id
            WHERE c.id = :course_id
            GROUP BY tr.studentId
            """,

            countQuery = """
                SELECT COUNT( DISTINCT tr.studentId) FROM TestResult tr
                JOIN Test t ON t.id = tr.test.id
                JOIN Lesson l ON l.id = t.lessonId
                JOIN Chapter ch ON l.chapter.id = ch.id
                JOIN Course c ON c.id = ch.course.id
                WHERE c.id = :course_id
                """
    )
    Page<StudentAverageDto> getStudentAverages(@Param("course_id") UUID courseId, Pageable pageable);



    @Query( value = """
           SELECT new org.elearning.backend.analytics.dto.MyPersonalTestStatsDto(
           tr.test.id,
           tr.test.title,
           CAST(COUNT(tr) AS integer),
           MIN(tr.scorePercent),
           MAX(tr.scorePercent),
           AVG(tr.scorePercent)
           )
           FROM TestResult tr
           WHERE tr.studentId = :student_id
           AND tr.test = :test
           """
    )
    MyPersonalTestStatsDto getMyPersonalTestStats(@Param("student_id") UUID studentId,
                                                  @Param("test") Test test);


    /** Returns the latest TestResult Entity */

    TestResult findTopByStudentIdAndTestOrderByCompletedAtDesc(UUID studentId, Test test);

    @Query( value = """
           SELECT new org.elearning.backend.analytics.dto.MyClassTestAverageDto(
           AVG(tr.scorePercent),
           CAST(COUNT(tr) as integer)
           )
           FROM TestResult tr
           WHERE tr.test = :test
           """
    )
    MyClassTestAverageDto getMyClassAverageStats(@Param("test") Test test);

    @Query( value = """
            SELECT new org.elearning.backend.analytics.dto.MyClassTestBestResultsDto
            (tr.studentId,
            MAX(tr.scorePercent))
            FROM TestResult tr
            WHERE tr.test = :test
            GROUP BY tr.studentId
            """)
    List<MyClassTestBestResultsDto> getAllByTestOrderByScorePercentAsc(Test test);




}