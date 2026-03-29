package org.elearning.backend.assessment.mapper;

import org.elearning.backend.assessment.dto.OptionForStudentDTO;
import org.elearning.backend.assessment.dto.QuestionForStudentDTO;
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
    @Mapping(target = "questionType", source = "questionType")
    QuestionForStudentDTO toQuestionForStudentDTO(Question question);

    List<QuestionForStudentDTO> toQuestionForStudentDTOList(List<Question> questions);

    /**
     * Mapează QuestionOption → OptionForStudentDTO.
     * is_correct e ignorat intenționat — nu se trimite elevului.
     */
    @Mapping(target = "optionId", source = "id")
    OptionForStudentDTO toOptionForStudentDTO(QuestionOption option);
}