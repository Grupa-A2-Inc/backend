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

    public StudentsStatsService(TestRepository testRepository, TestResultRepository testResultRepository, CourseRepository courseRepository, LessonDifficultyByStudentRepository lessonDifficultyByStudentRepository, CourseEnrollmentRepository courseEnrollmentRepository) {
        this.testRepository = testRepository;
        this.testResultRepository = testResultRepository;
        this.courseRepository = courseRepository;
        this.lessonDifficultyByStudentRepository = lessonDifficultyByStudentRepository;
        this.courseEnrollmentRepository = courseEnrollmentRepository;
    }

    /**
     * Computes the rank of a student
     * @param studentId the id of the student for whom the rank is computed
     * @param myClassBestResults the list of the best grades each student has, sorted from best to worst
     * @return the rank of the student inside a course
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
     * Computes the percentile of a student, determined by the formula (totalStudents - studentRank + 1) / totalStudents * 100
     * @param studentRank the id of the student for whom the percentile is computed
     * @param totalStudents  the total amount of students from a course
     * @return the percentile of a student
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
     * Computes the median of a class. If the total amount of students from a class is odd, it returns the score
     * situated at the middle, otherwise it returns the average of the two students at the middle of the top
     * @param myClassBestResults the list of the best results from a given class
     * @return the median of the class
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
     * Returns the stats of a student from a test
     * @param studentId the student for whom we get the data for
     * @param testId the test the data is taken from
     * @return  a MyTestStatsDto Object containing the id of the test, the tile of it, the total amount of attempts,
     * the best score, the worst score, the average score, the last score, the amount of students who took the test,
     * the class average, the class median, the rank and the percentile, all of a given student
     */

    public MyTestStatsDto getMyTestStats(UUID studentId, UUID testId){
        Test test = testRepository.findById(testId)
                .orElseThrow( () -> new DoesNotExistException(TEST_DOES_NOT_EXIST));

        //TO DO: verify if student is enrolled to the course the test comes from.

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
     * Gets the summary data of a course for a student
     * @param studentId the student for whom we get the data for
     * @param courseId the course from whom the data is extracted
     * @return a MySummaryDataDto Object containing the course title, total test count of a course, total tests done
     * by the student, total of tests passed, best score, worst score, average of score, the lessons where the student
     * is having difficulties the most and the last few attempts
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
