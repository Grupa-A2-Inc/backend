package org.elearning.backend.subscription;

import org.elearning.backend.auth.service.EmailService;
import org.elearning.backend.subscription.entity.SubscriptionPlan;
import org.elearning.backend.subscription.repository.SubscriptionPlanRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.mail.from=test@example.com")
class SubscriptionPlanRepositoryTest {

    @Autowired
    SubscriptionPlanRepository subscriptionPlanRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    JavaMailSender javaMailSender;

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM subscription_plans WHERE code IN ('TEST_PRO', 'TEST_BUSINESS', 'TEST_ACADEMY', 'TEST_ENTERPRISE', 'TEST_STARTER')");
    }

    @Test
    void save_shouldPersistSubscriptionPlanWithAllConfiguredFields() {
        SubscriptionPlan plan = buildPlan("TEST_PRO", "Test Pro");

        SubscriptionPlan savedPlan = subscriptionPlanRepository.saveAndFlush(plan);

        assertThat(savedPlan.getId()).isNotNull();
        assertThat(savedPlan.getCode()).isEqualTo("TEST_PRO");
        assertThat(savedPlan.getDisplayName()).isEqualTo("Test Pro");
        assertThat(savedPlan.getMaxUsers()).isEqualTo(100);
        assertThat(savedPlan.getMaxClassrooms()).isEqualTo(20);
        assertThat(savedPlan.getMaxCourses()).isEqualTo(300);
        assertThat(savedPlan.getHasPremiumFeatures()).isTrue();
        assertThat(savedPlan.getPriceMonthly()).isEqualByComparingTo("49.99");
        assertThat(savedPlan.getCurrency()).isEqualTo("USD");
        assertThat(savedPlan.getCreatedAt()).isNotNull();
        assertThat(savedPlan.getUpdatedAt()).isNotNull();
    }

    @Test
    void findByCodeIgnoreCase_shouldReturnPersistedPlan() {
        subscriptionPlanRepository.saveAndFlush(buildPlan("TEST_BUSINESS", "Test Business"));

        Optional<SubscriptionPlan> foundPlan = subscriptionPlanRepository.findByCodeIgnoreCase("test_business");

        assertThat(foundPlan).isPresent();
        assertThat(foundPlan.get().getDisplayName()).isEqualTo("Test Business");
        assertThat(foundPlan.get().getMaxUsers()).isEqualTo(100);
    }

    @Test
    void findAllByOrderByDisplayNameAsc_shouldReturnPlansSortedByDisplayName() {
        subscriptionPlanRepository.saveAndFlush(buildPlan("TEST_ENTERPRISE", "Test Enterprise"));
        subscriptionPlanRepository.saveAndFlush(buildPlan("TEST_ACADEMY", "Test Academy"));

        List<SubscriptionPlan> plans = subscriptionPlanRepository.findAllByOrderByDisplayNameAsc();

        assertThat(plans).extracting(SubscriptionPlan::getDisplayName)
                .contains("Test Academy", "Test Enterprise");
    }

    @Test
    void existsByCodeIgnoreCase_shouldReturnTrueForPersistedPlan() {
        subscriptionPlanRepository.saveAndFlush(buildPlan("TEST_STARTER", "Test Starter"));

        boolean exists = subscriptionPlanRepository.existsByCodeIgnoreCase("test_starter");

        assertThat(exists).isTrue();
    }

    private SubscriptionPlan buildPlan(String code, String displayName) {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setCode(code);
        plan.setDisplayName(displayName);
        plan.setMaxUsers(100);
        plan.setMaxClassrooms(20);
        plan.setMaxCourses(300);
        plan.setHasPremiumFeatures(true);
        plan.setPriceMonthly(new BigDecimal("49.99"));
        plan.setCurrency("USD");
        return plan;
    }
}
