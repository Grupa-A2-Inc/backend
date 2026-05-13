package org.elearning.backend.feedback;

import org.elearning.backend.auth.service.EmailService;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.jwt.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfessorRatingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private EmailService emailService;

    private UUID teacherId;
    private UUID studentId;
    private String teacherToken;
    private String studentToken;

    private UUID courseId;
    private UUID chapterId;
    private UUID lessonId;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        studentId = UUID.randomUUID();

        insertUser(teacherId, RoleName.TEACHER);
        insertUser(studentId, RoleName.STUDENT);

        teacherToken = jwtUtil.generateAccessToken(teacherId, RoleName.TEACHER);
        studentToken = jwtUtil.generateAccessToken(studentId, RoleName.STUDENT);

        courseId = UUID.randomUUID();
        chapterId = UUID.randomUUID();
        lessonId = UUID.randomUUID();

        insertCourse(courseId, teacherId);
        insertChapter(chapterId, courseId);
        insertLesson(lessonId, chapterId);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM lesson_ratings");
        jdbcTemplate.execute("DELETE FROM lessons");
        jdbcTemplate.execute("DELETE FROM chapters");
        jdbcTemplate.execute("DELETE FROM courses");
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", teacherId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", studentId);
    }

    private void insertUser(UUID userId, RoleName role) {
        String roleType = role == RoleName.STUDENT ? "STUDENT"
            : role == RoleName.PARENT ? "PARENT"
            : "User";
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, role_type, status) " +
                        "VALUES (?, ?, ?, ?, ?, (SELECT id FROM roles WHERE name = CAST(? AS role_name)), ?, CAST(? AS user_status))",
                userId,
                role.name().toLowerCase() + "-" + userId + "@test.com",
                "password-hash",
                "Test",
                role.name(),
                role.name(),
                roleType,
                "ACTIVE"
        );
    }

    private void insertCourse(UUID courseId, UUID creatorId) {
        jdbcTemplate.update(
                "INSERT INTO courses (id, title, created_by, status, visibility) VALUES (?, ?, ?, CAST(? AS course_status), CAST(? AS course_visibility))",
                courseId,
                "Professor ratings course",
                creatorId,
                "PUBLISHED",
                "PUBLIC"
        );
    }

    private void insertChapter(UUID chapterId, UUID courseId) {
        jdbcTemplate.update(
                "INSERT INTO chapters (id, course_id, title, order_index) VALUES (?, ?, ?, ?)",
                chapterId,
                courseId,
                "Professor ratings chapter",
                1
        );
    }

    private void insertLesson(UUID lessonId, UUID chapterId) {
        jdbcTemplate.update(
                "INSERT INTO lessons (id, chapter_id, title, order_index) VALUES (?, ?, ?, ?)",
                lessonId,
                chapterId,
                "Professor ratings lesson",
                1
        );
    }

    private void insertLessonRating(UUID lessonId, UUID studentId, int rating, String comment) {
        jdbcTemplate.update(
                "INSERT INTO lesson_ratings (lesson_id, student_id, rating, comment, created_at, updated_at) VALUES (?, ?, ?, ?, NOW(), NOW())",
                lessonId,
                studentId,
                rating,
                comment
        );
    }

    private MockHttpServletRequestBuilder authorizedGet(String token) {
        return get("/api/v1/professors/me/lessons/ratings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .with(csrf());
    }

    @Test
    void getAverageRatingsForAllLessons_shouldReturn200_whenTeacherRequestsRatings() throws Exception {
        insertLessonRating(lessonId, studentId, 4, "Good lesson");

        mockMvc.perform(authorizedGet(teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lessonId").value(lessonId.toString()))
                .andExpect(jsonPath("$[0].title").value("Professor ratings lesson"))
                .andExpect(jsonPath("$[0].averageRating").value(4.0))
                .andExpect(jsonPath("$[0].totalRatings").value(1));
    }

    @Test
    void getAverageRatingsForAllLessons_shouldReturn200_withNullAverage_whenLessonHasNoRatings() throws Exception {
        mockMvc.perform(authorizedGet(teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lessonId").value(lessonId.toString()))
                .andExpect(jsonPath("$[0].title").value("Professor ratings lesson"))
                .andExpect(jsonPath("$[0].averageRating").value(nullValue()))
                .andExpect(jsonPath("$[0].totalRatings").value(0));
    }

    @Test
    void getAverageRatingsForAllLessons_shouldReturn403_whenStudentCallsEndpoint() throws Exception {
        mockMvc.perform(authorizedGet(studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }
}
