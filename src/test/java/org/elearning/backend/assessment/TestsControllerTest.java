package org.elearning.backend.assessment;

import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.jwt.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TestsControllerTest {


    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    private UUID courseId;
    private UUID lessonId;
    private UUID chapterId;
    private UUID authenticatedUserId;
    private static final String REQUEST_MAPPING = "/api/v1";
    private static final String LESSONS = "/lessons/";
    private static final String TESTS = "/tests/";

    @BeforeEach
    void setUp() {
        authenticatedUserId = insertAuthenticatedUser();
        authorizeRequests();
        courseId = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO courses (id, title, created_by, status, visibility) " +
                        "VALUES (?, ?, ?, CAST(? AS course_status), CAST(? AS course_visibility))",
                courseId, "Test Course", authenticatedUserId, "DRAFT", "PRIVATE"
        );

        chapterId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO chapters (id, course_id, title) VALUES (?, ?, ?)",
                chapterId, courseId, "Test Chapter"
        );

        lessonId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO lessons (id, chapter_id, title, order_index) VALUES (?, ?, ?, ?)",
                lessonId, chapterId, "Test Lesson", 1
        );

    }

    @AfterEach
    void tearDown() {
        restTemplate.getRestTemplate().setInterceptors(List.of());
        jdbcTemplate.execute("DELETE FROM lesson_resources");
        jdbcTemplate.execute("DELETE FROM lessons");
        jdbcTemplate.execute("DELETE FROM chapters");
        jdbcTemplate.execute("DELETE FROM courses");
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", authenticatedUserId);
    }

    private UUID insertAuthenticatedUser() {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, status) " +
                        "VALUES (?, ?, ?, ?, ?, (SELECT id FROM roles WHERE name = CAST(? AS role_name)), CAST(? AS user_status))",
                userId,
                "lesson-resource-controller-" + userId + "@test.com",
                "password-hash",
                "Test",
                "User",
                RoleName.TEACHER.name(),
                "ACTIVE"
        );
        return userId;
    }

    private void authorizeRequests() {
        String token = jwtUtil.generateAccessToken(authenticatedUserId, RoleName.TEACHER);
        restTemplate.getRestTemplate().setInterceptors(List.of((request, body, execution) -> {
            request.getHeaders().setBearerAuth(token);
            return execution.execute(request, body);
        }));
    }

    private UUID insertTest(String title, String description, Integer timeLimitSec, Boolean aiEnabled, String status) {
        UUID testID = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO tests (id, lesson_id, created_by, title, description, time_limit_sec, ai_enabled, status)" +
                        " VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? as test_status))",
                testID, lessonId, authenticatedUserId, title, description, timeLimitSec, aiEnabled, status
        );

        return testID;
    }

    private void insertQuestion(UUID testId) {
        jdbcTemplate.update(
                "INSERT INTO questions (test_id, question_type, content) " +
                        "VALUES (?, CAST(? AS question_type), ?)",
                testId, "SINGLE_CHOICE", "Sample Question"
        );
    }


    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void shouldCreateNewTest(){
        String body = """
                {
                    "title": "Test 1",
                    "description": "Data base test",
                    "timeLimitSec": 600,
                    "aiEnabled": true
                }
                """;

        ResponseEntity<String> response = restTemplate.postForEntity(
                REQUEST_MAPPING + LESSONS + lessonId + "/test",
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    }

    @Test
    void shouldNotCreateNewTestWithIllegalData(){
        String body = """
                {
                    "id" : 00000000-0000-0000-0000-000000000001,
                    "title": "Test 1",
                    "description": "Data base test",
                    "timeLimitSec": 600,
                    "aiEnabled": true
                }
                """;

        ResponseEntity<String> response = restTemplate.postForEntity(
                REQUEST_MAPPING + LESSONS + lessonId + "/test",
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    }

    @Test
    void shouldReturnForbiddenWhenCreatingTestForMissingLessonIsRejectedByPreAuth(){
        String body = """
                {
                    "title": "Test 1",
                    "description": "Data base test",
                    "timeLimitSec": 600,
                    "aiEnabled": true
                }
                """;

        ResponseEntity<String> response = restTemplate.postForEntity(
                REQUEST_MAPPING + LESSONS + UUID.randomUUID() + "/test",
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    }

    @Test
    void shouldNotCreateNewTestIfLessonAlreadyHasATest(){
        String body = """
                {
                    "title": "Test 1",
                    "description": "Data base test",
                    "timeLimitSec": 600,
                    "aiEnabled": true
                }
                """;

        restTemplate.postForEntity(
                REQUEST_MAPPING + LESSONS + lessonId + "/test",
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        String bodyThatWillBeIgnored = """
                {
                    "title": "Ignored test",
                    "description": "A lesson cannot have more than 1 test",
                    "timeLimitSec": 600,
                    "aiEnabled": true
                }
                """;

        ResponseEntity<String> response = restTemplate.postForEntity(
                REQUEST_MAPPING + LESSONS + lessonId + "/test",
                new HttpEntity<>(bodyThatWillBeIgnored, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

    }

    @Test
    void shouldDeleteTest(){
        UUID testId = insertTest("Test1", "Testing tests", 600, false, "DRAFT");

        ResponseEntity<Void> response = restTemplate.exchange(
                REQUEST_MAPPING + TESTS + testId,
                HttpMethod.DELETE,
                new HttpEntity<>(jsonHeaders()),
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void shouldReturnForbiddenWhenDeletingMissingTestIsRejectedByPreAuth(){
        ResponseEntity<Void> response = restTemplate.exchange(
                REQUEST_MAPPING + TESTS + UUID.randomUUID(),
                HttpMethod.DELETE,
                new HttpEntity<>(jsonHeaders()),
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldGetTestFromLesson(){
        insertTest("Test1", "Testing tests", 600, false, "DRAFT");

        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + LESSONS + lessonId + "/test",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldReturnForbiddenWhenGettingTestFromMissingLessonIsRejectedByPreAuth(){
        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + LESSONS + UUID.randomUUID() + "/test",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldNotGetTestFromLessonWithNoTest(){
        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + LESSONS + lessonId + "/test",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldGetTestDetails(){
        UUID testId = insertTest("Test1", "Testing tests", 600, false, "DRAFT");

        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + TESTS + testId,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldReturnForbiddenWhenGettingMissingTestDetailsIsRejectedByPreAuth(){
        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + TESTS + UUID.randomUUID(),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldUpdateTest(){
        UUID testId = insertTest("Test1", "Testing tests", 600, false, "DRAFT");

        String body = """
            {
                "title": "Updated Title",
                "description": "Updated description",
                "timeLimitSec": 300,
                "aiEnabled": true
            }
            """;

        ResponseEntity<String> response = restTemplate.exchange(
                REQUEST_MAPPING + TESTS + testId,
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldReturnForbiddenWhenUpdatingMissingTestIsRejectedByPreAuth(){
        String body = """
            {
                "title": "Updated Title"
            }
            """;

        ResponseEntity<String> response = restTemplate.exchange(
                REQUEST_MAPPING + TESTS + UUID.randomUUID(),
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldPublishTest(){
        UUID testId = insertTest("Test1", "Testing tests", 600, false, "DRAFT");
        insertQuestion(testId);

        ResponseEntity<String> response = restTemplate.exchange(
                REQUEST_MAPPING + TESTS + testId + "/publish",
                HttpMethod.PATCH,
                new HttpEntity<>(jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldNotPublishTestWithNoQuestions(){
        UUID testId = insertTest("Test1", "Testing tests", 600, false, "DRAFT");

        ResponseEntity<String> response = restTemplate.exchange(
                REQUEST_MAPPING + TESTS + testId + "/publish",
                HttpMethod.PATCH,
                new HttpEntity<>(jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldReturnForbiddenWhenPublishingMissingTestIsRejectedByPreAuth(){
        ResponseEntity<String> response = restTemplate.exchange(
                REQUEST_MAPPING + TESTS + UUID.randomUUID() + "/publish",
                HttpMethod.PATCH,
                new HttpEntity<>(jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldNotPublishAlreadyPublishedTest(){
        UUID testId = insertTest("Test1", "Testing tests", 600, false, "PUBLISHED");
        insertQuestion(testId);

        ResponseEntity<String> response = restTemplate.exchange(
                REQUEST_MAPPING + TESTS + testId + "/publish",
                HttpMethod.PATCH,
                new HttpEntity<>(jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldGetQuestionsIfAuthor(){
        UUID testId = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO tests (id, lesson_id, created_by, title, description, time_limit_sec, ai_enabled, status)" +
                        " VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? as test_status))",
                testId, lessonId, authenticatedUserId, "Test1", "description", 600, true, "DRAFT"
        );

        insertQuestion(testId);

        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + TESTS + testId + "/questions",
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldReturnForbiddenWhenGettingQuestionsForMissingTestIsRejectedByPreAuth(){
        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + TESTS + UUID.randomUUID() + "/questions",
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }


    @Test
    void shouldNotGetQuestionsIfIsNotAuthor(){
        UUID otherTeacherId = insertAuthenticatedUser();
        String token = jwtUtil.generateAccessToken(otherTeacherId, RoleName.TEACHER);
        restTemplate.getRestTemplate().setInterceptors(List.of((request, body, execution) -> {
            request.getHeaders().setBearerAuth(token);
            return execution.execute(request, body);
        }));

        UUID testId = insertTest("Test1", "desc", 600, false, "DRAFT");
        insertQuestion(testId);

        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + TESTS + testId + "/questions",
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        jdbcTemplate.update("DELETE FROM users WHERE id = ?", otherTeacherId);
    }


    @Test
    void shouldTriggerOnUpdate(){
        UUID testId = insertTest("Test1", "desc", 600, false, "DRAFT");

        String body = """
        { "title": "Updated Title" }
        """;

        restTemplate.exchange(
                REQUEST_MAPPING + TESTS + testId,
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        LocalDateTime updatedAt = jdbcTemplate.queryForObject(
                "SELECT updated_at FROM tests WHERE id = ?",
                LocalDateTime.class, testId
        );
        assertThat(updatedAt).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("providePartialUpdateArguments")
    void shouldUpdateTestWithPartialData(String requestBody){
        UUID testId = insertTest("Test1", "desc", 600, false, "DRAFT");

        ResponseEntity<String> response = restTemplate.exchange(
                REQUEST_MAPPING + TESTS + testId,
                HttpMethod.PATCH,
                new HttpEntity<>(requestBody, jsonHeaders()),
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private static Stream<Arguments> providePartialUpdateArguments(){
        return Stream.of(
                Arguments.of("""
                        { "title": "Only title" }
                        """),
                Arguments.of("""
                        { "description": "Only desc" }
                        """),
                Arguments.of("""
                        { "timeLimitSec": 300 }
                        """),
                Arguments.of("""
                        { "aiEnabled": true }
                        """),
                Arguments.of("""
                                {}
                                """)
        );
    }

}
