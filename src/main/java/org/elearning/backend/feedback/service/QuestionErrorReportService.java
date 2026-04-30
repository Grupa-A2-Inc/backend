package org.elearning.backend.feedback.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.elearning.backend.assessment.repository.QuestionRepository;
import org.elearning.backend.enrollment.exception.StudentAccessForbiddenException;
import org.elearning.backend.feedback.dto.ErrorReportDto;
import org.elearning.backend.feedback.mapper.QuestionErrorReportMapper;
import org.elearning.backend.feedback.model.QuestionErrorReport;
import org.elearning.backend.feedback.repository.QuestionErrorReportRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class QuestionErrorReportService {
    private final QuestionRepository questionRepository;
    private final QuestionErrorReportRepository questionErrorReportRepository;
    private final QuestionErrorReportMapper questionErrorReportMapper;
    private final QuestionAccessValidatorService questionAccessValidatorService;
    private static final String QUESTION_DOES_NOT_EXIST = "Question does not exist";

    public QuestionErrorReportService(QuestionRepository questionRepository, QuestionErrorReportRepository questionErrorReportRepository, QuestionErrorReportMapper questionErrorReportMapper, QuestionAccessValidatorService questionAccessValidatorService) {
        this.questionRepository = questionRepository;
        this.questionErrorReportRepository = questionErrorReportRepository;
        this.questionErrorReportMapper = questionErrorReportMapper;
        this.questionAccessValidatorService = questionAccessValidatorService;
    }

    public ErrorReportDto createReport(Integer questionId, UUID studentId, @NotBlank @Size(min = 10, max = 256) String description){
        if(!questionRepository.existsById(questionId)){
            throw new DoesNotExistException(QUESTION_DOES_NOT_EXIST);
        }
        if(!questionAccessValidatorService.hasStudentAccessToQuestion(questionId, studentId)){
            throw new StudentAccessForbiddenException(studentId);
        }

        QuestionErrorReport questionErrorReport = new QuestionErrorReport();
        questionErrorReport.setQuestionId(questionId);
        questionErrorReport.setStudentId(studentId);
        questionErrorReport.setDescription(description);

        questionErrorReportRepository.save(questionErrorReport);

        return questionErrorReportMapper.toErrorReportDto(questionErrorReport);
    }
}
