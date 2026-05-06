package org.elearning.backend.feedback.service;

import lombok.extern.slf4j.Slf4j;
import org.elearning.backend.feedback.dto.DescriptionRequestDto;
import org.elearning.backend.feedback.dto.ErrorReportDto;
import org.elearning.backend.feedback.mapper.QuestionErrorReportMapper;
import org.elearning.backend.feedback.model.QuestionErrorReport;
import org.elearning.backend.feedback.repository.QuestionErrorReportRepository;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@Slf4j
public class QuestionErrorReportService {
    private final QuestionErrorReportRepository questionErrorReportRepository;
    private final QuestionErrorReportMapper questionErrorReportMapper;

    private final ErrorReportRoutingService errorReportRoutingService;

    public QuestionErrorReportService(QuestionErrorReportRepository questionErrorReportRepository, QuestionErrorReportMapper questionErrorReportMapper, ErrorReportRoutingService errorReportRoutingService) {
        this.questionErrorReportRepository = questionErrorReportRepository;
        this.questionErrorReportMapper = questionErrorReportMapper;
        this.errorReportRoutingService = errorReportRoutingService;
    }

    public ErrorReportDto createReport(Integer questionId, UUID studentId, DescriptionRequestDto description){

        QuestionErrorReport questionErrorReport = new QuestionErrorReport();
        questionErrorReport.setQuestionId(questionId);
        questionErrorReport.setStudentId(studentId);
        questionErrorReport.setDescription(description.getDescription());

        questionErrorReportRepository.save(questionErrorReport);

        //------ Dev 4 Routing to AI ------
        try {
            errorReportRoutingService.route(questionErrorReport.getId());
        } catch (Exception exception) {
            log.warn("[ERROR REPORT ROUTING] Eroare la rutarea raportului {}: {}", questionErrorReport.getId(), exception.getMessage());
        }

        return questionErrorReportMapper.toErrorReportDto(questionErrorReport);
    }
}
