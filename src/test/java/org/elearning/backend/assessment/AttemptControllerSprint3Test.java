package org.elearning.backend.assessment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elearning.backend.assessment.dto.test_dto.SubmitAnswerDto;
import org.elearning.backend.assessment.dto.test_dto.SubmitRequestDto;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.jwt.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AttemptControllerSprint3Test {

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
        jdbcTemplate.execute("DELETE FROM lesson_progress");
        jdbcTemplate.execute("DELETE FROM lessons");
        jdbcTemplate.execute("DELETE FROM chapters");
        jdbcTemplate.execute("DELETE FROM course_enrollments");
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

    private record TestContext(UUID testId, UUID courseId, UUID chapterId, UUID lessonId, Integer questionId, Integer correctOptionId) {}

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

        return new TestContext(testId, courseId, chapterId, lessonId, questionId, correctOptionId);
    }

    private UUID insertEnrollment(UUID studentId, UUID courseId) {
        UUID enrollmentId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO course_enrollments (id, student_id, course_id, enrolled_at) VALUES (?, ?, ?, NOW())",
                enrollmentId, studentId, courseId
        );
        return enrollmentId;
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

    private SubmitRequestDto submitRequestForAnswer(Integer questionId, Integer selectedOptionId) {
        SubmitAnswerDto answer = new SubmitAnswerDto();
        answer.setQuestionId(questionId);
        answer.setSelectedOptionIds(List.of(selectedOptionId));
        answer.setTimeSpent(BigDecimal.valueOf(30));

        SubmitRequestDto request = new SubmitRequestDto();
        request.setAnswers(List.of(answer));
        return request;
    }

    // =========================================================================
    // POST /api/v1/attempts/{attemptId}/submit - SPRINT 3 (100% COVERAGE)
    // =========================================================================

    @Test
    void submitAttempt_shouldMarkProgress_whenEnrollmentExists() throws Exception {
        TestContext context = insertTestWithQuestion("PUBLISHED", 1800);
        UUID enrollmentId = insertEnrollment(studentId, context.courseId());
        UUID attemptId = insertAttempt(context.testId(), studentId, 1, "IN_PROGRESS", "NOW()");

        mockMvc.perform(authorized(post("/api/v1/attempts/{attemptId}/submit", attemptId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                submitRequestForAnswer(context.questionId(), context.correctOptionId())
                        )))
                .andExpect(status().isOk());

        Integer progressCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lesson_progress WHERE lesson_id = ? AND student_id = ? AND enrollment_id = ?",
                Integer.class,
                context.lessonId(), studentId, enrollmentId
        );
        assertEquals(1, progressCount, "Progresul trebuie salvat dacă studentul e înrolat.");
    }

    @Test
    void submitAttempt_shouldNotMarkProgress_whenNoEnrollmentExists() throws Exception {
        TestContext context = insertTestWithQuestion("PUBLISHED", 1800);
        // NU apelăm insertEnrollment
        UUID attemptId = insertAttempt(context.testId(), studentId, 1, "IN_PROGRESS", "NOW()");

        mockMvc.perform(authorized(post("/api/v1/attempts/{attemptId}/submit", attemptId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                submitRequestForAnswer(context.questionId(), context.correctOptionId())
                        )))
                .andExpect(status().isOk());

        Integer progressCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lesson_progress WHERE lesson_id = ? AND student_id = ?",
                Integer.class,
                context.lessonId(), studentId
        );
        assertEquals(0, progressCount, "Progresul NU trebuie salvat dacă studentul nu e înrolat.");
    }

    @Test
    void submitAttempt_shouldNotMarkProgress_whenCourseNotFound() throws Exception {
        TestContext context = insertTestWithQuestion("PUBLISHED", 1800);
        jdbcTemplate.update("DELETE FROM courses WHERE id = ?", context.courseId());
        UUID attemptId = insertAttempt(context.testId(), studentId, 1, "IN_PROGRESS", "NOW()");

        mockMvc.perform(authorized(post("/api/v1/attempts/{attemptId}/submit", attemptId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                submitRequestForAnswer(context.questionId(), context.correctOptionId())
                        )))
                .andExpect(status().isOk());

        Integer progressCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lesson_progress WHERE lesson_id = ? AND student_id = ?",
                Integer.class,
                context.lessonId(), studentId
        );
        assertEquals(0, progressCount, "Progresul NU trebuie salvat dacă lipsește cursul.");
    }

    @Test
    void submitAttempt_shouldNotMarkProgress_whenChapterNotFound() throws Exception {
        TestContext context = insertTestWithQuestion("PUBLISHED", 1800);
        jdbcTemplate.update("DELETE FROM chapters WHERE id = ?", context.chapterId());
        UUID attemptId = insertAttempt(context.testId(), studentId, 1, "IN_PROGRESS", "NOW()");

        mockMvc.perform(authorized(post("/api/v1/attempts/{attemptId}/submit", attemptId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                submitRequestForAnswer(context.questionId(), context.correctOptionId())
                        )))
                .andExpect(status().isOk());

        Integer progressCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lesson_progress WHERE lesson_id = ? AND student_id = ?",
                Integer.class,
                context.lessonId(), studentId
        );
        assertEquals(0, progressCount, "Progresul NU trebuie salvat dacă lipsește capitolul.");
    }
}