package org.elearning.backend.assessment.repository;


import org.elearning.backend.analytics.dto.statistics.entity.CourseDetailsDto;
import org.elearning.backend.analytics.dto.statistics.entity.CourseStatsDto;
import org.elearning.backend.analytics.dto.statistics.student.MyClassTestAverageDto;
import org.elearning.backend.analytics.dto.statistics.student.MyClassTestBestResultsDto;
import org.elearning.backend.analytics.dto.statistics.student.MyPersonalTestStatsDto;
import org.elearning.backend.analytics.dto.statistics.teacher.ClassAverageDto;
import org.elearning.backend.analytics.dto.statistics.teacher.StudentAverageDto;
import org.elearning.backend.assessment.dto.attempt_dto.AttemptDetailsDto;
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

    boolean existsByTestId(UUID testId);

    List<TestResult> findByStudentIdAndTestIdOrderByAttemptStartedAtDesc(UUID studentId, UUID testId);

    @Query("""
        SELECT tr
        FROM TestResult tr
        JOIN tr.test t
        WHERE tr.studentId = :studentId
          AND t.lessonId = :lessonId
        ORDER BY tr.attempt.startedAt DESC
    """)
    List<TestResult> findByStudentIdAndLessonIdOrderByAttemptStartedAtDesc(@Param("studentId") UUID studentId,
                                                                           @Param("lessonId") UUID lessonId);

    Optional<TestResult> findTopByStudentIdAndTestIdAndAttemptStatusOrderByScorePercentDesc(UUID studentId, UUID testId, AttemptStatus status);

    /**
                                   * Retrieve the lesson IDs (limited to the provided list) for which the given student has a passing test attempt.
                                   *
                                   * @param studentId the UUID of the student whose attempts are evaluated
                                   * @param lessonIds the list of lesson UUIDs to consider
                                   * @return a set of lesson UUIDs from {@code lessonIds} where the student has at least one TestResult with {@code passed = true}
                                   */
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


    /**
     * Produces aggregated class-level statistics for the specified test.
     *
     * @param test the test to aggregate results for
     * @return a ClassAverageDto containing the test id and title, total result count, passed count, failed count,
     *         average score percent, minimum score percent, maximum score percent, and failure rate;
     *         the failure rate is `null` when no results exist
     */
    @Query(value = """
            SELECT new org.elearning.backend.analytics.dto.statistics.teacher.ClassAverageDto (
            tr.test.id,
            tr.test.title,
            CAST(COALESCE(COUNT(tr), 0) as integer),
            CAST(COALESCE(SUM(CASE WHEN tr.passed = true THEN 1 ELSE 0 END), 0) as integer),
            CAST(COALESCE(SUM(CASE WHEN tr.passed = false THEN 1 ELSE 0 END), 0) as integer),
            CAST(COALESCE(AVG(tr.scorePercent), 0) AS bigdecimal),
            CAST(COALESCE(MIN(tr.scorePercent), 0) AS bigdecimal),
            CAST(COALESCE(MAX(tr.scorePercent),0) AS bigdecimal),
            (CASE WHEN COUNT(tr) = 0 THEN CAST(null AS bigdecimal)
            ELSE (SUM(CASE WHEN tr.passed = false THEN 1 ELSE 0 END) * 100.0 / COUNT(tr))
            END))
            FROM TestResult tr
            WHERE tr.test = :test
            GROUP BY tr.test.id, tr.test.title
            """)
    ClassAverageDto getClassAverages(@Param("test") Test test);


    /**
     * Retrieve per-student score aggregates for a course as a pageable list.
     *
     * @param courseId the UUID of the course to aggregate results for
     * @param pageable pagination and sorting settings for the result page
     * @return a page of StudentAverageDto where each entry contains the student ID, average score percent,
     *         minimum score percent, maximum score percent, total test count, passed count, failed count,
     *         and the timestamp of the student's most recent test attempt
     */
    @Query(value = """
            SELECT new org.elearning.backend.analytics.dto.statistics.teacher.StudentAverageDto(tr.studentId,
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


    /**
                                                   * Retrieve aggregated personal statistics for a student on a specific test.
                                                   *
                                                   * @param studentId UUID of the student whose attempts will be aggregated
                                                   * @param test      the Test entity to filter attempts by
                                                   * @return          a MyPersonalTestStatsDto containing the test id, test title, total attempt count,
                                                   *                  minimum scorePercent, maximum scorePercent, and average scorePercent
                                                   */

    @Query( value = """
           SELECT new org.elearning.backend.analytics.dto.statistics.student.MyPersonalTestStatsDto(
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
           GROUP BY tr.test.id, tr.test.title
           """
    )
    MyPersonalTestStatsDto getMyPersonalTestStats(@Param("student_id") UUID studentId,
                                                  @Param("test") Test test);


    /**
 * Fetches the most recent TestResult for the given student and test.
 *
 * @param studentId UUID of the student
 * @param test      the Test entity
 * @return the latest TestResult for the student and test, or `null` if none exists
 */

    TestResult findTopByStudentIdAndTestOrderByCompletedAtDesc(UUID studentId, Test test);

    /**
     * Compute class-level statistics for a specific test: the average score percent and the total number of attempts.
     *
     * @param test the test to aggregate results for
     * @return a MyClassTestAverageDto containing the average score percent (may be `null` if there are no results) and the total number of attempts as an integer
     */

    @Query( value = """
           SELECT new org.elearning.backend.analytics.dto.statistics.student.MyClassTestAverageDto(
           AVG(tr.scorePercent),
           CAST(COUNT(tr) as integer)
           )
           FROM TestResult tr
           WHERE tr.test = :test
           """
    )
    MyClassTestAverageDto getMyClassAverageStats(@Param("test") Test test);


    /**
     * Aggregate each student's highest score percent for the given test and order the results from highest to lowest.
     *
     * @param test the Test entity to aggregate results for
     * @return a list of DTOs, each containing a student's id and their best score percent, ordered from highest to lowest
     */
    @Query( value = """
            SELECT new org.elearning.backend.analytics.dto.statistics.student.MyClassTestBestResultsDto
            (tr.studentId,
            MAX(tr.scorePercent))
            FROM TestResult tr
            WHERE tr.test = :test
            GROUP BY tr.studentId
            ORDER BY MAX(tr.scorePercent) DESC
            """)
    List<MyClassTestBestResultsDto> getAllTestsOrderByBestScoreDesc(Test test);

    /**
                                             * Retrieve the most recent test attempts by a student within a course.
                                             *
                                             * @param studentId the student's UUID
                                             * @param courseId  the course's UUID
                                             * @param pageable  paging/sorting information that controls how many attempts are returned
                                             * @return a list of AttemptDetailsDto containing attempt id, test id, test title, score, score percent, passed flag, and completion time, ordered by completion time descending
                                             */
    @Query(value = """
        SELECT new org.elearning.backend.assessment.dto.attempt_dto.AttemptDetailsDto(
        tr.attemptId,
        t.id,
        t.title,
        tr.score,
        tr.scorePercent,
        tr.passed,
        tr.completedAt
         )
        FROM TestResult tr
        JOIN tr.test t
        JOIN Lesson l ON l.id = t.lessonId
        JOIN l.chapter ch
        JOIN ch.course c
        WHERE tr.studentId = :studentId
        AND c.id = :courseId
        ORDER BY tr.completedAt DESC
        """)
    List<AttemptDetailsDto> getLastAttempts(@Param("studentId") UUID studentId,
                                            @Param("courseId") UUID courseId,
                                            Pageable pageable);

    /**
     * Fetches a course's title and the total number of tests taken for that course.
     *
     * @param courseId the UUID of the course to query
     * @return a CourseDetailsDto containing the course title and total tests taken as an integer
     */
    @Query(value = """
        SELECT new org.elearning.backend.analytics.dto.statistics.entity.CourseDetailsDto(
        c.title,
        CAST(COUNT(DISTINCT tr) as integer)
        )
        FROM TestResult tr
        RIGHT JOIN tr.test t
        RIGHT JOIN Lesson l ON l.id = t.lessonId
        RIGHT JOIN l.chapter ch
        RIGHT JOIN ch.course c
        WHERE c.id = :courseId
        GROUP BY c.title
        """)
    CourseDetailsDto getCourseDetails(@Param("courseId") UUID courseId);

    /**
                                   * Compute aggregated test statistics for a student within a specific course.
                                   *
                                   * @param studentId UUID of the student whose statistics are requested
                                   * @param courseId UUID of the course to aggregate statistics from
                                   * @return a CourseStatsDto with total tests taken, passed count, maximum score percent, minimum score percent, and average score percent
                                   */
    @Query(value = """
          SELECT new org.elearning.backend.analytics.dto.statistics.entity.CourseStatsDto(
              CAST(COALESCE(COUNT(tr), 0) AS integer),
              CAST(COALESCE(SUM(CASE WHEN tr.passed = true THEN 1 ELSE 0 END), 0) AS INTEGER),
              MAX(tr.scorePercent),
              MIN(tr.scorePercent),
              AVG(tr.scorePercent)
              )
        FROM TestResult tr
        JOIN tr.test t
        JOIN Lesson l ON l.id = t.lessonId
        JOIN l.chapter ch
        JOIN ch.course c
        WHERE tr.studentId = :studentId
        AND c.id = :courseId
    """)
    CourseStatsDto getCourseStats(@Param("studentId") UUID studentId,
                                  @Param("courseId") UUID courseId);


    /**
     * Fetches each student's best attempt for the specified test.
     *
     * <p>For each student who has attempts for the given test, returns a single TestResult representing
     * that student's highest-scoring attempt; when scores are tied, the most recently completed attempt
     * is chosen.</p>
     *
     * @param testId the UUID of the test to query
     * @return a list of TestResult objects containing one best attempt per student for the given test
     */
    @Query(value = """
        SELECT DISTINCT ON (student_id) * FROM test_results
        WHERE test_id = :testId
        ORDER BY student_id, score DESC, completed_at DESC
        """, nativeQuery = true)
    List<TestResult> findBestAttemptsByTestId(@Param("testId") UUID testId);


}
