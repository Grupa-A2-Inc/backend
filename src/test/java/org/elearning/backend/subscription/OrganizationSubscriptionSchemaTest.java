package org.elearning.backend.subscription;

import org.elearning.backend.auth.service.EmailService;
import org.junit.jupiter.api.AfterEach;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.mail.from=test@example.com")
class OrganizationSubscriptionSchemaTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    JavaMailSender javaMailSender;

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM organization_subscriptions");
        jdbcTemplate.execute("DELETE FROM subscription_plans WHERE code IN ('SCHEMA_PLAN')");
        jdbcTemplate.execute("DELETE FROM organizations WHERE name = 'Schema Subscription Org'");
        jdbcTemplate.execute("DELETE FROM users WHERE email = 'schema-subscription-owner@test.com'");
    }

    @Test
    void shouldHaveOrganizationSubscriptionsTable() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class
        );

        assertThat(tables).contains("organization_subscriptions");
    }

    @Test
    void shouldHaveCorrectColumnsForOrganizationSubscriptions() {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'organization_subscriptions'",
                String.class
        );

        assertThat(columns).contains(
                "id",
                "organization_id",
                "subscription_plan_id",
                "status",
                "provider",
                "provider_customer_id",
                "provider_subscription_id",
                "current_period_start",
                "current_period_end",
                "created_at",
                "updated_at"
        );
    }

    @Test
    void shouldHaveRelevantIndexesForOrganizationSubscriptions() {
        List<String> indexes = jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE schemaname = 'public' AND tablename = 'organization_subscriptions'",
                String.class
        );

        assertThat(indexes).contains(
                "organization_subscriptions_pkey",
                "idx_organization_subscriptions_organization_id",
                "uq_organization_subscriptions_provider_subscription_id",
                "idx_organization_subscriptions_status"
        );
    }

    @Test
    void shouldEnforceForeignKeyBetweenOrganizationSubscriptionsAndOrganizations() {
        insertPlanOnly();

        assertThrows(Exception.class, () ->
                jdbcTemplate.execute(
                        "INSERT INTO organization_subscriptions " +
                                "(id, organization_id, subscription_plan_id, status, provider, current_period_start, current_period_end) " +
                                "VALUES (" +
                                "gen_random_uuid(), gen_random_uuid(), " +
                                "(SELECT id FROM subscription_plans WHERE code = 'SCHEMA_PLAN'), " +
                                "'ACTIVE', 'STRIPE', NOW(), NOW() + INTERVAL '30 days')"
                )
        );
    }

    @Test
    void shouldEnforceForeignKeyBetweenOrganizationSubscriptionsAndSubscriptionPlans() {
        insertOrganizationOnly();

        assertThrows(Exception.class, () ->
                jdbcTemplate.execute(
                        "INSERT INTO organization_subscriptions " +
                                "(id, organization_id, subscription_plan_id, status, provider, current_period_start, current_period_end) " +
                                "VALUES (" +
                                "gen_random_uuid(), " +
                                "(SELECT id FROM organizations WHERE name = 'Schema Subscription Org'), " +
                                "gen_random_uuid(), 'ACTIVE', 'STRIPE', NOW(), NOW() + INTERVAL '30 days')"
                )
        );
    }

    private void insertOrganizationOnly() {
        jdbcTemplate.execute(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, status) " +
                        "VALUES (gen_random_uuid(), 'schema-subscription-owner@test.com', 'hash', 'Owner', 'Schema', 2, 'ACTIVE')"
        );

        jdbcTemplate.execute(
                "INSERT INTO organizations (id, name, country, city, organization_type, owner_id) " +
                        "VALUES (" +
                                "gen_random_uuid(), 'Schema Subscription Org', 'Romania', 'Cluj', 'School', " +
                                "(SELECT id FROM users WHERE email = 'schema-subscription-owner@test.com'))"
        );
    }

    private void insertPlanOnly() {
        jdbcTemplate.execute(
                "INSERT INTO subscription_plans (id, code, display_name, max_users, max_classrooms) " +
                        "VALUES (gen_random_uuid(), 'SCHEMA_PLAN', 'Schema Plan', 10, 5)"
        );
    }
}
