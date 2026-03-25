package org.elearning.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UserSchemaTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

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
                "first_name", "last_name", "role_id", "status",
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
    void shouldEnforceUniqueEmail() {
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            jdbcTemplate.execute(
                    "INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, status) " +
                            "VALUES (gen_random_uuid(), 'duplicate@test.com', 'hash', 'Test', 'User', 1, 'ACTIVE')"
            );
            jdbcTemplate.execute(
                    "INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, status) " +
                            "VALUES (gen_random_uuid(), 'duplicate@test.com', 'hash', 'Test2', 'User2', 1, 'ACTIVE')"
            );
        });
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

    @Test
    void shouldEnforceNotNullOnPasswordHash() {
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                jdbcTemplate.execute(
                        "INSERT INTO users (id, email, first_name, last_name, role_id, status) " +
                                "VALUES (gen_random_uuid(), 'test@test.com', 'Test', 'User', 1, 'ACTIVE')"
                )
        );
    }
}