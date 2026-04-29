package org.elearning.backend.analytics.service;

import org.elearning.backend.analytics.dto.statistics.entity.CourseDetailsDto;
import org.elearning.backend.analytics.dto.statistics.entity.CourseStatsDto;
import org.elearning.backend.analytics.dto.statistics.entity.DifficultyLessonDto;
import org.elearning.backend.analytics.dto.statistics.student.*;
import org.elearning.backend.analytics.exception.StudentNotEnrolledInCourseException;
import org.elearning.backend.analytics.repository.LessonDifficultyByStudentRepository;
import org.elearning.backend.assessment.dto.attempt_dto.AttemptDetailsDto;
import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.elearning.backend.assessment.model.Test;
import org.elearning.backend.assessment.repository.TestRepository;
import org.elearning.backend.assessment.repository.TestResultRepository;
import org.elearning.backend.content.repository.CourseRepository;
import org.elearning.backend.enrollment.repository.CourseEnrollmentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class StudentsStatsService {


    private static final String TEST_DOES_NOT_EXIST = "Test does not exist";
    private static final String COURSE_DOES_NOT_EXIST = "Course does not exist";
    private static final String STUDENT_NOT_ENROLLED = "Student is not enrolled to course";

    //Determines how many of the last attempts should be listed
    private static final Pageable LAST_ATTEMPT_COUNT = PageRequest.of(0,5);
    //Determines how many of the most problematic lessons should be listed
    private static final Pageable DIFFICULT_LESSON_COUNT = PageRequest.of(0,3);

    //Determines the gap that should be considered problematic for a student
    private static final BigDecimal PROBLEM_GAP = BigDecimal.valueOf(15);
    //Determines the percentage a student should aim for to pass a test
    private static final BigDecimal PASSING_GRADE_PERCENTAGE = BigDecimal.valueOf(60);

    private final TestRepository testRepository;
    private final TestResultRepository testResultRepository;
    private final CourseRepository courseRepository;
    private final LessonDifficultyByStudentRepository lessonDifficultyByStudentRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;

    /**
     * Constructs a StudentsStatsService with the repository dependencies it requires.
     *
     * <p>The provided repositories are retained for querying tests, test results,
     * course existence, per-student lesson difficulties, and course enrollments used
     * by the service's methods.
     */
    public StudentsStatsService(TestRepository testRepository, TestResultRepository testResultRepository, CourseRepository courseRepository, LessonDifficultyByStudentRepository lessonDifficultyByStudentRepository, CourseEnrollmentRepository courseEnrollmentRepository) {
        this.testRepository = testRepository;
        this.testResultRepository = testResultRepository;
        this.courseRepository = courseRepository;
        this.lessonDifficultyByStudentRepository = lessonDifficultyByStudentRepository;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
    }

    /**
         * Determine the 1-based rank of a student within a list of class best results.
         *
         * @param studentId the id of the student whose rank is computed
         * @param myClassBestResults list of each student's best result ordered from highest to lowest
         * @return the 1-based rank of the student; if the student is not present in the list, returns {@code myClassBestResults.size() + 1}
         */
    private int getRank(UUID studentId, List<MyClassTestBestResultsDto> myClassBestResults){
        int rank;

        for(rank=0;rank< myClassBestResults.size();rank++){
            if(myClassBestResults.get(rank).getStudentId().equals(studentId)){
                break;
            }
        }
        return rank+1;
    }

    /**
     * Calculate a student's percentile within a cohort based on their rank and the cohort size.
     *
     * @param studentRank  the 1-based rank of the student within the cohort (1 = top rank)
     * @param totalStudents the total number of students in the cohort
     * @return a BigDecimal representing the student's percentile between 0 and 100; returns `0` if `totalStudents` is 0. The value is computed as ((totalStudents - studentRank + 1) / totalStudents) * 100 and is provided with up to four decimal places.
     */

    private BigDecimal computePercentile(int studentRank, int totalStudents) {
        if (totalStudents == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = BigDecimal.valueOf(totalStudents);
        BigDecimal rank = BigDecimal.valueOf(studentRank);

        return total
                .subtract(rank)
                .add(BigDecimal.ONE)
                .divide(total, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Compute the median best-score percentage from the provided class results.
     *
     * @param myClassBestResults list of class best-result DTOs (ordered by score if caller provides such ordering)
     * @return the median of the best-score percentages; `0` if the list is empty
     */

    private BigDecimal computeMedian(List<MyClassTestBestResultsDto> myClassBestResults){
        int totalResults = myClassBestResults.size();
        if(totalResults==0){
            return BigDecimal.valueOf(0);
        }
        if(totalResults %2==1){
            return  myClassBestResults.get((totalResults /2)).getBestScorePercentage();
        }
        else{
            return myClassBestResults.get((totalResults /2)-1).getBestScorePercentage()
                    .add(myClassBestResults.get(totalResults /2).getBestScorePercentage())
                    .divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP);
        }
    }

    /**
     * Provide aggregated test statistics for a student.
     *
     * @param studentId UUID of the student
     * @param testId    UUID of the test
     * @return a MyTestStatsDto containing the student's personal test stats, the latest score, class average stats,
     *         class median, the student's rank within the class, and the percentile
     * @throws DoesNotExistException if the test identified by {@code testId} does not exist
     */

    public MyTestStatsDto getMyTestStats(UUID studentId, UUID testId){
        Test test = testRepository.findById(testId)
                .orElseThrow( () -> new DoesNotExistException(TEST_DOES_NOT_EXIST));
        
        MyPersonalTestStatsDto myPersonalTestStats = testResultRepository.getMyPersonalTestStats(studentId, test);

        BigDecimal latestScore = testResultRepository
                .findTopByStudentIdAndTestOrderByCompletedAtDesc(studentId, test).getScorePercent();

        MyClassTestAverageDto myClassAverageStats = testResultRepository.getMyClassAverageStats(test);



        List<MyClassTestBestResultsDto> myClassBestResults = new ArrayList<>(testResultRepository
                .getAllTestsOrderByBestScoreDesc(test));

        int totalResults = myClassBestResults.size();

        BigDecimal classMedian  = computeMedian(myClassBestResults);
        int rank = getRank(studentId, myClassBestResults);
        BigDecimal percentile = computePercentile(rank, totalResults);



        return new MyTestStatsDto(myPersonalTestStats,
                latestScore,
                myClassAverageStats,
                classMedian,
                rank,
                percentile);
    }

    /**
     * Assembles course-level summary data and recent activity for a student.
     *
     * @param studentId the UUID of the student
     * @param courseId  the UUID of the course
     * @return a MySummaryDataDto containing course details, the student's course statistics (counts and scores),
     *         the list of lessons where the student shows most difficulty, and the student's recent attempts
     * @throws DoesNotExistException                 if the course identified by {@code courseId} does not exist
     * @throws StudentNotEnrolledInCourseException   if the student is not enrolled in the specified course
     */

    public MySummaryDataDto getMySummaryData(UUID studentId, UUID courseId){
        if(!courseRepository.existsById(courseId)){
            throw new DoesNotExistException(COURSE_DOES_NOT_EXIST);
        }
        if(!courseEnrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)){
            throw new StudentNotEnrolledInCourseException(STUDENT_NOT_ENROLLED);
        }

        CourseDetailsDto courseDetailsDto = testResultRepository.getCourseDetails(courseId);
        CourseStatsDto courseStatsDto = testResultRepository.getCourseStats(studentId, courseId);
        List<DifficultyLessonDto> difficultyLessonDto = lessonDifficultyByStudentRepository.getLessonDifficultyList(
                        courseId,
                        studentId,
                        PASSING_GRADE_PERCENTAGE,
                        PROBLEM_GAP,
                        DIFFICULT_LESSON_COUNT);

        List<AttemptDetailsDto> lastFewAttempts = testResultRepository.getLastAttempts(studentId, courseId, LAST_ATTEMPT_COUNT);
        return new MySummaryDataDto(courseDetailsDto, courseStatsDto, difficultyLessonDto, lastFewAttempts);

    }
}
