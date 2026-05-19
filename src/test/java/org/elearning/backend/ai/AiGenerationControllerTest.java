package org.elearning.backend.ai;

import org.elearning.backend.ai.dto.AiGenerateJobResponse;
import org.elearning.backend.ai.exception.AiApiException;
import org.elearning.backend.ai.exception.AiTimeoutException;
import org.elearning.backend.ai.service.AiApiClient;
import org.elearning.backend.auth.service.EmailService;
import org.elearning.backend.ai.model.AiRequestStatus;
import org.elearning.backend.role.entity.RoleName;
import org.elearning.backend.security.jwt.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiGenerationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private EmailService emailService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private JwtUtil jwtUtil;
    @MockitoBean
    private AiApiClient aiApiClient;

    private UUID teacherId;
    private UUID studentId;
    private String teacherToken;
    private String studentToken;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        insertUser(teacherId, RoleName.TEACHER);
        insertUser(studentId, RoleName.STUDENT);
        teacherToken = jwtUtil.generateAccessToken(teacherId, RoleName.TEACHER);
        studentToken = jwtUtil.generateAccessToken(studentId, RoleName.STUDENT);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM ai_question_requests");
        jdbcTemplate.execute("DELETE FROM lesson_progress");
        jdbcTemplate.execute("DELETE FROM course_enrollments");
        jdbcTemplate.execute("DELETE FROM lessons");
        jdbcTemplate.execute("DELETE FROM chapters");
        jdbcTemplate.execute("DELETE FROM courses");
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", teacherId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", studentId);
    }

    // =========================================================================
    // Helpers
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

    private record LessonContext(UUID courseId, UUID lessonId) {}

    private LessonContext insertLessonOwnedBy(UUID creatorId) {
        UUID courseId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO courses (id, title, created_by, status, visibility) " +
                        "VALUES (?, ?, ?, CAST(? AS course_status), CAST(? AS course_visibility))",
                courseId, "Course AI Test", creatorId, "PUBLISHED", "PUBLIC"
        );
        jdbcTemplate.update(
                "INSERT INTO chapters (id, course_id, title, order_index) VALUES (?, ?, ?, ?)",
                chapterId, courseId, "Chapter AI Test", 1
        );
        jdbcTemplate.update(
                "INSERT INTO lessons (id, chapter_id, title, order_index) VALUES (?, ?, ?, ?)",
                lessonId, chapterId, "Lesson AI Test", 1
        );

        return new LessonContext(courseId, lessonId);
    }

    private void enrollStudent(UUID courseId, UUID studentId) {
        jdbcTemplate.update(
                "INSERT INTO course_enrollments (course_id, student_id) VALUES (?, ?)",
                courseId, studentId
        );
    }

    private MockHttpServletRequestBuilder authorizedPost(String urlTemplate, Object... uriVars) {
        return post(urlTemplate, uriVars)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"count\": 5}")
                .with(csrf());
    }

    private MockHttpServletRequestBuilder authorizedPostAs(String token, String urlTemplate, Object... uriVars) {
        return post(urlTemplate, uriVars)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"count\": 5}")
                .with(csrf());
    }

    private AiGenerateJobResponse mockJobResponse(AiRequestStatus status) {
        AiGenerateJobResponse response = new AiGenerateJobResponse();
        response.setJobId("job-123");
        response.setStatus(status);
        return response;
    }

    // =========================================================================
    // POST /api/v1/lessons/{lessonId}/ai/generate-test
    // =========================================================================

    @Test
    void generateForLesson_shouldReturn202_whenTeacherOwnsLesson() throws Exception {
        LessonContext ctx = insertLessonOwnedBy(teacherId);
        when(aiApiClient.startGenerateJob(any(), anyInt())).thenReturn(mockJobResponse(AiRequestStatus.PENDING));

        mockMvc.perform(authorizedPost("/api/v1/lessons/{lessonId}/ai/generate-test", ctx.lessonId()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.lessonId").value(ctx.lessonId().toString()));
    }

    @Test
    void generateForLesson_shouldReturn202_whenStudentIsEnrolled() throws Exception {
        LessonContext ctx = insertLessonOwnedBy(teacherId);
        enrollStudent(ctx.courseId(), studentId);
        when(aiApiClient.startGenerateJob(any(), anyInt())).thenReturn(mockJobResponse(AiRequestStatus.RUNNING));

        mockMvc.perform(authorizedPostAs(studentToken, "/api/v1/lessons/{lessonId}/ai/generate-test", ctx.lessonId()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.lessonId").value(ctx.lessonId().toString()));
    }

    @Test
    void generateForLesson_shouldReturn403_whenTeacherDoesNotOwnLesson() throws Exception {
        UUID otherTeacherId = UUID.randomUUID();
        insertUser(otherTeacherId, RoleName.TEACHER);
        LessonContext ctx = insertLessonOwnedBy(otherTeacherId);

        mockMvc.perform(authorizedPost("/api/v1/lessons/{lessonId}/ai/generate-test", ctx.lessonId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Access denied")));

        jdbcTemplate.update("DELETE FROM users WHERE id = ?", otherTeacherId);
    }

    @Test
    void generateForLesson_shouldReturn403_whenStudentIsNotEnrolled() throws Exception {
        LessonContext ctx = insertLessonOwnedBy(teacherId);

        mockMvc.perform(authorizedPostAs(studentToken, "/api/v1/lessons/{lessonId}/ai/generate-test", ctx.lessonId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }

    @Test
    void generateForLesson_shouldReturn401_whenNotAuthenticated() throws Exception {
        LessonContext ctx = insertLessonOwnedBy(teacherId);

        mockMvc.perform(post("/api/v1/lessons/{lessonId}/ai/generate-test", ctx.lessonId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"count\": 5}")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void generateForLesson_shouldReturn403_whenLessonDoesNotExist() throws Exception {
        UUID nonExistentLessonId = UUID.randomUUID();

        mockMvc.perform(authorizedPost("/api/v1/lessons/{lessonId}/ai/generate-test", nonExistentLessonId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }

    @Test
    void generateForLesson_shouldPersistRequestInDatabase_whenTeacherOwnsLesson() throws Exception {
        LessonContext ctx = insertLessonOwnedBy(teacherId);
        when(aiApiClient.startGenerateJob(any(), anyInt())).thenReturn(mockJobResponse(AiRequestStatus.PENDING));

        mockMvc.perform(authorizedPost("/api/v1/lessons/{lessonId}/ai/generate-test", ctx.lessonId()))
                .andExpect(status().isAccepted());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_question_requests WHERE lesson_id = ?",
                Integer.class,
                ctx.lessonId()
        );
        org.junit.jupiter.api.Assertions.assertEquals(1, count);
    }

    @Test
    void generateForLesson_shouldReturn202WithCount_whenProvided() throws Exception {
        LessonContext ctx = insertLessonOwnedBy(teacherId);
        when(aiApiClient.startGenerateJob(any(), anyInt())).thenReturn(mockJobResponse(AiRequestStatus.PENDING));

        mockMvc.perform(post("/api/v1/lessons/{lessonId}/ai/generate-test", ctx.lessonId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"count\": 5}")
                        .with(csrf()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.lessonId").value(ctx.lessonId().toString()));
    }

    @Test
    @Disabled("async - status se seteaza dupa response")
    void generateForLesson_shouldReturn202AndStatusSuccess_whenAiReturnsQuestions() throws Exception {
        LessonContext ctx = insertLessonOwnedBy(teacherId);
        when(aiApiClient.startGenerateJob(any(), anyInt())).thenReturn(mockJobResponse(AiRequestStatus.DONE));

        mockMvc.perform(authorizedPost("/api/v1/lessons/{lessonId}/ai/generate-test", ctx.lessonId()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requestId").exists());

        String savedStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM ai_question_requests WHERE lesson_id = ?",
                String.class,
                ctx.lessonId()
        );
        org.junit.jupiter.api.Assertions.assertEquals("DONE", savedStatus);
    }

    @Test
    @Disabled("async - status se seteaza dupa response")
    void generateForLesson_shouldReturn202AndStatusFallback_whenAiTimesOut() throws Exception {
        LessonContext ctx = insertLessonOwnedBy(teacherId);
        when(aiApiClient.startGenerateJob(any(), anyInt())).thenThrow(new AiTimeoutException("Timeout"));

        mockMvc.perform(authorizedPost("/api/v1/lessons/{lessonId}/ai/generate-test", ctx.lessonId()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requestId").exists());

        String savedStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM ai_question_requests WHERE lesson_id = ?",
                String.class,
                ctx.lessonId()
        );
        org.junit.jupiter.api.Assertions.assertEquals("FAILED", savedStatus);
    }

    @Test
    @Disabled("async - status se seteaza dupa response")
    void generateForLesson_shouldReturn202AndStatusFailed_whenAiApiThrows() throws Exception {
        LessonContext ctx = insertLessonOwnedBy(teacherId);
        when(aiApiClient.startGenerateJob(any(), anyInt())).thenThrow(new AiApiException("AI error"));

        mockMvc.perform(authorizedPost("/api/v1/lessons/{lessonId}/ai/generate-test", ctx.lessonId()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requestId").exists());

        String savedStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM ai_question_requests WHERE lesson_id = ?",
                String.class,
                ctx.lessonId()
        );
        org.junit.jupiter.api.Assertions.assertEquals("FAILED", savedStatus);
    }
}
