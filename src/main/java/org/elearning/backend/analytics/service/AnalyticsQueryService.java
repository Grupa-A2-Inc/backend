package org.elearning.backend.analytics.service;

import org.elearning.backend.analytics.dto.*;
import org.elearning.backend.analytics.exception.AccessDeniedException;
import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.elearning.backend.assessment.model.Test;
import org.elearning.backend.assessment.repository.TestRepository;
import org.elearning.backend.assessment.repository.TestResultRepository;
import org.elearning.backend.content.model.Course;
import org.elearning.backend.content.repository.CourseRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;
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

    /** ProfessorId can be removed once the proper pre-authorization for teachers to view the stats of a class are
     * implemented
     */

    public ClassAverageDto getClassAverage(UUID testId, UUID professorId){
        Test test = testRepository.findById(testId)
                .orElseThrow( () -> new DoesNotExistException(TEST_DOES_NOT_EXIST));
        if(!test.getCreatedBy().equals(professorId)){
            throw new AccessDeniedException(professorId);
        }

        return testResultRepository.getClassAverages(test);
    }

    public Page<StudentAverageDto> getStudentAverages(UUID courseId, UUID professorId, Pageable pageable){

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new DoesNotExistException(COURSE_DOES_NOT_EXIST));
        if(!course.getCreatedBy().equals(professorId)){
            throw new AccessDeniedException(professorId);
        }

        return testResultRepository.getStudentAverages(courseId, pageable);
    }


}
