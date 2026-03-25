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
class LessonsAPITests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID chapterID;

    @BeforeEach
    void setUp() {
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
        jdbcTemplate.execute("DELETE FROM lessons");
        jdbcTemplate.execute("DELETE FROM chapters");
        jdbcTemplate.execute("DELETE FROM courses");
    }

    @Test
    void shouldReturnTestEndpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/lesson-test", String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("functioneaza");
    }

    @Test
    void shouldCreateLesson() {
        String body = """
                {
                    "title": "Lectia 1",
                    "contentMd": "# Hello",
                    "orderIndex": 1
                }
                """;
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/chapters/" + chapterID + "/lessons",
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

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

    @Test
    void shouldUpdateLessonMetadata() {
        UUID lessonID = UUID.randomUUID();
        jdbcTemplate.execute(
                "INSERT INTO lessons (id, chapter_id, title, order_index) " +
                        "VALUES ('" + lessonID + "', '" + chapterID + "', 'Titlu Vechi', 1)"
        );
        String body = """
                {
                    "title": "Titlu Nou",
                    "orderIndex": 2
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

    @Test
    void shouldUpdateLessonContent() {
        UUID lessonID = UUID.randomUUID();
        jdbcTemplate.execute(
                "INSERT INTO lessons (id, chapter_id, title, order_index) " +
                        "VALUES ('" + lessonID + "', '" + chapterID + "', 'Lectia', 1)"
        );
        String newContent = "# Continut Nou";
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/lessons/" + lessonID + "/content",
                HttpMethod.PATCH,
                new HttpEntity<>(newContent, jsonHeaders()),
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Continut Nou");
    }

    @Test
    void shouldDeleteLesson() {
        UUID lessonID = UUID.randomUUID();
        jdbcTemplate.execute(
                "INSERT INTO lessons (id, chapter_id, title, order_index) " +
                        "VALUES ('" + lessonID + "', '" + chapterID + "', 'Lectia de sters', 1)"
        );
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

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}