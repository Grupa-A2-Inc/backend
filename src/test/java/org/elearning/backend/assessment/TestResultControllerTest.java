package org.elearning.backend.assessment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elearning.backend.assessment.dto.attempt_dto.AttemptReportDTO;
import org.elearning.backend.assessment.dto.attempt_dto.AttemptStatusDTO;
import org.elearning.backend.assessment.model.AttemptStatus;
import org.elearning.backend.assessment.service.TestResultService;
import org.elearning.backend.auth.service.EmailService;
import org.elearning.backend.role.entity.Role;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.elearning.backend.security.access.AccessService;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "STUDENT")
class TestResultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private EmailService emailService;
    @MockitoBean
    private TestResultService testResultService;

    @MockitoBean
    private AccessService accessService;

    private final UUID attemptId = UUID.randomUUID();
    private final UUID testId = UUID.randomUUID();
    private final UUID lessonId = UUID.randomUUID();
    private final UUID studentId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUpAccessService() {
        when(accessService.canViewAttemptResult(any(), any())).thenReturn(true);
        when(accessService.canViewMyTestAttempts(any(), any())).thenReturn(true);
        when(accessService.canViewMyLessonAttempts(any(), any())).thenReturn(true);
        when(accessService.canViewMyBestTestResult(any(), any())).thenReturn(true);
    }

    @Test
    void getResult_shouldReturnAttemptReport() throws Exception {
        AttemptReportDTO mockReport = AttemptReportDTO.builder()
                .attemptId(attemptId)
                .score(BigDecimal.valueOf(85.0))
                .scorePercent(BigDecimal.valueOf(85.0))
                .passed(true)
                .completedAt(LocalDateTime.now())
                .question(List.of()) // Assuming empty for simplicity
                .build();

        when(testResultService.getTestResult(attemptId, studentId)).thenReturn(mockReport);

        mockMvc.perform(get("/api/v1/attempts/{attemptId}/result", attemptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptId").value(attemptId.toString()))
                .andExpect(jsonPath("$.score").value(85.0))
                .andExpect(jsonPath("$.scorePercent").value(85.0))
                .andExpect(jsonPath("$.passed").value(true));

        verify(testResultService).getTestResult(attemptId, studentId);
    }

    @Test
    void getAttempts_shouldReturnListOfAttemptStatus() throws Exception {
        AttemptStatusDTO mockAttempt1 = AttemptStatusDTO.builder()
                .attemptID(UUID.randomUUID())
                .attemptNumber(1)
                .score(BigDecimal.valueOf(90.0))
                .scorePercent(BigDecimal.valueOf(90.0))
                .passed(true)
                .startedAt(LocalDateTime.now())
                .status(AttemptStatus.DONE)
                .build();

        AttemptStatusDTO mockAttempt2 = AttemptStatusDTO.builder()
                .attemptID(UUID.randomUUID())
                .attemptNumber(2)
                .score(BigDecimal.valueOf(75.0))
                .scorePercent(BigDecimal.valueOf(75.0))
                .passed(false)
                .startedAt(LocalDateTime.now())
                .status(AttemptStatus.DONE)
                .build();

        List<AttemptStatusDTO> mockAttempts = List.of(mockAttempt1, mockAttempt2);

        when(testResultService.getTestAttempts(testId, studentId)).thenReturn(mockAttempts);

        mockMvc.perform(get("/api/v1/tests/{testId}/my-attempts", testId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].attemptNumber").value(1))
                .andExpect(jsonPath("$[1].attemptNumber").value(2));

        verify(testResultService).getTestAttempts(testId, studentId);
    }

    @Test
    void getBestAttempt_shouldReturnBestAttemptStatus() throws Exception {
        AttemptStatusDTO mockBestAttempt = AttemptStatusDTO.builder()
                .attemptID(attemptId)
                .attemptNumber(1)
                .score(BigDecimal.valueOf(95.0))
                .scorePercent(BigDecimal.valueOf(95.0))
                .passed(true)
                .startedAt(LocalDateTime.now())
                .status(AttemptStatus.DONE)
                .build();

        when(testResultService.getBestTestAttempt(testId, studentId)).thenReturn(mockBestAttempt);

        mockMvc.perform(get("/api/v1/tests/{testId}/my-best", testId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptID").value(attemptId.toString()))
                .andExpect(jsonPath("$.scorePercent").value(95.0))
                .andExpect(jsonPath("$.passed").value(true));

        verify(testResultService).getBestTestAttempt(testId, studentId);
    }

    @Test
    void getLessonAttempts_shouldReturnAttemptsAcrossVersions() throws Exception {
        AttemptStatusDTO versionOneAttempt = AttemptStatusDTO.builder()
                .attemptID(UUID.randomUUID())
                .testId(UUID.randomUUID())
                .testTitle("Test v1")
                .testVersion(1)
                .attemptNumber(1)
                .score(BigDecimal.valueOf(90.0))
                .scorePercent(BigDecimal.valueOf(90.0))
                .passed(true)
                .startedAt(LocalDateTime.now())
                .status(AttemptStatus.DONE)
                .build();

        AttemptStatusDTO versionTwoAttempt = AttemptStatusDTO.builder()
                .attemptID(UUID.randomUUID())
                .testId(UUID.randomUUID())
                .testTitle("Test v2")
                .testVersion(2)
                .attemptNumber(1)
                .score(BigDecimal.valueOf(75.0))
                .scorePercent(BigDecimal.valueOf(75.0))
                .passed(false)
                .startedAt(LocalDateTime.now())
                .status(AttemptStatus.DONE)
                .build();

        when(testResultService.getLessonAttempts(lessonId, studentId))
                .thenReturn(List.of(versionTwoAttempt, versionOneAttempt));

        mockMvc.perform(get("/api/v1/lessons/{lessonId}/my-attempts", lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].testTitle").value("Test v2"))
                .andExpect(jsonPath("$[0].testVersion").value(2))
                .andExpect(jsonPath("$[1].testTitle").value("Test v1"))
                .andExpect(jsonPath("$[1].testVersion").value(1));

        verify(testResultService).getLessonAttempts(lessonId, studentId);
    }

    // Additional tests for edge cases with no completed attempts

    @Test
    void getAttempts_shouldReturnEmptyListWhenNoCompletedAttempts() throws Exception {
        when(testResultService.getTestAttempts(testId, studentId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/tests/{testId}/my-attempts", testId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(testResultService).getTestAttempts(testId, studentId);
    }

    @Test
    void getBestAttempt_shouldReturn404WhenNoFinishedAttempts() throws Exception {
        when(testResultService.getBestTestAttempt(testId, studentId))
                .thenThrow(new IllegalArgumentException("No finished attempts found for student " + studentId + " on test " + testId));

        mockMvc.perform(get("/api/v1/tests/{testId}/my-best", testId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No finished attempts found for student " + studentId + " on test " + testId));

        verify(testResultService).getBestTestAttempt(testId, studentId);
    }

    @Test
    void getResult_shouldReturn404WhenAttemptNotFound() throws Exception {
        when(testResultService.getTestResult(attemptId, studentId))
                .thenThrow(new IllegalArgumentException("Attempt with id " + attemptId + " does not exist"));

        mockMvc.perform(get("/api/v1/attempts/{attemptId}/result", attemptId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Attempt with id " + attemptId + " does not exist"));

        verify(testResultService).getTestResult(attemptId,studentId);
    }

    @Test
    void getResult_shouldReturn410WhenAttemptExpired() throws Exception {
        when(testResultService.getTestResult(attemptId, studentId))
                .thenThrow(new org.elearning.backend.assessment.exception.TimerExpiredException("The attempt expired before being submitted"));

        mockMvc.perform(get("/api/v1/attempts/{attemptId}/result", attemptId))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.status").value(410))
                .andExpect(jsonPath("$.message").value("The attempt expired before being submitted"));

        verify(testResultService).getTestResult(attemptId, studentId);
    }

    @Test
    void getResult_shouldReturn403WhenAttemptInProgress() throws Exception {
        when(testResultService.getTestResult(attemptId, studentId))
                .thenThrow(new org.elearning.backend.assessment.exception.AttemptInProgressException("The attempt is still in progress"));

        mockMvc.perform(get("/api/v1/attempts/{attemptId}/result", attemptId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("The attempt is still in progress"));

        verify(testResultService).getTestResult(attemptId, studentId);
    }

    @Test
    void getResult_shouldUseCustomUserDetailsPrincipalWhenPresent() throws Exception {
        User user = new User();
        user.setId(studentId);
        user.setEmail("student@test.com");
        user.setPasswordHash("hashed");
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(new Role(RoleName.STUDENT));
        CustomUserDetails customUserDetails = new CustomUserDetails(user);

        AttemptReportDTO mockReport = AttemptReportDTO.builder()
                .attemptId(attemptId)
                .score(BigDecimal.TEN)
                .scorePercent(BigDecimal.TEN)
                .passed(true)
                .completedAt(LocalDateTime.now())
                .question(List.of())
                .build();
        when(testResultService.getTestResult(attemptId, studentId)).thenReturn(mockReport);

        mockMvc.perform(get("/api/v1/attempts/{attemptId}/result", attemptId)
                        .with(user(customUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptId").value(attemptId.toString()));

        verify(testResultService).getTestResult(attemptId, studentId);
    }
}
