package org.elearning.backend.analytics.service;

import org.elearning.backend.analytics.exception.AccessDeniedException;
import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.elearning.backend.assessment.repository.TestRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AnalyticsQueryService {
    private final TestRepository testRepository;
    private static final String TEST_DOES_NOT_EXIST = "Test does not exist";

    public AnalyticsQueryService(TestRepository testRepository) {
        this.testRepository = testRepository;
    }

    public float getClassAverage(UUID testId, UUID professorId){
        if(!testRepository.existsById(testId)){
            throw new DoesNotExistException(TEST_DOES_NOT_EXIST);
        }
        if(!testRepository.existsByIdAndCreatedBy(testId, professorId)){
            throw new AccessDeniedException(professorId);
        }

        return 0.0f;
    }
}
