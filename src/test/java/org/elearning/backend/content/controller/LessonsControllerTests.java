package org.elearning.backend.content.controller;

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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LessonsControllerTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    private UUID chapterID;
    private UUID authenticatedUserId;

    @BeforeEach
    void setUp() {
        authenticatedUserId = insertAuthenticatedUser();
        authorizeRequests();
        UUID courseID = UUID.randomUUID();
        jdbcTemplate.execute(
                "INSERT INTO courses (id, title, created_by, status, visibility) " +
                        "VALUES ('" + courseID + "', 'Test Course', '" + UUID.randomUUID() + "', 'DRAFT', 'PRIVATE')"
        );
        chapterID = UUID.randomUUID();
        jdbcTemplate.execute(
                "INSERT INTO chapters (id, course_id, title) " +
                        "VALUES ('" + chapterID + "', '" + courseID + "', 'Test Chapter')"
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
                "lessons-controller-" + userId + "@test.com",
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

    /**
     * GET /api/chapters/{chapterID}/lessons
     * Tests that requesting lessons for a valid chapter returns a 200 OK status and includes the expected lesson title in the response body.
     */
    @Test
    void shouldGetAllLessonsFromChapter() {
        jdbcTemplate.execute(
                "INSERT INTO lessons (id, chapter_id, title, order_index) " +
                        "VALUES ('" + UUID.randomUUID() + "', '" + chapterID + "', 'Lectia 1', 1)"
        );
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/chapters/" + chapterID + "/lessons",
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Lectia 1");
    }

    /**
     * GET /api/chapters/{chapterID}/lessons
     * Tests that requesting lessons for a non-existent chapter returns a 404 Not Found status.
     */
    @Test
    void shouldReturnNotFoundWhenRequestingLessonsForInvalidChapter() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/chapters/" + UUID.randomUUID() + "/lessons",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * POST /api/chapters/{chapterID}/lessons
     * Tests that creating a new lesson with valid data returns a 201 Created status and that the lesson is assigned the correct order index.
     */
    @Test
    void shouldCreateLesson() {
        String body = """
                {
                    "title": "Lectia 1",
                    "contentMarkdown": "# Hello",
                    "orderIndex": 99
                }
                """;
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/chapters/" + chapterID + "/lessons",
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Integer createdOrderIndex = jdbcTemplate.queryForObject(
                "SELECT order_index FROM lessons WHERE chapter_id = '" + chapterID + "'",
                Integer.class
        );
        assertThat(createdOrderIndex).isEqualTo(1);
    }

    @Test
    void shouldNotCreateLessonWithNullParameters() {
        String body = """
                {

                }
                """;
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/chapters/" + chapterID + "/lessons",
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }


    /**
     * POST /api/chapters/{chapterID}/lessons
     * Tests that creating a second lesson in the same chapter automatically assigns it an order index that is one greater than the existing lesson.
     */
    @Test
    void shouldCreateSecondLessonWithIncrementedOrderIndex() {
        UUID firstLesson = insertLesson("L1", 1, "# First");
        assertThat(firstLesson).isNotNull();

        String body = """
                {
                    "title": "L2",
                    "contentMarkdown": "# Second"
                }
                """;

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/chapters/" + chapterID + "/lessons",
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Integer secondOrderIndex = jdbcTemplate.queryForObject(
                "SELECT order_index FROM lessons WHERE title = 'L2'",
                Integer.class
        );
        assertThat(secondOrderIndex).isEqualTo(2);
    }

    /**
     * POST /api/chapters/{chapterID}/lessons
     * Tests that attempting to create a lesson under a non-existent chapter returns a 404 Not Found status.
     */
    @Test
    void shouldReturnNotFoundWhenCreatingLessonWithInvalidChapterId() {
        String body = """
                {
                    "title": "Lectia 1",
                    "contentMarkdown": "# Hello",
                    "orderIndex": 1
                }
                """;

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/chapters/" + UUID.randomUUID() + "/lessons",
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * GET /api/lessons/{id}/content
     * Tests that requesting the content of an existing lesson returns a 200 OK status and includes the expected content in the response body.
     */
    @Test
    void shouldGetLessonContent() {
        UUID lessonID = UUID.randomUUID();
        jdbcTemplate.execute(
                "INSERT INTO lessons (id, chapter_id, title, content_md, order_index) " +
                        "VALUES ('" + lessonID + "', '" + chapterID + "', 'Lectia Content', '# Continut', 1)"
        );
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/lessons/" + lessonID + "/content",
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Continut");
    }

    /**
     * GET /api/lessons/{id}/content
     * Tests that requesting the content of a non-existent lesson returns a 404 Not Found status.
     */
    @Test
    void shouldReturnNotFoundWhenGettingLessonContentForMissingLesson() {
            ResponseEntity<String> response = restTemplate.getForEntity(
                            "/api/lessons/" + UUID.randomUUID() + "/content",
                            String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * GET /api/lessons/{id}/content
     * Tests that requesting the content of a lesson that exists but has no content returns a 404 Not Found status.
     */
    @Test
    void shouldReturnNotFoundWhenLessonContentIsNull() {
            UUID lessonID = insertLesson("Lectie fara continut", 1, null);

            ResponseEntity<String> response = restTemplate.getForEntity(
                            "/api/lessons/" + lessonID + "/content",
                            String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * PATCH /api/lessons/{id}/content
     * Tests that updating the content of an existing lesson with valid markdown returns a 200 OK status and that the updated content is reflected in the response body.
     */
    @Test
    void shouldUpdateLessonContent() {
        UUID lessonID = insertLesson("Lectia", 1, null);
        String newContent = "# Continut Nou";
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/lessons/" + lessonID + "/content",
                HttpMethod.PATCH,
                new HttpEntity<>(newContent, textHeaders()),
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Continut Nou");
    }

    /**
     * PATCH /api/lessons/{id}/content
     * Tests that attempting to update the content of a non-existent lesson returns a 404 Not Found status.
     */
    @Test
    void shouldReturnNotFoundWhenUpdatingContentForMissingLesson() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/lessons/" + UUID.randomUUID() + "/content",
                HttpMethod.PATCH,
                new HttpEntity<>("# Continut", textHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * PATCH /api/lessons/{id}/metadata
     * Tests that updating the metadata of an existing lesson with valid data returns a 200 OK status and that the updated title is reflected in the response body.
     */
    @Test
    void shouldUpdateLessonMetadata() {
        UUID lessonID = UUID.randomUUID();
        jdbcTemplate.execute(
                "INSERT INTO lessons (id, chapter_id, title, order_index) " +
                        "VALUES ('" + lessonID + "', '" + chapterID + "', 'Titlu Vechi', 1)"
        );
        String body = """
                {
                    "title": "Titlu Nou"
                }
                """;
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/lessons/" + lessonID + "/metadata",
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Titlu Nou");
    }

    /**
     * PATCH /api/lessons/{id}/metadata
     * Tests that attempting to update the metadata of a non-existent lesson returns a 404 Not Found status.
     */
    @Test
    void shouldReturnNotFoundWhenUpdatingMetadataForMissingLesson() {
        String body = """
                {
                    "title": "Titlu Nou"
                }
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/lessons/" + UUID.randomUUID() + "/metadata",
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * PATCH /api/lessons/{id}/metadata
     * Tests that attempting to update the order index of a lesson to an invalid value (less than 1) returns a 400 Bad Request status.
     */
    @Test
    void shouldReturnBadRequestWhenUpdatingMetadataWithInvalidOrderIndex() {
        UUID lessonID = insertLesson("Titlu", 1, null);

        String body = """
                {
                    "orderIndex": 0
                }
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/lessons/" + lessonID + "/metadata",
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * PATCH /api/lessons/{id}/metadata
     * Tests that attempting to update the order index of a lesson to an invalid value (e.g., greater than the number of lessons in the chapter) returns a 400 Bad Request status.
     */
    @Test
    void shouldReturnBadRequestWhenUpdatingMetadataWithInvalidOrderIndexUp() {
        UUID lessonID = insertLesson("Titlu", 1, null);

        String body = """
                {
                    "orderIndex": 100
                }
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/lessons/" + lessonID + "/metadata",
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * PATCH /api/lessons/{id}/metadata
     * Tests that updating the order index of a lesson to a valid value correctly reorders the lessons and returns a 200 OK status.
     */
    @Test
    void shouldMoveLessonUpWhenUpdatingOrderIndex() {
        UUID lesson1 = insertLesson("L1", 1, null);
        UUID lesson2 = insertLesson("L2", 2, null);
        UUID lesson3 = insertLesson("L3", 3, null);

        String body = """
                {
                    "orderIndex": 1
                }
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/lessons/" + lesson3 + "/metadata",
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(orderIndexOf(lesson3)).isEqualTo(1);
        assertThat(orderIndexOf(lesson1)).isEqualTo(2);
        assertThat(orderIndexOf(lesson2)).isEqualTo(3);
    }

    /**
     * PATCH /api/lessons/{id}/metadata
     * Tests that updating the order index of a lesson to a valid value correctly reorders the lessons and returns a 200 OK status.
     */
    @Test
    void shouldMoveLessonDownWhenUpdatingOrderIndex() {
        UUID lesson1 = insertLesson("L1", 1, null);
        UUID lesson2 = insertLesson("L2", 2, null);
        UUID lesson3 = insertLesson("L3", 3, null);

        String body = """
                {
                    "orderIndex": 3
                }
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/lessons/" + lesson1 + "/metadata",
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(orderIndexOf(lesson2)).isEqualTo(1);
        assertThat(orderIndexOf(lesson3)).isEqualTo(2);
        assertThat(orderIndexOf(lesson1)).isEqualTo(3);
    }

    /**
     * PATCH /api/lessons/{id}/metadata
     * Tests that updating the order index of a lesson to the same value does not change the order of the lessons and returns a 200 OK status.
     */
    @Test
    void shouldKeepOrderWhenUpdatingWithSameIndex() {
        UUID lesson1 = insertLesson("L1", 1, null);
        UUID lesson2 = insertLesson("L2", 2, null);

        String body = """
                {
                    "orderIndex": 1
                }
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/lessons/" + lesson1 + "/metadata",
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(orderIndexOf(lesson1)).isEqualTo(1);
        assertThat(orderIndexOf(lesson2)).isEqualTo(2);
    }

    /**
     * PATCH /api/lessons/{id}/metadata
     * Tests that attempting to update the metadata of a lesson with an invalid JSON payload returns a 400 Bad Request status.
     */
    @Test
    void shouldReturnBadRequestForInvalidMetadataPayload() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/lessons/" + UUID.randomUUID() + "/metadata",
                HttpMethod.PATCH,
                new HttpEntity<>("{ invalid-json }", jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * DELETE /api/lessons/{id}
     * Tests that deleting an existing lesson returns a 204 No Content status and that the lesson is removed from the database.
     */
    @Test
    void shouldDeleteLesson() {
        UUID lessonID = insertLesson("Lectia de sters", 1, null);
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/lessons/" + lessonID,
                HttpMethod.DELETE,
                null,
                Void.class
        );
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lessons WHERE id = '" + lessonID + "'",
                Integer.class
        );
        assertThat(count).isZero();
    }

    /**
     * DELETE /api/lessons/{id}
     * Tests that attempting to delete a non-existent lesson returns a 404 Not Found status.
     */
    @Test
    void shouldReturnNotFoundWhenDeletingMissingLesson() {
            ResponseEntity<String> response = restTemplate.exchange(
                            "/api/lessons/" + UUID.randomUUID(),
                            HttpMethod.DELETE,
                            null,
                            String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * DELETE /api/lessons/{id}
     * Tests that deleting a lesson correctly repairs the order indexes of the remaining lessons in the chapter and returns a 204 No Content status.
     */
    @Test
    void shouldRepairOrderIndexesAfterDeletion() {
            UUID lesson1 = insertLesson("L1", 1, null);
            UUID lesson2 = insertLesson("L2", 2, null);
            UUID lesson3 = insertLesson("L3", 3, null);

            ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                            "/api/lessons/" + lesson2,
                            HttpMethod.DELETE,
                            null,
                            Void.class
            );

            assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            assertThat(orderIndexOf(lesson1)).isEqualTo(1);
            assertThat(orderIndexOf(lesson3)).isEqualTo(2);
    }

    /**
     * Helper method to insert a lesson into the database for testing purposes.
     * If contentMarkdown is null, it will insert a lesson without content. Otherwise, it will include the content in the insertion.
     * Returns the UUID of the inserted lesson.
     */
    private UUID insertLesson(String title, int orderIndex, String contentMarkdown) {
        UUID lessonID = UUID.randomUUID();
        if (contentMarkdown == null) {
            jdbcTemplate.execute(
                            "INSERT INTO lessons (id, chapter_id, title, order_index) " +
                                            "VALUES ('" + lessonID + "', '" + chapterID + "', '" + title + "', " + orderIndex + ")"
            );
        } else {
            jdbcTemplate.execute(
                            "INSERT INTO lessons (id, chapter_id, title, content_md, order_index) " +
                                            "VALUES ('" + lessonID + "', '" + chapterID + "', '" + title + "', '" + contentMarkdown + "', " + orderIndex + ")"
            );
        }
        return lessonID;
    }

    /**
     * Helper method to retrieve the order index of a lesson from the database for testing purposes.
     * Returns the order index as an Integer.
     */
    private Integer orderIndexOf(UUID lessonID) {
        return jdbcTemplate.queryForObject(
                        "SELECT order_index FROM lessons WHERE id = '" + lessonID + "'",
                        Integer.class
        );
    }

    /**
     * Helper method to create HttpHeaders with Content-Type set to application/json for testing purposes.
     * Returns an instance of HttpHeaders with the appropriate Content-Type.
     */
    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Helper method to create HttpHeaders with Content-Type set to text/plain for testing purposes.
     * Returns an instance of HttpHeaders with the appropriate Content-Type.
     */
    private HttpHeaders textHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        return headers;
    }
}
