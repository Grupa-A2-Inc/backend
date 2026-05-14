package org.elearning.backend.feedback;

import org.elearning.backend.feedback.dto.DescriptionRequestDto;
import org.elearning.backend.feedback.dto.ErrorReportDto;
import org.elearning.backend.feedback.mapper.QuestionErrorReportMapper;
import org.elearning.backend.feedback.model.QuestionErrorReport;
import org.elearning.backend.feedback.repository.QuestionErrorReportRepository;
import org.elearning.backend.feedback.service.ErrorReportRoutingService;
import org.elearning.backend.feedback.service.QuestionErrorReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionErrorReportServiceTest {

    @Mock
    private QuestionErrorReportRepository questionErrorReportRepository;
    @Mock
    private QuestionErrorReportMapper questionErrorReportMapper;
    @Mock
    private ErrorReportRoutingService errorReportRoutingService;

    private QuestionErrorReportService service;

    @BeforeEach
    void setUp() {
        service = new QuestionErrorReportService(questionErrorReportRepository, questionErrorReportMapper, errorReportRoutingService);
    }

    @Test
    void createReportRoutesReportAndReturnsMappedDto() {
        UUID studentId = UUID.randomUUID();
        DescriptionRequestDto description = new DescriptionRequestDto("This question has a wrong answer.");
        ErrorReportDto dto = new ErrorReportDto();
        when(questionErrorReportMapper.toErrorReportDto(any())).thenReturn(dto);

        ErrorReportDto result = service.createReport(7, studentId, description);

        assertThat(result).isSameAs(dto);
        ArgumentCaptor<QuestionErrorReport> captor = ArgumentCaptor.forClass(QuestionErrorReport.class);
        verify(questionErrorReportRepository).save(captor.capture());
        assertThat(captor.getValue().getQuestionId()).isEqualTo(7);
        assertThat(captor.getValue().getStudentId()).isEqualTo(studentId);
        assertThat(captor.getValue().getDescription()).isEqualTo(description.getDescription());
        verify(errorReportRoutingService).route(captor.getValue().getId());
    }

    @Test
    void createReportSwallowsRoutingExceptionsAndStillReturnsMappedDto() {
        UUID studentId = UUID.randomUUID();
        DescriptionRequestDto description = new DescriptionRequestDto("This question explanation is inconsistent.");
        ErrorReportDto dto = new ErrorReportDto();
        doThrow(new RuntimeException("routing failed")).when(errorReportRoutingService).route(any());
        when(questionErrorReportMapper.toErrorReportDto(any())).thenReturn(dto);

        ErrorReportDto result = service.createReport(9, studentId, description);

        assertThat(result).isSameAs(dto);
        verify(questionErrorReportMapper).toErrorReportDto(any());
    }
}
