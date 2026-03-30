package org.elearning.backend.assessment.mapper;

import org.elearning.backend.assessment.dto.QuestionForStudentDTO;
import org.elearning.backend.assessment.dto.StartAttemptResponseDTO;
import org.elearning.backend.assessment.dto.TestResultDTO;
import org.elearning.backend.assessment.model.Test;
import org.elearning.backend.assessment.model.TestAttempt;
import org.elearning.backend.assessment.model.TestResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AttemptMapper {

    /**
     * Mapează TestAttempt → StartAttemptResponseDTO.
     * questions e setat manual în service după mapare
     * deoarece vine din altă sursă (questionRepository).
     */
    /*@Mapping(target = "questions", ignore = true)
    @Mapping(target = "timeLimitSec", ignore = true)
    StartAttemptResponseDTO toStartAttemptResponseDTO(TestAttempt attempt);*/

    /**
     * Mapează TestResult → TestResultDTO.
     */
    TestResultDTO toTestResultDTO(TestResult result);

    @Mapping(target = "timeLimitSec", source = "test.timeLimitSec")
    @Mapping(target = "questions", source = "questionsList")
        // Restul field-urilor (id, status etc.) le ia automat din "attempt"
    StartAttemptResponseDTO toStartAttemptResponseDTO(TestAttempt attempt, Test test, List<QuestionForStudentDTO> questionsList);
}