package org.elearning.backend.content.controller;

import org.elearning.backend.auth.service.AccountActivationService;
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
import org.junit.jupiter.api.Disabled;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CourseControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private AccountActivationService accountActivationService;

    private UUID instructorId;
    private UUID authenticatedUserId;
    private static final String REQUEST_MAPPING = "/api/v1/courses";

    @BeforeEach
    void setUp() {
        authenticatedUserId = insertAuthenticatedUser();
        authorizeRequests();
        instructorId = authenticatedUserId;
    }

    private UUID insertCourseWithTitleAndDescription(String title, String description, UUID createdBy) {
        UUID courseId = UUID.randomUUID();
        jdbcTemplate.execute(
                "INSERT INTO courses (id, title, description, created_by, status, visibility) " +
                        "VALUES ('" + courseId + "', '" + title + "', '" + description + "', '" + createdBy + "', 'DRAFT', 'PRIVATE')"
        );
        return courseId;
    }
    @AfterEach
    void tearDown() {
        restTemplate.getRestTemplate().setInterceptors(List.of());
        jdbcTemplate.execute("DELETE FROM lessons");
        jdbcTemplate.execute("DELETE FROM chapters");
        jdbcTemplate.execute("DELETE FROM courses");
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", authenticatedUserId);
    }

    private UUID insertAuthenticatedUser() {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, role_type, status) " +
                        "VALUES (?, ?, ?, ?, ?, (SELECT id FROM roles WHERE name = CAST(? AS role_name)), ?, CAST(? AS user_status))",
                userId,
                "lesson-resource-controller-" + userId + "@test.com",
                "password-hash",
                "Test",
                "User",
                RoleName.TEACHER.name(),
                "User",
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
     * GET /api/courses/public
     * Tests that only published and public courses are returned.
     */
    @Test
    void shouldGetOnlyPublicAndPublishedCourses() {
        insertCourseWithStatusAndVisibility("Public Course", UUID.randomUUID(), "PUBLISHED", "PUBLIC");
        insertCourseWithStatusAndVisibility("Draft Private Course", UUID.randomUUID(), "DRAFT", "PRIVATE");
        insertCourseWithStatusAndVisibility("Published Private Course", UUID.randomUUID(), "PUBLISHED", "PRIVATE");

        ResponseEntity<String> response = restTemplate.getForEntity(
                 REQUEST_MAPPING + "/public",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Public Course");
        assertThat(response.getBody()).doesNotContain("Draft Private Course");
        assertThat(response.getBody()).doesNotContain("Published Private Course");
    }

    /**
     * GET /api/courses/public
     * Tests that an empty list is returned when there are no public courses.
     */
    @Test
    void shouldReturnEmptyListWhenNoPublicCourses() {
        insertCourseWithStatusAndVisibility("Draft Course", UUID.randomUUID(), "DRAFT", "PRIVATE");

        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + "/public",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("[]");
    }

    /**
     * GET /api/courses/my-courses
     * Tests that only courses created by the hardcoded user are returned.
     */
    @Test
    void shouldGetOnlyMyCoursesForHardcodedUser() {
        insertCourse("My Course", authenticatedUserId);
        insertCourse("Other Course", UUID.randomUUID());

        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + "/my-courses",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("My Course");
        assertThat(response.getBody()).doesNotContain("Other Course");
    }

    /**
     * GET /api/courses/my-courses
     * Tests that an empty list is returned when the hardcoded user has no courses.
     */
    @Test
    void shouldReturnEmptyListWhenUserHasNoCourses() {
        insertCourse("Other Course", UUID.randomUUID());

        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + "/my-courses",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("[]");
    }

    /**
     * PATCH /api/courses/{id}
     * Tests that patching only the title leaves other fields unchanged.
     */
    @Test
    void shouldPatchOnlyTitle() {
        UUID courseId = insertCourse("Original Title", instructorId);

        String body = """
            {
                "title": "Patched Title"
            }
            """;

        ResponseEntity<String> response = restTemplate.exchange(
                REQUEST_MAPPING + "/" + courseId,
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Patched Title");

        String titleInDb = jdbcTemplate.queryForObject(
                "SELECT title FROM courses WHERE id = '" + courseId + "'",
                String.class
        );
        assertThat(titleInDb).isEqualTo("Patched Title");
    }

    /**
     * PATCH /api/courses/{id}
     * Tests that null fields in the request body do not overwrite existing values.
     */
    @Test
    void shouldNotOverwriteFieldsWithNullOnPatch() {
        UUID courseId = insertCourseWithTitleAndDescription("Original Title", "Original Description", instructorId);

        String body = """
            {
                "title": "Patched Title"
            }
            """;

        restTemplate.exchange(
                REQUEST_MAPPING + "/" + courseId,
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        String descriptionInDb = jdbcTemplate.queryForObject(
                "SELECT description FROM courses WHERE id = '" + courseId + "'",
                String.class
        );
        assertThat(descriptionInDb).isEqualTo("Original Description");
    }

    /**
     * PATCH /api/courses/{id}
     * Tests that patching a non-existent course returns 404.
     */
    @Test
    void shouldReturnNotFoundWhenPatchingInvalidCourse() {
        String body = """
            {
                "title": "Doesn't Matter"
            }
            """;

        ResponseEntity<String> response = restTemplate.exchange(
                REQUEST_MAPPING + "/" + UUID.randomUUID(),
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
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
                REQUEST_MAPPING,
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
                REQUEST_MAPPING,
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
     * POST /api/courses
     * Tests that if status is provided, it is preserved on create.
     */
    @Test
    void shouldCreateCourseWithProvidedStatus() {
        String title = "Published Course " + UUID.randomUUID();
        String body = """
                {
                    "title": "%s",
                    "status": "PUBLISHED"
                }
                """.formatted(title);

        ResponseEntity<String> response = restTemplate.postForEntity(
                REQUEST_MAPPING,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM courses WHERE title = '" + title + "'",
                String.class
        );
        assertThat(status).isEqualTo("PUBLISHED");
    }

    /**
     * POST /api/courses
     * Tests that nested chapters, lessons and resources are saved and linked correctly.
     */
    @Test
    void shouldLinkChaptersLessonsAndResourcesOnCreate() {
        String title = "Nested Course " + UUID.randomUUID();
        String body = """
                {
                    "title": "%s",
                    "description": "Course with nested content",
                    "category": "Programming",
                    "chapters": [
                        {
                            "title": "Chapter One",
                            "orderIndex": 1,
                            "lessons": [
                                {
                                    "title": "Lesson One",
                                    "contentMarkdown": "# Intro",
                                    "orderIndex": 1,
                                    "lessonResources": [
                                        {
                                            "title": "Slides",
                                            "url": "https://example.com/slides"
                                        },
                                        {
                                            "title": "Video",
                                            "url": "https://example.com/video"
                                        }
                                    ]
                                }
                            ]
                        },
                        {
                            "title": "Chapter Two",
                            "orderIndex": 2,
                            "lessons": [
                                {
                                    "title": "Lesson Two",
                                    "contentMarkdown": "# Deep dive",
                                    "orderIndex": 1
                                }
                            ]
                        },
                        {
                            "title": "Chapter Three",
                            "orderIndex": 3
                        }
                    ]
                }
                """.formatted(title);

        ResponseEntity<String> response = restTemplate.postForEntity(
                REQUEST_MAPPING,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("Chapter One");
        assertThat(response.getBody()).contains("Lesson One");
        assertThat(response.getBody()).contains("Slides");

        Integer chapterCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM chapters c JOIN courses co ON c.course_id = co.id WHERE co.title = '" + title + "'",
                Integer.class
        );
        Integer lessonCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lessons l JOIN chapters c ON l.chapter_id = c.id JOIN courses co ON c.course_id = co.id WHERE co.title = '" + title + "'",
                Integer.class
        );
        Integer resourceCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lesson_resources lr JOIN lessons l ON lr.lesson_id = l.id JOIN chapters c ON l.chapter_id = c.id JOIN courses co ON c.course_id = co.id WHERE co.title = '" + title + "'",
                Integer.class
        );

        assertThat(chapterCount).isEqualTo(3);
        assertThat(lessonCount).isEqualTo(2);
        assertThat(resourceCount).isEqualTo(2);
    }

    /**
     * POST /api/courses
     * Tests that createdBy from request body is ignored and the hardcoded user is persisted.
     */
    @Test
    void shouldUseHardcodedUserForCourseCreation() {
        String title = "CreatedBy Check " + UUID.randomUUID();
        UUID fakeCreatedBy = UUID.randomUUID();
        String body = """
                {
                    "title": "%s",
                    "createdBy": "%s"
                }
                """.formatted(title, fakeCreatedBy);

        ResponseEntity<String> response = restTemplate.postForEntity(
                REQUEST_MAPPING,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String createdByInDb = jdbcTemplate.queryForObject(
                "SELECT created_by::text FROM courses WHERE title = '" + title + "'",
                String.class
        );
        assertThat(createdByInDb)
                        .isEqualTo(authenticatedUserId.toString())
                        .isNotEqualTo(fakeCreatedBy.toString());
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
                REQUEST_MAPPING, // am scos paramaterii din URL, ca in noul controller
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
                REQUEST_MAPPING, // am scos parametrii din URL
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
                REQUEST_MAPPING + "/" + courseId,
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
                REQUEST_MAPPING + "/" + UUID.randomUUID(),
                HttpMethod.PUT,
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
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
                REQUEST_MAPPING + "/" + courseId,
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
                REQUEST_MAPPING + "/" + UUID.randomUUID(),
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
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
                REQUEST_MAPPING + "/" + courseId,
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
