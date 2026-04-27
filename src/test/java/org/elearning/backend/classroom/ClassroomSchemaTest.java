package org.elearning.backend.classroom;

import org.elearning.backend.auth.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.mail.from=test@example.com")
class ClassroomSchemaTest {

    @Autowired
    JdbcTemplate jdbcTemplate;
    @MockitoBean
    private EmailService emailService;
    @MockitoBean
    JavaMailSender javaMailSender;

    @Test
    void shouldHaveClassroomsTable() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class
        );

        assertThat(tables).contains("classrooms");
    }

    @Test
    void shouldHaveCorrectColumnsForClassrooms() {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'classrooms'",
                String.class
        );

        assertThat(columns).contains("id", "organization_id", "name", "description", "created_at", "updated_at");
    }

    @Test
    void shouldHaveIndexesForOrganizationAndUniqueNamePerOrganization() {
        List<String> indexes = jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE schemaname = 'public' AND tablename = 'classrooms'",
                String.class
        );

        assertThat(indexes).contains("idx_classrooms_organization_id", "uq_classrooms_org_name_lower");
    }

    @Test
    void shouldEnforceForeignKeyBetweenClassroomsAndOrganizations() {
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                jdbcTemplate.execute(
                        "INSERT INTO classrooms (id, organization_id, name) " +
                                "VALUES (gen_random_uuid(), gen_random_uuid(), 'Class A')"
                )
        );
    }

    @Test
    void shouldEnforceNotNullOnOrganizationId() {
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                jdbcTemplate.execute(
                        "INSERT INTO classrooms (id, name) VALUES (gen_random_uuid(), 'Class A')"
                )
        );
    }

    @Test
    void shouldEnforceNotNullOnName() {
        insertOrganizationOwnerAndOrganization();

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                jdbcTemplate.execute(
                        "INSERT INTO classrooms (id, organization_id) " +
                                "VALUES (gen_random_uuid(), (SELECT id FROM organizations WHERE name = 'Scoala Classroom Test'))"
                )
        );

        cleanupOrganizationFixtures();
    }

    @Test
    void shouldAllowNullDescription() {
        insertOrganizationOwnerAndOrganization();

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                jdbcTemplate.execute(
                        "INSERT INTO classrooms (id, organization_id, name) " +
                                "VALUES (" +
                                "gen_random_uuid(), " +
                                "(SELECT id FROM organizations WHERE name = 'Scoala Classroom Test'), " +
                                "'Class A')"
                )
        );

        jdbcTemplate.execute("DELETE FROM classrooms WHERE name = 'Class A'");
        cleanupOrganizationFixtures();
    }

    @Test
    void shouldEnforceUniqueNameCaseInsensitiveWithinOrganization() {
        insertOrganizationOwnerAndOrganization();

        jdbcTemplate.execute(
                "INSERT INTO classrooms (id, organization_id, name) " +
                        "VALUES (" +
                        "gen_random_uuid(), " +
                        "(SELECT id FROM organizations WHERE name = 'Scoala Classroom Test'), " +
                        "'Class A')"
        );

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                jdbcTemplate.execute(
                        "INSERT INTO classrooms (id, organization_id, name) " +
                                "VALUES (" +
                                "gen_random_uuid(), " +
                                "(SELECT id FROM organizations WHERE name = 'Scoala Classroom Test'), " +
                                "'class a')"
                )
        );

        jdbcTemplate.execute("DELETE FROM classrooms WHERE name IN ('Class A', 'class a')");
        cleanupOrganizationFixtures();
    }

    private void insertOrganizationOwnerAndOrganization() {
        jdbcTemplate.execute(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, status) " +
                        "VALUES (gen_random_uuid(), 'classroom-owner@test.com', 'hash', 'Owner', 'One', 2, 'ACTIVE')"
        );

        jdbcTemplate.execute(
                "INSERT INTO organizations (id, name, country, city, organization_type, owner_id) " +
                        "VALUES (" +
                        "gen_random_uuid(), " +
                        "'Scoala Classroom Test', 'Romania', 'Cluj', 'Scoala', " +
                        "(SELECT id FROM users WHERE email = 'classroom-owner@test.com'))"
        );
    }

    private void cleanupOrganizationFixtures() {
        jdbcTemplate.execute("DELETE FROM organizations WHERE name = 'Scoala Classroom Test'");
        jdbcTemplate.execute("DELETE FROM users WHERE email = 'classroom-owner@test.com'");
    }
}
