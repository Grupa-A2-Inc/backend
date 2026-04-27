package org.elearning.backend.enrollment;

import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.jwt.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProgressControllerTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    private UUID studentId;
    private UUID courseId;
    private UUID chapterId;
    private UUID enrollmentId;

    private static final String REQUEST_MAPPING = "/api/v1/courses";

    @BeforeEach
    void setUp() {
        studentId = insertUser(RoleName.STUDENT);
        authorizeRequests(studentId, RoleName.STUDENT);

        courseId = UUID.randomUUID();
        jdbcTemplate.execute(
                "INSERT INTO courses (id, title, created_by, status, visibility) " +
                        "VALUES ('" + courseId + "', 'Curs pentru Progres', '" + UUID.randomUUID() + "', 'PUBLISHED', 'PUBLIC')"
        );

        chapterId = UUID.randomUUID();
        jdbcTemplate.execute(
                "INSERT INTO chapters (id, course_id, title, order_index) " +
                        "VALUES ('" + chapterId + "', '" + courseId + "', 'Capitolul 1', 1)"
        );

        enrollmentId = insertEnrollment(studentId, courseId);
    }

    @AfterEach
    void tearDown() {
        restTemplate.getRestTemplate().setInterceptors(List.of());

        jdbcTemplate.execute("DELETE FROM lesson_progress");
        jdbcTemplate.execute("DELETE FROM course_enrollments");
        jdbcTemplate.execute("DELETE FROM lessons");
        jdbcTemplate.execute("DELETE FROM chapters");
        jdbcTemplate.execute("DELETE FROM courses");
        jdbcTemplate.execute("DELETE FROM users");
    }

    /**
     * Helper method for inserting a user with a specific role and returning its ID.
     */
    private UUID insertUser(RoleName role) {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, role_type, status) " +
                        "VALUES (?, ?, ?, ?, ?, (SELECT id FROM roles WHERE name = CAST(? AS role_name)), ?, CAST(? AS user_status))",
                userId,
                "user-progress-" + userId + "@test.com",
                "password-hash",
                "Test",
                "User",
                role.name(),
                roleTypeFor(role),
                "ACTIVE"
        );
        return userId;
    }

    /**
      * Helper to be able to choose the role type to insert according to the role of the user
     **/
    private String roleTypeFor(RoleName role) {
        return switch (role) {
            case STUDENT -> "STUDENT";
            case PARENT -> "PARENT";
            default -> "User";
        };
    }

    /**
     * Helper method for generating a JWT token for a user and setting it in the RestTemplate's interceptors for authenticated requests.
     */
    private void authorizeRequests(UUID userId, RoleName role) {
        String token = jwtUtil.generateAccessToken(userId, role);
        restTemplate.getRestTemplate().setInterceptors(List.of((request, body, execution) -> {
            request.getHeaders().setBearerAuth(token);
            return execution.execute(request, body);
        }));
    }

    /**
     * Helper method for inserting a course enrollment and returning its ID.
     */
    private UUID insertEnrollment(UUID studentId, UUID courseId) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO course_enrollments (id, student_id, course_id, enrolled_at) VALUES (?, ?, ?, NOW())",
                id, studentId, courseId
        );
        return id;
    }

    /**
     * Helper method for inserting a lesson into the database and returning its ID.
     */
    private UUID insertLesson(String title, int orderIndex) {
        UUID lessonID = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO lessons (id, chapter_id, title, order_index) VALUES (?, ?, ?, ?)",
                lessonID, chapterId, title, orderIndex
        );
        return lessonID;
    }

    /**
     * Helper method for inserting a lesson progress record, simulating that a student has visited a lesson.
     */
    private void insertLessonProgress(UUID lessonId) {
        jdbcTemplate.update(
                "INSERT INTO lesson_progress (id, lesson_id, student_id, enrollment_id, visited_at) VALUES (?, ?, ?, ?, NOW())",
                UUID.randomUUID(), lessonId, studentId, enrollmentId
        );
    }

    // =========================================================================
    // TESTE PENTRU GET /api/v1/courses/{courseId}/my-progress
    // =========================================================================

    /**
     * GET /api/v1/courses/{courseId}/my-progress
     * Checks that the endpoint returns the correct progress information for a student, including total lessons, visited lessons, progress percentage, and the list of lessons with their visited status.
     */
    @Test
    void shouldGetMyCourseProgressWithCorrectCalculations() {
        UUID lesson1 = insertLesson("Lectia 1", 1);
        insertLesson("Lectia 2", 2);

        insertLessonProgress(lesson1);

        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + "/" + courseId + "/my-progress",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        String body = response.getBody();
        assertThat(body)
                .isNotNull()
                .contains("\"totalLessons\":2")
                .contains("\"visitedLessons\":1")
                .contains("\"progressPercent\":50.0")
                .contains("Lectia 1")
                .contains("\"visited\":true")
                .contains("Lectia 2")
                .contains("\"visited\":false");
    }

    /**
     * GET /api/v1/courses/{courseId}/my-progress
     * Checks that if a student tries to access the progress of a course they are not enrolled in, the service returns 404 Not Found.
     */
    @Test
    void shouldReturnNotFoundWhenNotEnrolled() {
        UUID wrongCourseId = UUID.randomUUID();

        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + "/" + wrongCourseId + "/my-progress",
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * GET /api/v1/courses/{courseId}/my-progress
     * Checks that if a user with a non STUDENT role tries to access the endpoint, the service returns 403 Forbidden, since only STUDENT role is allowed.
     */
    @Test
    void shouldReturnForbiddenWhenUserIsTeacher() {
        UUID teacherId = insertUser(RoleName.TEACHER);
        authorizeRequests(teacherId, RoleName.TEACHER);

        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + "/" + courseId + "/my-progress",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // =========================================================================
    // TESTE PENTRU DEV 4: PROGRES PROFESOR SI PARINTE
    // =========================================================================

    /**
     * GET /api/v1/courses/{courseId}/students-progress
     * Test: The professor sees the progress of the course he created. (200 OK)
     */
    @Test
    void shouldGetCourseProgressForProfessorOwner() {
        UUID teacherId = insertUser(RoleName.TEACHER);
        authorizeRequests(teacherId, RoleName.TEACHER);

        UUID myCourseId = UUID.randomUUID();
        jdbcTemplate.execute(
                "INSERT INTO courses (id, title, created_by, status, visibility) " +
                        "VALUES ('" + myCourseId + "', 'Cursul Meu', '" + teacherId + "', 'PUBLISHED', 'PUBLIC')"
        );

        insertEnrollment(studentId, myCourseId);

        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + "/" + myCourseId + "/students-progress",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains(studentId.toString());
    }

    /**
     * GET /api/v1/courses/{courseId}/students-progress
     * Test: The professor tries to see the progress of a course owned by another professor -> 403 Forbidden.
     */
    @Test
    void shouldReturnForbiddenWhenTeacherNotOwner() {
        UUID hackerTeacherId = insertUser(RoleName.TEACHER);
        authorizeRequests(hackerTeacherId, RoleName.TEACHER);

        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + "/" + courseId + "/students-progress",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * GET /api/v1/students/{studentId}/courses-progress
     * Test: The parent sees the student's progress. (200 OK)
     */
    @Test
    void shouldGetStudentProgressForParent() {
        UUID parentId = insertUser(RoleName.PARENT);
        authorizeRequests(parentId, RoleName.PARENT);

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/students/" + studentId + "/courses-progress",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains(courseId.toString());
    }
}
