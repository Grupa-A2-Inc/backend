package org.elearning.backend.feedback;

import org.elearning.backend.auth.service.EmailService;
import org.elearning.backend.feedback.dto.LessonRatingSummaryDto;
import org.elearning.backend.feedback.service.LessonRatingService;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.jwt.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.stream.Stream;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LessonRatingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtUtil jwtUtil;

        @Autowired
        private LessonRatingService lessonRatingService;

    @MockitoBean
    private EmailService emailService;

    private UUID teacherId;
    private UUID studentId1;
    private UUID studentId2;
    private String teacherToken;
    private String studentToken1;
    private String studentToken2;

    private UUID courseId;
    private UUID chapterId;
    private UUID lessonId;

    @BeforeEach
    void setUp() {
        // Create users
        teacherId = UUID.randomUUID();
        studentId1 = UUID.randomUUID();
        studentId2 = UUID.randomUUID();

        insertUser(teacherId, RoleName.TEACHER);
        insertUser(studentId1, RoleName.STUDENT);
        insertUser(studentId2, RoleName.STUDENT);

        // Generate tokens
        teacherToken = jwtUtil.generateAccessToken(teacherId, RoleName.TEACHER);
        studentToken1 = jwtUtil.generateAccessToken(studentId1, RoleName.STUDENT);
        studentToken2 = jwtUtil.generateAccessToken(studentId2, RoleName.STUDENT);

        // Create course, chapter, lesson
        courseId = UUID.randomUUID();
        chapterId = UUID.randomUUID();
        lessonId = UUID.randomUUID();

        insertCourse(courseId, teacherId);
        insertChapter(chapterId, courseId);
        insertLesson(lessonId, chapterId);

        // Enroll student1 in course
        enrollStudent(courseId, studentId1);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM lesson_ratings");
        jdbcTemplate.execute("DELETE FROM lesson_progress");
        jdbcTemplate.execute("DELETE FROM course_enrollments");
        jdbcTemplate.execute("DELETE FROM lessons");
        jdbcTemplate.execute("DELETE FROM chapters");
        jdbcTemplate.execute("DELETE FROM courses");
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", teacherId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", studentId1);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", studentId2);
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

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
                "INSERT INTO courses (id, title, created_by, status, visibility) " +
                        "VALUES (?, ?, ?, CAST(? AS course_status), CAST(? AS course_visibility))",
                courseId, "Test Course for Ratings", creatorId, "PUBLISHED", "PUBLIC"
        );
    }

    private void insertChapter(UUID chapterId, UUID courseId) {
        jdbcTemplate.update(
                "INSERT INTO chapters (id, course_id, title, order_index) VALUES (?, ?, ?, ?)",
                chapterId, courseId, "Test Chapter", 1
        );
    }

    private void insertLesson(UUID lessonId, UUID chapterId) {
        jdbcTemplate.update(
                "INSERT INTO lessons (id, chapter_id, title, order_index) VALUES (?, ?, ?, ?)",
                lessonId, chapterId, "Test Lesson", 1
        );
    }

    private void enrollStudent(UUID courseId, UUID studentId) {
        jdbcTemplate.update(
                "INSERT INTO course_enrollments (course_id, student_id) VALUES (?, ?)",
                courseId, studentId
        );
    }

    private MockHttpServletRequestBuilder authorizedPostWithBody(String token, String urlTemplate, String body, Object... uriVars) {
        return post(urlTemplate, uriVars)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(csrf());
    }

    private MockHttpServletRequestBuilder authorizedGet(String token, String urlTemplate, Object... uriVars) {
        return get(urlTemplate, uriVars)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .with(csrf());
    }

    // =========================================================================
    // POST /api/v1/lessons/{lessonId}/ratings
    // =========================================================================

    @Test
    void rateLesson_shouldReturn200_whenStudentIsEnrolledAndRatingValid() throws Exception {
        String body = "{\"rating\": 4, \"comment\": \"Great lesson!\"}";

        mockMvc.perform(authorizedPostWithBody(studentToken1, "/api/v1/lessons/{lessonId}/ratings", body, lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonId").value(lessonId.toString()))
                .andExpect(jsonPath("$.lessonTitle").value("Test Lesson"))
                .andExpect(jsonPath("$.myRating").value(4))
                .andExpect(jsonPath("$.myComment").value("Great lesson!"))
                .andExpect(jsonPath("$.totalRatings").value(1))
                .andExpect(jsonPath("$.avgRating").value(4.0));
    }

    @Test
    void rateLesson_shouldReturn200AndUpdateRating_whenStudentRatesAgain() throws Exception {
        String firstBody = "{\"rating\": 2, \"comment\": \"Not great\"}";
        mockMvc.perform(authorizedPostWithBody(studentToken1, "/api/v1/lessons/{lessonId}/ratings", firstBody, lessonId))
                .andExpect(status().isOk());

        String secondBody = "{\"rating\": 5, \"comment\": \"Actually, excellent!\"}";
        mockMvc.perform(authorizedPostWithBody(studentToken1, "/api/v1/lessons/{lessonId}/ratings", secondBody, lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRating").value(5))
                .andExpect(jsonPath("$.myComment").value("Actually, excellent!"))
                .andExpect(jsonPath("$.totalRatings").value(1))
                .andExpect(jsonPath("$.avgRating").value(5.0));
    }

    @Test
    void rateLesson_shouldReturn200WithoutComment_whenOnlyRatingProvided() throws Exception {
        String body = "{\"rating\": 3}";

        mockMvc.perform(authorizedPostWithBody(studentToken1, "/api/v1/lessons/{lessonId}/ratings", body, lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRating").value(3))
                .andExpect(jsonPath("$.myComment").value(nullValue()))
                .andExpect(jsonPath("$.totalRatings").value(1));
    }

    @Test
    void rateLesson_shouldReturn403_whenStudentNotEnrolled() throws Exception {
        String body = "{\"rating\": 4, \"comment\": \"Good\"}";

        mockMvc.perform(authorizedPostWithBody(studentToken2, "/api/v1/lessons/{lessonId}/ratings", body, lessonId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("not enrolled")));
    }

    @Test
    void rateLesson_shouldReturn404_whenLessonDoesNotExist() throws Exception {
        UUID nonExistentLessonId = UUID.randomUUID();
        String body = "{\"rating\": 4, \"comment\": \"Good\"}";

        mockMvc.perform(authorizedPostWithBody(studentToken1, "/api/v1/lessons/{lessonId}/ratings", body, nonExistentLessonId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

        @ParameterizedTest
        @MethodSource("invalidRatingPayloads")
        void rateLesson_shouldReturn400_forInvalidRatingPayloads(String body) throws Exception {
                mockMvc.perform(authorizedPostWithBody(studentToken1, "/api/v1/lessons/{lessonId}/ratings", body, lessonId))
                                .andExpect(status().isBadRequest());
        }

        private static Stream<Arguments> invalidRatingPayloads() {
                return Stream.of(
                                Arguments.of("{\"rating\": 0, \"comment\": \"Bad\"}"),
                                Arguments.of("{\"rating\": 6, \"comment\": \"Excellent\"}"),
                                Arguments.of("{\"comment\": \"No rating\"}")
                );
        }

    @Test
    void rateLesson_shouldReturn400_whenCommentExceedsMaxLength() throws Exception {
        String longComment = "a".repeat(1001);
        String body = "{\"rating\": 4, \"comment\": \"" + longComment + "\"}";

        mockMvc.perform(authorizedPostWithBody(studentToken1, "/api/v1/lessons/{lessonId}/ratings", body, lessonId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rateLesson_shouldReturn401_whenNotAuthenticated() throws Exception {
        String body = "{\"rating\": 4, \"comment\": \"Good\"}";

        mockMvc.perform(post("/api/v1/lessons/{lessonId}/ratings", lessonId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // GET /api/v1/lessons/{lessonId}/ratings/summary
    // =========================================================================

    @Test
    void getRatingSummary_shouldReturn200_whenTeacherOwnsLesson() throws Exception {
        // Add some ratings first
        String body1 = "{\"rating\": 4, \"comment\": \"Good\"}";
        mockMvc.perform(authorizedPostWithBody(studentToken1, "/api/v1/lessons/{lessonId}/ratings", body1, lessonId))
                .andExpect(status().isOk());

        mockMvc.perform(authorizedGet(teacherToken, "/api/v1/lessons/{lessonId}/ratings/summary", lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonId").value(lessonId.toString()))
                .andExpect(jsonPath("$.lessonTitle").value("Test Lesson"))
                .andExpect(jsonPath("$.avgRating").value(4.0))
                .andExpect(jsonPath("$.totalRatings").value(1))
                .andExpect(jsonPath("$.belowThreshold").value(false))
                .andExpect(jsonPath("$.distribution").exists())
                .andExpect(jsonPath("$.distribution['4']").value(1))
                .andExpect(jsonPath("$.recentComments").isArray());
    }

    @Test
    void getRatingSummary_shouldIncludeDistribution_whenTeacherOwnsLesson() throws Exception {
        // Add multiple ratings with different values
        mockMvc.perform(authorizedPostWithBody(studentToken1, "/api/v1/lessons/{lessonId}/ratings", "{\"rating\": 5, \"comment\": \"Excellent\"}", lessonId))
                .andExpect(status().isOk());

        enrollStudent(courseId, studentId2);
        mockMvc.perform(authorizedPostWithBody(studentToken2, "/api/v1/lessons/{lessonId}/ratings", "{\"rating\": 3, \"comment\": \"OK\"}", lessonId))
                .andExpect(status().isOk());

        mockMvc.perform(authorizedGet(teacherToken, "/api/v1/lessons/{lessonId}/ratings/summary", lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distribution['3']").value(1))
                .andExpect(jsonPath("$.distribution['5']").value(1))
                .andExpect(jsonPath("$.avgRating").value(4.0));
    }

    @Test
    void getRatingSummary_shouldShowBelowThreshold_whenAverageRatingLessThan3() throws Exception {
        mockMvc.perform(authorizedPostWithBody(studentToken1, "/api/v1/lessons/{lessonId}/ratings", "{\"rating\": 2, \"comment\": \"Poor\"}", lessonId))
                .andExpect(status().isOk());

        mockMvc.perform(authorizedGet(teacherToken, "/api/v1/lessons/{lessonId}/ratings/summary", lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.belowThreshold").value(true))
                .andExpect(jsonPath("$.avgRating").value(2.0));
    }

    @Test
    void getRatingSummary_shouldShowBelowThresholdFalse_whenTeacherHasNoRatings() throws Exception {
        mockMvc.perform(authorizedGet(teacherToken, "/api/v1/lessons/{lessonId}/ratings/summary", lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonId").value(lessonId.toString()))
                .andExpect(jsonPath("$.lessonTitle").value("Test Lesson"))
                .andExpect(jsonPath("$.avgRating").value(0.0))
                .andExpect(jsonPath("$.totalRatings").value(0))
                .andExpect(jsonPath("$.belowThreshold").value(false))
                .andExpect(jsonPath("$.distribution").exists())
                .andExpect(jsonPath("$.distribution['1']").value(0))
                .andExpect(jsonPath("$.distribution['5']").value(0))
                .andExpect(jsonPath("$.recentComments").isArray())
                .andExpect(jsonPath("$.recentComments.length()").value(0));
    }

    @Test
    void getRatingSummary_shouldIncludeRecentComments_whenTeacherOwnsLesson() throws Exception {
        mockMvc.perform(authorizedPostWithBody(studentToken1, "/api/v1/lessons/{lessonId}/ratings", "{\"rating\": 5, \"comment\": \"Great lesson!\"}", lessonId))
                .andExpect(status().isOk());

        mockMvc.perform(authorizedGet(teacherToken, "/api/v1/lessons/{lessonId}/ratings/summary", lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentComments").isArray())
                .andExpect(jsonPath("$.recentComments.length()").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.recentComments[0].rating").value(5))
                .andExpect(jsonPath("$.recentComments[0].comment").value("Great lesson!"));
    }

    @Test
    void getRatingSummary_shouldReturn403_whenTeacherDoesNotOwnLesson() throws Exception {
        UUID otherTeacherId = UUID.randomUUID();
        insertUser(otherTeacherId, RoleName.TEACHER);
        UUID otherCourseId = UUID.randomUUID();
        UUID otherChapterId = UUID.randomUUID();
        UUID otherLessonId = UUID.randomUUID();

        insertCourse(otherCourseId, otherTeacherId);
        insertChapter(otherChapterId, otherCourseId);
        insertLesson(otherLessonId, otherChapterId);

        String otherTeacherToken = jwtUtil.generateAccessToken(otherTeacherId, RoleName.TEACHER);

        mockMvc.perform(authorizedGet(otherTeacherToken, "/api/v1/lessons/{lessonId}/ratings/summary", lessonId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("does not own")));

        // Cleanup
        jdbcTemplate.update("DELETE FROM lessons WHERE id = ?", otherLessonId);
        jdbcTemplate.update("DELETE FROM chapters WHERE id = ?", otherChapterId);
        jdbcTemplate.update("DELETE FROM courses WHERE id = ?", otherCourseId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", otherTeacherId);
    }

    @Test
    void getRatingSummary_shouldReturn200_whenStudentIsEnrolledAndViewsOwnRating() throws Exception {
        mockMvc.perform(authorizedPostWithBody(studentToken1, "/api/v1/lessons/{lessonId}/ratings", "{\"rating\": 3, \"comment\": \"Decent\"}", lessonId))
                .andExpect(status().isOk());

        mockMvc.perform(authorizedGet(studentToken1, "/api/v1/lessons/{lessonId}/ratings/summary", lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonId").value(lessonId.toString()))
                .andExpect(jsonPath("$.avgRating").value(3.0))
                .andExpect(jsonPath("$.totalRatings").value(1))
                .andExpect(jsonPath("$.myRating").value(3))
                .andExpect(jsonPath("$.myComment").value("Decent"))
                .andExpect(jsonPath("$.belowThreshold").doesNotExist())
                .andExpect(jsonPath("$.distribution").doesNotExist());
    }

    @Test
    void getRatingSummary_shouldReturn200WithoutOwnRating_whenStudentHasNotRated() throws Exception {
        mockMvc.perform(authorizedGet(studentToken1, "/api/v1/lessons/{lessonId}/ratings/summary", lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonId").value(lessonId.toString()))
                .andExpect(jsonPath("$.avgRating").value(0.0))
                .andExpect(jsonPath("$.totalRatings").value(0))
                .andExpect(jsonPath("$.myRating").doesNotExist())
                .andExpect(jsonPath("$.myComment").doesNotExist());
    }

        @Test
        void getRatingSummary_shouldReturnCommonFields_only_whenRoleIsUnsupported() {
                UUID parentId = UUID.randomUUID();
                insertUser(parentId, RoleName.PARENT);

                LessonRatingSummaryDto summary = lessonRatingService.getLessonSummary(lessonId, parentId, RoleName.PARENT);

                org.junit.jupiter.api.Assertions.assertEquals(lessonId, summary.getLessonId());
                org.junit.jupiter.api.Assertions.assertEquals("Test Lesson", summary.getLessonTitle());
                org.junit.jupiter.api.Assertions.assertEquals(0.0, summary.getAvgRating());
                org.junit.jupiter.api.Assertions.assertEquals(0, summary.getTotalRatings());
                org.junit.jupiter.api.Assertions.assertNull(summary.getBelowThreshold());
                org.junit.jupiter.api.Assertions.assertNull(summary.getDistribution());
                org.junit.jupiter.api.Assertions.assertNull(summary.getRecentComments());
                org.junit.jupiter.api.Assertions.assertNull(summary.getMyRating());
                org.junit.jupiter.api.Assertions.assertNull(summary.getMyComment());

                jdbcTemplate.update("DELETE FROM users WHERE id = ?", parentId);
        }

    @Test
    void getRatingSummary_shouldReturn403_whenStudentNotEnrolled() throws Exception {
        mockMvc.perform(authorizedGet(studentToken2, "/api/v1/lessons/{lessonId}/ratings/summary", lessonId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("not enrolled")));
    }

    @Test
    void getRatingSummary_shouldReturn404_whenLessonDoesNotExist() throws Exception {
        UUID nonExistentLessonId = UUID.randomUUID();

        mockMvc.perform(authorizedGet(studentToken1, "/api/v1/lessons/{lessonId}/ratings/summary", nonExistentLessonId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    void getRatingSummary_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/lessons/{lessonId}/ratings/summary", lessonId)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getRatingSummary_shouldHandleNoCommentsCorrectly_whenTeacherOwnsLesson() throws Exception {
        // Add rating without comment
        mockMvc.perform(authorizedPostWithBody(studentToken1, "/api/v1/lessons/{lessonId}/ratings", "{\"rating\": 5}", lessonId))
                .andExpect(status().isOk());

        mockMvc.perform(authorizedGet(teacherToken, "/api/v1/lessons/{lessonId}/ratings/summary", lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentComments").isArray())
                .andExpect(jsonPath("$.recentComments.length()").value(0));
    }
}
