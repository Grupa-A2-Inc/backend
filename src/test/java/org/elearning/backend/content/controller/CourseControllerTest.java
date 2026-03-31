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
import org.junit.jupiter.api.Disabled;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CourseControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID instructorId;

    @BeforeEach
    void setUp() {
        instructorId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM lessons");
        jdbcTemplate.execute("DELETE FROM chapters");
        jdbcTemplate.execute("DELETE FROM courses");
    }

    /**
     * POST /api/courses
     * Tests that creating a new course with valid data returns a 201 Created status
     * and that the response body contains the course title.
     */
    @Test
    void shouldCreateCourse() {
        String body = """
                {
                    "title": "Introduction to Java",
                    "description": "A beginner course on Java",
                    "category": "Programming",
                    "createdBy": "%s"
                }
                """.formatted(instructorId);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/courses",
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("Introduction to Java");
    }

    /**
     * POST /api/courses
     * Tests that a newly created course has DRAFT status and PRIVATE visibility by default.
     */
    @Test
    void shouldCreateCourseWithDefaultStatusAndVisibility() {
        String body = """
                {
                    "title": "Default Status Course",
                    "createdBy": "%s"
                }
                """.formatted(instructorId);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/courses",
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM courses WHERE title = 'Default Status Course'",
                String.class
        );
        String visibility = jdbcTemplate.queryForObject(
                "SELECT visibility FROM courses WHERE title = 'Default Status Course'",
                String.class
        );

        assertThat(status).isEqualTo("DRAFT");
        assertThat(visibility).isEqualTo("PRIVATE");
    }

    /**
     * GET /api/courses?role=INSTRUCTOR&userId={id}
     * Tests that an instructor can retrieve only their own courses.
     */
    @Test
    @Disabled("Temporar dezactivat pana cand Echipa 2 implementeaza extragerea userului din token-ul de securitate")
    void shouldGetCoursesForInstructor() {
        insertCourse("My Course", instructorId);
        insertCourse("Other Course", UUID.randomUUID());

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/courses", // am scos paramaterii din URL, ca in noul controller
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("My Course");
        assertThat(response.getBody()).doesNotContain("Other Course");
    }

    /**
     * GET /api/courses?role=STUDENT&userId={id}
     * Tests that a student can retrieve all published/public courses.
     */
    @Test
    @Disabled("Temporar dezactivat pana cand Echipa 2 implementeaza extragerea userului din token-ul de securitate")
    void shouldGetPublicCoursesForStudent() {
        insertCourseWithStatusAndVisibility("Public Course", UUID.randomUUID(), "PUBLISHED", "PUBLIC");
        insertCourseWithStatusAndVisibility("Private Course", UUID.randomUUID(), "DRAFT", "PRIVATE");

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/courses", // am scos parametrii din URL
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Public Course");
        assertThat(response.getBody()).doesNotContain("Private Course");
    }
    /**
     * PUT /api/courses/{id}
     * Tests that updating a course with valid data returns a 200 OK status
     * and that the response body reflects the updated title.
     */
    @Test
    void shouldUpdateCourse() {
        UUID courseId = insertCourse("Old Title", instructorId);

        String body = """
                {
                    "title": "New Title",
                    "description": "Updated description",
                    "createdBy": "%s"
                }
                """.formatted(instructorId);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/courses/" + courseId,
                HttpMethod.PUT,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("New Title");

        String updatedTitle = jdbcTemplate.queryForObject(
                "SELECT title FROM courses WHERE id = '" + courseId + "'",
                String.class
        );
        assertThat(updatedTitle).isEqualTo("New Title");
    }

    /**
     * PUT /api/courses/{id}
     * Tests that updating a non-existent course returns a 404 Not Found status.
     */
    @Test
    void shouldReturnNotFoundWhenUpdatingInvalidCourse() {
        String body = """
                {
                    "title": "Doesn't Matter",
                    "createdBy": "%s"
                }
                """.formatted(instructorId);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/courses/" + UUID.randomUUID(),
                HttpMethod.PUT,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * DELETE /api/courses/{id}
     * Tests that deleting an existing course returns a 204 No Content status
     * and that the course is removed from the database.
     */
    @Test
    void shouldDeleteCourse() {
        UUID courseId = insertCourse("Course to Delete", instructorId);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/courses/" + courseId,
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM courses WHERE id = '" + courseId + "'",
                Integer.class
        );
        assertThat(count).isZero();
    }

    /**
     * DELETE /api/courses/{id}
     * Tests that deleting a non-existent course returns a 404 Not Found status.
     */
    @Test
    void shouldReturnNotFoundWhenDeletingInvalidCourse() {
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/courses/" + UUID.randomUUID(),
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * DELETE /api/courses/{id}
     * Tests that deleting a course also removes all its associated chapters and lessons (cascade delete).
     */
    @Test
    void shouldCascadeDeleteChaptersAndLessonsWhenCourseIsDeleted() {
        UUID courseId = insertCourse("Course with Chapters", instructorId);
        UUID chapterId = UUID.randomUUID();
        jdbcTemplate.execute(
                "INSERT INTO chapters (id, course_id, title, order_index) " +
                        "VALUES ('" + chapterId + "', '" + courseId + "', 'Chapter 1', 1)"
        );

        restTemplate.exchange(
                "/api/courses/" + courseId,
                HttpMethod.DELETE,
                null,
                Void.class
        );

        Integer chapterCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM chapters WHERE course_id = '" + courseId + "'",
                Integer.class
        );
        assertThat(chapterCount).isZero();
    }

    /**
     * Helper method to insert a course into the database for testing purposes.
     * Returns the UUID of the inserted course.
     */
    private UUID insertCourse(String title, UUID createdBy) {
        return insertCourseWithStatusAndVisibility(title, createdBy, "DRAFT", "PRIVATE");
    }

    /**
     * Helper method to insert a course with explicit status and visibility into the database.
     * Returns the UUID of the inserted course.
     */
    private UUID insertCourseWithStatusAndVisibility(String title, UUID createdBy, String status, String visibility) {
        UUID courseId = UUID.randomUUID();
        jdbcTemplate.execute(
                "INSERT INTO courses (id, title, created_by, status, visibility) " +
                        "VALUES ('" + courseId + "', '" + title + "', '" + createdBy + "', '" + status + "', '" + visibility + "')"
        );
        return courseId;
    }

    /**
     * Helper method to create HttpHeaders with Content-Type set to application/json for testing purposes.
     */
    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}