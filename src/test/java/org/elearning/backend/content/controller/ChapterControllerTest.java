package org.elearning.backend.content.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ChapterControllerTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID courseId;

    @BeforeEach
    void setUp() {
        courseId = UUID.randomUUID();
        jdbcTemplate.execute(
                "INSERT INTO courses (id, title, created_by, status, visibility) " +
                        "VALUES ('" + courseId + "', 'Test Course', '" + UUID.randomUUID() + "', 'DRAFT', 'PRIVATE')"
        );
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM lessons");
        jdbcTemplate.execute("DELETE FROM chapters");
        jdbcTemplate.execute("DELETE FROM courses");
    }

    /**
     * POST /api/courses/{courseId}/chapters
     * Tests that creating a new chapter with valid data returns a 201 Created status
     * and that the chapter is assigned the correct order index.
     */
    @Test
    void shouldCreateChapter() {
        String body = "Test Chapter";
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/courses/" + courseId + "/chapters",
                new HttpEntity<>(body, textHeaders()),
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("Test Chapter");

        Integer createdOrderIndex = jdbcTemplate.queryForObject(
                "SELECT order_index FROM chapters WHERE course_id = '" + courseId + "'",
                Integer.class
        );
        assertThat(createdOrderIndex).isEqualTo(1);
    }

    /**
     * POST /api/courses/{courseId}/chapters
     * Tests that creating a second chapter in the same course automatically assigns it
     * an order index that is one greater than the existing chapter.
     */
    @Test
    void shouldCreateSecondChapterWithIncrementedOrderIndex() {
        insertChapter("Chapter 1", 1);

        String body = "Chapter 2";
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/courses/" + courseId + "/chapters",
                new HttpEntity<>(body, textHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Integer secondOrderIndex = jdbcTemplate.queryForObject(
                "SELECT order_index FROM chapters WHERE title = 'Chapter 2'",
                Integer.class
        );
        assertThat(secondOrderIndex).isEqualTo(2);
    }

    /**
     * POST /api/courses/{courseId}/chapters
     * Tests that creating a chapter for a non-existent course returns a 404 Not Found status.
     */
    @Test
    void shouldReturnNotFoundWhenCreatingChapterForInvalidCourse() {
        String body = "Test Chapter";
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/courses/" + UUID.randomUUID() + "/chapters",
                new HttpEntity<>(body, textHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * GET /api/courses/{courseId}/chapters
     * Tests that requesting chapters for a valid course returns a 200 OK status
     * and includes the expected chapter title in the response body.
     */
    @Test
    void shouldGetAllChaptersFromCourse() {
        insertChapter("Chapter 1", 1);
        insertChapter("Chapter 2", 2);

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/courses/" + courseId + "/chapters",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Chapter 1");
        assertThat(response.getBody()).contains("Chapter 2");
    }

    /**
     * GET /api/courses/{courseId}/chapters
     * Tests that requesting chapters for a non-existent course returns a 404 Not Found status.
     */
    @Test
    void shouldReturnNotFoundWhenRequestingChaptersForInvalidCourse() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/courses/" + UUID.randomUUID() + "/chapters",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * DELETE /api/chapters/{id}
     * Tests that deleting an existing chapter returns a 204 No Content status.
     */
    @Test
    void shouldDeleteChapter() {
        UUID chapterId = insertChapter("Chapter to Delete", 1);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/chapters/" + chapterId,
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM chapters WHERE id = '" + chapterId + "'",
                Integer.class
        );
        assertThat(count).isZero();
    }

    /**
     * DELETE /api/chapters/{id}
     * Tests that deleting a non-existent chapter returns a 404 Not Found status.
     */
    @Test
    void shouldReturnNotFoundWhenDeletingInvalidChapter() {
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/chapters/" + UUID.randomUUID(),
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * DELETE /api/chapters/{id}
     * Tests that deleting a chapter repairs the order indices of remaining chapters in the same course.
     */
    @Test
    void shouldRepairOrderIndicesAfterDeletion() {
        UUID chapter1 = insertChapter("Chapter 1", 1);
        UUID chapter2 = insertChapter("Chapter 2", 2);
        UUID chapter3 = insertChapter("Chapter 3", 3);

        restTemplate.exchange(
                "/api/chapters/" + chapter2,
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertThat(orderIndexOf(chapter1)).isEqualTo(1);
        assertThat(orderIndexOf(chapter3)).isEqualTo(2);
    }

    /**
     * PATCH /api/chapters/{id}
     * Tests that updating a chapter's title returns a 200 OK status.
     */
    @Test
    void shouldUpdateChapterTitle() {
        UUID chapterId = insertChapter("Original Title", 1);

        String body = """
                {
                    "title": "Updated Title"
                }
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/chapters/" + chapterId,
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Updated Title");

        String title = jdbcTemplate.queryForObject(
                "SELECT title FROM chapters WHERE id = '" + chapterId + "'",
                String.class
        );
        assertThat(title).isEqualTo("Updated Title");
    }

    /**
     * PATCH /api/chapters/{id}
     * Tests that updating a chapter's order index returns a 200 OK status.
     */
    @Test
    void shouldUpdateChapterOrderIndex() {
        UUID chapter1 = insertChapter("Chapter 1", 1);
        UUID chapter2 = insertChapter("Chapter 2", 2);
        UUID chapter3 = insertChapter("Chapter 3", 3);

        String body = """
                {
                    "orderIndex": 3
                }
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/chapters/" + chapter1,
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(orderIndexOf(chapter1)).isEqualTo(3);
        assertThat(orderIndexOf(chapter2)).isEqualTo(1);
        assertThat(orderIndexOf(chapter3)).isEqualTo(2);
    }

    /**
     * PATCH /api/chapters/{id}
     * Tests that updating both title and order index returns a 200 OK status.
     */
    @Test
    void shouldUpdateChapterTitleAndOrderIndex() {
        UUID chapter1 = insertChapter("Chapter 1", 1);
        UUID chapter2 = insertChapter("Chapter 2", 2);

        String body = """
                {
                    "title": "Updated Chapter",
                    "orderIndex": 2
                }
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/chapters/" + chapter1,
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Updated Chapter");

        String title = jdbcTemplate.queryForObject(
                "SELECT title FROM chapters WHERE id = '" + chapter1 + "'",
                String.class
        );
        assertThat(title).isEqualTo("Updated Chapter");
        assertThat(orderIndexOf(chapter1)).isEqualTo(2);
        assertThat(orderIndexOf(chapter2)).isEqualTo(1);
    }

    /**
     * PATCH /api/chapters/{id}
     * Tests that updating a non-existent chapter returns a 404 Not Found status.
     */
    @Test
    void shouldReturnNotFoundWhenUpdatingInvalidChapter() {
        String body = """
                {
                    "title": "Updated Title"
                }
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/chapters/" + UUID.randomUUID(),
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * PATCH /api/chapters/{id}
     * Tests that updating a chapter with an invalid order index (too large) returns a 400 Bad Request status.
     */
    @Test
    void shouldReturnBadRequestWhenOrderIndexTooLarge() {
        UUID chapter1 = insertChapter("Chapter 1", 1);

        String body = """
                {
                    "orderIndex": 10
                }
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/chapters/" + chapter1,
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /**
     * PATCH /api/chapters/{id}
     * Tests that updating a chapter with an invalid order index (too small or zero) returns a 400 Bad Request status.
     */
    @Test
    void shouldReturnBadRequestWhenOrderIndexTooSmall() {
        UUID chapter1 = insertChapter("Chapter 1", 1);

        String body = """
                {
                    "orderIndex": 0
                }
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/chapters/" + chapter1,
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /**
     * PATCH /api/chapters/{id}
     * Tests that updating a chapter with a negative order index returns a 400 Bad Request status.
     */
    @Test
    void shouldReturnBadRequestWhenOrderIndexNegative() {
        UUID chapter1 = insertChapter("Chapter 1", 1);

        String body = """
                {
                    "orderIndex": -1
                }
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/chapters/" + chapter1,
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /**
     * Helper method to insert a chapter into the database for testing purposes.
     * Returns the UUID of the inserted chapter.
     */
    private UUID insertChapter(String title, int orderIndex) {
        UUID chapterId = UUID.randomUUID();
        jdbcTemplate.execute(
                "INSERT INTO chapters (id, course_id, title, order_index) " +
                        "VALUES ('" + chapterId + "', '" + courseId + "', '" + title + "', " + orderIndex + ")"
        );
        return chapterId;
    }

    /**
     * Helper method to retrieve the order index of a chapter from the database for testing purposes.
     * Returns the order index as an Integer.
     */
    private Integer orderIndexOf(UUID chapterId) {
        return jdbcTemplate.queryForObject(
                "SELECT order_index FROM chapters WHERE id = '" + chapterId + "'",
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
