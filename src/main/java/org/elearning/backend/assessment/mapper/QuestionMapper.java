package org.elearning.backend.assessment.mapper;

import org.elearning.backend.assessment.dto.OptionForStudentDto;
import org.elearning.backend.assessment.dto.QuestionForStudentDto;
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
    OptionForStudentDto toOptionForStudentDTO(QuestionOption option);
}