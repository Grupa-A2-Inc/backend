package org.elearning.backend.assestment.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TestResultControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // This matches the hardcoded UUID in your controller right now
    private static final UUID STUDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ANOTHER_STUDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

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
        jdbcTemplate.execute("DELETE FROM tests");
    }

    /**
     * Tests that a valid, DONE attempt successfully returns the test result payload.
     */
    @Test
    void shouldReturnTestResultForDoneAttempt() {
        UUID testId = insertTest(lessonId, instructorId, "PUBLISHED", 300);
        UUID attemptId = insertAttempt(testId, STUDENT_ID, "DONE");

        // CHANGED: score from 85.5 to 8.5 so it fits in NUMERIC(5,4)
        insertTestResult(attemptId, STUDENT_ID, testId, 8.5, 85.5, true);

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/attempts/" + attemptId + "/result",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("\"passed\":true");
    }

    /**
     * Tests that requesting a result for an attempt that doesn't exist returns 404 Not Found.
     */
    @Test
    void shouldReturn404ForNonExistentAttempt() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/attempts/" + UUID.randomUUID() + "/result",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * Tests that requesting a result for an attempt still IN_PROGRESS returns the mapped exception status.
     * Assuming AttemptInProgressException maps to 409 Conflict (or adjust to your actual @ExceptionHandler).
     */
    @Test
    void shouldReturnErrorWhenAttemptIsStillInProgress() {
        UUID testId = insertTest(lessonId, instructorId, "PUBLISHED", 300);
        UUID attemptId = insertAttempt(testId, STUDENT_ID, "IN_PROGRESS");

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/attempts/" + attemptId + "/result",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * Tests that requesting a result for an EXPIRED attempt returns the mapped exception status.
     * Assuming TimerExpiredException maps to 410 Gone.
     */
    @Test
    void shouldReturnErrorWhenAttemptIsExpired() {
        UUID testId = insertTest(lessonId, instructorId, "PUBLISHED", 300);
        UUID attemptId = insertAttempt(testId, STUDENT_ID, "EXPIRED");

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/attempts/" + attemptId + "/result",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
    }




    private UUID insertTest(UUID lessonId, UUID createdBy, String status, int timeLimitSec) {
        UUID testId = UUID.randomUUID();
        jdbcTemplate.execute(
                "INSERT INTO tests (id, lesson_id, created_by, title, time_limit_sec, status, ai_enabled) " +
                        "VALUES ('" + testId + "', '" + lessonId + "', '" + createdBy + "', 'Test Title', "
                        + timeLimitSec + ", '" + status + "', false)"
        );
        return testId;
    }

    private UUID insertAttempt(UUID testId, UUID studentId, String status) {
        UUID attemptId = UUID.randomUUID();
        jdbcTemplate.execute(
                "INSERT INTO test_attempts (id, test_id, student_id, status) " +
                        "VALUES ('" + attemptId + "', '" + testId + "', '" + studentId + "', '" + status + "')"
        );
        return attemptId;
    }

    private void insertTestResult(UUID attemptId, UUID studentId, UUID testId, double score, double scorePercent, boolean passed) {
        jdbcTemplate.execute(
                "INSERT INTO test_results (attempt_id, student_id, test_id, score, score_percent, passed, completed_at) " +
                        "VALUES ('" + attemptId + "', '" + studentId + "', '" + testId + "', " + score + ", " + scorePercent + ", " + passed + ", NOW())"
        );
    }
}