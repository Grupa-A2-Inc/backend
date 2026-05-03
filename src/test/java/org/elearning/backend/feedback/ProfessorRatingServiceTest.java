package org.elearning.backend.feedback;

import org.elearning.backend.auth.service.EmailService;
import org.elearning.backend.feedback.dto.LessonRatingFullStatsDto;
import org.elearning.backend.feedback.service.ProfessorRatingService;
import org.elearning.backend.role.entity.RoleName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProfessorRatingServiceTest {

    @MockitoBean
    private EmailService emailService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProfessorRatingService professorRatingService;

    private UUID teacherId;

    @BeforeEach
    void setUp() {
        teacherId = insertUser(RoleName.TEACHER);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM lesson_ratings");
        jdbcTemplate.execute("DELETE FROM lessons");
        jdbcTemplate.execute("DELETE FROM chapters");
        jdbcTemplate.execute("DELETE FROM courses");
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", teacherId);
    }

    private UUID insertUser(RoleName role) {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, role_type, status) " +
                        "VALUES (?, ?, ?, ?, ?, (SELECT id FROM roles WHERE name = CAST(? AS role_name)), ?, CAST(? AS user_status))",
                userId, role.name().toLowerCase() + "-" + userId + "@test.com", "password",
                "Test", role.name(), role.name(), "User", "ACTIVE"
        );
        return userId;
    }

    private UUID insertLessonForTeacher(UUID creatorId) {
        UUID courseId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO courses (id, title, created_by, status, visibility) " +
                        "VALUES (?, ?, ?, CAST(? AS course_status), CAST(? AS course_visibility))",
                courseId, "Test Course", creatorId, "PUBLISHED", "PUBLIC"
        );

        UUID chapterId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO chapters (id, course_id, title) VALUES (?, ?, ?)",
                chapterId, courseId, "Test Chapter"
        );

        UUID lessonId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO lessons (id, chapter_id, title, order_index) VALUES (?, ?, ?, ?)",
                lessonId, chapterId, "Test Lesson", 1
        );

        return lessonId;
    }

    @Test
    void shouldReturnNullAverageWhenLessonHasNoRatings() {
        insertLessonForTeacher(teacherId);

        List<LessonRatingFullStatsDto> results = professorRatingService.getAverageRatingsForAllLessons(teacherId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAverageRating()).isNull();
        assertThat(results.get(0).getTotalRatings()).isZero();
    }
}