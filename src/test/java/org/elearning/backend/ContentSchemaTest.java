package org.elearning.backend;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ContentSchemaTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void shouldHaveAllTables() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class
        );
        assertThat(tables).contains("courses", "chapters", "lessons", "lesson_resources");
    }

    @Test
    void shouldHaveCorrectColumnsForCourses() {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'courses'",
                String.class
        );
        assertThat(columns).contains("id", "title", "description", "category",
                "status", "visibility", "created_by", "created_at", "updated_at");
    }

    @Test
    void shouldHaveCorrectColumnsForChapters() {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'chapters'",
                String.class
        );
        assertThat(columns).contains("id", "course_id", "title", "order_index",
                "created_at", "updated_at");
    }

    @Test
    void shouldHaveCorrectColumnsForLessons() {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'lessons'",
                String.class
        );
        assertThat(columns).contains("id", "chapter_id", "title", "content_md",
                "order_index", "created_at", "updated_at");
    }

    @Test
    void shouldHaveCorrectColumnsForLessonResources() {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'lesson_resources'",
                String.class
        );
        assertThat(columns).contains("id", "lesson_id", "title", "url", "created_at");
    }

    @Test
    void shouldEnforceForeignKeys() {
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                jdbcTemplate.execute(
                        "INSERT INTO chapters (id, course_id, title) " +
                                "VALUES (gen_random_uuid(), gen_random_uuid(), 'test')"
                )
        );
    }
}