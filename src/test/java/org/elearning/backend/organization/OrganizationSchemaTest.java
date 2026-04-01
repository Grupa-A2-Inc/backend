package org.elearning.backend.organization;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrganizationSchemaTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void shouldHaveOrganizationsTable() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class
        );
        assertThat(tables).contains("organizations");
    }

    @Test
    void shouldHaveCorrectColumnsForOrganizations() {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'organizations'",
                String.class
        );
        assertThat(columns).contains("id", "name", "country", "city",
                "organization_type", "address", "phone_number",
                "owner_id", "created_at", "updated_at");
    }

    @Test
    void shouldEnforceForeignKeyBetweenOrganizationsAndUsers() {
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                jdbcTemplate.execute(
                        "INSERT INTO organizations (id, name, country, city, organization_type, owner_id) " +
                                "VALUES (gen_random_uuid(), 'Scoala', 'Romania', 'Cluj', 'Scoala', gen_random_uuid())"
                )
        );
    }

    @Test
    void shouldEnforceNotNullOnName() {
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                jdbcTemplate.execute(
                        "INSERT INTO organizations (id, country, city, organization_type, owner_id) " +
                                "VALUES (gen_random_uuid(), 'Romania', 'Cluj', 'Scoala', gen_random_uuid())"
                )
        );
    }

    @Test
    void shouldEnforceNotNullOnCountry() {
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                jdbcTemplate.execute(
                        "INSERT INTO organizations (id, name, city, organization_type, owner_id) " +
                                "VALUES (gen_random_uuid(), 'Scoala', 'Cluj', 'Scoala', gen_random_uuid())"
                )
        );
    }

    @Test
    void shouldEnforceNotNullOnCity() {
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                jdbcTemplate.execute(
                        "INSERT INTO organizations (id, name, country, organization_type, owner_id) " +
                                "VALUES (gen_random_uuid(), 'Scoala', 'Romania', 'Scoala', gen_random_uuid())"
                )
        );
    }

    @Test
    void shouldEnforceNotNullOnOrganizationType() {
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                jdbcTemplate.execute(
                        "INSERT INTO organizations (id, name, country, city, owner_id) " +
                                "VALUES (gen_random_uuid(), 'Scoala', 'Romania', 'Cluj', gen_random_uuid())"
                )
        );
    }

    @Test
    void shouldAllowNullForAddressAndPhoneNumber() {
        jdbcTemplate.execute(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, status) " +
                        "VALUES (gen_random_uuid(), 'test-org@test.com', 'hash', 'Test', 'User', 1, 'ACTIVE')"
        );

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
            jdbcTemplate.execute(
                    "INSERT INTO organizations (id, name, country, city, organization_type, owner_id) " +
                            "VALUES (gen_random_uuid(), 'Scoala', 'Romania', 'Cluj', 'Scoala', " +
                            "(SELECT id FROM users WHERE email = 'test-org@test.com'))"
            );
        });

        jdbcTemplate.execute("DELETE FROM organizations WHERE name = 'Scoala'");
        jdbcTemplate.execute("DELETE FROM users WHERE email = 'test-org@test.com'");
    }
}