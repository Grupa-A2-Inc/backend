package org.elearning.backend.assessment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elearning.backend.assessment.dto.assigment_dto.TestResultDto;
import org.elearning.backend.assessment.dto.test_dto.StartAttemptResponseDto;
import org.elearning.backend.assessment.dto.test_dto.SubmitAnswerDto;
import org.elearning.backend.assessment.dto.test_dto.SubmitRequestDto;
import org.elearning.backend.assessment.exception.AttemptAlreadySubmittedException;
import org.elearning.backend.assessment.exception.TimerExpiredException;
import org.elearning.backend.assessment.service.AttemptService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false) // controllerul foloseste hardcoded UUID, nu @AuthenticationPrincipal
class AttemptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AttemptService attemptService;

    private final UUID testId = UUID.randomUUID();
    private final UUID attemptId = UUID.randomUUID();
    // hardcodat in controller
    private final UUID studentId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private StartAttemptResponseDto mockStartResponse() {
        StartAttemptResponseDto dto = new StartAttemptResponseDto();
        dto.setAttemptId(attemptId);
        dto.setAttemptNumber(1);
        dto.setStartedAt(LocalDateTime.now());
        dto.setTimeLimitSec(1800);
        dto.setQuestions(List.of());

        StartAttemptResponseDto.TestInfoForAttemptDto testInfo =
                new StartAttemptResponseDto.TestInfoForAttemptDto();
        testInfo.setId(testId);
        testInfo.setTitle("Test Java");
        dto.setTest(testInfo);

        return dto;
    }

    private TestResultDto mockResultDto() {
        TestResultDto dto = new TestResultDto();
        dto.setAttemptId(attemptId);
        dto.setScore(BigDecimal.valueOf(80));
        dto.setScorePercent(BigDecimal.valueOf(80));
        dto.setPassed(true);
        dto.setCompletedAt(LocalDateTime.now());
        dto.setQuestions(List.of());
        return dto;
    }

    private SubmitRequestDto mockSubmitRequest() {
        SubmitAnswerDto answer = new SubmitAnswerDto();
        answer.setQuestionId(1);
        answer.setSelectedOptionIds(List.of(2));
        answer.setTimeSpent(BigDecimal.valueOf(30));

        SubmitRequestDto request = new SubmitRequestDto();
        request.setAnswers(List.of(answer));
        return request;
    }

    // =========================================================================
    // POST /api/v1/tests/{testId}/start
    // =========================================================================

    @Test
    void startAttempt_shouldReturn200_whenValid() throws Exception {
        when(attemptService.startAttempt(eq(testId), eq(studentId)))
                .thenReturn(mockStartResponse());

        mockMvc.perform(post("/api/v1/tests/{testId}/start", testId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptId").value(attemptId.toString()))
                .andExpect(jsonPath("$.attemptNumber").value(1))
                .andExpect(jsonPath("$.timeLimitSec").value(1800))
                .andExpect(jsonPath("$.test.title").value("Test Java"));

        verify(attemptService).startAttempt(testId, studentId);
    }

    @Test
    void startAttempt_shouldReturn404_whenTestNotFound() throws Exception {
        when(attemptService.startAttempt(any(), any()))
                .thenThrow(new IllegalArgumentException("Test not found."));

        mockMvc.perform(post("/api/v1/tests/{testId}/start", testId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Test not found."));
    }

    @Test
    void startAttempt_shouldReturn400_whenTestNotPublished() throws Exception {
        when(attemptService.startAttempt(any(), any()))
                .thenThrow(new org.elearning.backend.assessment.exception.TestNotPublishedException("Test is not PUBLISHED."));

        mockMvc.perform(post("/api/v1/tests/{testId}/start", testId)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Test is not PUBLISHED."));
    }

    // =========================================================================
    // POST /api/v1/attempts/{attemptId}/submit
    // =========================================================================

    @Test
    void submitAttempt_shouldReturn200_whenValid() throws Exception {
        when(attemptService.submitAttempt(eq(attemptId), eq(studentId), any(SubmitRequestDto.class)))
                .thenReturn(mockResultDto());

        mockMvc.perform(post("/api/v1/attempts/{attemptId}/submit", attemptId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockSubmitRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptId").value(attemptId.toString()))
                .andExpect(jsonPath("$.score").value(80))
                .andExpect(jsonPath("$.scorePercent").value(80))
                .andExpect(jsonPath("$.passed").value(true));

        verify(attemptService).submitAttempt(eq(attemptId), eq(studentId), any(SubmitRequestDto.class));
    }

    @Test
    void submitAttempt_shouldReturn404_whenAttemptNotFound() throws Exception {
        when(attemptService.submitAttempt(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Attempt not found."));

        mockMvc.perform(post("/api/v1/attempts/{attemptId}/submit", attemptId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockSubmitRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Attempt not found."));
    }

    @Test
    void submitAttempt_shouldReturn409_whenAlreadySubmitted() throws Exception {
        when(attemptService.submitAttempt(any(), any(), any()))
                .thenThrow(new AttemptAlreadySubmittedException("Attempt already submitted."));

        mockMvc.perform(post("/api/v1/attempts/{attemptId}/submit", attemptId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockSubmitRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Attempt already submitted."));
    }

    @Test
    void submitAttempt_shouldReturn410_whenTimerExpired() throws Exception {
        when(attemptService.submitAttempt(any(), any(), any()))
                .thenThrow(new TimerExpiredException("Timer expired."));

        mockMvc.perform(post("/api/v1/attempts/{attemptId}/submit", attemptId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockSubmitRequest())))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.message").value("Timer expired."));
    }

    @Test
    void submitAttempt_shouldReturn400_whenBodyMissing() throws Exception {
        mockMvc.perform(post("/api/v1/attempts/{attemptId}/submit", attemptId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(attemptService);
    }
}