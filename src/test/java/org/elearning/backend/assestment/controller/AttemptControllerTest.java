package org.elearning.backend.assestment.controller;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AttemptControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final UUID STUDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private UUID lessonId;
    private UUID instructorId;

    @BeforeEach
    void setUp() {
        lessonId = UUID.randomUUID();
        instructorId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM test_results");
        jdbcTemplate.execute("DELETE FROM attempt_answers");
        jdbcTemplate.execute("DELETE FROM test_attempts");
        jdbcTemplate.execute("DELETE FROM question_options");
        jdbcTemplate.execute("DELETE FROM questions");
        jdbcTemplate.execute("DELETE FROM tests");
    }

    // =========================================================
    // POST /api/tests/{testId}/start
    // =========================================================

    /**
     * POST /api/tests/{testId}/start
     * Tests that starting a PUBLISHED test creates an attempt and returns 200 OK
     * with questions (without correct answers).
     */
    @Test
    void shouldStartAttemptForPublishedTest() {
        UUID testId = insertTest(lessonId, instructorId, "PUBLISHED", 300, false);
        int questionId = insertQuestionAndReturnId(testId, "SINGLE_CHOICE", "What is Java?");
        insertOption(questionId, "A language", 1, false);
        insertOption(questionId, "A coffee", 2, true);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/tests/" + testId + "/start",
                new HttpEntity<>(null, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("What is Java?");

        Integer attemptCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM test_attempts WHERE test_id = '" + testId + "' AND student_id = '" + STUDENT_ID + "'",
                Integer.class
        );
        assertThat(attemptCount).isEqualTo(1);
    }

    /**
     * POST /api/tests/{testId}/start
     * Tests that the response does NOT expose which options are correct.
     */
    @Test
    void shouldNotExposeCorrectAnswersWhenStartingAttempt() {
        UUID testId = insertTest(lessonId, instructorId, "PUBLISHED", 300, false);
        int questionId = insertQuestionAndReturnId(testId, "SINGLE_CHOICE", "Capital of France?");
        insertOption(questionId, "Paris", 1, true);
        insertOption(questionId, "Berlin", 2, false);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/tests/" + testId + "/start",
                new HttpEntity<>(null, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // isCorrect field should not be present or should always be false/null
        assertThat(response.getBody()).doesNotContain("\"isCorrect\":true");
    }

    /**
     * POST /api/tests/{testId}/start
     * Tests that starting a DRAFT test returns 400 Bad Request.
     */
    @Test
    void shouldReturn400WhenStartingDraftTest() {
        UUID testId = insertTest(lessonId, instructorId, "DRAFT", 300, false);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/tests/" + testId + "/start",
                new HttpEntity<>(null, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /**
     * POST /api/tests/{testId}/start
     * Tests that starting a non-existent test returns 404 Not Found.
     */
    @Test
    void shouldReturn404WhenStartingNonExistentTest() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/tests/" + UUID.randomUUID() + "/start",
                new HttpEntity<>(null, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * POST /api/tests/{testId}/start
     * Tests that the created attempt has IN_PROGRESS status in the database.
     */
    @Test
    void shouldCreateAttemptWithInProgressStatus() {
        UUID testId = insertTest(lessonId, instructorId, "PUBLISHED", 300, false);

        restTemplate.postForEntity(
                "/api/tests/" + testId + "/start",
                new HttpEntity<>(null, jsonHeaders()),
                String.class
        );

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM test_attempts WHERE test_id = '" + testId + "' AND student_id = '" + STUDENT_ID + "'",
                String.class
        );
        assertThat(status).isEqualTo("IN_PROGRESS");
    }

    // =========================================================
    // POST /api/attempts/{attemptId}/submit
    // =========================================================

    /**
     * POST /api/attempts/{attemptId}/submit
     * Tests that submitting correct answers returns 200 OK with a passing result.
     */
    @Test
    void shouldSubmitAttemptAndReturnResult() {
        UUID testId = insertTest(lessonId, instructorId, "PUBLISHED", 300, false);
        int qId = insertQuestionAndReturnId(testId, "SINGLE_CHOICE", "What is 2+2?");
        int correctOptionId = insertOptionAndReturnId(qId, "4", 1, true);
        insertOptionAndReturnId(qId, "5", 2, false);

        UUID attemptId = insertAttempt(testId, STUDENT_ID, "IN_PROGRESS");
        String submitBody = buildSubmitBody(qId, correctOptionId, 5.0);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/attempts/" + attemptId + "/submit",
                HttpMethod.POST,
                new HttpEntity<>(submitBody, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    /**
     * POST /api/attempts/{attemptId}/submit
     * Tests that after submitting, the attempt status changes to DONE.
     */
    @Test
    void shouldMarkAttemptAsDoneAfterSubmit() {
        UUID testId = insertTest(lessonId, instructorId, "PUBLISHED", 300, false);
        int qId = insertQuestionAndReturnId(testId, "TRUE_FALSE", "Java is OOP?");
        int optionId = insertOptionAndReturnId(qId, "True", 1, true);

        UUID attemptId = insertAttempt(testId, STUDENT_ID, "IN_PROGRESS");
        String submitBody = buildSubmitBody(qId, optionId, 3.0);

        restTemplate.exchange(
                "/api/attempts/" + attemptId + "/submit",
                HttpMethod.POST,
                new HttpEntity<>(submitBody, jsonHeaders()),
                String.class
        );

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM test_attempts WHERE id = '" + attemptId + "'",
                String.class
        );
        assertThat(status).isEqualTo("DONE");
    }

    /**
     * POST /api/attempts/{attemptId}/submit
     * Tests that submitting an already DONE attempt returns 409 Conflict.
     */
    @Test
    void shouldReturn409WhenSubmittingAlreadyDoneAttempt() {
        UUID testId = insertTest(lessonId, instructorId, "PUBLISHED", 300, false);
        int qId = insertQuestionAndReturnId(testId, "TRUE_FALSE", "Is sky blue?");
        int optionId = insertOptionAndReturnId(qId, "True", 1, true);

        UUID attemptId = insertAttempt(testId, STUDENT_ID, "DONE");
        String submitBody = buildSubmitBody(qId, optionId, 2.0);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/attempts/" + attemptId + "/submit",
                HttpMethod.POST,
                new HttpEntity<>(submitBody, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    /**
     * POST /api/attempts/{attemptId}/submit
     * Tests that submitting an EXPIRED attempt returns 410 Gone.
     */
    @Test
    void shouldReturn410WhenSubmittingExpiredAttempt() {
        UUID testId = insertTest(lessonId, instructorId, "PUBLISHED", 1, false); // 1 secundă
        int qId = insertQuestionAndReturnId(testId, "TRUE_FALSE", "Fast question?");
        int optionId = insertOptionAndReturnId(qId, "Yes", 1, true);

        UUID attemptId = insertAttempt(testId, STUDENT_ID, "EXPIRED");
        String submitBody = buildSubmitBody(qId, optionId, 2.0);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/attempts/" + attemptId + "/submit",
                HttpMethod.POST,
                new HttpEntity<>(submitBody, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
    }

    /**
     * POST /api/attempts/{attemptId}/submit
     * Tests that submitting for a non-existent attempt returns 404 Not Found.
     */
    @Test
    void shouldReturn404WhenSubmittingNonExistentAttempt() {
        String submitBody = """
                { "answers": [] }
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/attempts/" + UUID.randomUUID() + "/submit",
                HttpMethod.POST,
                new HttpEntity<>(submitBody, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * POST /api/attempts/{attemptId}/submit
     * Tests that a test result record is saved in the database after a successful submit.
     */
    @Test
    void shouldPersistTestResultAfterSubmit() {
        UUID testId = insertTest(lessonId, instructorId, "PUBLISHED", 300, false);
        int qId = insertQuestionAndReturnId(testId, "SINGLE_CHOICE", "Best language?");
        int optionId = insertOptionAndReturnId(qId, "Java", 1, true);
        insertOptionAndReturnId(qId, "PHP", 2, false);

        UUID attemptId = insertAttempt(testId, STUDENT_ID, "IN_PROGRESS");
        String submitBody = buildSubmitBody(qId, optionId, 10.0);

        restTemplate.exchange(
                "/api/attempts/" + attemptId + "/submit",
                HttpMethod.POST,
                new HttpEntity<>(submitBody, jsonHeaders()),
                String.class
        );

        Integer resultCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM test_results WHERE attempt_id = '" + attemptId + "'",
                Integer.class
        );
        assertThat(resultCount).isEqualTo(1);
    }

    // =========================================================
    // Helper methods
    // =========================================================

    private UUID insertTest(UUID lessonId, UUID createdBy, String status, int timeLimitSec, boolean aiEnabled) {
        UUID testId = UUID.randomUUID();
        jdbcTemplate.execute(
                "INSERT INTO tests (id, lesson_id, created_by, title, time_limit_sec, status, ai_enabled) " +
                        "VALUES ('" + testId + "', '" + lessonId + "', '" + createdBy + "', 'Test Title', "
                        + timeLimitSec + ", '" + status + "', " + aiEnabled + ")"
        );
        return testId;
    }

    /**
     * Inserts a question and returns its generated SERIAL integer id.
     */
    private int insertQuestionAndReturnId(UUID testId, String type, String content) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO questions (test_id, question_type, content) " +
                        "VALUES (?::uuid, ?::question_type, ?) RETURNING id",
                Integer.class,
                testId.toString(), type, content
        );
    }


    private void insertOption(int questionId, String text, int order, boolean isCorrect) {
        jdbcTemplate.execute(
                "INSERT INTO question_options (question_id, text, display_order, is_correct) " +
                        "VALUES (" + questionId + ", '" + text + "', " + order + ", " + isCorrect + ")"
        );
    }

    private int insertOptionAndReturnId(int questionId, String text, int order, boolean isCorrect) {
        jdbcTemplate.execute(
                "INSERT INTO question_options (question_id, text, display_order, is_correct) " +
                        "VALUES (" + questionId + ", '" + text + "', " + order + ", " + isCorrect + ")"
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM question_options WHERE question_id = " + questionId + " ORDER BY id DESC LIMIT 1",
                Integer.class
        );
    }

    private UUID insertAttempt(UUID testId, UUID studentId, String status) {
        UUID attemptId = UUID.randomUUID();
        jdbcTemplate.execute(
                "INSERT INTO test_attempts (id, test_id, student_id, status) " +
                        "VALUES ('" + attemptId + "', '" + testId + "', '" + studentId + "', '" + status + "')"
        );
        return attemptId;
    }

    private String buildSubmitBody(int questionId, int optionId, double timeSpent) {
        return String.format(Locale.US, """
                {
                    "answers": [
                        {
                            "questionId": %d,
                            "selectedOptionIds": [%d],
                            "timeSpent": %.1f
                        }
                    ]
                }
                """, questionId, optionId, timeSpent);
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}