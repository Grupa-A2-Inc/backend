package org.elearning.backend.assessment.mapper;

import org.elearning.backend.assessment.dto.AttemptReportDTO;
import org.elearning.backend.assessment.dto.QuestionForAttemptReportDTO;
import org.elearning.backend.assessment.dto.TestResultDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AttemptReportMapper {
    /**
     *
     * @param resultDTO
     * @param questions
     * @return
     */
    @Mapping(target = "attemptId", source = "resultDTO.attemptId")
    @Mapping(target = "score", source = "resultDTO.score")
    @Mapping(target = "scorePercent", source = "resultDTO.scorePercent")
    @Mapping(target = "passed", source = "resultDTO.passed")
    @Mapping(target = "completedAt", source = "resultDTO.completedAt")
    @Mapping(target = "question", source = "questions")
    AttemptReportDTO toAttemptReportDTO(TestResultDto resultDTO, List<QuestionForAttemptReportDTO> questions);
}
