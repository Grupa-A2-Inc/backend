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
class SubscriptionPlanSchemaTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    JavaMailSender javaMailSender;

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM subscription_plans WHERE code IN ('TEST_STARTER', 'TEST_STARTER_PLUS', 'NEGATIVE')");
    }

    @Test
    void shouldHaveSubscriptionPlansTable() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class
        );

        assertThat(tables).contains("subscription_plans");
    }

    @Test
    void shouldHaveCorrectColumnsForSubscriptionPlans() {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'subscription_plans'",
                String.class
        );

        assertThat(columns).contains(
                "id",
                "code",
                "display_name",
                "max_users",
                "max_classrooms",
                "max_courses",
                "has_premium_features",
                "price_monthly",
                "currency",
                "created_at",
                "updated_at"
        );
    }

    @Test
    void shouldHaveRelevantIndexesForSubscriptionPlans() {
        List<String> indexes = jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE schemaname = 'public' AND tablename = 'subscription_plans'",
                String.class
        );

        assertThat(indexes).contains(
                "subscription_plans_pkey",
                "uq_subscription_plans_code_lower",
                "uq_subscription_plans_display_name_lower"
        );
    }

    @Test
    void shouldSeedDefaultSubscriptionPlans() {
        List<String> codes = jdbcTemplate.queryForList(
                "SELECT code FROM subscription_plans ORDER BY code",
                String.class
        );

        Integer freeMaxUsers = jdbcTemplate.queryForObject(
                "SELECT max_users FROM subscription_plans WHERE code = 'FREE'",
                Integer.class
        );

        Integer freeMaxClassrooms = jdbcTemplate.queryForObject(
                "SELECT max_classrooms FROM subscription_plans WHERE code = 'FREE'",
                Integer.class
        );

        assertThat(codes).contains("FREE", "STARTER", "SCHOOL", "ENTERPRISE");
        assertThat(freeMaxUsers).isEqualTo(31);
        assertThat(freeMaxClassrooms).isEqualTo(1);
    }

    @Test
    void shouldEnforceUniqueCodeIgnoringCase() {
        insertPlan("TEST_STARTER", "Test Starter");

        assertThrows(Exception.class, () ->
                jdbcTemplate.execute(
                        "INSERT INTO subscription_plans (id, code, display_name, max_users, max_classrooms) " +
                                "VALUES (gen_random_uuid(), 'test_starter', 'Starter Duplicate', 5, 2)"
                )
        );
    }

    @Test
    void shouldEnforceUniqueDisplayNameIgnoringCase() {
        insertPlan("TEST_STARTER", "Test Starter");

        assertThrows(Exception.class, () ->
                jdbcTemplate.execute(
                        "INSERT INTO subscription_plans (id, code, display_name, max_users, max_classrooms) " +
                                "VALUES (gen_random_uuid(), 'TEST_STARTER_PLUS', 'test starter', 5, 2)"
                )
        );
    }

    @Test
    void shouldEnforceNonNegativeNumericLimits() {
        assertThrows(Exception.class, () ->
                jdbcTemplate.execute(
                        "INSERT INTO subscription_plans (id, code, display_name, max_users, max_classrooms) " +
                                "VALUES (gen_random_uuid(), 'NEGATIVE', 'Negative Limits', -1, 2)"
                )
        );
    }

    private void insertPlan(String code, String displayName) {
        jdbcTemplate.execute(
                "INSERT INTO subscription_plans (id, code, display_name, max_users, max_classrooms) " +
                        "VALUES (gen_random_uuid(), '" + code + "', '" + displayName + "', 10, 3)"
        );
    }
}
