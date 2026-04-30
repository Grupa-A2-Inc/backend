package org.elearning.backend.feedback.mapper;


import org.elearning.backend.feedback.dto.ErrorReportDto;
import org.elearning.backend.feedback.model.QuestionErrorReport;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface QuestionErrorReportMapper {
    ErrorReportDto toErrorReportDto(QuestionErrorReport questionErrorReport);
}
