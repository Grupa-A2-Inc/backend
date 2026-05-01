package org.elearning.backend.feedback.service;



import org.elearning.backend.feedback.dto.DescriptionRequestDto;
import org.elearning.backend.feedback.dto.ErrorReportDto;
import org.elearning.backend.feedback.mapper.QuestionErrorReportMapper;
import org.elearning.backend.feedback.model.QuestionErrorReport;
import org.elearning.backend.feedback.repository.QuestionErrorReportRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class QuestionErrorReportService {
    private final QuestionErrorReportRepository questionErrorReportRepository;
    private final QuestionErrorReportMapper questionErrorReportMapper;

    public QuestionErrorReportService(QuestionErrorReportRepository questionErrorReportRepository, QuestionErrorReportMapper questionErrorReportMapper) {
        this.questionErrorReportRepository = questionErrorReportRepository;
        this.questionErrorReportMapper = questionErrorReportMapper;
    }

    public ErrorReportDto createReport(Integer questionId, UUID studentId, DescriptionRequestDto description){

        QuestionErrorReport questionErrorReport = new QuestionErrorReport();
        questionErrorReport.setQuestionId(questionId);
        questionErrorReport.setStudentId(studentId);
        questionErrorReport.setDescription(description.getDescription());

        questionErrorReportRepository.save(questionErrorReport);

        return questionErrorReportMapper.toErrorReportDto(questionErrorReport);
    }
}
