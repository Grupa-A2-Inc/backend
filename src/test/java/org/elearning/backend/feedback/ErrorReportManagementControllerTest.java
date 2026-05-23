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
class ErrorReportManagementControllerTest {

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
                "INSERT INTO tests (id, lesson_id, created_by, title, description, time_limit_sec, ai_enabled, status, version) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS test_status), ?)",
                testId, lessonId, creatorId, "Test", "Desc", 600, false, "PUBLISHED", 1
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

    private UUID getFirstReportId() {
        return jdbcTemplate.queryForObject("SELECT id FROM question_error_reports LIMIT 1", UUID.class);
    }

    // --- TESTS ---

    @Test
    void shouldGetReportsForProfessorSuccessfully() {
        QuestionSetup setup = insertQuestionForTeacher(teacherId);
        enrollStudent(setup.courseId(), studentId);

        restTemplate.postForEntity(
                REQUEST_MAPPING + "/questions/" + setup.questionId() + "/error-reports",
                authenticatedRequest(bodyWithDescription(validDescription()), studentId, RoleName.STUDENT),
                String.class
        );

        ResponseEntity<String> response = restTemplate.exchange(
                REQUEST_MAPPING + "/professors/" + teacherId + "/error-reports?page=0&size=10",
                HttpMethod.GET,
                authenticatedRequest(null, teacherId, RoleName.TEACHER),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("content");
        assertThat(response.getBody()).contains("totalElements");
    }

    @Test
    void shouldReturnForbiddenWhenStudentTriesToGetReports() {
        ResponseEntity<String> response = restTemplate.exchange(
                REQUEST_MAPPING + "/professors/" + teacherId + "/error-reports",
                HttpMethod.GET,
                authenticatedRequest(null, studentId, RoleName.STUDENT),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldReturnForbiddenWhenTeacherTriesToGetReportsForAnotherTeacher() {
        UUID anotherTeacherId = insertUser(RoleName.TEACHER);

        ResponseEntity<String> response = restTemplate.exchange(
                REQUEST_MAPPING + "/professors/" + teacherId + "/error-reports",
                HttpMethod.GET,
                authenticatedRequest(null, anotherTeacherId, RoleName.TEACHER),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldReturnUnauthorizedWhenGettingReportsWithoutToken() {
        ResponseEntity<String> response = restTemplate.exchange(
                REQUEST_MAPPING + "/professors/" + teacherId + "/error-reports",
                HttpMethod.GET,
                unauthenticatedRequest(null),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturnPaginatedReportsCorrectlyAcrossMultiplePages() {
        QuestionSetup setup = insertQuestionForTeacher(teacherId);
        enrollStudent(setup.courseId(), studentId);

        for (int i = 0; i < 3; i++) {
            restTemplate.postForEntity(
                    REQUEST_MAPPING + "/questions/" + setup.questionId() + "/error-reports",
                    authenticatedRequest(bodyWithDescription("This is valid description number " + i + " for testing"), studentId, RoleName.STUDENT),
                    String.class
            );
        }

        ResponseEntity<String> page0Response = restTemplate.exchange(
                REQUEST_MAPPING + "/professors/" + teacherId + "/error-reports?page=0&size=2",
                HttpMethod.GET,
                authenticatedRequest(null, teacherId, RoleName.TEACHER),
                String.class
        );

        ResponseEntity<String> page1Response = restTemplate.exchange(
                REQUEST_MAPPING + "/professors/" + teacherId + "/error-reports?page=1&size=2",
                HttpMethod.GET,
                authenticatedRequest(null, teacherId, RoleName.TEACHER),
                String.class
        );

        ResponseEntity<String> page2Response = restTemplate.exchange(
                REQUEST_MAPPING + "/professors/" + teacherId + "/error-reports?page=2&size=2",
                HttpMethod.GET,
                authenticatedRequest(null, teacherId, RoleName.TEACHER),
                String.class
        );

        assertThat(page0Response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body0 = page0Response.getBody();

        assertThat(body0).contains("\"totalElements\":3",
                "\"totalPages\":2",
                "\"number\":0",
                "\"size\":2");

        assertThat(page1Response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body1 = page1Response.getBody();

        assertThat(body1).contains("\"totalElements\":3",
                "\"totalPages\":2",
                "\"number\":1");

        assertThat(page2Response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(page2Response.getBody()).contains("\"content\":[]");
        assertThat(page2Response.getBody()).contains("\"number\":2");
    }

    @Test
    void shouldFilterReportsByStatus() {
        QuestionSetup setup = insertQuestionForTeacher(teacherId);
        enrollStudent(setup.courseId(), studentId);

        restTemplate.postForEntity(
                REQUEST_MAPPING + "/questions/" + setup.questionId() + "/error-reports",
                authenticatedRequest(bodyWithDescription(validDescription()), studentId, RoleName.STUDENT),
                String.class
        );
        ResponseEntity<String> matchingResponse = restTemplate.exchange(
                REQUEST_MAPPING + "/professors/" + teacherId + "/error-reports?status=NEW",
                HttpMethod.GET,
                authenticatedRequest(null, teacherId, RoleName.TEACHER),
                String.class
        );

        ResponseEntity<String> emptyResponse = restTemplate.exchange(
                REQUEST_MAPPING + "/professors/" + teacherId + "/error-reports?status=RESOLVED",
                HttpMethod.GET,
                authenticatedRequest(null, teacherId, RoleName.TEACHER),
                String.class
        );

        assertThat(matchingResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(matchingResponse.getBody()).contains("\"totalElements\":1");

        assertThat(emptyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(emptyResponse.getBody()).contains("\"totalElements\":0");
    }

    @Test
    void shouldFilterReportsByCourseId() {
        QuestionSetup setupCourse1 = insertQuestionForTeacher(teacherId);
        enrollStudent(setupCourse1.courseId(), studentId);

        QuestionSetup setupCourse2 = insertQuestionForTeacher(teacherId);
        enrollStudent(setupCourse2.courseId(), studentId);

        restTemplate.postForEntity(
                REQUEST_MAPPING + "/questions/" + setupCourse1.questionId() + "/error-reports",
                authenticatedRequest(bodyWithDescription(validDescription()), studentId, RoleName.STUDENT),
                String.class
        );
        ResponseEntity<String> matchResponse = restTemplate.exchange(
                REQUEST_MAPPING + "/professors/" + teacherId + "/error-reports?courseId=" + setupCourse1.courseId(),
                HttpMethod.GET,
                authenticatedRequest(null, teacherId, RoleName.TEACHER),
                String.class
        );

        ResponseEntity<String> emptyResponse = restTemplate.exchange(
                REQUEST_MAPPING + "/professors/" + teacherId + "/error-reports?courseId=" + setupCourse2.courseId(),
                HttpMethod.GET,
                authenticatedRequest(null, teacherId, RoleName.TEACHER),
                String.class
        );

        assertThat(matchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(matchResponse.getBody()).contains("\"totalElements\":1");

        assertThat(emptyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(emptyResponse.getBody()).contains("\"totalElements\":0");
    }

    @Test
    void shouldFilterReportsByBothCourseIdAndStatusSimultaneously() {
        QuestionSetup setupCourse1 = insertQuestionForTeacher(teacherId);
        enrollStudent(setupCourse1.courseId(), studentId);

        QuestionSetup setupCourse2 = insertQuestionForTeacher(teacherId);
        enrollStudent(setupCourse2.courseId(), studentId);

        restTemplate.postForEntity(
                REQUEST_MAPPING + "/questions/" + setupCourse1.questionId() + "/error-reports",
                authenticatedRequest(bodyWithDescription("Specific description for Course 1"), studentId, RoleName.STUDENT),
                String.class
        );

        restTemplate.postForEntity(
                REQUEST_MAPPING + "/questions/" + setupCourse2.questionId() + "/error-reports",
                authenticatedRequest(bodyWithDescription("Specific description for Course 2"), studentId, RoleName.STUDENT),
                String.class
        );
        ResponseEntity<String> perfectMatchResponse = restTemplate.exchange(
                REQUEST_MAPPING + "/professors/" + teacherId + "/error-reports?courseId=" + setupCourse1.courseId() + "&status=NEW",
                HttpMethod.GET,
                authenticatedRequest(null, teacherId, RoleName.TEACHER),
                String.class
        );

        ResponseEntity<String> wrongStatusResponse = restTemplate.exchange(
                REQUEST_MAPPING + "/professors/" + teacherId + "/error-reports?courseId=" + setupCourse1.courseId() + "&status=RESOLVED",
                HttpMethod.GET,
                authenticatedRequest(null, teacherId, RoleName.TEACHER),
                String.class
        );

        UUID randomCourseId = UUID.randomUUID();
        ResponseEntity<String> wrongCourseResponse = restTemplate.exchange(
                REQUEST_MAPPING + "/professors/" + teacherId + "/error-reports?courseId=" + randomCourseId + "&status=NEW",
                HttpMethod.GET,
                authenticatedRequest(null, teacherId, RoleName.TEACHER),
                String.class
        );

        assertThat(perfectMatchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(perfectMatchResponse.getBody()).contains("\"totalElements\":1");
        assertThat(perfectMatchResponse.getBody()).contains("Specific description for Course 1");

        assertThat(wrongStatusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(wrongStatusResponse.getBody()).contains("\"totalElements\":0");

        assertThat(wrongCourseResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(wrongCourseResponse.getBody()).contains("\"totalElements\":0");
    }

    @Test
    void shouldResolveReportSuccessfully() {
        QuestionSetup setup = insertQuestionForTeacher(teacherId);
        enrollStudent(setup.courseId(), studentId);
        restTemplate.postForEntity(
                REQUEST_MAPPING + "/questions/" + setup.questionId() + "/error-reports",
                authenticatedRequest(bodyWithDescription(validDescription()), studentId, RoleName.STUDENT),
                String.class
        );

        UUID reportId = getFirstReportId();

        ResponseEntity<String> response = restTemplate.exchange(
                REQUEST_MAPPING + "/error-reports/" + reportId + "/resolve",
                HttpMethod.PATCH,
                authenticatedRequest(null, teacherId, RoleName.TEACHER),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"RESOLVED\"");
    }

    @Test
    void shouldReturnNotFoundWhenResolvingNonExistentReport() {
        UUID fakeReportId = UUID.randomUUID();

        ResponseEntity<String> response = restTemplate.exchange(
                REQUEST_MAPPING + "/error-reports/" + fakeReportId + "/resolve",
                HttpMethod.PATCH,
                authenticatedRequest(null, teacherId, RoleName.TEACHER),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturnForbiddenWhenResolvingAnotherTeachersReport() {
        QuestionSetup setup = insertQuestionForTeacher(teacherId);
        enrollStudent(setup.courseId(), studentId);
        restTemplate.postForEntity(
                REQUEST_MAPPING + "/questions/" + setup.questionId() + "/error-reports",
                authenticatedRequest(bodyWithDescription(validDescription()), studentId, RoleName.STUDENT),
                String.class
        );

        UUID reportId = getFirstReportId();

        UUID teacher2Id = insertUser(RoleName.TEACHER);

        ResponseEntity<String> response = restTemplate.exchange(
                REQUEST_MAPPING + "/error-reports/" + reportId + "/resolve",
                HttpMethod.PATCH,
                authenticatedRequest(null, teacher2Id, RoleName.TEACHER),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldReturnConflictWhenReportIsAlreadyResolved() {
        QuestionSetup setup = insertQuestionForTeacher(teacherId);
        enrollStudent(setup.courseId(), studentId);
        restTemplate.postForEntity(
                REQUEST_MAPPING + "/questions/" + setup.questionId() + "/error-reports",
                authenticatedRequest(bodyWithDescription(validDescription()), studentId, RoleName.STUDENT),
                String.class
        );

        UUID reportId = getFirstReportId();

        restTemplate.exchange(
                REQUEST_MAPPING + "/error-reports/" + reportId + "/resolve",
                HttpMethod.PATCH,
                authenticatedRequest(null, teacherId, RoleName.TEACHER),
                String.class
        );

        ResponseEntity<String> response = restTemplate.exchange(
                REQUEST_MAPPING + "/error-reports/" + reportId + "/resolve",
                HttpMethod.PATCH,
                authenticatedRequest(null, teacherId, RoleName.TEACHER),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldReturnForbiddenWhenStudentTriesToResolveReport() {
        QuestionSetup setup = insertQuestionForTeacher(teacherId);
        enrollStudent(setup.courseId(), studentId);
        restTemplate.postForEntity(
                REQUEST_MAPPING + "/questions/" + setup.questionId() + "/error-reports",
                authenticatedRequest(bodyWithDescription(validDescription()), studentId, RoleName.STUDENT),
                String.class
        );

        UUID reportId = getFirstReportId();

        ResponseEntity<String> response = restTemplate.exchange(
                REQUEST_MAPPING + "/error-reports/" + reportId + "/resolve",
                HttpMethod.PATCH,
                authenticatedRequest(null, studentId, RoleName.STUDENT),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldReturnUnauthorizedWhenNoTokenProvidedForResolve() {
        UUID reportId = UUID.randomUUID();

        ResponseEntity<String> response = restTemplate.exchange(
                REQUEST_MAPPING + "/error-reports/" + reportId + "/resolve",
                HttpMethod.PATCH,
                unauthenticatedRequest(null),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
