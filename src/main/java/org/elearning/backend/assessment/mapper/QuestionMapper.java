package org.elearning.backend.assessment.mapper;

import org.elearning.backend.assessment.dto.*;
import org.elearning.backend.assessment.model.Question;
import org.elearning.backend.assessment.model.QuestionOption;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface QuestionMapper {

    /**
     * Mapează Question → QuestionForStudentDTO.
     * options e setat manual în service după mapare.
     */
    @Mapping(target = "questionId", source = "id")
    //@Mapping(target = "questionType", source = "questionType")
    QuestionForStudentDto toQuestionForStudentDTO(Question question);

    List<QuestionForStudentDto> toQuestionForStudentDTOList(List<Question> questions);

    /**
     * Mapează QuestionOption → OptionForStudentDTO.
     * is_correct e ignorat intenționat — nu se trimite elevului.
     */
    @Mapping(target = "optionId", source = "id")
    QuestionForStudentDto.OptionForStudentDto toOptionForStudentDTO(QuestionOption option);

    Question toEntity(QuestionRequestDto dto);

    QuestionOption toOptionEntity(OptionRequestDto dto);

    @Mapping(target = "questionId", source = "id")
    QuestionResponseDto toResponseDto(Question question);

    @Mapping(target = "optionId", source = "id")
    OptionResponseDto toOptionResponseDto(QuestionOption option);
}