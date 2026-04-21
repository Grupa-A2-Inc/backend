package org.elearning.backend.analytics.service;

import org.elearning.backend.analytics.dto.statistics.student.MyClassTestAverageDto;
import org.elearning.backend.analytics.dto.statistics.student.MyClassTestBestResultsDto;
import org.elearning.backend.analytics.dto.statistics.student.MyPersonalTestStatsDto;
import org.elearning.backend.analytics.dto.statistics.student.MyTestStatsDto;
import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.elearning.backend.assessment.model.Test;
import org.elearning.backend.assessment.repository.TestRepository;
import org.elearning.backend.assessment.repository.TestResultRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class StudentsStatsService {

    private final TestRepository testRepository;
    private final TestResultRepository testResultRepository;

    private static final String TEST_DOES_NOT_EXIST = "Test does not exist";

    public StudentsStatsService(TestRepository testRepository, TestResultRepository testResultRepository) {
        this.testRepository = testRepository;
        this.testResultRepository = testResultRepository;
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
}
