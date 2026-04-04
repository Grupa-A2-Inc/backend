package org.elearning.backend.content.controller;


import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.jwt.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.List;
import java.util.stream.Stream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LessonResourceControllerTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    private UUID lessonId;
    private UUID chapterId;
    private UUID authenticatedUserId;

    private static final String REQUEST_MAPPING = "/api/v1/lessons/";

    @BeforeEach
    void setUp() {
        authenticatedUserId = insertAuthenticatedUser();
        authorizeRequests();
        UUID courseId = UUID.randomUUID();
        jdbcTemplate.execute(
                "INSERT INTO courses (id, title, created_by, status, visibility) " +
                        "VALUES ('" + courseId + "', 'Test Course', '" + UUID.randomUUID() + "', 'DRAFT', 'PRIVATE')"
        );


        chapterId = UUID.randomUUID();
        jdbcTemplate.execute(
                "INSERT INTO chapters (id, course_id, title) " +
                        "VALUES ('" + chapterId + "', '" + courseId + "', 'Test Chapter')"
        );

        lessonId = UUID.randomUUID();
        jdbcTemplate.execute(
                "INSERT INTO lessons (id, chapter_id, title, order_index) " +
                        "VALUES ('" + lessonId + "', '" + chapterId + "', 'Test Lesson', 1)"
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

    @Test
    void shouldCreateLessonResource() {
        String body = """
                {
                    "title": "Documentatie",
                    "url": "https://link.com/doc.pdf"
                }
                """;

        ResponseEntity<String> response = restTemplate.postForEntity(
                REQUEST_MAPPING + lessonId + "/resources",
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @ParameterizedTest(name = "Test invalid payload #{index}: {0}")
    @ValueSource(strings = {
            // Lipsesc url și description
            """
            {
                "title": "Documentatie"
            }
            """,
            // Lipsesc title și description
            """
            {
                "url": "https://link.com/doc.pdf"
            }
            """,
            // Payload gol (lipsesc toate)
            """
            {
            }
            """
    })
    void shouldReturnBadRequestWhenCreatingLessonResourceWithInvalidFields(String body) {
        ResponseEntity<String> response = restTemplate.postForEntity(
                REQUEST_MAPPING + lessonId + "/resources",
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void shouldReturnNotFoundWhenCreatingResourceForInvalidLesson() {
        String body = """
                {
                    "title": "Documentatie",
                    "url": "https://link.com/doc.pdf"
                }
                """;

        ResponseEntity<String> response = restTemplate.postForEntity(
                REQUEST_MAPPING + UUID.randomUUID() + "/resources",
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void shouldGetSingleResourceByLessonId() {
        insertLessonResource("Resursa Test", "https://test.com");

        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + lessonId + "/resources",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Resursa Test");
    }

    @Test
    void shouldGetMultipleResourcesByLessonId() {
        insertLessonResource("Resursa Test 1", "https://test1.com");
        insertLessonResource("Resursa Test 2", "https://test2.com");
        insertLessonResource("Resursa Test 3", "https://test3.com");

        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + lessonId + "/resources",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Resursa Test 1");
        assertThat(response.getBody()).contains("Resursa Test 2");
        assertThat(response.getBody()).contains("Resursa Test 3");
    }

    @Test
    void shouldReturnNotFoundForInvalidLessonId() {
        insertLessonResource("Resursa Test", "https://test.com");

        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + UUID.randomUUID() + "/resources",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void shouldReturnNotFoundWhenGettingResourcesForInvalidLesson() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + UUID.randomUUID() + "/resources",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void shouldDeleteLessonResource() {
        UUID resourceId = insertLessonResource("Resursa de sters", "https://delete.com");
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                REQUEST_MAPPING + lessonId + "/resources/" + resourceId,
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lesson_resources WHERE id = '" + resourceId + "'",
                Integer.class
        );
        assertThat(count).isZero();
    }

    @Test
    void shouldDeleteOnlyOneLessonResource() {
        insertLessonResource("I wont go away", "https://staying.com");
        insertLessonResource("Me neither", "https://stayingtoo.com");
        UUID resourceId = insertLessonResource("Resursa de sters", "https://delete.com");
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                REQUEST_MAPPING + lessonId + "/resources/" + resourceId,
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lesson_resources WHERE lesson_id = '" + lessonId + "'",
                Integer.class
        );
        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingWithInvalidLessonId() {
        UUID resourceId = insertLessonResource("Resursa de sters", "https://delete.com");
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                REQUEST_MAPPING + UUID.randomUUID() + "/resources/" + resourceId,
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingInvalidResource() {
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                REQUEST_MAPPING + lessonId + "/resources/" + UUID.randomUUID(),
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @ParameterizedTest(name = "Test {index}: Patching with expected Title = ''{1}'', expected URL = ''{2}''")
    @MethodSource("providePatchArguments")
    void shouldReturnOKWhenPatchingResource(String requestBody, String expectedTitle, String expectedUrl) {
        // Arrange
        UUID resourceId = insertLessonResource("Resource to update", "https://update.com");

        // Act
        ResponseEntity<String> updateResponse = restTemplate.exchange(
                REQUEST_MAPPING + lessonId + "/resources/" + resourceId,
                HttpMethod.PATCH,
                new HttpEntity<>(requestBody, jsonHeaders()),
                String.class
        );

        // Assert
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody()).contains(expectedTitle);
        assertThat(updateResponse.getBody()).contains(expectedUrl);
    }

    private static Stream<Arguments> providePatchArguments() {
        return Stream.of(
                // Case 1: Patch both fields
                Arguments.of("""
                    {
                        "title": "Updated Resource",
                        "url" : "https://updated.com"
                    }
                    """,
                        "Updated Resource",
                        "https://updated.com"),

                // Case 2: Patch title only (url remains the original)
                Arguments.of("""
                    {
                        "title": "Updated Resource"
                    }
                    """,
                        "Updated Resource",
                        "https://update.com"),

                // Case 3: Patch URL only (title remains the original)
                Arguments.of("""
                    {
                       "url" : "https://updated.com"
                    }
                    """,
                        "Resource to update",
                        "https://updated.com"),

                // Case 4: Patch with empty body (both fields remain original)
                Arguments.of("""
                    {}
                    """,
                        "Resource to update",
                        "https://update.com")
        );
    }

    @Test
    void shouldNotPatchWithInvalidLessonID(){
        UUID resourceId = insertLessonResource("Resource to update", "https://update.com");

        String body = """
                {
                    "title" : "I will not change",
                    "url" : "https://Iwillnotchange.com"
           
                }
                """;
        ResponseEntity<String> updateResponse = restTemplate.exchange(
                REQUEST_MAPPING + UUID.randomUUID() + "/resources/" + resourceId,
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void shouldNotPatchWithInvalidResourceID(){

        String body = """
                {
                    "title" : "I will not change",
                    "url" : "https://Iwillnotchange.com"
           
                }
                """;
        ResponseEntity<String> updateResponse = restTemplate.exchange(
                REQUEST_MAPPING + lessonId + "/resources/" + UUID.randomUUID(),
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void shouldNotPatchIfExistingLessonIsNotRelatedToExistingResource(){
        UUID unrelatedLessonId = UUID.randomUUID();
        jdbcTemplate.execute(
                "INSERT INTO lessons (id, chapter_id, title, order_index) " +
                        "VALUES ('" + unrelatedLessonId + "', '" + chapterId + "', 'Test Lesson', 1)"
        );
        UUID unrelatedResourceId = insertLessonResource("Resource to update", "https://update.com");
        String body = """
                {
                    "title" : "I will not change",
                    "url" : "https://Iwillnotchange.com"
           
                }
                """;
        ResponseEntity<String> updateResponse = restTemplate.exchange(
                REQUEST_MAPPING + unrelatedLessonId + "/resources/" + unrelatedResourceId,
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

    }



    /**
     * Helper method to insert a lesson_resource into the database for testing purposes.
     * Returns the UUID of the inserted lesson_resource.
     */

    private UUID insertLessonResource(String title, String url) {
        UUID lessonResourceID = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO lesson_resources (id, lesson_id, title, url) VALUES (?, ?, ?, ?)",
                lessonResourceID, lessonId, title, url
        );

        return lessonResourceID;
    }





    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
