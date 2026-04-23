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

    private static final Pageable LAST_ATTEMPT_COUNT = PageRequest.of(0,5);
    private static final Pageable DIFFICULT_LESSON_COUNT = PageRequest.of(0,3);

    private static final BigDecimal PROBLEM_GAP = BigDecimal.valueOf(15);
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

    private int getRank(UUID studentId, List<MyClassTestBestResultsDto> myClassBestResults){
        int rank;

        for(rank=0;rank< myClassBestResults.size();rank++){
            if(myClassBestResults.get(rank).getStudentId().equals(studentId)){
                break;
            }
        }
        return rank+1;
    }

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

    private BigDecimal computeMedian(List<MyClassTestBestResultsDto> myClassBestResults){
        int totalResults = myClassBestResults.size();
        if(totalResults %2==1){
            return  myClassBestResults.get((totalResults /2)-1).getBestScorePercentage();
        }
        else{
            return myClassBestResults.get((totalResults /2)-1).getBestScorePercentage()
                    .add(myClassBestResults.get(totalResults /2).getBestScorePercentage())
                    .divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP);
        }
    }

    public MyTestStatsDto getMyTestStats(UUID studentId, UUID testId){
        Test test = testRepository.findById(testId)
                .orElseThrow( () -> new DoesNotExistException(TEST_DOES_NOT_EXIST));

        //TO DO: verify if student is enrolled to the course the test comes from.

        MyPersonalTestStatsDto myPersonalTestStats = testResultRepository.getMyPersonalTestStats(studentId, test);

        BigDecimal latestScore = testResultRepository
                .findTopByStudentIdAndTestOrderByCompletedAtDesc(studentId, test).getScorePercent();

        MyClassTestAverageDto myClassAverageStats = testResultRepository.getMyClassAverageStats(test);



        List<MyClassTestBestResultsDto> myClassBestResults = new ArrayList<>(testResultRepository
                .getAllByTestOrderByScorePercentAsc(test));

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
