package org.elearning.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elearning.backend.ai.dto.InjectRequestDto;
import org.elearning.backend.auth.service.EmailService;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.jwt.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private EmailService emailService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    private UUID teacherId;
    private String teacherToken;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        insertUser(teacherId, RoleName.TEACHER);
        teacherToken = jwtUtil.generateAccessToken(teacherId, RoleName.TEACHER);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM ai_question_requests");
        jdbcTemplate.execute("DELETE FROM question_options");
        jdbcTemplate.execute("DELETE FROM questions");
        jdbcTemplate.execute("DELETE FROM tests");
        jdbcTemplate.execute("DELETE FROM lessons");
        jdbcTemplate.execute("DELETE FROM chapters");
        jdbcTemplate.execute("DELETE FROM courses");
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", teacherId);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void insertUser(UUID userId, RoleName role) {
        String roleType = role == RoleName.STUDENT ? "STUDENT"
                : role == RoleName.PARENT ? "PARENT"
                : "User";
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, role_type, status) " +
                        "VALUES (?, ?, ?, ?, ?, (SELECT id FROM roles WHERE name = CAST(? AS role_name)), ?, CAST(? AS user_status))",
                userId,
                role.name().toLowerCase() + "-" + userId + "@test.com",
                "password-hash",
                "Test",
                role.name(),
                role.name(),
                roleType,
                "ACTIVE"
        );
    }

    private record LessonContext(UUID courseId, UUID lessonId) {}

    private LessonContext insertLessonOwnedBy(UUID creatorId) {
        UUID courseId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO courses (id, title, created_by, status, visibility) " +
                        "VALUES (?, ?, ?, CAST(? AS course_status), CAST(? AS course_visibility))",
                courseId, "Course", creatorId, "PUBLISHED", "PUBLIC"
        );
        jdbcTemplate.update(
                "INSERT INTO chapters (id, course_id, title, order_index) VALUES (?, ?, ?, ?)",
                chapterId, courseId, "Chapter", 1
        );
        jdbcTemplate.update(
                "INSERT INTO lessons (id, chapter_id, title, order_index) VALUES (?, ?, ?, ?)",
                lessonId, chapterId, "Lesson", 1
        );

        return new LessonContext(courseId, lessonId);
    }

    private UUID insertAiRequest(UUID lessonId, String status, String generatedQuestions) {
        UUID requestId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO ai_question_requests (id, lesson_id, status, generated_questions) VALUES (?, ?, ?, ?)",
                requestId, lessonId, status, generatedQuestions
        );
        return requestId;
    }

    private UUID insertTest(UUID lessonId, UUID createdBy) {
        UUID testId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO tests (id, lesson_id, created_by, title, time_limit_sec, status, ai_enabled) " +
                        "VALUES (?, ?, ?, ?, ?, CAST(? AS test_status), ?)",
                testId, lessonId, createdBy, "Existing Test", 1800, "DRAFT", true
        );
        return testId;
    }

    private String validGeneratedQuestionsJson() {
        return "[{" +
                "\"text\": \"What is Java?\"," +
                "\"type\": \"SINGLE_CHOICE\"," +
                                "\"answers\": [\"A language\", \"A database\"]," +
                                "\"correctAnswers\": [\"A language\"]," +
                                "\"difficulty\": 0.6" +
                "}]";
    }

    private String multipleQuestionsJson() {
        return "[" +
                                "{\"text\": \"Q1?\", \"type\": \"SINGLE_CHOICE\", \"answers\": [\"A\", \"B\"], \"correctAnswers\": [\"A\"], \"difficulty\": 0.5}," +
                                "{\"text\": \"Q2?\", \"type\": \"TRUE_FALSE\", \"answers\": [\"True\", \"False\"], \"correctAnswers\": [\"True\"], \"difficulty\": 0.4}" +
                "]";
    }

    private String invalidQuestionsJson_emptyText() {
        return "[{" +
                "\"text\": \"\"," +
                "\"type\": \"SINGLE_CHOICE\"," +
                                "\"answers\": [\"A\", \"B\"]," +
                                "\"correctAnswers\": [\"A\"]," +
                                "\"difficulty\": 0.5" +
                "}]";
    }

    private String invalidQuestionsJson_noCorrectAnswer() {
        return "[{" +
                "\"text\": \"What is Java?\"," +
                "\"type\": \"SINGLE_CHOICE\"," +
                                "\"answers\": [\"A language\", \"A database\"]," +
                                "\"correctAnswers\": []," +
                                "\"difficulty\": 0.7" +
                "}]";
    }

    private String invalidQuestionsJson_singleChoiceTwoCorrect() {
        return "[{" +
                "\"text\": \"What is Java?\"," +
                "\"type\": \"SINGLE_CHOICE\"," +
                                "\"answers\": [\"A language\", \"A database\"]," +
                                "\"correctAnswers\": [\"A language\", \"A database\"]," +
                                "\"difficulty\": 0.6" +
                "}]";
    }

        private String invalidQuestionsJson_lessThanTwoOptions() {
                return "[{" +
                                "\"text\": \"Question with one option\"," +
                                "\"type\": \"SINGLE_CHOICE\"," +
                                "\"answers\": [\"Only option\"]," +
                                "\"correctAnswers\": [\"Only option\"]," +
                                "\"difficulty\": 0.2" +
                                "}]";
        }

        private String invalidQuestionsJson_trueFalseWrongOptionCount() {
                return "[{" +
                                "\"text\": \"Java is compiled?\"," +
                                "\"type\": \"TRUE_FALSE\"," +
                                "\"answers\": [\"True\", \"False\", \"Maybe\"]," +
                                "\"correctAnswers\": [\"True\"]," +
                                "\"difficulty\": 0.3" +
                                "}]";
        }

        private String invalidQuestionsJson_trueFalseTwoCorrectAnswers() {
                return "[{" +
                                "\"text\": \"Java is object oriented?\"," +
                                "\"type\": \"TRUE_FALSE\"," +
                                "\"answers\": [\"True\", \"False\"]," +
                                "\"correctAnswers\": [\"True\", \"False\"]," +
                                "\"difficulty\": 0.3" +
                                "}]";
        }

        private String validMultipleChoiceQuestionJson() {
                return "[{" +
                                "\"text\": \"Select JVM languages\"," +
                                "\"type\": \"MULTI_CHOICE\"," +
                                "\"answers\": [\"Java\", \"Kotlin\", \"MySQL\"]," +
                                "\"correctAnswers\": [\"Java\", \"Kotlin\"]," +
                                "\"difficulty\": 0.8" +
                                "}]";
        }

        private String invalidQuestionsJson_nullText() {
                return "[{" +
                                "\"text\": null," +
                                "\"type\": \"SINGLE_CHOICE\"," +
                                "\"answers\": [\"A\", \"B\"]," +
                                "\"correctAnswers\": [\"A\"]," +
                                "\"difficulty\": 0.5" +
                                "}]";
        }

        private String invalidQuestionsJson_nullOptions() {
                return "[{" +
                                "\"text\": \"Question without options\"," +
                                "\"type\": \"SINGLE_CHOICE\"," +
                                "\"answers\": null," +
                                "\"correctAnswers\": [\"A\"]," +
                                "\"difficulty\": 0.5" +
                                "}]";
        }

        private String invalidQuestionsJson_nullQuestionType() {
                return "[{" +
                                "\"text\": \"Question with missing type\"," +
                                "\"type\": null," +
                                "\"answers\": [\"A\", \"B\"]," +
                                "\"correctAnswers\": [\"A\"]," +
                                "\"difficulty\": 0.5" +
                                "}]";
        }

        private String invalidQuestionsJson_correctAnswerNotInAnswers() {
                return "[{" +
                                "\"text\": \"Pick the correct answer\"," +
                                "\"type\": \"SINGLE_CHOICE\"," +
                                "\"answers\": [\"A\", \"B\"]," +
                                "\"correctAnswers\": [\"C\"]," +
                                "\"difficulty\": 0.4" +
                                "}]";
        }

    private MockHttpServletRequestBuilder authorizedPost(String urlTemplate, Object... uriVars) {
        return post(urlTemplate, uriVars)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf());
    }

    // =========================================================================
    // POST /api/v1/ai/request/{requestId}/inject
    // =========================================================================

    @Test
    void injectQuestions_shouldReturn200_andCreateNewTest_whenNoTestIdProvided() throws Exception {
        LessonContext ctx = insertLessonOwnedBy(teacherId);
        UUID requestId = insertAiRequest(ctx.lessonId(), "SUCCESS", validGeneratedQuestionsJson());

        mockMvc.perform(authorizedPost("/api/v1/ai/request/{requestId}/inject", requestId)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testCreated").value(true))
                .andExpect(jsonPath("$.injectedCount").value(1))
                .andExpect(jsonPath("$.newTotalQuestions").value(1))
                .andExpect(jsonPath("$.lessonId").value(ctx.lessonId().toString()))
                .andExpect(jsonPath("$.testId").exists());
    }

    @Test
    void injectQuestions_shouldReturn200_andInjectIntoExistingTest_whenTestIdProvided() throws Exception {
        LessonContext ctx = insertLessonOwnedBy(teacherId);
        UUID testId = insertTest(ctx.lessonId(), teacherId);
        UUID requestId = insertAiRequest(ctx.lessonId(), "SUCCESS", validGeneratedQuestionsJson());

        InjectRequestDto body = new InjectRequestDto(testId);

        mockMvc.perform(authorizedPost("/api/v1/ai/request/{requestId}/inject", requestId)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testCreated").value(false))
                .andExpect(jsonPath("$.testId").value(testId.toString()))
                .andExpect(jsonPath("$.injectedCount").value(1));
    }

    @Test
    void injectQuestions_shouldReturn200_andInjectMultipleQuestions() throws Exception {
        LessonContext ctx = insertLessonOwnedBy(teacherId);
        UUID requestId = insertAiRequest(ctx.lessonId(), "SUCCESS", multipleQuestionsJson());

        mockMvc.perform(authorizedPost("/api/v1/ai/request/{requestId}/inject", requestId)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.injectedCount").value(2))
                .andExpect(jsonPath("$.newTotalQuestions").value(2));
    }

    @Test
    void injectQuestions_shouldPersistQuestionsInDatabase() throws Exception {
        LessonContext ctx = insertLessonOwnedBy(teacherId);
        UUID requestId = insertAiRequest(ctx.lessonId(), "SUCCESS", validGeneratedQuestionsJson());

        mockMvc.perform(authorizedPost("/api/v1/ai/request/{requestId}/inject", requestId)
                        .content("{}"))
                .andExpect(status().isOk());

        Integer questionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM questions q " +
                        "JOIN tests t ON q.test_id = t.id " +
                        "WHERE t.lesson_id = ?",
                Integer.class, ctx.lessonId()
        );
        assertEquals(1, questionCount);
    }

    @Test
    void injectQuestions_shouldUpdateAiRequestWithTestId_afterInjection() throws Exception {
        LessonContext ctx = insertLessonOwnedBy(teacherId);
        UUID requestId = insertAiRequest(ctx.lessonId(), "SUCCESS", validGeneratedQuestionsJson());

        mockMvc.perform(authorizedPost("/api/v1/ai/request/{requestId}/inject", requestId)
                        .content("{}"))
                .andExpect(status().isOk());

        UUID savedTestId = jdbcTemplate.queryForObject(
                "SELECT test_id FROM ai_question_requests WHERE id = ?",
                UUID.class, requestId
        );
        assertNotNull(savedTestId);
    }

    @Test
    void injectQuestions_shouldReturn200_withNullBody() throws Exception {
        LessonContext ctx = insertLessonOwnedBy(teacherId);
        UUID requestId = insertAiRequest(ctx.lessonId(), "SUCCESS", validGeneratedQuestionsJson());

        mockMvc.perform(post("/api/v1/ai/request/{requestId}/inject", requestId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testCreated").value(true));
    }

    @Test
    void injectQuestions_shouldReturn404_whenAiRequestDoesNotExist() throws Exception {
        UUID missingRequestId = UUID.randomUUID();

        mockMvc.perform(authorizedPost("/api/v1/ai/request/{requestId}/inject", missingRequestId)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

        @Test
        void injectQuestions_shouldReturn404_whenLessonDoesNotExistForAiRequest() throws Exception {
                UUID missingLessonId = UUID.randomUUID();
                UUID requestId = insertAiRequest(missingLessonId, "SUCCESS", validGeneratedQuestionsJson());

                mockMvc.perform(authorizedPost("/api/v1/ai/request/{requestId}/inject", requestId)
                                                .content("{}"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message", containsString("Lesson not found")));
        }

    @Test
    void injectQuestions_shouldReturn404_whenTestIdProvidedButDoesNotExist() throws Exception {
        LessonContext ctx = insertLessonOwnedBy(teacherId);
        UUID requestId = insertAiRequest(ctx.lessonId(), "SUCCESS", validGeneratedQuestionsJson());
        UUID missingTestId = UUID.randomUUID();

        InjectRequestDto body = new InjectRequestDto(missingTestId);

        mockMvc.perform(authorizedPost("/api/v1/ai/request/{requestId}/inject", requestId)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    void injectQuestions_shouldReturn403_whenTeacherDoesNotOwnLesson() throws Exception {
        UUID otherTeacherId = UUID.randomUUID();
        insertUser(otherTeacherId, RoleName.TEACHER);
        LessonContext ctx = insertLessonOwnedBy(otherTeacherId);
        UUID requestId = insertAiRequest(ctx.lessonId(), "SUCCESS", validGeneratedQuestionsJson());

        mockMvc.perform(authorizedPost("/api/v1/ai/request/{requestId}/inject", requestId)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Access denied")));

        jdbcTemplate.update("DELETE FROM users WHERE id = ?", otherTeacherId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"FAILED", "PENDING", "FALLBACK"})
    void injectQuestions_shouldReturn409_whenAiStatusIsNotCompleted(String status) throws Exception {

        LessonContext ctx = insertLessonOwnedBy(teacherId);

        UUID requestId = insertAiRequest(ctx.lessonId(), status, null);

        mockMvc.perform(authorizedPost("/api/v1/ai/request/{requestId}/inject", requestId)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("not completed")));
    }

    @Test
    void injectQuestions_shouldReturn422_whenQuestionTextIsEmpty() throws Exception {
        LessonContext ctx = insertLessonOwnedBy(teacherId);
        UUID requestId = insertAiRequest(ctx.lessonId(), "SUCCESS", invalidQuestionsJson_emptyText());

        mockMvc.perform(authorizedPost("/api/v1/ai/request/{requestId}/inject", requestId)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("text")));
    }

        @Test
        void injectQuestions_shouldReturn422_whenQuestionTextIsNull() throws Exception {
                LessonContext ctx = insertLessonOwnedBy(teacherId);
                UUID requestId = insertAiRequest(ctx.lessonId(), "SUCCESS", invalidQuestionsJson_nullText());

                mockMvc.perform(authorizedPost("/api/v1/ai/request/{requestId}/inject", requestId)
                                                .content("{}"))
                                .andExpect(status().isUnprocessableEntity())
                                .andExpect(jsonPath("$.message", containsString("text")));
        }

    @Test
    void injectQuestions_shouldReturn422_whenNoCorrectAnswerDefined() throws Exception {
        LessonContext ctx = insertLessonOwnedBy(teacherId);
        UUID requestId = insertAiRequest(ctx.lessonId(), "SUCCESS", invalidQuestionsJson_noCorrectAnswer());

        mockMvc.perform(authorizedPost("/api/v1/ai/request/{requestId}/inject", requestId)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("correct answer")));
    }

    @Test
    void injectQuestions_shouldReturn422_whenSingleChoiceHasTwoCorrectAnswers() throws Exception {
        LessonContext ctx = insertLessonOwnedBy(teacherId);
        UUID requestId = insertAiRequest(ctx.lessonId(), "SUCCESS", invalidQuestionsJson_singleChoiceTwoCorrect());

        mockMvc.perform(authorizedPost("/api/v1/ai/request/{requestId}/inject", requestId)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("SINGLE_CHOICE")));
    }

        @Test
        void injectQuestions_shouldReturn422_whenQuestionHasLessThanTwoOptions() throws Exception {
                LessonContext ctx = insertLessonOwnedBy(teacherId);
                UUID requestId = insertAiRequest(ctx.lessonId(), "SUCCESS", invalidQuestionsJson_lessThanTwoOptions());

                mockMvc.perform(authorizedPost("/api/v1/ai/request/{requestId}/inject", requestId)
                                                .content("{}"))
                                .andExpect(status().isUnprocessableEntity())
                                .andExpect(jsonPath("$.message", containsString("at least 2 answer options")));
        }

        @Test
        void injectQuestions_shouldReturn422_whenQuestionOptionsAreNull() throws Exception {
                LessonContext ctx = insertLessonOwnedBy(teacherId);
                UUID requestId = insertAiRequest(ctx.lessonId(), "SUCCESS", invalidQuestionsJson_nullOptions());

                mockMvc.perform(authorizedPost("/api/v1/ai/request/{requestId}/inject", requestId)
                                                .content("{}"))
                                .andExpect(status().isUnprocessableEntity())
                                .andExpect(jsonPath("$.message", containsString("at least 2 answer options")));
        }

        @Test
        void injectQuestions_shouldReturn422_whenTrueFalseHasWrongOptionCount() throws Exception {
                LessonContext ctx = insertLessonOwnedBy(teacherId);
                UUID requestId = insertAiRequest(ctx.lessonId(), "SUCCESS", invalidQuestionsJson_trueFalseWrongOptionCount());

                mockMvc.perform(authorizedPost("/api/v1/ai/request/{requestId}/inject", requestId)
                                                .content("{}"))
                                .andExpect(status().isUnprocessableEntity())
                                .andExpect(jsonPath("$.message", containsString("exactly 2 options")));
        }

        @Test
        void injectQuestions_shouldReturn422_whenTrueFalseHasTwoCorrectAnswers() throws Exception {
                LessonContext ctx = insertLessonOwnedBy(teacherId);
                UUID requestId = insertAiRequest(ctx.lessonId(), "SUCCESS", invalidQuestionsJson_trueFalseTwoCorrectAnswers());

                mockMvc.perform(authorizedPost("/api/v1/ai/request/{requestId}/inject", requestId)
                                                .content("{}"))
                                .andExpect(status().isUnprocessableEntity())
                                .andExpect(jsonPath("$.message", containsString("TRUE_FALSE")));
        }

        @Test
        void injectQuestions_shouldReturn422_whenGeneratedQuestionsJsonCannotBeParsed() throws Exception {
                LessonContext ctx = insertLessonOwnedBy(teacherId);
                UUID requestId = insertAiRequest(ctx.lessonId(), "SUCCESS", "not-json");

                mockMvc.perform(authorizedPost("/api/v1/ai/request/{requestId}/inject", requestId)
                                                .content("{}"))
                                .andExpect(status().isUnprocessableEntity())
                                .andExpect(jsonPath("$.message", containsString("parsing generated questions")));
        }

        @Test
        void injectQuestions_shouldReturn422_whenQuestionTypeIsNull() throws Exception {
                LessonContext ctx = insertLessonOwnedBy(teacherId);
                UUID requestId = insertAiRequest(ctx.lessonId(), "SUCCESS", invalidQuestionsJson_nullQuestionType());

                mockMvc.perform(authorizedPost("/api/v1/ai/request/{requestId}/inject", requestId)
                                                .content("{}"))
                                .andExpect(status().isUnprocessableEntity())
                                .andExpect(jsonPath("$.message", containsString("Unknown question type")));
        }

        @Test
        void injectQuestions_shouldReturn200_whenMultipleChoiceIsValid() throws Exception {
                LessonContext ctx = insertLessonOwnedBy(teacherId);
                UUID requestId = insertAiRequest(ctx.lessonId(), "SUCCESS", validMultipleChoiceQuestionJson());

                mockMvc.perform(authorizedPost("/api/v1/ai/request/{requestId}/inject", requestId)
                                .content("{}"))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.injectedCount").value(1));
        }

        @Test
        void injectQuestions_shouldReturn422_whenCorrectAnswerIsNotInAnswers() throws Exception {
                LessonContext ctx = insertLessonOwnedBy(teacherId);
                UUID requestId = insertAiRequest(ctx.lessonId(), "SUCCESS", invalidQuestionsJson_correctAnswerNotInAnswers());

                mockMvc.perform(authorizedPost("/api/v1/ai/request/{requestId}/inject", requestId)
                                .content("{}"))
                        .andExpect(status().isUnprocessableEntity())
                        .andExpect(jsonPath("$.message", containsString("provided answer options")));
        }

    @Test
    void injectQuestions_shouldReturn401_whenNotAuthenticated() throws Exception {
        UUID requestId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/ai/request/{requestId}/inject", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
