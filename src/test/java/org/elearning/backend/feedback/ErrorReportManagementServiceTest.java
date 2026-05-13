package org.elearning.backend.feedback;

import org.elearning.backend.assessment.model.Question;
import org.elearning.backend.assessment.repository.QuestionRepository;
import org.elearning.backend.content.repository.LessonRepository;
import org.elearning.backend.feedback.dto.ErrorReportDto;
import org.elearning.backend.feedback.mapper.QuestionErrorReportMapper;
import org.elearning.backend.feedback.model.QuestionErrorReport;
import org.elearning.backend.feedback.model.ReportStatus;
import org.elearning.backend.feedback.repository.QuestionErrorReportRepository;
import org.elearning.backend.feedback.service.ErrorReportManagementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrorReportManagementServiceTest {

    @Mock private QuestionErrorReportRepository questionErrorReportRepository;
    @Mock private QuestionErrorReportMapper questionErrorReportMapper;
    @Mock private QuestionRepository questionRepository;
    @Mock private LessonRepository lessonRepository;

    @InjectMocks
    private ErrorReportManagementService errorReportManagementService;

    @Test
    void resolveReport_shouldResolveWhenQuestionIsMissing() {
        UUID reportId = UUID.randomUUID();
        UUID professorId = UUID.randomUUID();
        QuestionErrorReport report = QuestionErrorReport.builder()
                .id(reportId)
                .questionId(1)
                .status(ReportStatus.NEW)
                .build();
        ErrorReportDto dto = new ErrorReportDto(reportId, 1, "desc", ReportStatus.RESOLVED, null);

        when(questionErrorReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(questionRepository.findById(1)).thenReturn(Optional.empty());
        when(questionErrorReportMapper.toErrorReportDto(report)).thenReturn(dto);

        ErrorReportDto result = errorReportManagementService.resolveReport(reportId, professorId);

        assertThat(result).isEqualTo(dto);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        verify(questionErrorReportRepository).save(report);
    }

    @Test
    void resolveReport_shouldResolveWhenLessonIsMissing() {
        UUID reportId = UUID.randomUUID();
        UUID professorId = UUID.randomUUID();
        QuestionErrorReport report = QuestionErrorReport.builder()
                .id(reportId)
                .questionId(2)
                .status(ReportStatus.NEW)
                .build();
        ErrorReportDto dto = new ErrorReportDto(reportId, 2, "desc", ReportStatus.RESOLVED, null);

        org.elearning.backend.assessment.model.Test test = new org.elearning.backend.assessment.model.Test();
        UUID lessonId = UUID.randomUUID();
        test.setLessonId(lessonId);
        Question question = new Question();
        question.setId(2);
        question.setTest(test);

        when(questionErrorReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(questionRepository.findById(2)).thenReturn(Optional.of(question));
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.empty());
        when(questionErrorReportMapper.toErrorReportDto(report)).thenReturn(dto);

        ErrorReportDto result = errorReportManagementService.resolveReport(reportId, professorId);

        assertThat(result).isEqualTo(dto);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        verify(questionErrorReportRepository).save(report);
    }
}
