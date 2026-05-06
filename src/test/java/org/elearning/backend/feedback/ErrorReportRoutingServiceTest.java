package org.elearning.backend.feedback;

import org.elearning.backend.assessment.model.Question;
import org.elearning.backend.assessment.model.QuestionSource;
import org.elearning.backend.assessment.repository.QuestionRepository;
import org.elearning.backend.feedback.model.QuestionErrorReport;
import org.elearning.backend.feedback.repository.QuestionErrorReportRepository;
import org.elearning.backend.feedback.service.ErrorReportRoutingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class ErrorReportRoutingServiceTest {

    @Mock
    private QuestionErrorReportRepository questionErrorReportRepository;

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private ErrorReportRoutingService errorReportRoutingService;

    @Test
    void shouldLogWarningWhenReportNotFound(CapturedOutput output) {
        UUID reportId = UUID.randomUUID();
        when(questionErrorReportRepository.findById(reportId)).thenReturn(Optional.empty());
        errorReportRoutingService.route(reportId);
        assertThat(output.getOut())
                .contains("[ERROR REPORT ROUTING] Raport cu id " + reportId + " nu a fost gasit pentru rutare.");
        verify(questionRepository, never()).findById(any());
    }

    @Test
    void shouldLogWarningWhenQuestionNotFound(CapturedOutput output) {
        UUID reportId = UUID.randomUUID();
        Integer questionId = 100;
        QuestionErrorReport report = new QuestionErrorReport();
        report.setId(reportId);
        report.setQuestionId(questionId);
        when(questionErrorReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(questionRepository.findById(questionId)).thenReturn(Optional.empty());
        errorReportRoutingService.route(reportId);
        assertThat(output.getOut())
                .contains("[ERROR REPORT ROUTING] Intrebarea cu id " + questionId + " raportata in raportul " + reportId + " nu a fost gasita pentru rutare.");
    }

    @Test
    void shouldLogInfoWhenQuestionIsAiGenerated(CapturedOutput output) {
        UUID reportId = UUID.randomUUID();
        Integer questionId = 100;
        QuestionErrorReport report = new QuestionErrorReport();
        report.setId(reportId);
        report.setQuestionId(questionId);
        Question question = new Question();
        question.setId(questionId);
        question.setSource(QuestionSource.AI_GENERATED);
        when(questionErrorReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        errorReportRoutingService.route(reportId);
        assertThat(output.getOut())
                .contains("[AI ROUTING] Raport " + reportId + " rutat la AI team - question " + questionId + " este AI_GENERATED");
    }

    @Test
    void shouldDoNothingWhenQuestionIsManual(CapturedOutput output) {
        UUID reportId = UUID.randomUUID();
        Integer questionId = 100;
        QuestionErrorReport report = new QuestionErrorReport();
        report.setId(reportId);
        report.setQuestionId(questionId);
        Question question = new Question();
        question.setId(questionId);
        question.setSource(QuestionSource.MANUAL);
        when(questionErrorReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        errorReportRoutingService.route(reportId);
        assertThat(output.getOut()).doesNotContain("[ERROR REPORT ROUTING]");
        assertThat(output.getOut()).doesNotContain("[AI ROUTING]");
    }
}