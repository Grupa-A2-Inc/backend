package org.elearning.backend.analytics;

import org.elearning.backend.auth.service.EmailService;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.jwt.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FailureRateControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmailService emailService;
    @Autowired
    private JwtUtil jwtUtil;

    private UUID authenticatedUserId;
    private static final String REQUEST_MAPPING = "/api/v1";

    @BeforeEach
    void setUp() {
        authenticatedUserId = insertAuthenticatedUser();
        authorizeRequests(authenticatedUserId);
    }

    @AfterEach
    void tearDown() {
        restTemplate.getRestTemplate().setInterceptors(List.of());
        jdbcTemplate.execute("DELETE FROM test_results");
        jdbcTemplate.execute("DELETE FROM test_attempts");
        jdbcTemplate.execute("DELETE FROM analytics_alerts");
        jdbcTemplate.execute("DELETE FROM tests");
        jdbcTemplate.execute("DELETE FROM lessons");
        jdbcTemplate.execute("DELETE FROM chapters");
        jdbcTemplate.execute("DELETE FROM courses");
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", authenticatedUserId);
    }

    // --- SETUP HELPERS ---

    private UUID insertAuthenticatedUser() {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, role_type, status) " +
                        "VALUES (?, ?, ?, ?, ?, (SELECT id FROM roles WHERE name = CAST(? AS role_name)), ?, CAST(? AS user_status))",
                userId, "analytics-teacher-" + userId + "@test.com", "password", "Test", "Teacher", RoleName.TEACHER.name(), "User", "ACTIVE"
        );
        return userId;
    }

    private void authorizeRequests(UUID userId) {
        String token = jwtUtil.generateAccessToken(userId, RoleName.TEACHER);
        restTemplate.getRestTemplate().setInterceptors(List.of((request, body, execution) -> {
            request.getHeaders().setBearerAuth(token);
            return execution.execute(request, body);
        }));
    }

    private UUID insertCourse(UUID creatorId) {
        UUID courseId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO courses (id, title, created_by, status, visibility) VALUES (?, ?, ?, CAST(? AS course_status), CAST(? AS course_visibility))",
                courseId, "Analytics Course", creatorId, "PUBLISHED", "PUBLIC"
        );
        return courseId;
    }

    private UUID insertChapter(UUID courseId) {
        UUID chapterId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO chapters (id, course_id, title) VALUES (?, ?, ?)",
                chapterId, courseId, "Analytics Chapter"
        );
        return chapterId;
    }

    private UUID insertLesson(UUID chapterId) {
        UUID lessonId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO lessons (id, chapter_id, title, order_index) VALUES (?, ?, ?, ?)",
                lessonId, chapterId, "Analytics Lesson", 1
        );
        return lessonId;
    }

    private UUID insertTest(UUID lessonId, UUID creatorId) {
        UUID testId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO tests (id, lesson_id, created_by, title, description, time_limit_sec, ai_enabled, status, version) VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? as test_status), ?)",
                testId, lessonId, creatorId, "Analytics Test", "Test details", 600, false, "PUBLISHED", 1
        );
        return testId;
    }

    private void insertAnalyticsAlert(UUID testId, UUID professorId, double threshold) {
        jdbcTemplate.update(
                "INSERT INTO analytics_alerts (id, test_id, professor_id, failure_threshold, current_failure_rate, is_active) VALUES (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), testId, professorId, threshold, 0.0, true
        );
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private UUID insertStudent() {
        UUID studentId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, role_type, status) " +
                        "VALUES (?, ?, ?, ?, ?, (SELECT id FROM roles WHERE name = 'STUDENT'), 'User', 'ACTIVE')",
                studentId, "student-" + studentId + "@test.com", "password", "Test", "Student"
        );
        return studentId;
    }

    private void insertTestResult(UUID testId, UUID studentId, boolean isPassed) {
        UUID attemptId = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO test_attempts (id, test_id, student_id, started_at, status) " +
                        "VALUES (?, ?, ?, CURRENT_TIMESTAMP, 'DONE')", // <-- Changed from 'COMPLETED' to 'DONE'
                attemptId, testId, studentId
        );

        jdbcTemplate.update(
                "INSERT INTO test_results (attempt_id, student_id, test_id, score, score_percent, passed, completed_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                attemptId,
                studentId,
                testId,
                isPassed ? 1.0000 : 0.4000,
                isPassed ? 100.00 : 40.00,
                isPassed
        );
    }

    // --- TESTS FOR GET /tests/{testId}/analytics/failure-rate ---

    @Test
    void shouldGetTestFailureRateSuccessfully() {
        UUID courseId = insertCourse(authenticatedUserId);
        UUID chapterId = insertChapter(courseId);
        UUID lessonId = insertLesson(chapterId);
        UUID testId = insertTest(lessonId, authenticatedUserId);
        insertAnalyticsAlert(testId, authenticatedUserId, 50.0);

        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + "/tests/" + testId + "/analytics/failure-rate",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("failureRate");
        assertThat(response.getBody()).contains("threshold");
        assertThat(response.getBody()).contains("alertTriggered");
    }

    @Test
    void shouldReturnForbiddenWhenGettingTestFailureRateForOtherTeachersTest() {
        UUID otherTeacherId = insertAuthenticatedUser();
        UUID courseId = insertCourse(otherTeacherId);
        UUID chapterId = insertChapter(courseId);
        UUID lessonId = insertLesson(chapterId);
        UUID testId = insertTest(lessonId, otherTeacherId);

        // Making the request as the primary authenticatedUserId (who doesn't own it)
        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + "/tests/" + testId + "/analytics/failure-rate",
                String.class
        );

        assertThat(response.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", otherTeacherId);
    }

    @Test
    void shouldReturnNotFoundWhenTestDoesNotExistForFailureRate() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + "/tests/" + UUID.randomUUID() + "/analytics/failure-rate",
                String.class
        );

        assertThat(response.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND);
    }

    // --- TESTS FOR GET /lessons/{lessonId}/analytics/failure-rate ---

    @Test
    void shouldGetLessonFailureRateSuccessfully() {
        UUID courseId = insertCourse(authenticatedUserId);
        UUID chapterId = insertChapter(courseId);
        UUID lessonId = insertLesson(chapterId);
        UUID testId = insertTest(lessonId, authenticatedUserId);
        insertAnalyticsAlert(testId, authenticatedUserId, 50.0);

        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + "/lessons/" + lessonId + "/analytics/failure-rate",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldReturnNotFoundWhenLessonHasNoTestForFailureRate() {
        UUID courseId = insertCourse(authenticatedUserId);
        UUID chapterId = insertChapter(courseId);
        UUID lessonId = insertLesson(chapterId);
        // Do not insert test

        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + "/lessons/" + lessonId + "/analytics/failure-rate",
                String.class
        );

        assertThat(response.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND);
    }


    // --- TESTS FOR POST /tests/{testId}/analytics/alerts ---

    @Test
    void shouldCreateOrUpdateAlertSuccessfully() {
        UUID courseId = insertCourse(authenticatedUserId);
        UUID chapterId = insertChapter(courseId);
        UUID lessonId = insertLesson(chapterId);
        UUID testId = insertTest(lessonId, authenticatedUserId);

        String body = """
                {
                    "failureThreshold": 65.5
                }
                """;

        ResponseEntity<String> response = restTemplate.postForEntity(
                REQUEST_MAPPING + "/tests/" + testId + "/analytics/alerts",
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldReturnForbiddenWhenPostingAlertForOtherTeachersTest() {
        UUID otherTeacherId = insertAuthenticatedUser();
        UUID courseId = insertCourse(otherTeacherId);
        UUID chapterId = insertChapter(courseId);
        UUID lessonId = insertLesson(chapterId);
        UUID testId = insertTest(lessonId, otherTeacherId);

        String body = """
                {
                    "failureThreshold": 65.5
                }
                """;

        ResponseEntity<String> response = restTemplate.postForEntity(
                REQUEST_MAPPING + "/tests/" + testId + "/analytics/alerts",
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", otherTeacherId);
    }

    // --- TESTS FOR GET /professors/me/alerts ---

    @Test
    void shouldGetActiveAlertsForAuthenticatedProfessor() {
        UUID courseId = insertCourse(authenticatedUserId);
        UUID chapterId = insertChapter(courseId);
        UUID lessonId = insertLesson(chapterId);
        UUID testId = insertTest(lessonId, authenticatedUserId);
        insertAnalyticsAlert(testId, authenticatedUserId, 40.0);

        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + "/professors/me/alerts",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Expecting a JSON array
        assertThat(response.getBody()).startsWith("[");
    }

    // --- TESTS FOR GET /course/{courseId}/analytics/chart-data ---

    @Test
    void shouldGetCourseFailureRateChartDataSuccessfully() {
        UUID courseId = insertCourse(authenticatedUserId);
        UUID chapterId = insertChapter(courseId);
        UUID lessonId = insertLesson(chapterId);
        insertTest(lessonId, authenticatedUserId);

        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + "/courses/" + courseId + "/analytics/chart-data",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).startsWith("[");
    }

    @Test
    void shouldReturnForbiddenWhenGettingChartDataForOtherTeachersCourse() {
        UUID otherTeacherId = insertAuthenticatedUser();
        UUID courseId = insertCourse(otherTeacherId);

        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + "/courses/" + courseId + "/analytics/chart-data",
                String.class
        );

        assertThat(response.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", otherTeacherId);
    }

    @Test
    void shouldCalculateTestFailureRateWithAttemptsSuccessfully() {
        UUID courseId = insertCourse(authenticatedUserId);
        UUID chapterId = insertChapter(courseId);
        UUID lessonId = insertLesson(chapterId);
        UUID testId = insertTest(lessonId, authenticatedUserId);
        insertAnalyticsAlert(testId, authenticatedUserId, 50.0);

        UUID studentId1 = insertStudent();
        UUID studentId2 = insertStudent();
        insertTestResult(testId, studentId1, false);
        insertTestResult(testId, studentId2, true);

        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + "/tests/" + testId + "/analytics/failure-rate",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"failureRate\":50.0");
    }

    @Test
    void shouldReturnNotFoundWhenTestHasNoAnalyticsAlert() {
        UUID courseId = insertCourse(authenticatedUserId);
        UUID chapterId = insertChapter(courseId);
        UUID lessonId = insertLesson(chapterId);
        UUID testId = insertTest(lessonId, authenticatedUserId);
        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + "/tests/" + testId + "/analytics/failure-rate",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturnForbiddenWhenGettingLessonFailureRateForOtherTeachersLesson() {
        UUID otherTeacherId = insertAuthenticatedUser();
        UUID courseId = insertCourse(otherTeacherId);
        UUID chapterId = insertChapter(courseId);
        UUID lessonId = insertLesson(chapterId);
        insertTest(lessonId, otherTeacherId);

        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + "/lessons/" + lessonId + "/analytics/failure-rate",
                String.class
        );

        assertThat(response.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturnForbiddenWhenStudentGetsAlerts() {
        UUID studentId = insertStudent();
        String studentToken = jwtUtil.generateAccessToken(studentId, RoleName.STUDENT);

        restTemplate.getRestTemplate().setInterceptors(List.of((request, body, execution) -> {
            request.getHeaders().setBearerAuth(studentToken);
            return execution.execute(request, body);
        }));

        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + "/professors/me/alerts",
                String.class
        );

        assertThat(response.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldGetCourseFailureRateChartDataWithPointsSuccessfully() {
        UUID courseId = insertCourse(authenticatedUserId);
        UUID chapterId = insertChapter(courseId);
        UUID lessonId = insertLesson(chapterId);
        UUID testId = insertTest(lessonId, authenticatedUserId);

        UUID studentId = insertStudent();
        insertTestResult(testId, studentId, false);

        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + "/courses/" + courseId + "/analytics/chart-data",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).startsWith("[");
        assertThat(response.getBody()).contains("failureRatePoints");
    }

    @Test
    void shouldReturnNotFoundWhenPostingAlertForNonExistentTest() {
        String body = """
                {
                    "failureThreshold": 65.5
                }
                """;

        ResponseEntity<String> response = restTemplate.postForEntity(
                REQUEST_MAPPING + "/tests/" + UUID.randomUUID() + "/analytics/alerts",
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );
        assertThat(response.getStatusCode()).isIn(HttpStatus.NOT_FOUND, HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldReturnNotFoundWhenGettingChartDataForNonExistentCourse() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + "/courses/" + UUID.randomUUID() + "/analytics/chart-data",
                String.class
        );

        assertThat(response.getStatusCode()).isIn(HttpStatus.NOT_FOUND, HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldGetCourseFailureRateChartDataWhenSomeLessonsHaveNoTests() {
        UUID courseId = insertCourse(authenticatedUserId);
        UUID chapterId = insertChapter(courseId);
        UUID lessonId1 = insertLesson(chapterId);
        insertTest(lessonId1, authenticatedUserId);
        UUID lessonId2 = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO lessons (id, chapter_id, title, order_index) VALUES (?, ?, ?, ?)",
                lessonId2, chapterId, "Lesson Without Test", 2
        );

        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + "/courses/" + courseId + "/analytics/chart-data",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("failureRatePoints");
    }

    @ParameterizedTest(name = "threshold={0}, expectedTriggered={1}")
    @MethodSource("alertTriggeredScenarios")
    void shouldCalculateAlertTriggeredForDifferentThresholds(double threshold, boolean expectedTriggered) {
        UUID courseId = insertCourse(authenticatedUserId);
        UUID chapterId = insertChapter(courseId);
        UUID lessonId = insertLesson(chapterId);
        UUID testId = insertTest(lessonId, authenticatedUserId);
        insertAnalyticsAlert(testId, authenticatedUserId, threshold);

        // Create 2 students - 1 failed, 1 passed = 50% failure rate
        UUID student1 = insertStudent();
        UUID student2 = insertStudent();
        insertTestResult(testId, student1, false); // Failed
        insertTestResult(testId, student2, true);  // Passed

        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + "/tests/" + testId + "/analytics/failure-rate",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"failureRate\":50.0");
        assertThat(response.getBody()).contains("\"alertTriggered\":" + expectedTriggered);
    }

    private static Stream<Arguments> alertTriggeredScenarios() {
        return Stream.of(
                Arguments.of(30.0, true),
                Arguments.of(80.0, false),
                Arguments.of(50.0, false)
        );
    }

}
