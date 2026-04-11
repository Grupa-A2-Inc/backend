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
        // 1. Creăm un student și setăm token-ul pe RestTemplate
        studentId = insertUser(RoleName.STUDENT);
        authorizeRequests(studentId, RoleName.STUDENT);

        // 2. Creăm cursul și un capitol
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

        // 3. Înrolăm studentul la curs
        enrollmentId = insertEnrollment(studentId, courseId);
    }

    @AfterEach
    void tearDown() {
        restTemplate.getRestTemplate().setInterceptors(List.of());

        // Ordinea de ștergere este importantă pentru a nu încălca Foreign Keys
        jdbcTemplate.execute("DELETE FROM lesson_progress");
        jdbcTemplate.execute("DELETE FROM course_enrollments");
        jdbcTemplate.execute("DELETE FROM lessons");
        jdbcTemplate.execute("DELETE FROM chapters");
        jdbcTemplate.execute("DELETE FROM courses");
        jdbcTemplate.execute("DELETE FROM users");
    }

    /**
     * Helper method pentru a insera un user cu un rol specific.
     */
    private UUID insertUser(RoleName role) {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, status) " +
                        "VALUES (?, ?, ?, ?, ?, (SELECT id FROM roles WHERE name = CAST(? AS role_name)), 'ACTIVE')",
                userId,
                "user-progress-" + userId + "@test.com",
                "password-hash",
                "Test",
                "User",
                role.name()
        );
        return userId;
    }

    /**
     * Helper method pentru a seta token-ul JWT.
     */
    private void authorizeRequests(UUID userId, RoleName role) {
        String token = jwtUtil.generateAccessToken(userId, role);
        restTemplate.getRestTemplate().setInterceptors(List.of((request, body, execution) -> {
            request.getHeaders().setBearerAuth(token);
            return execution.execute(request, body);
        }));
    }

    /**
     * Helper method pentru a insera o înrolare.
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
     * Helper method pentru a insera o lecție.
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
     * Helper method pentru a marca o lecție ca vizitată.
     */
    private void insertLessonProgress(UUID lessonId) {
        jdbcTemplate.update(
                // Am modificat "created_at" in "visited_at"
                "INSERT INTO lesson_progress (id, lesson_id, student_id, enrollment_id, visited_at) VALUES (?, ?, ?, ?, NOW())",
                UUID.randomUUID(), lessonId, studentId, enrollmentId
        );
    }

    // =========================================================================
    // TESTE PENTRU GET /api/v1/courses/{courseId}/my-progress
    // =========================================================================

    /**
     * GET /api/v1/courses/{courseId}/my-progress
     * Verifică un "Happy Path" complet: curs cu 2 lecții, 1 completată -> 50% progres.
     */
    @Test
    void shouldGetMyCourseProgressWithCorrectCalculations() {
        // Creăm 2 lecții
        UUID lesson1 = insertLesson("Lectia 1", 1);
        insertLesson("Lectia 2", 2);

        // Marcăm doar prima lecție ca vizitată
        insertLessonProgress(lesson1);

        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + "/" + courseId + "/my-progress",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verificăm conținutul JSON al DTO-ului returnat
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
     * Verifică comportamentul când cursul nu există sau studentul nu este înrolat (ar trebui să dea 404).
     */
    @Test
    void shouldReturnNotFoundWhenNotEnrolled() {
        // Generăm un courseId aleatoriu la care studentul nu este înrolat
        UUID wrongCourseId = UUID.randomUUID();

        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + "/" + wrongCourseId + "/my-progress",
                String.class
        );

        // Ne așteptăm ca serviciul să arunce CourseNotFoundException, interceptat cu 404
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * GET /api/v1/courses/{courseId}/my-progress
     * Verifică că un profesor primește 403 Forbidden datorită @PreAuthorize("hasRole('STUDENT')").
     */
    @Test
    void shouldReturnForbiddenWhenUserIsTeacher() {
        // Înlocuim token-ul curent (de student) cu unul de profesor
        UUID teacherId = insertUser(RoleName.TEACHER);
        authorizeRequests(teacherId, RoleName.TEACHER);

        ResponseEntity<String> response = restTemplate.getForEntity(
                REQUEST_MAPPING + "/" + courseId + "/my-progress",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
