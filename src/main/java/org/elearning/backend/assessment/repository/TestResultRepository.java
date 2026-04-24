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


    /**
     * Get statistic data about the class average of a given test. Some parameters will be null if nobody took the test,
     * it's best the returned value is checked to not be null when used.
     * @param test the given test we want the class stats of
     * @return the test id, test title, the total count of results, the number of passed/failed tests, the average score,
     *  the lowest score, the best score and failure rate, inside a ClassAverageDto
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
     * Gets the student data of a given course. The given data are limited by the pageable parameter, that limits the amount
     *  of data the professor gets, making sure the data can be split in multiple pages
     * @param courseId the course the data is extracted from
     * @param pageable the page settings (e.g. third page with 20 students)
     * @return a page with a certain amount of students, containing the student ID, their average/lowest/best score,
     * total test count, how many were passed, how many were failed and day of their latest test attempt
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
     * Gets the personal stats of a student regarding a test
     * @param studentId the id of the student
     * @param test the test the student wants data from
     * @return the test id, test title, total amount of attempts, worst score, best score, average score from a given test,
     *  saved in MyPersonalTestStatsDto Object
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


    /** Returns the latest TestResult Entity */

    TestResult findTopByStudentIdAndTestOrderByCompletedAtDesc(UUID studentId, Test test);

    /**
     * Gets the average results of a class from a given test. Used as a point of comparison for the student.
     * Note that a test cannot be associated with multiple courses, ensuring the data comes from the same class
     * the student is part of
     * @param test the test of which we want the data from
     * @return score average and total number of tests, saved inside a MyClassTestAverageDto Object
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
     * Gets every best result of each student from a class at a given test, ordered from best to worst
     * Used in StudentsStatsService to determine the rank of a student
     * @param test the test we extract the data from
     * @return a MyClassTestBestResultsDto list containing each student's id and best score in terms of percentage
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
     * Gets the last few attempts of a student at a given course. The number of attempts is determined by the pageable
     * variable
     * @param studentId the student looking for the data
     * @param courseId the course the student is getting the data from
     * @param pageable determines how many of the last attempts the student did should be returned.
     * @return a list of AttemptDetailsDto Objects, containing the id of each attempt, each id of the test, each title of the
     * test, each score, each score percentage, if it passed and moment of completion
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
     * Gets the title of a course and how many tests were taken in total
     * @param courseId the course the data is taken from
     * @return the title of a course and how many tests were taken in total, in a CourseDetailsDto Object
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
     * Returns the course details of a given student
     * @param studentId the student for whom we get the data from
     * @param courseId the course where the data is extracted from
     * @return a CourseStatsDto Object containing the total amount of tests taken by a student, how many wee passed,
     * the best, worst and average score from a course
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


    @Query(value = """
        SELECT DISTINCT ON (student_id) * FROM test_results
        WHERE test_id = :testId
        ORDER BY student_id, score DESC, completed_at DESC
        """, nativeQuery = true)
    List<TestResult> findBestAttemptsByTestId(@Param("testId") UUID testId);


}

