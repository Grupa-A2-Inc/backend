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
class LessonResourceControllerTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID lessonId;

    @BeforeEach
    void setUp() {
        UUID courseId = UUID.randomUUID();
        jdbcTemplate.execute(
                "INSERT INTO courses (id, title, created_by, status, visibility) " +
                        "VALUES ('" + courseId + "', 'Test Course', '" + UUID.randomUUID() + "', 'DRAFT', 'PRIVATE')"
        );

        UUID chapterId = UUID.randomUUID();
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
        jdbcTemplate.execute("DELETE FROM lesson_resources");
        jdbcTemplate.execute("DELETE FROM lessons");
        jdbcTemplate.execute("DELETE FROM chapters");
        jdbcTemplate.execute("DELETE FROM courses");
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
                "/api/lessons/" + lessonId + "/resources",
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
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
                "/api/lessons/" + UUID.randomUUID() + "/resources",
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldGetResourcesByLessonId() {
        UUID resourceId = UUID.randomUUID();
        jdbcTemplate.execute(
                "INSERT INTO lesson_resources (id, lesson_id, title, url) " +
                        "VALUES ('" + resourceId + "', '" + lessonId + "', 'Resursa Test', 'https://test.com')"
        );

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/lessons/" + lessonId + "/resources",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Resursa Test");
    }

    @Test
    void shouldReturnNotFoundWhenGettingResourcesForInvalidLesson() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/lessons/" + UUID.randomUUID() + "/resources",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldDeleteLessonResource() {
        UUID resourceId = UUID.randomUUID();
        jdbcTemplate.execute(
                "INSERT INTO lesson_resources (id, lesson_id, title, url) " +
                        "VALUES ('" + resourceId + "', '" + lessonId + "', 'Resursa de sters', 'https://delete.com')"
        );

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/lessons/" + lessonId + "/resources/" + resourceId,
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
    void shouldReturnNotFoundWhenDeletingInvalidResource() {
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/lessons/" + lessonId + "/resources/" + UUID.randomUUID(),
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}