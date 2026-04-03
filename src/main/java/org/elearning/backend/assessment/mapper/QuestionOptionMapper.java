package org.elearning.backend.assessment.mapper;

import org.elearning.backend.assessment.dto.QuestionDataForUsersDto;
import org.elearning.backend.assessment.dto.QuestionOptionsDataDto;
import org.elearning.backend.assessment.model.QuestionOption;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * Mapper interface for mapping between Test and its corresponding DTOs.
 */

@Mapper(componentModel = "spring")
public interface QuestionOptionMapper {
    /**
     * Map the QuestionOption list to a QuestionDataForUsersDto list;
     * @param questionOptions The list of QuestionOption entities to be mapped to a QuestionDataForUsersDto list.
     * @return A QuestionDataForUsersDto list containing the mapped data from the QuestionOption entity.
     */
    List<QuestionOptionsDataDto> toDataForUsersDto(List<QuestionOption> questionOptions);
}
