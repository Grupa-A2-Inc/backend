package org.elearning.backend.analytics;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AnalyticsAndStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;
    @MockitoBean
    private EmailService emailService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String BASE = "/api/v1";

    private UUID teacherId;
    private UUID studentId;
    private UUID courseId;
    private UUID testId;

    private String teacherToken;
    private String studentToken;

    private static final int TOTAL_QUESTIONS = 5;
    private static final int PASSING_SCORE   = 4;   // 80%
    private static final int FAILING_SCORE   = 2;   // 40%

    // =================================================================
    //  SETUP / TEARDOWN
    // =================================================================

    @BeforeEach
    void setUp() {
        teacherId    = insertUser(RoleName.TEACHER);
        studentId    = insertUser(RoleName.STUDENT);
        teacherToken = jwtUtil.generateAccessToken(teacherId,  RoleName.TEACHER);
        studentToken = jwtUtil.generateAccessToken(studentId, RoleName.STUDENT);

        courseId  = insertCourse(teacherId);
        UUID chapterId = insertChapter(courseId);
        UUID lessonId = insertLesson(chapterId);
        testId    = insertTest(lessonId, teacherId);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM test_results");
        jdbcTemplate.execute("DELETE FROM test_attempts");
        jdbcTemplate.execute("DELETE FROM course_enrollments");
        jdbcTemplate.execute("DELETE FROM question_options");
        jdbcTemplate.execute("DELETE FROM questions");
        jdbcTemplate.execute("DELETE FROM tests");
        jdbcTemplate.execute("DELETE FROM lessons");
        jdbcTemplate.execute("DELETE FROM chapters");
        jdbcTemplate.execute("DELETE FROM courses");
        jdbcTemplate.update("DELETE FROM users WHERE id IN (?, ?)", teacherId, studentId);
    }

    // =================================================================
    //  HELPERS — request builders
    // =================================================================

    private MockHttpServletRequestBuilder asTeacher(MockHttpServletRequestBuilder req) {
        return req.header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken);
    }

    private MockHttpServletRequestBuilder asStudent(MockHttpServletRequestBuilder req) {
        return req.header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken);
    }

    // =================================================================
    //  HELPERS — JDBC inserts
    // =================================================================

    private UUID insertUser(RoleName role) {
        UUID id = UUID.randomUUID();
        String roleType = role == RoleName.STUDENT ? "STUDENT"
                : role == RoleName.PARENT ? "PARENT"
                : "User";
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, role_type, status) " +
                        "VALUES (?, ?, ?, ?, ?, (SELECT id FROM roles WHERE name = CAST(? AS role_name)), ?, CAST(? AS user_status))",
                id,
                role.name().toLowerCase() + "-" + id + "@test.com",
                "hash",
                "Test", "User",
                role.name(),
                roleType,
                "ACTIVE"
        );
        return id;
    }

    private UUID insertCourse(UUID createdBy) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO courses (id, title, created_by, status, visibility) " +
                        "VALUES (?, ?, ?, CAST(? AS course_status), CAST(? AS course_visibility))",
                id, "Test Course", createdBy, "PUBLISHED", "PUBLIC"
        );
        return id;
    }

    private UUID insertChapter(UUID courseId) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO chapters (id, course_id, title) VALUES (?, ?, ?)",
                id, courseId, "Chapter 1"
        );
        return id;
    }

    private UUID insertLesson(UUID chapterId) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO lessons (id, chapter_id, title, order_index) VALUES (?, ?, ?, ?)",
                id, chapterId, "Lesson 1", 1
        );
        return id;
    }

    private UUID insertTest(UUID lessonId, UUID createdBy) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO tests (id, lesson_id, created_by, title, description, time_limit_sec, ai_enabled, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS test_status))",
                id, lessonId, createdBy, "Test 1", "desc", 600, false, "PUBLISHED"
        );
        return id;
    }

    private void enrollStudent(UUID studentId, UUID courseId) {
        jdbcTemplate.update(
                "INSERT INTO course_enrollments (student_id, course_id, enrolled_at) " +
                        "VALUES (?, ?, CURRENT_TIMESTAMP)",
                studentId, courseId
        );
    }

    private void insertAttemptWithResult(UUID studentId, UUID testId, int score, boolean passed) {
        UUID attemptId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO test_attempts (id, test_id, student_id, attempt_number, status, started_at) " +
                        "VALUES (?, ?, ?, 1, CAST(? AS attempt_status), CURRENT_TIMESTAMP)",
                attemptId, testId, studentId, "DONE"
        );
        double fraction = (double) score / TOTAL_QUESTIONS;  // NUMERIC(5,4): 0.0000-1.0000
        double pct      = fraction * 100.0;                  // NUMERIC(5,2): 0.00-100.00
        jdbcTemplate.update(
                "INSERT INTO test_results (attempt_id, test_id, student_id, score, score_percent, passed, completed_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                attemptId, testId, studentId, fraction, pct, passed
        );
    }

    // =================================================================
    //  1. GET /tests/{testId}/analytics/class-average  (TEACHER)
    // =================================================================

    @Test
    @Order(10)
    @DisplayName("1.1 — class-average -> 200 with data when teacher owns test")
    void classAverage_ReturnsOk_WhenTeacherOwnsTest() throws Exception {
        insertAttemptWithResult(studentId, testId, PASSING_SCORE, true);

        mockMvc.perform(asTeacher(get(BASE + "/tests/{testId}/analytics/class-average", testId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testId",        is(testId.toString())))
                .andExpect(jsonPath("$.totalAttempts", is(1)))
                .andExpect(jsonPath("$.passedCount",   is(1)))
                .andExpect(jsonPath("$.failedCount",   is(0)))
                .andExpect(jsonPath("$.averageScore").exists())
                .andExpect(jsonPath("$.minScore").exists())
                .andExpect(jsonPath("$.maxScore").exists());
    }

    @Test
    @Order(11)
    @DisplayName("1.2 — class-average -> 200 with zero attempts")
    void classAverage_ReturnsOk_WithZeroAttempts() throws Exception {
        mockMvc.perform(asTeacher(get(BASE + "/tests/{testId}/analytics/class-average", testId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAttempts", is(0)));
    }

    @Test
    @Order(12)
    @DisplayName("1.3 — class-average -> mixed pass/fail counts are correct")
    void classAverage_PassFailCounts_AreCorrect() throws Exception {
        UUID student2 = insertUser(RoleName.STUDENT);
        try {
            insertAttemptWithResult(studentId, testId, PASSING_SCORE, true);
            insertAttemptWithResult(student2,  testId, FAILING_SCORE, false);

            mockMvc.perform(asTeacher(get(BASE + "/tests/{testId}/analytics/class-average", testId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalAttempts", is(2)))
                    .andExpect(jsonPath("$.passedCount",   is(1)))
                    .andExpect(jsonPath("$.failedCount",   is(1)));
        } finally {
            jdbcTemplate.update("DELETE FROM test_results WHERE student_id = ?", student2);
            jdbcTemplate.update("DELETE FROM test_attempts WHERE student_id = ?", student2);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", student2);
        }
    }

    @Test
    @Order(13)
    @DisplayName("1.4 — class-average -> 403 when teacher does not own test")
    void classAverage_ReturnsForbidden_WhenTeacherDoesNotOwnTest() throws Exception {
        UUID otherTeacher = insertUser(RoleName.TEACHER);
        String otherToken = jwtUtil.generateAccessToken(otherTeacher, RoleName.TEACHER);
        try {
            mockMvc.perform(get(BASE + "/tests/{testId}/analytics/class-average", testId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
                    .andExpect(status().isForbidden());
        } finally {
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", otherTeacher);
        }
    }

    @Test
    @Order(14)
    @DisplayName("1.5 — class-average -> 403 for non-existent testId (PreAuthorize rejects)")
    void classAverage_ReturnsForbidden_WhenTestDoesNotExist() throws Exception {
        mockMvc.perform(asTeacher(get(BASE + "/tests/{testId}/analytics/class-average", UUID.randomUUID())))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(15)
    @DisplayName("1.6 — class-average -> 401 when not authenticated")
    void classAverage_ReturnsUnauthorized_WhenNoToken() throws Exception {
        mockMvc.perform(get(BASE + "/tests/{testId}/analytics/class-average", testId))
                .andExpect(status().isUnauthorized());
    }

    // =================================================================
    //  2. GET /courses/{courseId}/analytics/student-averages  (TEACHER)
    // =================================================================

    @Test
    @Order(20)
    @DisplayName("2.1 — student-averages -> 200 paged when teacher owns course")
    void studentAverages_ReturnsOkPaged_WhenTeacherOwnsCourse() throws Exception {
        enrollStudent(studentId, courseId);
        insertAttemptWithResult(studentId, testId, PASSING_SCORE, true);

        mockMvc.perform(asTeacher(get(BASE + "/courses/{courseId}/analytics/student-averages", courseId)
                        .param("page", "0").param("size", "10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    @Order(21)
    @DisplayName("2.2 — student-averages -> 200 empty page when no students attempted tests")
    void studentAverages_ReturnsEmptyPage_WhenNoAttempts() throws Exception {
        mockMvc.perform(asTeacher(get(BASE + "/courses/{courseId}/analytics/student-averages", courseId)
                        .param("page", "0").param("size", "10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    @Order(22)
    @DisplayName("2.3 — student-averages -> 403 when teacher does not own course")
    void studentAverages_ReturnsForbidden_WhenTeacherDoesNotOwnCourse() throws Exception {
        UUID otherTeacher = insertUser(RoleName.TEACHER);
        String otherToken = jwtUtil.generateAccessToken(otherTeacher, RoleName.TEACHER);
        try {
            mockMvc.perform(get(BASE + "/courses/{courseId}/analytics/student-averages", courseId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
                    .andExpect(status().isForbidden());
        } finally {
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", otherTeacher);
        }
    }

    @Test
    @Order(23)
    @DisplayName("2.4 — student-averages -> 403 for non-existent courseId (PreAuthorize rejects)")
    void studentAverages_ReturnsNotFound_WhenCourseDoesNotExist() throws Exception {
        mockMvc.perform(asTeacher(get(BASE + "/courses/{courseId}/analytics/student-averages", UUID.randomUUID())))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(24)
    @DisplayName("2.5 — student-averages -> 401 when not authenticated")
    void studentAverages_ReturnsUnauthorized_WhenNoToken() throws Exception {
        mockMvc.perform(get(BASE + "/courses/{courseId}/analytics/student-averages", courseId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(25)
    @DisplayName("2.6 — student-averages -> pagination works correctly")
    void studentAverages_PaginationWorks() throws Exception {
        UUID s1 = insertUser(RoleName.STUDENT);
        UUID s2 = insertUser(RoleName.STUDENT);
        UUID s3 = insertUser(RoleName.STUDENT);
        try {
            insertAttemptWithResult(s1, testId, PASSING_SCORE, true);
            insertAttemptWithResult(s2, testId, FAILING_SCORE, false);
            insertAttemptWithResult(s3, testId, PASSING_SCORE, true);

            mockMvc.perform(asTeacher(get(BASE + "/courses/{courseId}/analytics/student-averages", courseId)
                            .param("page", "0").param("size", "2")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content",       hasSize(2)))
                    .andExpect(jsonPath("$.totalElements", is(3)))
                    .andExpect(jsonPath("$.totalPages",    is(2)));
        } finally {
            for (UUID s : new UUID[]{s1, s2, s3}) {
                jdbcTemplate.update("DELETE FROM test_results  WHERE student_id = ?", s);
                jdbcTemplate.update("DELETE FROM test_attempts WHERE student_id = ?", s);
                jdbcTemplate.update("DELETE FROM users WHERE id = ?", s);
            }
        }
    }

    // =================================================================
    //  3. GET /students/me/tests/{testId}/stats  (STUDENT)
    // =================================================================

    @Test
    @Order(30)
    @DisplayName("3.1 — my test stats -> 200 with correct fields after one attempt")
    void myTestStats_ReturnsOk_AfterOneAttempt() throws Exception {
        enrollStudent(studentId, courseId);
        insertAttemptWithResult(studentId, testId, PASSING_SCORE, true);

        mockMvc.perform(asStudent(get(BASE + "/students/me/tests/{testId}/stats", testId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testId",            is(testId.toString())))
                .andExpect(jsonPath("$.totalAttemptCount", is(1)))
                .andExpect(jsonPath("$.bestScore").exists())
                .andExpect(jsonPath("$.lowestScore").exists())
                .andExpect(jsonPath("$.averageScore").exists())
                .andExpect(jsonPath("$.lastScore").exists())
                .andExpect(jsonPath("$.classMedian").exists())
                .andExpect(jsonPath("$.rank").exists())
                .andExpect(jsonPath("$.percentile").exists());
    }

    @Test
    @Order(31)
    @DisplayName("3.2 — my test stats -> totalAttemptCount reflects multiple attempts by same student")
    void myTestStats_ReturnsOk_MultipleSameStudentAttempts() throws Exception {
        enrollStudent(studentId, courseId);
        insertAttemptWithResult(studentId, testId, FAILING_SCORE, false);
        insertAttemptWithResult(studentId, testId, PASSING_SCORE, true);

        mockMvc.perform(asStudent(get(BASE + "/students/me/tests/{testId}/stats", testId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAttemptCount", is(2)));
    }

    @Test
    @Order(32)
    @DisplayName("3.3 — my test stats -> rank = 1 when student has highest score")
    void myTestStats_RankIsOne_WhenStudentIsTopScorer() throws Exception {
        UUID student2 = insertUser(RoleName.STUDENT);
        try {
            enrollStudent(studentId, courseId);
            insertAttemptWithResult(studentId, testId, PASSING_SCORE, true);   // 80%
            insertAttemptWithResult(student2,  testId, FAILING_SCORE, false);  // 40%
            mockMvc.perform(asStudent(get(BASE + "/students/me/tests/{testId}/stats", testId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rank", is(1)));
        } finally {
            jdbcTemplate.update("DELETE FROM test_results  WHERE student_id = ?", student2);
            jdbcTemplate.update("DELETE FROM test_attempts WHERE student_id = ?", student2);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", student2);
        }
    }


    @Test
    @Order(33)
    @DisplayName("3.4 — my test stats -> 403 for non-existent testId (PreAuthorize rejects)")
    void myTestStats_ReturnsForbidden_WhenTestDoesNotExist() throws Exception {
        mockMvc.perform(asStudent(get(BASE + "/students/me/tests/{testId}/stats", UUID.randomUUID())))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(34)
    @DisplayName("3.5 — my test stats -> 401 when not authenticated")
    void myTestStats_ReturnsUnauthorized_WhenNoToken() throws Exception {
        mockMvc.perform(get(BASE + "/students/me/tests/{testId}/stats", testId))
                .andExpect(status().isUnauthorized());
    }

    // =================================================================
    //  4. GET /students/me/courses/{courseId}/stats  (STUDENT)
    // =================================================================

    @Test
    @Order(40)
    @DisplayName("4.1 — my course stats -> 200 with correct aggregated data")
    void myCourseStats_ReturnsOk_WithAggregatedData() throws Exception {
        enrollStudent(studentId, courseId);
        insertAttemptWithResult(studentId, testId, PASSING_SCORE, true);

        mockMvc.perform(asStudent(get(BASE + "/students/me/courses/{courseId}/stats", courseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseTitle").exists())
                .andExpect(jsonPath("$.totalTestCount").exists())
                .andExpect(jsonPath("$.totalTestDone",   is(1)))
                .andExpect(jsonPath("$.totalTestPassed", is(1)))
                .andExpect(jsonPath("$.bestScore").exists())
                .andExpect(jsonPath("$.lowestScore").exists())
                .andExpect(jsonPath("$.averageScore").exists())
                .andExpect(jsonPath("$.difficultyLessons").isArray())
                .andExpect(jsonPath("$.lastAttempts").isArray())
                .andExpect(jsonPath("$.lastAttempts", hasSize(1)));
    }

    @Test
    @Order(41)
    @DisplayName("4.2 — my course stats -> lastAttempts capped at 5")
    void myCourseStats_LastAttempts_CappedAtFive() throws Exception {
        enrollStudent(studentId, courseId);
        for (int i = 0; i < 7; i++) {
            insertAttemptWithResult(studentId, testId, PASSING_SCORE, true);
        }

        mockMvc.perform(asStudent(get(BASE + "/students/me/courses/{courseId}/stats", courseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastAttempts", hasSize(5)));
    }

    @Test
    @Order(42)
    @DisplayName("4.3 — my course stats -> difficultyLessons capped at 3")
    void myCourseStats_DifficultyLessons_CappedAtThree() throws Exception {
        enrollStudent(studentId, courseId);
        for (int i = 0; i < 4; i++) {
            UUID ch  = insertChapter(courseId);
            UUID les = insertLesson(ch);
            UUID tst = insertTest(les, teacherId);
            insertAttemptWithResult(studentId, tst, FAILING_SCORE, false);
        }

        mockMvc.perform(asStudent(get(BASE + "/students/me/courses/{courseId}/stats", courseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.difficultyLessons", hasSize(lessThanOrEqualTo(3))));
    }

    @Test
    @Order(43)
    @DisplayName("4.4 — my course stats -> 200 with empty lists when no attempts")
    void myCourseStats_ReturnsOk_WithEmptyListsWhenNoAttempts() throws Exception {
        enrollStudent(studentId, courseId);

        mockMvc.perform(asStudent(get(BASE + "/students/me/courses/{courseId}/stats", courseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTestDone",    is(0)))
                .andExpect(jsonPath("$.totalTestPassed",  is(0)))
                .andExpect(jsonPath("$.lastAttempts",     hasSize(0)))
                .andExpect(jsonPath("$.difficultyLessons", hasSize(0)));
    }

    @Test
    @Order(44)
    @DisplayName("4.5 — my course stats -> 403 when student is not enrolled")
    void myCourseStats_ReturnsForbidden_WhenStudentNotEnrolled() throws Exception {
        mockMvc.perform(asStudent(get(BASE + "/students/me/courses/{courseId}/stats", courseId)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(45)
    @DisplayName("4.6 — my course stats -> 404 when course does not exist")
    void myCourseStats_ReturnsNotFound_WhenCourseDoesNotExist() throws Exception {
        mockMvc.perform(asStudent(get(BASE + "/students/me/courses/{courseId}/stats", UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(46)
    @DisplayName("4.7 — my course stats -> 401 when not authenticated")
    void myCourseStats_ReturnsUnauthorized_WhenNoToken() throws Exception {
        mockMvc.perform(get(BASE + "/students/me/courses/{courseId}/stats", courseId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(50)
    @DisplayName(" 5.1 - lastAttempts is empty when student is enrolled but has no attempts")
    void lastAttempts_IsEmpty_WhenNoAttempts() throws Exception {
        enrollStudent(studentId, courseId);
        mockMvc.perform(asStudent(get(BASE + "/students/me/courses/{courseId}/stats", courseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastAttempts").isArray())
                .andExpect(jsonPath("$.lastAttempts", hasSize(0)));
    }

    @Test
    @Order(51)
    @DisplayName(" 5.2 - lastAttempts contains only the student's own attempts, not other students'")
    void lastAttempts_ContainsOnlyOwnAttempts_NotOtherStudents() throws Exception {
        UUID otherStudent = insertUser(RoleName.STUDENT);
        try {
            // other student attempts the same test
            insertAttemptWithResult(otherStudent, testId, 4, true);

            // our student has no attempts
            enrollStudent(studentId, courseId);
            mockMvc.perform(asStudent(get(BASE + "/students/me/courses/{courseId}/stats", courseId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lastAttempts", hasSize(0)));
        } finally {
            jdbcTemplate.update("DELETE FROM test_results  WHERE student_id = ?", otherStudent);
            jdbcTemplate.update("DELETE FROM test_attempts WHERE student_id = ?", otherStudent);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", otherStudent);
        }
    }

    @Test
    @Order(52)
    @DisplayName("5.3 - lastAttempts shows own attempt after student attempts the test")
    void lastAttempts_ShowsOwnAttempt_AfterStudentAttempts() throws Exception {
        insertAttemptWithResult(studentId, testId, 4, true);
        enrollStudent(studentId, courseId);
        mockMvc.perform(asStudent(get(BASE + "/students/me/courses/{courseId}/stats", courseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastAttempts", hasSize(1)))
                .andExpect(jsonPath("$.lastAttempts[0].testId", is(testId.toString())))
                .andExpect(jsonPath("$.lastAttempts[0].passed", is(true)));
    }

    @Test
    @Order(53)
    @DisplayName(" 5.3 - lastAttempts does not include attempts from a different course")
    void lastAttempts_ExcludesAttemptsFromOtherCourses() throws Exception {
        // create a second unrelated course with its own test
        UUID otherCourseId  = insertCourse(teacherId);
        UUID otherChapterId = insertChapter(otherCourseId);
        UUID otherLessonId  = insertLesson(otherChapterId);
        UUID otherTestId    = insertTest(otherLessonId, teacherId);
        try {
            insertAttemptWithResult(studentId, otherTestId, 4, true);

            // our original course should have no attempts
            enrollStudent(studentId, courseId);
            mockMvc.perform(asStudent(get(BASE + "/students/me/courses/{courseId}/stats", courseId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lastAttempts", hasSize(0)));
        } finally {
            jdbcTemplate.update("DELETE FROM test_results  WHERE test_id = ?", otherTestId);
            jdbcTemplate.update("DELETE FROM test_attempts WHERE test_id = ?", otherTestId);
            jdbcTemplate.update("DELETE FROM tests    WHERE id = ?", otherTestId);
            jdbcTemplate.update("DELETE FROM lessons  WHERE id = ?", otherLessonId);
            jdbcTemplate.update("DELETE FROM chapters WHERE id = ?", otherChapterId);
            jdbcTemplate.update("DELETE FROM course_enrollments WHERE course_id = ?", otherCourseId);
            jdbcTemplate.update("DELETE FROM courses  WHERE id = ?", otherCourseId);
        }
    }

    // ─── difficultyLessons edge cases ─────────────────────────────────────────

    @Test
    @Order(60)
    @DisplayName("6.1 - difficultyLessons is empty when student has no attempts")
    void difficultyLessons_IsEmpty_WhenNoAttempts() throws Exception {
        enrollStudent(studentId, courseId);
        mockMvc.perform(asStudent(get(BASE + "/students/me/courses/{courseId}/stats", courseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.difficultyLessons").isArray())
                .andExpect(jsonPath("$.difficultyLessons", hasSize(0)));
    }

    @Test
    @Order(61)
    @DisplayName(" 6.2. - difficultyLessons is empty when student passes all lessons above 60%")
    void difficultyLessons_IsEmpty_WhenStudentPassesEverythingAbove60Percent() throws Exception {
        // score = 4/5 = 80% — above passing grade of 60%, no gap issue
        insertAttemptWithResult(studentId, testId, 4, true);
        enrollStudent(studentId, courseId);
        mockMvc.perform(asStudent(get(BASE + "/students/me/courses/{courseId}/stats", courseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.difficultyLessons", hasSize(0)));
    }

    @Test
    @Order(62)
    @DisplayName(" 6.3 - difficultyLessons includes lesson when student scores below 60%")
    void difficultyLessons_IncludesLesson_WhenStudentScoresBelowPassingGrade() throws Exception {
        // score = 2/5 = 40% — below passing grade of 60%
        enrollStudent(studentId, courseId);
        insertAttemptWithResult(studentId, testId, 2, false);

        mockMvc.perform(asStudent(get(BASE + "/students/me/courses/{courseId}/stats", courseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.difficultyLessons").isArray())
                .andExpect(jsonPath("$.difficultyLessons", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @Order(63)
    @DisplayName("6.4 - difficultyLessons includes lesson when gap between student and class average exceeds 15%")
    void difficultyLessons_IncludesLesson_WhenGapExceeds15Percent() throws Exception {
        UUID otherStudent = insertUser(RoleName.STUDENT);
        enrollStudent(studentId, courseId);
        try {
            // other student scores 5/5 = 100%
            insertAttemptWithResult(otherStudent, testId, 5, true);
            // our student scores 4/5 = 80% — classAverage = (100+80)/2 = 90%, gap = 10% — not enough

            // let's add a second other student with 100% too: classAverage = (100+100+80)/3 ≈ 93%, gap ≈ 13% — still not enough
            // so let's make our student score lower: 3/5 = 60%, classAverage = (100+60)/2 = 80%, gap = 20% > 15 ✓
            insertAttemptWithResult(studentId, testId, 3, false);

            mockMvc.perform(asStudent(get(BASE + "/students/me/courses/{courseId}/stats", courseId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.difficultyLessons", hasSize(greaterThanOrEqualTo(1))));
        } finally {
            jdbcTemplate.update("DELETE FROM test_results  WHERE student_id = ?", otherStudent);
            jdbcTemplate.update("DELETE FROM test_attempts WHERE student_id = ?", otherStudent);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", otherStudent);
        }
    }

    @Test
    @Order(64)
    @DisplayName("6.5. - difficultyLessons does not include lesson when student is at or above class average with passing score")
    void difficultyLessons_ExcludesLesson_WhenStudentIsAboveAverageAndPassing() throws Exception {
        UUID otherStudent = insertUser(RoleName.STUDENT);
        enrollStudent(studentId, courseId);
        try {
            // other student: 2/5 = 40%, our student: 4/5 = 80%
            // classAverage = 60%, gap for our student = 60-80 = negative → no difficulty
            insertAttemptWithResult(otherStudent, testId, 2, false);
            insertAttemptWithResult(studentId,    testId, 4, true);

            mockMvc.perform(asStudent(get(BASE + "/students/me/courses/{courseId}/stats", courseId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.difficultyLessons", hasSize(0)));
        } finally {
            jdbcTemplate.update("DELETE FROM test_results  WHERE student_id = ?", otherStudent);
            jdbcTemplate.update("DELETE FROM test_attempts WHERE student_id = ?", otherStudent);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", otherStudent);
        }
    }

    @Test
    @Order(65)
    @DisplayName("6.6 - difficultyLessons respects the cap of 3 even with many struggling lessons")
    void difficultyLessons_CappedAtThree_EvenWithManyFailingLessons() throws Exception {
        // create 5 extra lessons all with failing scores
        enrollStudent(studentId, courseId);
        for (int i = 0; i < 5; i++) {
            UUID ch  = insertChapter(courseId);
            UUID les = insertLesson(ch);
            UUID tst = insertTest(les, teacherId);
            insertAttemptWithResult(studentId, tst, 1, false); // 20% — well below 60%
        }

        mockMvc.perform(asStudent(get(BASE + "/students/me/courses/{courseId}/stats", courseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.difficultyLessons", hasSize(lessThanOrEqualTo(3))));
    }

    @Test
    @Order(66)
    @DisplayName(" 6.7. - difficultyLessons only shows lessons from the requested course, not from other courses")
    void difficultyLessons_OnlyShowsLessonsFromRequestedCourse() throws Exception {
        // create a second course where student fails
        UUID otherCourseId  = insertCourse(teacherId);
        UUID otherChapterId = insertChapter(otherCourseId);
        UUID otherLessonId  = insertLesson(otherChapterId);
        UUID otherTestId    = insertTest(otherLessonId, teacherId);
        try {
            // student fails in other course
            insertAttemptWithResult(studentId, otherTestId, 1, false);
            // student passes in original course
            insertAttemptWithResult(studentId, testId, 4, true);

            // original course should have no difficulty lessons
            enrollStudent(studentId, courseId);
            mockMvc.perform(asStudent(get(BASE + "/students/me/courses/{courseId}/stats", courseId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.difficultyLessons", hasSize(0)));
        } finally {
            jdbcTemplate.update("DELETE FROM test_results  WHERE test_id = ?", otherTestId);
            jdbcTemplate.update("DELETE FROM test_attempts WHERE test_id = ?", otherTestId);
            jdbcTemplate.update("DELETE FROM tests    WHERE id = ?", otherTestId);
            jdbcTemplate.update("DELETE FROM lessons  WHERE id = ?", otherLessonId);
            jdbcTemplate.update("DELETE FROM chapters WHERE id = ?", otherChapterId);
            jdbcTemplate.update("DELETE FROM course_enrollments WHERE course_id = ?", otherCourseId);
            jdbcTemplate.update("DELETE FROM courses  WHERE id = ?", otherCourseId);
        }
    }

}
