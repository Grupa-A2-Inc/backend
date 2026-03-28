package org.elearning.backend.content.controller;


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
    private UUID chapterId;

    @BeforeEach
    void setUp() {
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
                "/api/lessons/" + lessonId + "/resources",
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
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
    void shouldGetSingleResourceByLessonId() {
        insertLessonResource("Resursa Test", "https://test.com");

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/lessons/" + lessonId + "/resources",
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
                "/api/lessons/" + lessonId + "/resources",
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
                "/api/lessons/" + UUID.randomUUID() + "/resources",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
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
        UUID resourceId = insertLessonResource("Resursa de sters", "https://delete.com");
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
    void shouldDeleteOnlyOneLessonResource() {
        insertLessonResource("I wont go away", "https://staying.com");
        insertLessonResource("Me neither", "https://stayingtoo.com");
        UUID resourceId = insertLessonResource("Resursa de sters", "https://delete.com");
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/lessons/" + lessonId + "/resources/" + resourceId,
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
                "/api/lessons/" + UUID.randomUUID() + "/resources/" + resourceId,
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
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

    @Test
    void shouldReturnOKWhenPatchingWithAllData(){
        UUID resourceId = insertLessonResource("Resource to update", "https://update.com");

        String body = """
                {
                    "title": "Updated Resource",
                    "url" : "https://updated.com"
                }
                """;
        ResponseEntity<String> updateResponse = restTemplate.exchange(
                "/api/lessons/" + lessonId + "/resources/" + resourceId,
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody()).contains("Updated Resource");
        assertThat(updateResponse.getBody()).contains("https://updated.com");
    }

    @Test
    void shouldReturnOKWhenPatchingTitle(){
        UUID resourceId = insertLessonResource("Resource to update", "https://update.com");

        String body = """
                {
                    "title": "Updated Resource"
                }
                """;
        ResponseEntity<String> updateResponse = restTemplate.exchange(
                "/api/lessons/" + lessonId + "/resources/" + resourceId,
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody()).contains("Updated Resource");
        assertThat(updateResponse.getBody()).contains("https://update.com");
    }

    @Test
    void shouldReturnOKWhenPatchingURL(){
        UUID resourceId = insertLessonResource("Resource to update", "https://update.com");

        String body = """
                {
                   "url" : "https://updated.com"
                }
                """;
        ResponseEntity<String> updateResponse = restTemplate.exchange(
                "/api/lessons/" + lessonId + "/resources/" + resourceId,
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody()).contains("https://updated.com");
        assertThat(updateResponse.getBody()).contains("Resource to update");
    }

    @Test
    void shouldReturnOKWhenPatchingWithEmptyBody(){
        UUID resourceId = insertLessonResource("Resource to update", "https://update.com");

        String body = """
                {
                
                }
                """;
        ResponseEntity<String> updateResponse = restTemplate.exchange(
                "/api/lessons/" + lessonId + "/resources/" + resourceId,
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody()).contains("https://update.com");
        assertThat(updateResponse.getBody()).contains("Resource to update");
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
                "/api/lessons/" + UUID.randomUUID() + "/resources/" + resourceId,
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
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
                "/api/lessons/" + lessonId + "/resources/" + UUID.randomUUID(),
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
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
                "/api/lessons/" + unrelatedLessonId + "/resources/" + unrelatedResourceId,
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

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