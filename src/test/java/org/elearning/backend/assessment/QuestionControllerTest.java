package org.elearning.backend.assessment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elearning.backend.assessment.dto.question_dto.QuestionRequestDto;
import org.elearning.backend.assessment.dto.question_dto.QuestionResponseDto;
import org.elearning.backend.assessment.model.QuestionType;
import org.elearning.backend.assessment.service.QuestionService;
import org.elearning.backend.auth.service.EmailService;
import org.elearning.backend.security.access.AccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private QuestionService questionService;
    @MockitoBean
    private EmailService emailService;
    @MockitoBean
    private AccessService accessService;

    private final UUID professorId = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private final UUID testId = UUID.randomUUID();
    private final Integer questionId = 1;

    @BeforeEach
    void setUpAccessService() {
        when(accessService.canCreateTestQuestion(any(), any())).thenReturn(true);
        when(accessService.canViewTestQuestion(any(), any(), any())).thenReturn(true);
        when(accessService.canViewTestQuestions(any(), any())).thenReturn(true);
        when(accessService.canEditTestQuestion(any(), any(), any())).thenReturn(true);
        when(accessService.canDeleteTestQuestion(any(), any(), any())).thenReturn(true);
    }

    private QuestionResponseDto mockResponse() {
        return QuestionResponseDto.builder()
                .questionId(1L)
                .content("Ce este Java?")
                .questionType(QuestionType.SINGLE_CHOICE)
                .difficulty(BigDecimal.valueOf(2.5))
                .options(List.of())
                .build();
    }

    private QuestionRequestDto mockRequest() {
        QuestionRequestDto dto = new QuestionRequestDto();
        dto.setContent("Ce este Java?");
        dto.setQuestionType(QuestionType.SINGLE_CHOICE);
        dto.setDifficulty(BigDecimal.valueOf(2.5));
        dto.setOptions(List.of());
        return dto;
    }


    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000099", roles = "PROFESSOR")
    void createQuestion_shouldReturn201_whenValid() throws Exception {
        when(questionService.createQuestion(eq(testId), any(QuestionRequestDto.class), eq(professorId)))
                .thenReturn(mockResponse());

        mockMvc.perform(post("/api/tests/{testId}/questions", testId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.questionId").value(1))
                .andExpect(jsonPath("$.content").value("Ce este Java?"))
                .andExpect(jsonPath("$.questionType").value("SINGLE_CHOICE"));

        verify(questionService).createQuestion(eq(testId), any(QuestionRequestDto.class), eq(professorId));
    }

    @Test
    void createQuestion_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/tests/{testId}/questions", testId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockRequest())))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(questionService);
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000099", roles = "PROFESSOR")
    void createQuestion_shouldReturn403_whenNotOwner() throws Exception {
        when(questionService.createQuestion(any(), any(), any()))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Not the owner"));

        mockMvc.perform(post("/api/tests/{testId}/questions", testId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000099", roles = "PROFESSOR")
    void createQuestion_shouldReturn404_whenTestNotFound() throws Exception {
        when(questionService.createQuestion(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Test not found"));

        mockMvc.perform(post("/api/tests/{testId}/questions", testId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockRequest())))
                .andExpect(status().isNotFound());
    }


    @Test
    @WithMockUser(roles = "STUDENT")
    void getQuestionById_shouldReturn200_whenExists() throws Exception {
        when(questionService.getQuestionById(testId, questionId)).thenReturn(mockResponse());

        mockMvc.perform(get("/api/tests/{testId}/questions/{questionId}", testId, questionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value(1))
                .andExpect(jsonPath("$.difficulty").value(2.5));

        verify(questionService).getQuestionById(testId, questionId);
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void getQuestionById_shouldReturn404_whenNotFound() throws Exception {
        when(questionService.getQuestionById(testId, questionId))
                .thenThrow(new IllegalArgumentException("Question not found"));

        mockMvc.perform(get("/api/tests/{testId}/questions/{questionId}", testId, questionId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getQuestionById_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/tests/{testId}/questions/{questionId}", testId, questionId))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(questionService);
    }

     @Test
    @WithMockUser(roles = "PROFESSOR")
    void getQuestions_shouldReturn200WithList_whenNoFilters() throws Exception {
        when(questionService.getFilteredAndSortedQuestions(testId, null, null, "displayOrder", "asc"))
                .thenReturn(List.of(mockResponse(), mockResponse()));

        mockMvc.perform(get("/api/tests/{testId}/questions", testId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(roles = "PROFESSOR")
    void getQuestions_shouldReturn200WithEmptyList_whenNoneMatch() throws Exception {
        when(questionService.getFilteredAndSortedQuestions(any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/tests/{testId}/questions", testId)
                        .param("questionType", "TRUE_FALSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getQuestions_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/tests/{testId}/questions", testId))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(questionService);
    }


    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000099", roles = "PROFESSOR")
    void updateQuestion_shouldReturn200_whenValid() throws Exception {
        when(questionService.updateQuestion(eq(testId), eq(questionId), any(QuestionRequestDto.class), eq(professorId)))
                .thenReturn(mockResponse());

        mockMvc.perform(put("/api/tests/{testId}/questions/{questionId}", testId, questionId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value(1));

        verify(questionService).updateQuestion(eq(testId), eq(questionId), any(), eq(professorId));
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000099", roles = "PROFESSOR")
    void updateQuestion_shouldReturn403_whenNotOwner() throws Exception {
        when(questionService.updateQuestion(any(), any(), any(), any()))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Not the owner"));

        mockMvc.perform(put("/api/tests/{testId}/questions/{questionId}", testId, questionId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateQuestion_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(put("/api/tests/{testId}/questions/{questionId}", testId, questionId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockRequest())))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(questionService);
    }
  @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000099", roles = "PROFESSOR")
    void deleteQuestion_shouldReturn204_whenValid() throws Exception {
        doNothing().when(questionService).deleteQuestion(testId, questionId, professorId);

        mockMvc.perform(delete("/api/tests/{testId}/questions/{questionId}", testId, questionId)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(questionService).deleteQuestion(testId, questionId, professorId);
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000099", roles = "PROFESSOR")
    void deleteQuestion_shouldReturn400_whenTestNotDraft() throws Exception {
        doThrow(new IllegalStateException("Test is not in DRAFT state"))
                .when(questionService).deleteQuestion(any(), any(), any());

        mockMvc.perform(delete("/api/tests/{testId}/questions/{questionId}", testId, questionId)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000099", roles = "PROFESSOR")
    void deleteQuestion_shouldReturn404_whenNotFound() throws Exception {
        doThrow(new IllegalArgumentException("Question not found"))
                .when(questionService).deleteQuestion(any(), any(), any());

        mockMvc.perform(delete("/api/tests/{testId}/questions/{questionId}", testId, questionId)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteQuestion_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/tests/{testId}/questions/{questionId}", testId, questionId)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(questionService);
    }
}
