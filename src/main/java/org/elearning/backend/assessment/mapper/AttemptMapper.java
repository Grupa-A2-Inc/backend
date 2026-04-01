package org.elearning.backend.assessment.mapper;

import org.elearning.backend.assessment.dto.QuestionForStudentDto;
import org.elearning.backend.assessment.dto.StartAttemptResponseDto;
import org.elearning.backend.assessment.dto.TestResultDto;
import org.elearning.backend.assessment.model.Test;
import org.elearning.backend.assessment.model.TestAttempt;
import org.elearning.backend.assessment.model.TestResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper interface for mapping between TestAttempt, TestResult entities and their corresponding DTOs.
 * This interface uses MapStruct to generate the implementation at compile time.
 */
@Mapper(componentModel = "spring")
public interface AttemptMapper {

    /**
     * Map the TestResult entity to a TestResultDTO.
     * @param result The TestResult entity to be mapped to a TestResultDTO.
     * @return A TestResultDTO containing the mapped data from the TestResult entity.
     */
    TestResultDto toTestResultDTO(TestResult result);

    /**
     * Map the TestAttempt entity, along with the associated Test and a list of QuestionForStudentDTO, to a StartAttemptResponseDTO.
     * @param attempt The TestAttempt entity to be mapped.
     * @param test The Test entity associated with the attempt, used to map the time limit.
     * @param questionsList A list of QuestionForStudentDTO representing the questions for the student, used to map the questions field.
     * @return A StartAttemptResponseDTO containing the mapped data from the TestAttempt, Test, and questions list.
     */
    @Mapping(target = "timeLimitSec", source = "test.timeLimitSec")
    @Mapping(target = "questions", source = "questionsList")
    StartAttemptResponseDto toStartAttemptResponseDTO(TestAttempt attempt, Test test, List<QuestionForStudentDto> questionsList);

    StartAttemptResponseDto.TestInfoForAttemptDto toTestInfoForAttemptDTO(Test test);
}