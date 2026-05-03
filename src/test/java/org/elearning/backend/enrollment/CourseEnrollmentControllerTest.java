package org.elearning.backend.enrollment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elearning.backend.auth.service.EmailService;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.jwt.JwtUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CourseEnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;
    @MockitoBean
    private EmailService emailService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID studentId;
    private UUID courseId;
    private UUID coursePrivateId;
    private UUID courseDraftId;
    private String accessToken;

    private static final String REQUEST_MAPPING = "/api/v1";
    private static final UUID INSTRUCTOR_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        coursePrivateId = UUID.randomUUID();
        courseDraftId = UUID.randomUUID();

        insertStudent(studentId);
        accessToken = jwtUtil.generateAccessToken(studentId, RoleName.STUDENT);

        insertCourse(courseId, "Public Course", "Public Course Description",
                "Computer Science", "PUBLIC", "PUBLISHED", INSTRUCTOR_ID);

        insertCourse(coursePrivateId, "Private Course", "Private Course Description",
                "Computer Science", "PRIVATE", "PUBLISHED", INSTRUCTOR_ID);

        insertCourse(courseDraftId, "Draft Course", "Draft Course Description",
                "Computer Science", "PUBLIC", "DRAFT", INSTRUCTOR_ID);
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM course_enrollments WHERE student_id = ? OR course_id IN (?, ?, ?)",
                studentId, courseId, coursePrivateId, courseDraftId);
        jdbcTemplate.update("DELETE FROM courses WHERE id IN (?, ?, ?)", courseId, coursePrivateId, courseDraftId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", studentId);
    }

    private void insertStudent(UUID userId) {
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, role_type, status) " +
                        "VALUES (?, ?, ?, ?, ?, (SELECT id FROM roles WHERE name = CAST(? AS role_name)), ?, CAST(? AS user_status))",
                userId,
                "student-" + userId + "@test.com",
                "password-hash",
                "Test",
                "Student",
                RoleName.STUDENT.name(),
                "STUDENT",
                "ACTIVE"
        );
    }

    private void insertCourse(UUID courseId, String title, String description, String category,
                              String visibility, String status, UUID createdBy) {
        jdbcTemplate.update(
                "INSERT INTO courses (id, title, description, category, visibility, status, created_by, created_at) " +
                        "VALUES (?, ?, ?, ?, CAST(? AS course_visibility), CAST(? AS course_status), ?, CURRENT_TIMESTAMP)",
                courseId, title, description, category, visibility, status, createdBy
        );
    }

    private MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder requestBuilder) {
        return requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    }

    // ================================================================
    //  1. ENROLL IN COURSE
    // ================================================================

    @Test
    @Order(1)
    @DisplayName("1.1 — POST /api/v1/courses/{courseId}/enroll → 201 Created")
    void enrollInCourse_Success_Returns201Created() throws Exception {
        mockMvc.perform(authorized(post(REQUEST_MAPPING + "/courses/{courseId}/enroll", courseId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enrollmentId").exists())
                .andExpect(jsonPath("$.courseId", is(courseId.toString())))
                .andExpect(jsonPath("$.studentId", is(studentId.toString())))
                .andExpect(jsonPath("$.progressPercent", is(0)))
                .andReturn();

        Long enrollmentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM course_enrollments WHERE course_id = ? AND student_id = ?",
                Long.class, courseId, studentId
        );
        assert enrollmentCount != null && enrollmentCount == 1 : "Enrollment not persisted to database";
    }

    @Test
    @Order(2)
    @DisplayName("1.2 — POST /api/v1/courses/{courseId}/enroll with non-existent course → 404 NotFound")
    void enrollInCourse_CourseNotFound_Returns404() throws Exception {
        UUID nonExistentCourseId = UUID.randomUUID();

        mockMvc.perform(authorized(post(REQUEST_MAPPING + "/courses/{courseId}/enroll", nonExistentCourseId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(3)
    @DisplayName("1.3 — POST /api/v1/courses/{courseId}/enroll with PRIVATE course → 403 Forbidden")
    void enrollInCourse_CoursePrivate_Returns403Forbidden() throws Exception {
        mockMvc.perform(authorized(post(REQUEST_MAPPING + "/courses/{courseId}/enroll", coursePrivateId)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(4)
    @DisplayName("1.4 — POST /api/v1/courses/{courseId}/enroll with DRAFT course → 404 NotFound")
    void enrollInCourse_CourseNotPublished_Returns404() throws Exception {
        mockMvc.perform(authorized(post(REQUEST_MAPPING + "/courses/{courseId}/enroll", courseDraftId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(5)
    @DisplayName("1.5 — POST /api/v1/courses/{courseId}/enroll — student already enrolled → 409 Conflict")
    void enrollInCourse_StudentAlreadyEnrolled_Returns409Conflict() throws Exception {
        mockMvc.perform(authorized(post(REQUEST_MAPPING + "/courses/{courseId}/enroll", courseId)))
                .andExpect(status().isCreated());

        mockMvc.perform(authorized(post(REQUEST_MAPPING + "/courses/{courseId}/enroll", courseId)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(6)
    @DisplayName("1.6 — POST /api/v1/courses/{courseId}/enroll without auth → 401 Unauthorized")
    void enrollInCourse_Unauthorized_Returns401() throws Exception {
        mockMvc.perform(post(REQUEST_MAPPING + "/courses/{courseId}/enroll", courseId))
                .andExpect(status().isUnauthorized());
    }

    // ================================================================
    //  2. UNENROLL FROM COURSE
    // ================================================================

    @Test
    @Order(7)
    @DisplayName("2.1 — DELETE /api/v1/courses/{courseId}/unenroll → 204 NoContent")
    void unenrollFromCourse_Success_Returns204NoContent() throws Exception {
        mockMvc.perform(authorized(post(REQUEST_MAPPING + "/courses/{courseId}/enroll", courseId)))
                .andExpect(status().isCreated());

        mockMvc.perform(authorized(delete(REQUEST_MAPPING + "/courses/{courseId}/unenroll", courseId)))
                .andExpect(status().isNoContent());

        Long enrollmentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM course_enrollments WHERE course_id = ? AND student_id = ?",
                Long.class, courseId, studentId
        );
        assert enrollmentCount != null && enrollmentCount == 0 : "Enrollment was not deleted from database";
    }

    @Test
    @Order(8)
    @DisplayName("2.2 — DELETE /api/v1/courses/{courseId}/unenroll — enrollment not found → 404 NotFound")
    void unenrollFromCourse_EnrollmentNotFound_Returns404() throws Exception {
        mockMvc.perform(authorized(delete(REQUEST_MAPPING + "/courses/{courseId}/unenroll", courseId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(9)
    @DisplayName("2.3 — DELETE /api/v1/courses/{courseId}/unenroll without auth → 401 Unauthorized")
    void unenrollFromCourse_Unauthorized_Returns401() throws Exception {
        mockMvc.perform(delete(REQUEST_MAPPING + "/courses/{courseId}/unenroll", courseId))
                .andExpect(status().isUnauthorized());
    }

    // ================================================================
    //  3. GET ENROLLED COURSES
    // ================================================================

    @Test
    @Order(10)
    @DisplayName("3.1 — GET /api/v1/students/me/courses — student not enrolled in any → 200 EmptyPage")
    void getEnrolledCourses_NoEnrollments_Returns200EmptyPage() throws Exception {
        mockMvc.perform(authorized(get(REQUEST_MAPPING + "/students/me/courses")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    @Order(11)
    @DisplayName("3.2 — GET /api/v1/students/me/courses — student enrolled in 1 course → 200 WithPage")
    void getEnrolledCourses_SingleCourse_Returns200WithPage() throws Exception {
        mockMvc.perform(authorized(post(REQUEST_MAPPING + "/courses/{courseId}/enroll", courseId)))
                .andExpect(status().isCreated());

        mockMvc.perform(authorized(get(REQUEST_MAPPING + "/students/me/courses")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].courseId", is(courseId.toString())))
                .andExpect(jsonPath("$.content[0].courseTitle", is("Public Course")))
                .andExpect(jsonPath("$.content[0].courseCategory", is("Computer Science")))
                .andExpect(jsonPath("$.content[0].progressPercent", is(0.0)))
                .andExpect(jsonPath("$.content[0].enrolledAt").exists());
    }

    @Test
    @Order(12)
    @DisplayName("3.3 — GET /api/v1/students/me/courses — student enrolled in multiple courses → 200 WithPage")
    void getEnrolledCourses_MultipleCourses_Returns200WithPage() throws Exception {
        UUID courseId2 = UUID.randomUUID();
        insertCourse(courseId2, "Second Course", "Second Course Description",
                "Mathematics", "PUBLIC", "PUBLISHED", INSTRUCTOR_ID);

        try {
            mockMvc.perform(authorized(post(REQUEST_MAPPING + "/courses/{courseId}/enroll", courseId)))
                    .andExpect(status().isCreated());

            mockMvc.perform(authorized(post(REQUEST_MAPPING + "/courses/{courseId}/enroll", courseId2)))
                    .andExpect(status().isCreated());

            mockMvc.perform(authorized(get(REQUEST_MAPPING + "/students/me/courses")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements", is(2)))
                    .andExpect(jsonPath("$.content[*].courseId", hasItems(courseId.toString(), courseId2.toString())))
                    .andExpect(jsonPath("$.content[*].courseTitle", hasItems("Public Course", "Second Course")));
        } finally {
            jdbcTemplate.update("DELETE FROM course_enrollments WHERE course_id = ?", courseId2);
            jdbcTemplate.update("DELETE FROM courses WHERE id = ?", courseId2);
        }
    }

    @Test
    @Order(13)
    @DisplayName("3.4 — GET /api/v1/students/me/courses without auth → 401 Unauthorized")
    void getEnrolledCourses_Unauthorized_Returns401() throws Exception {
        mockMvc.perform(get(REQUEST_MAPPING + "/students/me/courses"))
                .andExpect(status().isUnauthorized());
    }
}