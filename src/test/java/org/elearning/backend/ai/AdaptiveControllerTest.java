package org.elearning.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elearning.backend.ai.dto.AiAdaptiveJobResponse;
import org.elearning.backend.ai.dto.AiAdaptiveJobStatusResponse;
import org.elearning.backend.ai.dto.AdaptiveStartRequestDto;
import org.elearning.backend.ai.dto.AiAdaptiveExerciseDto;
import org.elearning.backend.ai.dto.AiAdaptiveResponse;
import org.elearning.backend.ai.exception.AiApiException;
import org.elearning.backend.ai.model.AiRequestStatus;
import org.elearning.backend.ai.service.AdaptiveSessionService;
import org.elearning.backend.ai.service.AiApiClient;
import org.elearning.backend.ai.dto.AdaptiveSubmitRequestDto;
import org.elearning.backend.assessment.model.QuestionType;
import org.elearning.backend.auth.service.EmailService;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.jwt.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdaptiveControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private EmailService emailService;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private AiApiClient aiApiClient;

    private UUID studentId;
    private String studentToken;

    @Autowired
    private AdaptiveSessionService adaptiveSubmitService;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        insertUser(studentId, RoleName.STUDENT);
        studentToken = jwtUtil.generateAccessToken(studentId, RoleName.STUDENT);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM adaptive_exercise_jobs");
        jdbcTemplate.execute("DELETE FROM adaptive_session_answers");
        jdbcTemplate.execute("DELETE FROM adaptive_session_exercises");
        jdbcTemplate.execute("DELETE FROM adaptive_sessions");
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", studentId);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void insertUser(UUID userId, RoleName role) {
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, role_type, status) " +
                        "VALUES (?, ?, ?, ?, ?, (SELECT id FROM roles WHERE name = CAST(? AS role_name)), ?, CAST(? AS user_status))",
                userId,
                role.name().toLowerCase() + "-" + userId + "@test.com",
                "password-hash",
                "Test",
                role.name(),
                role.name(),
                role.name(),
                "ACTIVE"
        );
    }

    private UUID insertSession(UUID studentId, String status, LocalDateTime expiresAt) {
        UUID sessionId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO adaptive_sessions (id, student_id, subject_id, topic_id, status, expires_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                sessionId, studentId, 1, 1, status,
                java.sql.Timestamp.valueOf(expiresAt)
        );
        return sessionId;
    }

    private record ExerciseContext(UUID exerciseId, String mlExerciseId) {}

    private ExerciseContext insertExercise(UUID sessionId, String mlExerciseId, String type,
                                           String answersRaw, String correctAnswersRaw) {
        UUID exerciseId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO adaptive_session_exercises " +
                        "(id, session_id, ml_exercise_id, exercise_text, exercise_type, answers_raw, correct_answers_raw) " +
                        "VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb)",
                exerciseId, sessionId, mlExerciseId,
                "Exercise text for " + mlExerciseId,
                type, answersRaw, correctAnswersRaw
        );
        return new ExerciseContext(exerciseId, mlExerciseId);
    }

    private MockHttpServletRequestBuilder authorizedPost(String urlTemplate, Object... uriVars) {
        return post(urlTemplate, uriVars)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf());
    }

    private MockHttpServletRequestBuilder authorizedGet(String urlTemplate, Object... uriVars) {
        return get(urlTemplate, uriVars)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON);
    }

    private AdaptiveSubmitRequestDto buildRequest(String exerciseId, List<String> givenAnswers) {
        AdaptiveSubmitRequestDto.AnswerDto answer = new AdaptiveSubmitRequestDto.AnswerDto();
        answer.setExerciseId(exerciseId);
        answer.setGivenAnswers(givenAnswers);
        answer.setTimeSpent(10);

        AdaptiveSubmitRequestDto request = new AdaptiveSubmitRequestDto();
        request.setAnswers(List.of(answer));
        return request;
    }

    private AdaptiveSubmitRequestDto emptyRequest() {
        AdaptiveSubmitRequestDto request = new AdaptiveSubmitRequestDto();
        request.setAnswers(List.of());
        return request;
    }

    @Test
    void createAdaptiveJob_shouldReturn202AndPersistJob() throws Exception {
        AiAdaptiveJobResponse response = new AiAdaptiveJobResponse();
        response.setJobId("adaptive-job-123");
        response.setStatus(AiRequestStatus.PENDING);
        when(aiApiClient.startAdaptiveJob(any(), anyInt(), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(authorizedPost("/api/v1/adaptive/jobs")
                        .content("{\"subjectId\":1,\"topicId\":2,\"count\":4}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").exists())
                .andExpect(jsonPath("$.status").value("PENDING"));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM adaptive_exercise_jobs WHERE student_id = ?",
                Integer.class,
                studentId
        );
        assertEquals(1, count);
    }

    @Test
    void getAdaptiveJobStatus_shouldReturnRunningWhenRemoteJobStillRunning() throws Exception {
        AiAdaptiveJobResponse createResponse = new AiAdaptiveJobResponse();
        createResponse.setJobId("adaptive-job-124");
        createResponse.setStatus(AiRequestStatus.PENDING);
        when(aiApiClient.startAdaptiveJob(any(), anyInt(), anyInt(), anyInt())).thenReturn(createResponse);

        String createResponseBody = mockMvc.perform(authorizedPost("/api/v1/adaptive/jobs")
                        .content("{\"subjectId\":1,\"topicId\":2,\"count\":4}"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String jobId = objectMapper.readTree(createResponseBody).get("jobId").asText();

        AiAdaptiveJobStatusResponse statusResponse = new AiAdaptiveJobStatusResponse();
        statusResponse.setJobId("adaptive-job-124");
        statusResponse.setStatus(AiRequestStatus.RUNNING);
        when(aiApiClient.getAdaptiveJobStatus("adaptive-job-124")).thenReturn(statusResponse);

        mockMvc.perform(authorizedGet("/api/v1/adaptive/jobs/{jobId}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId))
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.session").value(nullValue()));
    }

    @Test
    void getAdaptiveJobStatus_shouldReturnSessionWhenRemoteJobIsDone() throws Exception {
        AiAdaptiveJobResponse createResponse = new AiAdaptiveJobResponse();
        createResponse.setJobId("adaptive-job-125");
        createResponse.setStatus(AiRequestStatus.PENDING);
        when(aiApiClient.startAdaptiveJob(any(), anyInt(), anyInt(), anyInt())).thenReturn(createResponse);

        String createResponseBody = mockMvc.perform(authorizedPost("/api/v1/adaptive/jobs")
                        .content("{\"subjectId\":1,\"topicId\":2,\"count\":1}"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String jobId = objectMapper.readTree(createResponseBody).get("jobId").asText();

        AiAdaptiveExerciseDto exercise = new AiAdaptiveExerciseDto();
        exercise.setExerciseId("ex-1");
        exercise.setText("Question");
        exercise.setType(QuestionType.SINGLE_CHOICE);
        exercise.setAnswers(List.of("A", "B"));
        exercise.setCorrectAnswers(List.of("A"));
        exercise.setDifficulty(0.5);

        AiAdaptiveJobStatusResponse statusResponse = new AiAdaptiveJobStatusResponse();
        statusResponse.setJobId("adaptive-job-125");
        statusResponse.setStatus(AiRequestStatus.DONE);
        statusResponse.setExercises(List.of(exercise));
        when(aiApiClient.getAdaptiveJobStatus("adaptive-job-125")).thenReturn(statusResponse);

        mockMvc.perform(authorizedGet("/api/v1/adaptive/jobs/{jobId}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.session.sessionId").exists())
                .andExpect(jsonPath("$.session.exercises[0].exerciseId").value("ex-1"));
    }

    // =========================================================================
    // POST /api/v1/adaptive/sessions/{sessionId}/submit
    // =========================================================================

    @Test
    void submitSession_shouldReturn200_whenValidAndCorrectAnswer() throws Exception {
        when(aiApiClient.sendAdaptiveFeedback(any())).thenReturn(true);

        UUID sessionId = insertSession(studentId, "ACTIVE", LocalDateTime.now().plusHours(1));
        ExerciseContext ex = insertExercise(sessionId, "ex-1", "SINGLE_CHOICE",
                "[\"Java\", \"Python\"]", "[\"Java\"]");

        AdaptiveSubmitRequestDto request = buildRequest(ex.mlExerciseId(), List.of("Java"));

        mockMvc.perform(authorizedPost("/api/v1/adaptive/sessions/{sessionId}/submit", sessionId)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.totalScore").value(1.0))
                .andExpect(jsonPath("$.clientResults[0].correct").value(true))
                .andExpect(jsonPath("$.clientResults[0].score").value(1.0));
    }

    @Test
    void submitSession_shouldReturn200_whenAnswerIsWrong() throws Exception {
        when(aiApiClient.sendAdaptiveFeedback(any())).thenReturn(true);

        UUID sessionId = insertSession(studentId, "ACTIVE", LocalDateTime.now().plusHours(1));
        ExerciseContext ex = insertExercise(sessionId, "ex-1", "SINGLE_CHOICE",
                "[\"Java\", \"Python\"]", "[\"Java\"]");

        AdaptiveSubmitRequestDto request = buildRequest(ex.mlExerciseId(), List.of("Python"));

        mockMvc.perform(authorizedPost("/api/v1/adaptive/sessions/{sessionId}/submit", sessionId)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScore").value(0.0))
                .andExpect(jsonPath("$.clientResults[0].correct").value(false))
                .andExpect(jsonPath("$.clientResults[0].score").value(0.0));
    }

    @Test
    void submitSession_shouldReturn200_whenNoAnswerSubmittedForExercise() throws Exception {
        when(aiApiClient.sendAdaptiveFeedback(any())).thenReturn(true);

        UUID sessionId = insertSession(studentId, "ACTIVE", LocalDateTime.now().plusHours(1));
        insertExercise(sessionId, "ex-1", "SINGLE_CHOICE",
                "[\"Java\", \"Python\"]", "[\"Java\"]");

        mockMvc.perform(authorizedPost("/api/v1/adaptive/sessions/{sessionId}/submit", sessionId)
                        .content(objectMapper.writeValueAsString(emptyRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScore").value(0.0))
                .andExpect(jsonPath("$.clientResults[0].score").value(0.0));
    }

    @Test
    void submitSession_shouldReturn200_withPartialScoreForMultipleChoice() throws Exception {
        when(aiApiClient.sendAdaptiveFeedback(any())).thenReturn(true);

        UUID sessionId = insertSession(studentId, "ACTIVE", LocalDateTime.now().plusHours(1));
        ExerciseContext ex = insertExercise(sessionId, "ex-1", "MULTI_CHOICE",
                "[\"A\", \"B\", \"C\"]", "[\"A\", \"B\"]");

        // Studentul selectează doar A (subset corect) → 0.5
        AdaptiveSubmitRequestDto request = buildRequest(ex.mlExerciseId(), List.of("A"));

        mockMvc.perform(authorizedPost("/api/v1/adaptive/sessions/{sessionId}/submit", sessionId)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScore").value(0.5))
                .andExpect(jsonPath("$.clientResults[0].score").value(0.5));
    }

    @Test
    void submitSession_shouldReturn200_whenMultipleChoiceFullyCorrect() throws Exception {
        when(aiApiClient.sendAdaptiveFeedback(any())).thenReturn(true);

        UUID sessionId = insertSession(studentId, "ACTIVE", LocalDateTime.now().plusHours(1));
        ExerciseContext ex = insertExercise(sessionId, "ex-1", "MULTI_CHOICE",
                "[\"A\", \"B\", \"C\"]", "[\"A\", \"B\"]");

        AdaptiveSubmitRequestDto request = buildRequest(ex.mlExerciseId(), List.of("A", "B"));

        mockMvc.perform(authorizedPost("/api/v1/adaptive/sessions/{sessionId}/submit", sessionId)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScore").value(1.0))
                .andExpect(jsonPath("$.clientResults[0].score").value(1.0));
    }

    @Test
    void submitSession_shouldReturn200_andFeedbackSentTrue_whenAiSucceeds() throws Exception {
        when(aiApiClient.sendAdaptiveFeedback(any())).thenReturn(true);

        UUID sessionId = insertSession(studentId, "ACTIVE", LocalDateTime.now().plusHours(1));
        insertExercise(sessionId, "ex-1", "SINGLE_CHOICE",
                "[\"True\", \"False\"]", "[\"True\"]");

        mockMvc.perform(authorizedPost("/api/v1/adaptive/sessions/{sessionId}/submit", sessionId)
                        .content(objectMapper.writeValueAsString(emptyRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedbackSent").value(true));

        Boolean feedbackSent = jdbcTemplate.queryForObject(
                "SELECT ai_feedback_sent FROM adaptive_sessions WHERE id = ?",
                Boolean.class, sessionId
        );
        assertEquals(Boolean.TRUE, feedbackSent);
    }

    @Test
    void submitSession_shouldReturn200_andFeedbackSentFalse_whenAiFails() throws Exception {
        when(aiApiClient.sendAdaptiveFeedback(any())).thenReturn(false);

        UUID sessionId = insertSession(studentId, "ACTIVE", LocalDateTime.now().plusHours(1));
        insertExercise(sessionId, "ex-1", "SINGLE_CHOICE",
                "[\"True\", \"False\"]", "[\"True\"]");

        mockMvc.perform(authorizedPost("/api/v1/adaptive/sessions/{sessionId}/submit", sessionId)
                        .content(objectMapper.writeValueAsString(emptyRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedbackSent").value(false));
    }

    @Test
    void submitSession_shouldMarkSessionAsCompleted_afterSubmit() throws Exception {
        when(aiApiClient.sendAdaptiveFeedback(any())).thenReturn(true);

        UUID sessionId = insertSession(studentId, "ACTIVE", LocalDateTime.now().plusHours(1));

        mockMvc.perform(authorizedPost("/api/v1/adaptive/sessions/{sessionId}/submit", sessionId)
                        .content(objectMapper.writeValueAsString(emptyRequest())))
                .andExpect(status().isOk());

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM adaptive_sessions WHERE id = ?",
                String.class, sessionId
        );
        assertEquals("COMPLETED", status);
    }

    @Test
    void submitSession_shouldReturn404_whenSessionDoesNotExist() throws Exception {
        UUID missingSessionId = UUID.randomUUID();

        mockMvc.perform(authorizedPost("/api/v1/adaptive/sessions/{sessionId}/submit", missingSessionId)
                        .content(objectMapper.writeValueAsString(emptyRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("does not exist")));
    }

    @Test
    void submitSession_shouldReturn409_whenSessionIsNotActive() throws Exception {
        UUID sessionId = insertSession(studentId, "COMPLETED", LocalDateTime.now().plusHours(1));

        mockMvc.perform(authorizedPost("/api/v1/adaptive/sessions/{sessionId}/submit", sessionId)
                        .content(objectMapper.writeValueAsString(emptyRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("not active")));
    }

    @Test
    void submitSession_shouldReturn409_whenSessionIsExpiredByTime() throws Exception {
        UUID sessionId = insertSession(studentId, "ACTIVE", LocalDateTime.now().minusMinutes(5));

        mockMvc.perform(authorizedPost("/api/v1/adaptive/sessions/{sessionId}/submit", sessionId)
                        .content(objectMapper.writeValueAsString(emptyRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("expired")));

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM adaptive_sessions WHERE id = ?",
                String.class, sessionId
        );
        assertEquals("EXPIRED", status);
    }

    @Test
    void submitSession_shouldReturn404_whenSessionBelongsToAnotherStudent() throws Exception {
        UUID otherStudentId = UUID.randomUUID();
        insertUser(otherStudentId, RoleName.STUDENT);
        UUID sessionId = insertSession(otherStudentId, "ACTIVE", LocalDateTime.now().plusHours(1));

        mockMvc.perform(authorizedPost("/api/v1/adaptive/sessions/{sessionId}/submit", sessionId)
                        .content(objectMapper.writeValueAsString(emptyRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("does not exist")));

        jdbcTemplate.update("DELETE FROM adaptive_sessions WHERE student_id = ?", otherStudentId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", otherStudentId);
    }

    @Test
    void submitSession_shouldReturn401_whenNotAuthenticated() throws Exception {
        UUID sessionId = insertSession(studentId, "ACTIVE", LocalDateTime.now().plusHours(1));

        mockMvc.perform(post("/api/v1/adaptive/sessions/{sessionId}/submit", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyRequest()))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void submitSession_shouldReturn400_whenBodyIsMissing() throws Exception {
        UUID sessionId = insertSession(studentId, "ACTIVE", LocalDateTime.now().plusHours(1));

        mockMvc.perform(authorizedPost("/api/v1/adaptive/sessions/{sessionId}/submit", sessionId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitSession_shouldThrowException_whenDbContainsInvalidJson() throws Exception {
        UUID sessionId = insertSession(studentId, "ACTIVE", LocalDateTime.now().plusHours(1));
        
        // Insert an exercise with invalid JSON in the answers_raw column
        insertExercise(sessionId, "ex-1", "SINGLE_CHOICE", "[\"A\"]", "{\"bad\": \"data\"}");

        mockMvc.perform(authorizedPost("/api/v1/adaptive/sessions/{sessionId}/submit", sessionId)
                        .content(objectMapper.writeValueAsString(emptyRequest())))
                .andExpect(status().isUnprocessableEntity()); // Asumând că ValidationException returnează 422
    }

    @Test
    void submitSession_shouldReturn200_andScore0_whenGivenAnswersIsEmptyList() throws Exception {
        when(aiApiClient.sendAdaptiveFeedback(any())).thenReturn(true);
        UUID sessionId = insertSession(studentId, "ACTIVE", LocalDateTime.now().plusHours(1));
        ExerciseContext ex = insertExercise(sessionId, "ex-1", "SINGLE_CHOICE", "[\"A\", \"B\"]", "[\"A\"]");

        // Send an AnswerDto with an empty list of given answers
        AdaptiveSubmitRequestDto request = buildRequest(ex.mlExerciseId(), List.of());

        mockMvc.perform(authorizedPost("/api/v1/adaptive/sessions/{sessionId}/submit", sessionId)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScore").value(0.0));
    }

    @Test
    void submitSession_shouldReturn200_andScore0_whenGivenAnswersIsNull() throws Exception {
        when(aiApiClient.sendAdaptiveFeedback(any())).thenReturn(true);
        UUID sessionId = insertSession(studentId, "ACTIVE", LocalDateTime.now().plusHours(1));
        ExerciseContext ex = insertExercise(sessionId, "ex-1", "SINGLE_CHOICE", "[\"A\", \"B\"]", "[\"A\"]");

        // Send an AnswerDto with null given answers
        AdaptiveSubmitRequestDto request = buildRequest(ex.mlExerciseId(), null);

        mockMvc.perform(authorizedPost("/api/v1/adaptive/sessions/{sessionId}/submit", sessionId)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScore").value(0.0));
    }

    @Test
    void submitSession_shouldReturn200_andScore0_whenMultipleChoiceHasWrongAnswer() throws Exception {
        when(aiApiClient.sendAdaptiveFeedback(any())).thenReturn(true);
        UUID sessionId = insertSession(studentId, "ACTIVE", LocalDateTime.now().plusHours(1));
        ExerciseContext ex = insertExercise(sessionId, "ex-1", "MULTI_CHOICE",
                "[\"A\", \"B\", \"C\"]", "[\"A\", \"B\"]");

        // The user selects one correct answer (A) but also one incorrect answer (C) → total score should be 0.0
        AdaptiveSubmitRequestDto request = buildRequest(ex.mlExerciseId(), List.of("A", "C"));

        mockMvc.perform(authorizedPost("/api/v1/adaptive/sessions/{sessionId}/submit", sessionId)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScore").value(0.0));
    }

   @Test
    void submitSession_shouldThrowException_whenJsonSerializationFails() throws Exception {
        UUID sessionId = insertSession(studentId, "ACTIVE", LocalDateTime.now().plusHours(1));
        ExerciseContext ex = insertExercise(sessionId, "ex-1", "SINGLE_CHOICE", "[\"A\"]", "[\"A\"]");

        AdaptiveSubmitRequestDto request = buildRequest(ex.mlExerciseId(), List.of("A"));
        
        String requestJson = objectMapper.writeValueAsString(request);

        ObjectMapper fakeMapper = Mockito.mock(ObjectMapper.class);
        when(fakeMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Simulated JSON Error"));

        org.springframework.test.util.ReflectionTestUtils.setField(adaptiveSubmitService, "objectMapper", fakeMapper);

        try {
            mockMvc.perform(authorizedPost("/api/v1/adaptive/sessions/{sessionId}/submit", sessionId)
                            .content(requestJson))
                    .andExpect(status().isUnprocessableEntity());
        } finally {
            org.springframework.test.util.ReflectionTestUtils.setField(adaptiveSubmitService, "objectMapper", objectMapper);
        }
    }

    @Test
    void submitSession_shouldReturn200_andScore0_whenCorrectAnswersIsEmpty() throws Exception {
        when(aiApiClient.sendAdaptiveFeedback(any())).thenReturn(true);
        UUID sessionId = insertSession(studentId, "ACTIVE", LocalDateTime.now().plusHours(1));
        
        ExerciseContext ex = insertExercise(sessionId, "ex-1", "SINGLE_CHOICE", "[\"A\", \"B\"]", "[]");

        AdaptiveSubmitRequestDto request = buildRequest(ex.mlExerciseId(), List.of("A"));

        mockMvc.perform(authorizedPost("/api/v1/adaptive/sessions/{sessionId}/submit", sessionId)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScore").value(0.0));
    }

    @Test
    void submitSession_shouldReturn200_whenTypeIsTrueFalse() throws Exception {
        when(aiApiClient.sendAdaptiveFeedback(any())).thenReturn(true);
        UUID sessionId = insertSession(studentId, "ACTIVE", LocalDateTime.now().plusHours(1));
        
        ExerciseContext ex = insertExercise(sessionId, "ex-1", "TRUE_FALSE", "[\"True\", \"False\"]", "[\"False\"]");

        AdaptiveSubmitRequestDto request = buildRequest(ex.mlExerciseId(), List.of("False"));

        mockMvc.perform(authorizedPost("/api/v1/adaptive/sessions/{sessionId}/submit", sessionId)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScore").value(1.0));
    }

    @Test
    void submitSession_shouldReturn200_andScore0_whenMultipleChoiceHasNoCorrectAnswers() throws Exception {
        when(aiApiClient.sendAdaptiveFeedback(any())).thenReturn(true);
        UUID sessionId = insertSession(studentId, "ACTIVE", LocalDateTime.now().plusHours(1));
        ExerciseContext ex = insertExercise(sessionId, "ex-1", "MULTI_CHOICE", "[\"A\", \"B\", \"C\"]", "[\"A\", \"B\"]");

        AdaptiveSubmitRequestDto request = buildRequest(ex.mlExerciseId(), List.of("C"));

        mockMvc.perform(authorizedPost("/api/v1/adaptive/sessions/{sessionId}/submit", sessionId)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScore").value(0.0));
    }

    @Test
    void submitSession_shouldReturn200_andScore0_whenExerciseTypeIsUnknown() throws Exception {
        when(aiApiClient.sendAdaptiveFeedback(any())).thenReturn(true);
        UUID sessionId = insertSession(studentId, "ACTIVE", LocalDateTime.now().plusHours(1));
        
        ExerciseContext ex = insertExercise(sessionId, "ex-1", "UNKNOWN_FORMAT", "[\"A\", \"B\"]", "[\"A\"]");

        AdaptiveSubmitRequestDto request = buildRequest(ex.mlExerciseId(), List.of("A"));

        mockMvc.perform(authorizedPost("/api/v1/adaptive/sessions/{sessionId}/submit", sessionId)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScore").value(0.0));
    }

    // =========================================================================
    // POST /api/v1/adaptive/start
    // =========================================================================

    @Test
    void startSession_shouldReturn200_andHideCorrectAnswers_whenSuccessful() throws Exception {
        AiAdaptiveExerciseDto mockExercise = new AiAdaptiveExerciseDto();
        mockExercise.setExerciseId("ai-123");
        mockExercise.setText("Care este capitala Frantei?");
        mockExercise.setType(QuestionType.SINGLE_CHOICE);
        mockExercise.setAnswers(List.of("Paris", "Londra"));
        mockExercise.setCorrectAnswers(List.of("Paris"));
        mockExercise.setDifficulty(0.5);

        AiAdaptiveResponse mockResponse = new AiAdaptiveResponse();
        mockResponse.setExercises(List.of(mockExercise));

        when(aiApiClient.requestAdaptiveExercises(any(), any(), anyInt(), anyInt(), anyInt()))
                .thenReturn(mockResponse);

        AdaptiveStartRequestDto request = new AdaptiveStartRequestDto();
        request.setSubjectId(1);
        request.setTopicId(10);
        request.setCount(5);

        String responseJson = mockMvc.perform(post("/api/v1/adaptive/start")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").exists())
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.exercises[0].exerciseId").value("ai-123"))
                .andExpect(jsonPath("$.exercises[0].correctAnswers").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        String sessionId = objectMapper.readTree(responseJson).get("sessionId").asText();

        String correctAnswersDb = jdbcTemplate.queryForObject(
                "SELECT correct_answers_raw FROM adaptive_session_exercises WHERE session_id = CAST(? AS uuid) LIMIT 1",
                String.class,
                sessionId
        );

        assertTrue(correctAnswersDb.contains("Paris"));
    }

    @Test
    void startSession_shouldReturn503_whenAiFails() throws Exception {
        when(aiApiClient.requestAdaptiveExercises(any(), any(), anyInt(), anyInt(), anyInt()))
                .thenThrow(new AiApiException("AI is down"));

        AdaptiveStartRequestDto request = new AdaptiveStartRequestDto();
        request.setSubjectId(1);
        request.setTopicId(10);
        request.setCount(5);

        mockMvc.perform(post("/api/v1/adaptive/start")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void startSession_shouldReturn401_whenNotAuthenticated() throws Exception {
        AdaptiveStartRequestDto request = new AdaptiveStartRequestDto();
        request.setSubjectId(1);
        request.setTopicId(10);
        request.setCount(5);

        mockMvc.perform(post("/api/v1/adaptive/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
