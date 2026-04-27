package org.elearning.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.mail.from=test@example.com")
class UserSchemaTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @MockitoBean
    JavaMailSender javaMailSender;

    @Test
    void shouldHaveAllUserTables() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class
        );
        assertThat(tables).contains("users", "roles");
    }

    @Test
    void shouldHaveCorrectColumnsForUsers() {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'users'",
                String.class
        );
        assertThat(columns).contains("id", "email", "password_hash",
                "first_name", "last_name", "role_id", "organization_id", "status",
                "created_at", "updated_at");
    }

    @Test
    void shouldHaveCorrectColumnsForRoles() {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'roles'",
                String.class
        );
        assertThat(columns).contains("id", "name");
    }

    @Test
    void shouldEnforceForeignKeyBetweenUsersAndRoles() {
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                jdbcTemplate.execute(
                        "INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, status) " +
                                "VALUES (gen_random_uuid(), 'test@test.com', 'hash', 'Test', 'User', 999, 'ACTIVE')"
                )
        );
    }

    @Test
    void shouldHaveRolesSeeded() {
        List<String> roles = jdbcTemplate.queryForList(
                "SELECT name FROM roles",
                String.class
        );
        assertThat(roles).contains("ADMIN", "ORGANIZATION_ADMIN", "TEACHER", "STUDENT", "PARENT");
    }

    @Test
    void shouldEnforceForeignKeyBetweenUsersAndOrganizations() {
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                jdbcTemplate.execute(
                        "INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, organization_id, status) " +
                                "VALUES (gen_random_uuid(), 'org@test.com', 'hash', 'Test', 'User', 1, gen_random_uuid(), 'ACTIVE')"
                )
        );
    }

    @Test
    void shouldEnforceUniqueEmail() {
        String email = "duplicate-" + UUID.randomUUID() + "@test.com";

        jdbcTemplate.execute(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, status) " +
                        "VALUES (gen_random_uuid(), '" + email + "', 'hash', 'Test', 'User', 1, 'ACTIVE')"
        );

        try {
            org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                    jdbcTemplate.execute(
                            "INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, status) " +
                                    "VALUES (gen_random_uuid(), '" + email + "', 'hash', 'Test2', 'User2', 1, 'ACTIVE')"
                    )
            );
        } finally {
            jdbcTemplate.update("DELETE FROM users WHERE email = ?", email);
        }
    }

    @Test
    void shouldEnforceNotNullOnEmail() {
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                jdbcTemplate.execute(
                        "INSERT INTO users (id, password_hash, first_name, last_name, role_id, status) " +
                                "VALUES (gen_random_uuid(), 'hash', 'Test', 'User', 1, 'ACTIVE')"
                )
        );
    }
}
