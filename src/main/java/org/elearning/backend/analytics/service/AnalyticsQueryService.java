package org.elearning.backend.analytics.service;

import org.elearning.backend.analytics.dto.statistics.teacher.ClassAverageDto;
import org.elearning.backend.analytics.dto.statistics.teacher.StudentAverageDto;
import org.elearning.backend.analytics.exception.WithoutAccessException;
import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.elearning.backend.assessment.model.Test;
import org.elearning.backend.assessment.repository.TestRepository;
import org.elearning.backend.assessment.repository.TestResultRepository;
import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.repository.CourseRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AnalyticsQueryService {
    private final TestRepository testRepository;
    private final CourseRepository courseRepository;
    private final TestResultRepository testResultRepository;
    private static final String TEST_DOES_NOT_EXIST = "Test does not exist";
    private static final String COURSE_DOES_NOT_EXIST = "Course does not exist";

    /**
     * Creates an AnalyticsQueryService backed by the provided repositories.
     */
    public AnalyticsQueryService(TestRepository testRepository, CourseRepository courseRepository, TestResultRepository testResultRepository) {
        this.testRepository = testRepository;
        this.courseRepository = courseRepository;
        this.testResultRepository = testResultRepository;
    }

    /**
     * Fetch aggregated class statistics for the specified test.
     *
     * Throws DoesNotExistException(TEST_DOES_NOT_EXIST) if the test cannot be found.
     * Throws WithoutAccessException(professorId) if the provided professorId is not the test's creator.
     * If no results exist for the test, returns a ClassAverageDto with counts set to 0, score fields set to BigDecimal.ZERO, and failure rate 0.0.
     *
     * @param testId      the identifier of the test to query
     * @param professorId the professor's identifier used to validate access to the test data
     * @return a ClassAverageDto containing the test id and title, total result count, passed and failed counts, average score, lowest score, highest score, and failure rate
     */

    public ClassAverageDto getClassAverage(UUID testId, UUID professorId){
        Test test = testRepository.findById(testId)
                .orElseThrow( () -> new DoesNotExistException(TEST_DOES_NOT_EXIST));
        if(!test.getCreatedBy().equals(professorId)){
            throw new WithoutAccessException(professorId);
        }

        ClassAverageDto result = testResultRepository.getClassAverages(test);

        if(result==null){
            return new ClassAverageDto(testId, test.getTitle(), 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0.0);
        }

        return result;
    }

    /**
     * Retrieve a paginated page of per-student average analytics for the specified course.
     *
     * @param courseId    the course identifier to query
     * @param professorId the professor identifier used to verify access to the course
     * @param pageable    paging and sorting parameters for the returned page
     * @return            a page of StudentAverageDto objects containing per-student aggregates:
     *                    student ID, average/lowest/best score, total test count, passed count,
     *                    failed count, and timestamp of the latest test attempt
     * @throws DoesNotExistException if no course exists with the given `courseId`
     * @throws WithoutAccessException if the `professorId` is not the creator of the course
     */

    public Page<StudentAverageDto> getStudentAverages(UUID courseId, UUID professorId, Pageable pageable){

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new DoesNotExistException(COURSE_DOES_NOT_EXIST));
        if(!course.getCreatedBy().equals(professorId)){
            throw new WithoutAccessException(professorId);
        }

        return testResultRepository.getStudentAverages(courseId, pageable);
    }


}
