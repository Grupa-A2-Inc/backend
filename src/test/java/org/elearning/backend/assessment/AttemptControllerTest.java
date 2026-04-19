package org.elearning.backend.assessment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elearning.backend.assessment.dto.test_dto.SubmitAnswerDto;
import org.elearning.backend.assessment.dto.test_dto.SubmitRequestDto;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AttemptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    private UUID studentId;
    private String accessToken;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        insertStudent(studentId);
        accessToken = jwtUtil.generateAccessToken(studentId, RoleName.STUDENT);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM attempt_answers");
        jdbcTemplate.execute("DELETE FROM test_results");
        jdbcTemplate.execute("DELETE FROM test_attempts");
        jdbcTemplate.execute("DELETE FROM question_options");
        jdbcTemplate.execute("DELETE FROM questions");
        jdbcTemplate.execute("DELETE FROM tests");
        jdbcTemplate.execute("DELETE FROM lessons");
        jdbcTemplate.execute("DELETE FROM chapters");
        jdbcTemplate.execute("DELETE FROM courses");
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", studentId);
    }

    private void insertStudent(UUID userId) {
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, role_type, status) " +
                        "VALUES (?, ?, ?, ?, ?, (SELECT id FROM roles WHERE name = CAST(? AS role_name)), ?, CAST(? AS user_status))",
                userId,
                "student-" + userId + "@test.com",
                "password-hash",
                "Test",
                "Student",
                RoleName.STUDENT.name(),
                "STUDENT",
                "ACTIVE"
        );
    }

    private MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder requestBuilder) {
        return requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    }

    private record TestContext(UUID testId, Integer questionId, Integer correctOptionId) {}

    private TestContext insertTestWithQuestion(String status, int timeLimitSec) {
        return insertTestWithQuestion(status, timeLimitSec, false);
    }

    private TestContext insertTestWithQuestion(String status, int timeLimitSec, boolean aiEnabled) {
        UUID courseId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID testId = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO courses (id, title, created_by, status, visibility) " +
                        "VALUES (?, ?, ?, CAST(? AS course_status), CAST(? AS course_visibility))",
                courseId, "Course for attempt tests", UUID.randomUUID(), "DRAFT", "PRIVATE"
        );

        jdbcTemplate.update(
                "INSERT INTO chapters (id, course_id, title, order_index) VALUES (?, ?, ?, ?)",
                chapterId, courseId, "Chapter for attempt tests", 1
        );

        jdbcTemplate.update(
                "INSERT INTO lessons (id, chapter_id, title, order_index) VALUES (?, ?, ?, ?)",
                lessonId, chapterId, "Lesson for attempt tests", 1
        );

        jdbcTemplate.update(
                "INSERT INTO tests (id, lesson_id, created_by, title, description, time_limit_sec, ai_enabled, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS test_status))",
                testId,
                lessonId,
                UUID.randomUUID(),
                "Test Java",
                "Integration test",
                timeLimitSec,
                aiEnabled,
                status
        );

        Integer questionId = jdbcTemplate.queryForObject(
                "INSERT INTO questions (test_id, question_type, content, difficulty, is_active) " +
                        "VALUES (?, CAST(? AS question_type), ?, ?, ?) RETURNING id",
                Integer.class,
                testId, "SINGLE_CHOICE", "What is Java?", BigDecimal.valueOf(1.00), true
        );

        Integer correctOptionId = jdbcTemplate.queryForObject(
                "INSERT INTO question_options (question_id, text, display_order, is_correct) " +
                        "VALUES (?, ?, ?, ?) RETURNING id",
                Integer.class,
                questionId, "A programming language", 1, true
        );

        jdbcTemplate.update(
                "INSERT INTO question_options (question_id, text, display_order, is_correct) VALUES (?, ?, ?, ?)",
                questionId, "A database", 2, false
        );

        return new TestContext(testId, questionId, correctOptionId);
    }

    private UUID insertTestWithoutQuestions(String status, int timeLimitSec, boolean aiEnabled) {
        UUID courseId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID testId = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO courses (id, title, created_by, status, visibility) " +
                        "VALUES (?, ?, ?, CAST(? AS course_status), CAST(? AS course_visibility))",
                courseId, "Course for attempt tests", UUID.randomUUID(), "DRAFT", "PRIVATE"
        );

        jdbcTemplate.update(
                "INSERT INTO chapters (id, course_id, title, order_index) VALUES (?, ?, ?, ?)",
                chapterId, courseId, "Chapter for attempt tests", 1
        );

        jdbcTemplate.update(
                "INSERT INTO lessons (id, chapter_id, title, order_index) VALUES (?, ?, ?, ?)",
                lessonId, chapterId, "Lesson for attempt tests", 1
        );

        jdbcTemplate.update(
                "INSERT INTO tests (id, lesson_id, created_by, title, description, time_limit_sec, ai_enabled, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS test_status))",
                testId,
                lessonId,
                UUID.randomUUID(),
                "Test Java",
                "Integration test",
                timeLimitSec,
                aiEnabled,
                status
        );

        return testId;
    }

    private TestContext insertAdditionalQuestion(UUID testId, String content) {
        Integer questionId = jdbcTemplate.queryForObject(
                "INSERT INTO questions (test_id, question_type, content, difficulty, is_active) " +
                        "VALUES (?, CAST(? AS question_type), ?, ?, ?) RETURNING id",
                Integer.class,
                testId, "SINGLE_CHOICE", content, BigDecimal.valueOf(1.00), true
        );

        Integer correctOptionId = jdbcTemplate.queryForObject(
                "INSERT INTO question_options (question_id, text, display_order, is_correct) " +
                        "VALUES (?, ?, ?, ?) RETURNING id",
                Integer.class,
                questionId, "Correct", 1, true
        );

        jdbcTemplate.update(
                "INSERT INTO question_options (question_id, text, display_order, is_correct) VALUES (?, ?, ?, ?)",
                questionId, "Wrong", 2, false
        );

        return new TestContext(testId, questionId, correctOptionId);
    }

    private UUID insertAttempt(UUID testId, UUID studentId, int attemptNumber, String status, String startedAtExpression) {
        UUID attemptId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO test_attempts (id, test_id, student_id, attempt_number, started_at, status) " +
                        "VALUES (?, ?, ?, ?, " + startedAtExpression + ", CAST(? AS attempt_status))",
                attemptId, testId, studentId, attemptNumber, status
        );
        return attemptId;
    }

    private SubmitRequestDto mockSubmitRequest() {
        SubmitAnswerDto answer = new SubmitAnswerDto();
        answer.setQuestionId(1);
        answer.setSelectedOptionIds(List.of(1));
        answer.setTimeSpent(BigDecimal.valueOf(30));

        SubmitRequestDto request = new SubmitRequestDto();
        request.setAnswers(List.of(answer));
        return request;
    }

    private SubmitRequestDto submitRequestForAnswer(Integer questionId, Integer selectedOptionId) {
        SubmitAnswerDto answer = new SubmitAnswerDto();
        answer.setQuestionId(questionId);
        answer.setSelectedOptionIds(List.of(selectedOptionId));
        answer.setTimeSpent(BigDecimal.valueOf(30));

        SubmitRequestDto request = new SubmitRequestDto();
        request.setAnswers(List.of(answer));
        return request;
    }

    private SubmitRequestDto emptySubmitRequest() {
        SubmitRequestDto request = new SubmitRequestDto();
        request.setAnswers(List.of());
        return request;
    }

    // =========================================================================
    // POST /api/v1/tests/{testId}/start
    // =========================================================================

    @Test
    void startAttempt_shouldReturn200_whenValid() throws Exception {
        TestContext context = insertTestWithQuestion("PUBLISHED", 1800);

        mockMvc.perform(authorized(post("/api/v1/tests/{testId}/start", context.testId()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptId").exists())
                .andExpect(jsonPath("$.attemptNumber").value(1))
                .andExpect(jsonPath("$.timeLimitSec").value(1800))
                .andExpect(jsonPath("$.test.title").value("Test Java"));
    }

    @Test
    void startAttempt_shouldReturn403_whenTestNotFoundIsRejectedByPreAuth() throws Exception {
        UUID missingTestId = UUID.randomUUID();

        mockMvc.perform(authorized(post("/api/v1/tests/{testId}/start", missingTestId))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Access Denied")));
    }

    @Test
    void startAttempt_shouldReturn400_whenTestNotPublished() throws Exception {
        TestContext context = insertTestWithQuestion("DRAFT", 1800);

        mockMvc.perform(authorized(post("/api/v1/tests/{testId}/start", context.testId()))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("is not published yet")));
    }

    // =========================================================================
    // POST /api/v1/attempts/{attemptId}/submit
    // =========================================================================

    @Test
    void submitAttempt_shouldReturn200_whenValid() throws Exception {
        TestContext context = insertTestWithQuestion("PUBLISHED", 1800);
        UUID attemptId = insertAttempt(context.testId(), studentId, 1, "IN_PROGRESS", "NOW()");

        SubmitAnswerDto answer = new SubmitAnswerDto();
        answer.setQuestionId(context.questionId());
        answer.setSelectedOptionIds(List.of(context.correctOptionId()));
        answer.setTimeSpent(BigDecimal.valueOf(30));

        SubmitRequestDto request = new SubmitRequestDto();
        request.setAnswers(List.of(answer));

        mockMvc.perform(authorized(post("/api/v1/attempts/{attemptId}/submit", attemptId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptId").value(attemptId.toString()))
                .andExpect(jsonPath("$.score").value(1.0))
                .andExpect(jsonPath("$.scorePercent").value(100.0))
                .andExpect(jsonPath("$.passed").value(true));
    }

    @Test
    void submitAttempt_shouldReturn403_whenAttemptNotFoundIsRejectedByPreAuth() throws Exception {
        UUID missingAttemptId = UUID.randomUUID();

        mockMvc.perform(authorized(post("/api/v1/attempts/{attemptId}/submit", missingAttemptId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockSubmitRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Access Denied")));
    }

    @Test
    void submitAttempt_shouldReturn409_whenAlreadySubmitted() throws Exception {
        TestContext context = insertTestWithQuestion("PUBLISHED", 1800);
        UUID attemptId = insertAttempt(context.testId(), studentId, 1, "DONE", "NOW()");

        mockMvc.perform(authorized(post("/api/v1/attempts/{attemptId}/submit", attemptId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockSubmitRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already been submitted")));
    }

    @Test
    void submitAttempt_shouldReturn410_whenTimerExpired() throws Exception {
        TestContext context = insertTestWithQuestion("PUBLISHED", 1800);
        UUID attemptId = insertAttempt(context.testId(), studentId, 1, "EXPIRED", "NOW()");

        mockMvc.perform(authorized(post("/api/v1/attempts/{attemptId}/submit", attemptId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockSubmitRequest())))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.message", containsString("expired")));
    }

    @Test
    void submitAttempt_shouldReturn403_whenAttemptBelongsToAnotherStudent() throws Exception {
        TestContext context = insertTestWithQuestion("PUBLISHED", 1800);
        UUID anotherStudentId = UUID.randomUUID();
        UUID attemptId = insertAttempt(context.testId(), anotherStudentId, 1, "IN_PROGRESS", "NOW()");

        mockMvc.perform(authorized(post("/api/v1/attempts/{attemptId}/submit", attemptId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                submitRequestForAnswer(context.questionId(), context.correctOptionId())
                        )))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Access Denied")));
    }

    @Test
    void submitAttempt_shouldReturn410_whenTimeLimitExceededDuringSubmit() throws Exception {
        TestContext context = insertTestWithQuestion("PUBLISHED", 1);
        UUID attemptId = insertAttempt(
                context.testId(),
                studentId,
                1,
                "IN_PROGRESS",
                "NOW() - INTERVAL '5 minutes'"
        );

        mockMvc.perform(authorized(post("/api/v1/attempts/{attemptId}/submit", attemptId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                submitRequestForAnswer(context.questionId(), context.correctOptionId())
                        )))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.message", containsString("time limit")));

        String attemptStatus = jdbcTemplate.queryForObject(
                "SELECT status::text FROM test_attempts WHERE id = ?",
                String.class,
                attemptId
        );
        assertEquals("EXPIRED", attemptStatus);
    }

    @Test
    void submitAttempt_shouldReturnPartialScore_whenSomeQuestionsAreUnanswered() throws Exception {
        TestContext firstQuestion = insertTestWithQuestion("PUBLISHED", 1800);
        insertAdditionalQuestion(firstQuestion.testId(), "Second question");

        UUID attemptId = insertAttempt(firstQuestion.testId(), studentId, 1, "IN_PROGRESS", "NOW()");

        mockMvc.perform(authorized(post("/api/v1/attempts/{attemptId}/submit", attemptId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                submitRequestForAnswer(firstQuestion.questionId(), firstQuestion.correctOptionId())
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(0.5))
                .andExpect(jsonPath("$.scorePercent").value(50.0))
                .andExpect(jsonPath("$.passed").value(false))
                .andExpect(jsonPath("$.questions.length()").value(2));
    }

    @Test
    void submitAttempt_shouldReturnPartialScore_whenSomeQuestionsAnswersAreWrong() throws Exception {
        TestContext firstQuestion = insertTestWithQuestion("PUBLISHED", 1800);
        TestContext secondQuestion = insertAdditionalQuestion(firstQuestion.testId(), "Second question");

        UUID attemptId = insertAttempt(firstQuestion.testId(), studentId, 1, "IN_PROGRESS", "NOW()");

        SubmitAnswerDto correctAns = new SubmitAnswerDto();
        correctAns.setQuestionId(firstQuestion.questionId());
        correctAns.setSelectedOptionIds(List.of(firstQuestion.correctOptionId()));
        correctAns.setTimeSpent(BigDecimal.valueOf(10));

        SubmitAnswerDto wrongAns = new SubmitAnswerDto();
        wrongAns.setQuestionId(secondQuestion.questionId());

        wrongAns.setSelectedOptionIds(Collections.emptyList());
        wrongAns.setTimeSpent(BigDecimal.valueOf(15));

        SubmitRequestDto requestBody = new SubmitRequestDto();
        requestBody.setAnswers(List.of(correctAns, wrongAns));

        mockMvc.perform(authorized(post("/api/v1/attempts/{attemptId}/submit", attemptId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(0.5))
                .andExpect(jsonPath("$.scorePercent").value(50.0))
                .andExpect(jsonPath("$.passed").value(false))
                .andExpect(jsonPath("$.questions.length()").value(2));
    }

    @Test
    void submitAttempt_shouldReturnZeroScore_whenTestHasNoQuestions_andAiEnabled() throws Exception {
        UUID testId = insertTestWithoutQuestions("PUBLISHED", 1800, true);
        UUID attemptId = insertAttempt(testId, studentId, 1, "IN_PROGRESS", "NOW()");

        mockMvc.perform(authorized(post("/api/v1/attempts/{attemptId}/submit", attemptId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptySubmitRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(0))
                .andExpect(jsonPath("$.scorePercent").value(0))
                .andExpect(jsonPath("$.passed").value(false))
                .andExpect(jsonPath("$.questions.length()").value(0));
    }

    @Test
    void submitAttempt_shouldReturn404_whenSubmittedQuestionDoesNotExist() throws Exception {
        TestContext context = insertTestWithQuestion("PUBLISHED", 1800);
        UUID attemptId = insertAttempt(context.testId(), studentId, 1, "IN_PROGRESS", "NOW()");

        SubmitAnswerDto invalidAnswer = new SubmitAnswerDto();
        invalidAnswer.setQuestionId(999999);
        invalidAnswer.setSelectedOptionIds(List.of(context.correctOptionId()));
        invalidAnswer.setTimeSpent(BigDecimal.valueOf(10));

        SubmitRequestDto requestBody = new SubmitRequestDto();
        requestBody.setAnswers(List.of(invalidAnswer));

        mockMvc.perform(authorized(post("/api/v1/attempts/{attemptId}/submit", attemptId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("question")))
                .andExpect(jsonPath("$.message", containsString("does not exist")));

        Integer answersCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attempt_answers WHERE attempt_id = ?",
                Integer.class,
                attemptId
        );
        Integer resultCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM test_results WHERE attempt_id = ?",
                Integer.class,
                attemptId
        );
        String attemptStatus = jdbcTemplate.queryForObject(
                "SELECT status::text FROM test_attempts WHERE id = ?",
                String.class,
                attemptId
        );

        assertEquals(0, answersCount);
        assertEquals(0, resultCount);
        assertEquals("IN_PROGRESS", attemptStatus);
    }

    @Test
    void submitAttempt_shouldReturn400_whenBodyMissing() throws Exception {
        TestContext context = insertTestWithQuestion("PUBLISHED", 1800);
        UUID attemptId = insertAttempt(context.testId(), studentId, 1, "IN_PROGRESS", "NOW()");

        mockMvc.perform(authorized(post("/api/v1/attempts/{attemptId}/submit", attemptId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
