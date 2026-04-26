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

    public AnalyticsQueryService(TestRepository testRepository, CourseRepository courseRepository, TestResultRepository testResultRepository) {
        this.testRepository = testRepository;
        this.courseRepository = courseRepository;
        this.testResultRepository = testResultRepository;
    }

    /* ProfessorId can be removed once the proper pre-authorization for teachers to view the stats of a class are
     * implemented
     */

    /**
     * Get statistic data about the class average of a given test. Some parameters will be null if nobody took the test,
     * it's best the returned value is checked to not be null when used.
     * @param testId the given test we want the class stats of
     * @param professorId the professor id to attest if the professor has access to test data or not
     * @return the test id, test title, the total count of results, the number of passed/failed tests, the average score,
     *  the lowest score, the best score and failure rate, inside a ClassAverageDto
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
     * Gets the student data of a given course. The given data are limited by the pageable parameter, that limits the amount
     *  of data the professor gets, making sure the data can be split in multiple pages
     * @param courseId the course the data is extracted from
     * @param pageable the page settings (e.g. third page with 20 students)
     * @param professorId the professor id to attest if the professor has access to course data or not
     * @return a page with a certain amount of students, containing the student ID, their average/lowest/best score,
     * total test count, how many were passed, how many were failed and day of their latest test attempt
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
