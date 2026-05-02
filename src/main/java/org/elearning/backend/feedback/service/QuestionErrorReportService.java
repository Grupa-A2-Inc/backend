package org.elearning.backend.feedback.service;



import org.elearning.backend.assessment.model.QuestionSource;
import org.elearning.backend.feedback.dto.DescriptionRequestDto;
import org.elearning.backend.feedback.dto.ErrorReportDto;
import org.elearning.backend.feedback.dto.GetErrorReportDto;
import org.elearning.backend.feedback.dto.projections.GetErrorReportProjection;
import org.elearning.backend.feedback.mapper.QuestionErrorReportMapper;
import org.elearning.backend.feedback.model.QuestionErrorReport;
import org.elearning.backend.feedback.model.ReportStatus;
import org.elearning.backend.feedback.repository.QuestionErrorReportRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    //-------- Dev 4 --------
    @Transactional(readOnly = true)
    public Page<GetErrorReportDto> getReports(UUID professorId, ReportStatus status, UUID courseId, Pageable pageable) {
        String statusString = status != null ? status.name() : null;
        Page<GetErrorReportProjection> projections = questionErrorReportRepository.findErrorReportsForProfessor(professorId, statusString, courseId, pageable);

        return projections.map(proj -> {
            ReportStatus reportStatus = ReportStatus.valueOf(proj.getStatus());
            QuestionSource sourceEnum = QuestionSource.valueOf(proj.getQuestionSource());
            return new GetErrorReportDto(
                    proj.getId(),
                    proj.getQuestionId(),
                    proj.getStudentId(),
                    reportStatus,
                    proj.getDescription(),
                    proj.getResolvedAt(),
                    proj.getResolvedBy(),
                    proj.getCreatedAt(),
                    proj.getContent(),
                    sourceEnum,
                    proj.getLessonTitle(),
                    proj.getCourseTitle()
            );
        });
    }
}
