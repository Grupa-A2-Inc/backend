package org.elearning.backend.subscription;

import org.elearning.backend.auth.service.EmailService;
import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.subscription.entity.OrganizationSubscription;
import org.elearning.backend.subscription.entity.OrganizationSubscriptionStatus;
import org.elearning.backend.subscription.entity.SubscriptionPlan;
import org.elearning.backend.subscription.entity.SubscriptionProvider;
import org.elearning.backend.subscription.repository.OrganizationSubscriptionRepository;
import org.elearning.backend.subscription.repository.SubscriptionPlanRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.mail.from=test@example.com")
class OrganizationSubscriptionRepositoryTest {

    @Autowired
    OrganizationSubscriptionRepository organizationSubscriptionRepository;

    @Autowired
    OrganizationRepository organizationRepository;

    @Autowired
    SubscriptionPlanRepository subscriptionPlanRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    JavaMailSender javaMailSender;

    private Organization organization;
    private SubscriptionPlan subscriptionPlan;

    @BeforeEach
    void setUp() {
        cleanupAll();

        jdbcTemplate.execute(
                "INSERT INTO users (id, email, password_hash, first_name, last_name, role_id, status) " +
                        "VALUES (gen_random_uuid(), 'org-sub-owner@test.com', 'hash', 'Owner', 'Subscription', 2, 'ACTIVE')"
        );

        jdbcTemplate.execute(
                "INSERT INTO organizations (id, name, country, city, organization_type, owner_id) " +
                        "VALUES (" +
                                "gen_random_uuid(), 'Organization Subscription Test', 'Romania', 'Iasi', 'School', " +
                                "(SELECT id FROM users WHERE email = 'org-sub-owner@test.com'))"
        );

        String organizationId = jdbcTemplate.queryForObject(
                "SELECT id::text FROM organizations WHERE name = 'Organization Subscription Test'",
                String.class
        );
        organization = organizationRepository.getReferenceById(UUID.fromString(organizationId));

        subscriptionPlan = new SubscriptionPlan();
        subscriptionPlan.setCode("ORG_SUB_PLAN");
        subscriptionPlan.setDisplayName("Organization Subscription Plan");
        subscriptionPlan.setMaxUsers(250);
        subscriptionPlan.setMaxClassrooms(25);
        subscriptionPlan.setMaxCourses(500);
        subscriptionPlan.setHasPremiumFeatures(true);
        subscriptionPlan.setPriceMonthly(new BigDecimal("99.99"));
        subscriptionPlan.setCurrency("USD");
        subscriptionPlan = subscriptionPlanRepository.saveAndFlush(subscriptionPlan);
    }

    @AfterEach
    void tearDown() {
        cleanupAll();
    }

    @Test
    void save_shouldPersistOrganizationSubscriptionWithOrganizationAndPlan() {
        OrganizationSubscription subscription = buildSubscription("sub_org_001");

        OrganizationSubscription saved = organizationSubscriptionRepository.saveAndFlush(subscription);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOrganization().getId()).isEqualTo(organization.getId());
        assertThat(saved.getSubscriptionPlan().getId()).isEqualTo(subscriptionPlan.getId());
        assertThat(saved.getStatus()).isEqualTo(OrganizationSubscriptionStatus.ACTIVE);
        assertThat(saved.getProvider()).isEqualTo(SubscriptionProvider.STRIPE);
        assertThat(saved.getProviderCustomerId()).isEqualTo("cus_org_001");
        assertThat(saved.getProviderSubscriptionId()).isEqualTo("sub_org_001");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void findByProviderSubscriptionId_shouldReadPersistedSubscriptionWithRelations() {
        organizationSubscriptionRepository.saveAndFlush(buildSubscription("sub_org_002"));

        Optional<OrganizationSubscription> found = organizationSubscriptionRepository.findByProviderSubscriptionId("sub_org_002");

        assertThat(found).isPresent();
        assertThat(found.get().getOrganization().getName()).isEqualTo("Organization Subscription Test");
        assertThat(found.get().getSubscriptionPlan().getCode()).isEqualTo("ORG_SUB_PLAN");
    }

    @Test
    void save_shouldUpdateStatusAndBillingPeriod() {
        OrganizationSubscription subscription = organizationSubscriptionRepository.saveAndFlush(buildSubscription("sub_org_003"));
        LocalDateTime updatedPeriodEnd = subscription.getCurrentPeriodEnd().plusMonths(1);

        subscription.setStatus(OrganizationSubscriptionStatus.CANCELED);
        subscription.setCurrentPeriodEnd(updatedPeriodEnd);

        organizationSubscriptionRepository.saveAndFlush(subscription);

        OrganizationSubscription updated = organizationSubscriptionRepository.findById(subscription.getId())
                .orElseThrow();

        assertThat(updated.getStatus()).isEqualTo(OrganizationSubscriptionStatus.CANCELED);
        assertThat(updated.getCurrentPeriodEnd()).isEqualTo(updatedPeriodEnd);
        assertThat(updated.getSubscriptionPlan().getId()).isEqualTo(subscriptionPlan.getId());
    }

    private OrganizationSubscription buildSubscription(String providerSubscriptionId) {
        OrganizationSubscription subscription = new OrganizationSubscription();
        subscription.setOrganization(organization);
        subscription.setSubscriptionPlan(subscriptionPlan);
        subscription.setStatus(OrganizationSubscriptionStatus.ACTIVE);
        subscription.setProvider(SubscriptionProvider.STRIPE);
        subscription.setProviderCustomerId("cus_org_001");
        subscription.setProviderSubscriptionId(providerSubscriptionId);
        subscription.setCurrentPeriodStart(LocalDateTime.of(2026, 1, 1, 0, 0));
        subscription.setCurrentPeriodEnd(LocalDateTime.of(2026, 2, 1, 0, 0));
        return subscription;
    }

    private void cleanupAll() {
        jdbcTemplate.execute("DELETE FROM organization_subscriptions");
        jdbcTemplate.execute("DELETE FROM subscription_plans WHERE code = 'ORG_SUB_PLAN'");
        jdbcTemplate.execute("DELETE FROM organizations WHERE name = 'Organization Subscription Test'");
        jdbcTemplate.execute("DELETE FROM users WHERE email = 'org-sub-owner@test.com'");
    }
}
