package org.elearning.backend.feedback;

import org.elearning.backend.auth.service.EmailService;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.jwt.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class QuestionErrorReportControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private EmailService emailService;

    private UUID studentId;
    private UUID teacherId;
    private static final String REQUEST_MAPPING = "/api/v1";

    @BeforeEach
    void setUp() {
        studentId = insertUser(RoleName.STUDENT);
        teacherId = insertUser(RoleName.TEACHER);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM question_error_reports");
        jdbcTemplate.execute("DELETE FROM question_options");
        jdbcTemplate.execute("DELETE FROM questions");
        jdbcTemplate.execute("DELETE FROM test_attempts");
        jdbcTemplate.execute("DELETE FROM test_results");
        jdbcTemplate.execute("DELETE FROM tests");
        jdbcTemplate.execute("DELETE FROM course_enrollments");
        jdbcTemplate.execute("DELETE FROM lessons");
        jdbcTemplate.execute("DELETE FROM chapters");
        jdbcTemplate.execute("DELETE FROM courses");
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", studentId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", teacherId);
    }

    // --- HELPERS ---

    private UUID insertUser(RoleName role) {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, role_type, status) " +
                        "VALUES (?, ?, ?, ?, ?, (SELECT id FROM roles WHERE name = CAST(? AS role_name)), ?, CAST(? AS user_status))",
                userId, role.name().toLowerCase() + "-" + userId + "@test.com", "password",
                "Test", role.name(), role.name(), "User", "ACTIVE"
        );
        return userId;
    }

    private record QuestionSetup(Integer questionId, UUID courseId) {}

    private QuestionSetup insertQuestionForTeacher(UUID creatorId) {
        UUID courseId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO courses (id, title, created_by, status, visibility) " +
                        "VALUES (?, ?, ?, CAST(? AS course_status), CAST(? AS course_visibility))",
                courseId, "Test Course", creatorId, "PUBLISHED", "PUBLIC"
        );

        UUID chapterId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO chapters (id, course_id, title) VALUES (?, ?, ?)",
                chapterId, courseId, "Test Chapter"
        );

        UUID lessonId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO lessons (id, chapter_id, title, order_index) VALUES (?, ?, ?, ?)",
                lessonId, chapterId, "Test Lesson", 1
        );

        UUID testId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO tests (id, lesson_id, created_by, title, description, time_limit_sec, ai_enabled, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS test_status))",
                testId, lessonId, creatorId, "Test", "Desc", 600, false, "PUBLISHED"
        );

        Integer questionId = jdbcTemplate.queryForObject(
                "INSERT INTO questions (test_id, question_type, content) " +
                        "VALUES (?, CAST(? AS question_type), ?) RETURNING id",
                Integer.class,
                testId, "SINGLE_CHOICE", "Sample question content?"
        );

        return new QuestionSetup(questionId, courseId);
    }

    private void enrollStudent(UUID courseId, UUID studentId) {
        jdbcTemplate.update(
                "INSERT INTO course_enrollments (id, course_id, student_id) VALUES (?, ?, ?)",
                UUID.randomUUID(), courseId, studentId
        );
    }

    private HttpEntity<String> authenticatedRequest(String body, UUID userId, RoleName role) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtUtil.generateAccessToken(userId, role));
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<String> unauthenticatedRequest(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private String bodyWithDescription(String description) {
        return """
                {
                    "description": "%s"
                }
                """.formatted(description);
    }

    private String validDescription() {
        return "This is a valid description with enough characters";
    }

    // --- TESTS ---

    @Test
    void shouldCreateReportSuccessfully() {
        QuestionSetup setup = insertQuestionForTeacher(teacherId);
        enrollStudent(setup.courseId(), studentId);

        ResponseEntity<String> response = restTemplate.postForEntity(
                REQUEST_MAPPING + "/questions/" + setup.questionId() + "/error-reports",
                authenticatedRequest(bodyWithDescription(validDescription()), studentId, RoleName.STUDENT),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("questionId");
        assertThat(response.getBody()).contains("description");
        assertThat(response.getBody()).contains("status");
    }

    @Test
    void shouldReturnForbiddenWhenStudentNotEnrolled() {
        QuestionSetup setup = insertQuestionForTeacher(teacherId);

        ResponseEntity<String> response = restTemplate.postForEntity(
                REQUEST_MAPPING + "/questions/" + setup.questionId() + "/error-reports",
                authenticatedRequest(bodyWithDescription(validDescription()), studentId, RoleName.STUDENT),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldReturnForbiddenWhenQuestionDoesNotExist() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                REQUEST_MAPPING + "/questions/999999/error-reports",
                authenticatedRequest(bodyWithDescription(validDescription()), studentId, RoleName.STUDENT),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldReturnForbiddenWhenTeacherTriesToCreateReport() {
        QuestionSetup setup = insertQuestionForTeacher(teacherId);

        ResponseEntity<String> response = restTemplate.postForEntity(
                REQUEST_MAPPING + "/questions/" + setup.questionId() + "/error-reports",
                authenticatedRequest(bodyWithDescription(validDescription()), teacherId, RoleName.TEACHER),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldReturnUnauthorizedWhenNoTokenProvided() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                REQUEST_MAPPING + "/questions/1/error-reports",
                unauthenticatedRequest(bodyWithDescription(validDescription())),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturnBadRequestWhenDescriptionIsTooShort() {
        QuestionSetup setup = insertQuestionForTeacher(teacherId);
        enrollStudent(setup.courseId(), studentId);

        ResponseEntity<String> response = restTemplate.postForEntity(
                REQUEST_MAPPING + "/questions/" + setup.questionId() + "/error-reports",
                authenticatedRequest(bodyWithDescription("short"), studentId, RoleName.STUDENT),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturnBadRequestWhenDescriptionIsBlank() {
        QuestionSetup setup = insertQuestionForTeacher(teacherId);
        enrollStudent(setup.courseId(), studentId);

        ResponseEntity<String> response = restTemplate.postForEntity(
                REQUEST_MAPPING + "/questions/" + setup.questionId() + "/error-reports",
                authenticatedRequest(bodyWithDescription("   "), studentId, RoleName.STUDENT),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturnBadRequestWhenDescriptionIsTooLong() {
        QuestionSetup setup = insertQuestionForTeacher(teacherId);
        enrollStudent(setup.courseId(), studentId);

        ResponseEntity<String> response = restTemplate.postForEntity(
                REQUEST_MAPPING + "/questions/" + setup.questionId() + "/error-reports",
                authenticatedRequest(bodyWithDescription("a".repeat(2001)), studentId, RoleName.STUDENT),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturnUnauthorizedWhenUserDoesNotExistInDatabase() {
        UUID nonExistentStudentId = UUID.randomUUID();

        ResponseEntity<String> response = restTemplate.postForEntity(
                REQUEST_MAPPING + "/questions/1/error-reports",
                authenticatedRequest(bodyWithDescription(validDescription()), nonExistentStudentId, RoleName.STUDENT),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturnUnauthorizedWhenNullToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth("invalid.token.here");

        ResponseEntity<String> response = restTemplate.postForEntity(
                REQUEST_MAPPING + "/questions/1/error-reports",
                new HttpEntity<>(bodyWithDescription(validDescription()), headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}