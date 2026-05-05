package org.elearning.backend.subscription.service;

import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.subscription.dto.response.SubscriptionPlanResponse;
import org.elearning.backend.subscription.entity.SubscriptionPlan;
import org.elearning.backend.subscription.repository.OrganizationSubscriptionRepository;
import org.elearning.backend.subscription.repository.SubscriptionPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationSubscriptionServicePlanListTest {

    @Mock
    private OrganizationSubscriptionRepository organizationSubscriptionRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @InjectMocks
    private OrganizationSubscriptionService organizationSubscriptionService;

    @Test
    void getAllSubscriptionPlans_returnsMappedPlansOrderedByRepository() {
        SubscriptionPlan starterPlan = buildPlan("STARTER", "Starter", new BigDecimal("29.99"));
        SubscriptionPlan schoolPlan = buildPlan("SCHOOL", "School", new BigDecimal("99.99"));
        when(subscriptionPlanRepository.findAllByOrderByDisplayNameAsc()).thenReturn(List.of(starterPlan, schoolPlan));

        List<SubscriptionPlanResponse> response = organizationSubscriptionService.getAllSubscriptionPlans();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getId()).isEqualTo(starterPlan.getId());
        assertThat(response.get(0).getCode()).isEqualTo("STARTER");
        assertThat(response.get(0).getDisplayName()).isEqualTo("Starter");
        assertThat(response.get(0).getPriceMonthly()).isEqualByComparingTo("29.99");
        assertThat(response.get(1).getId()).isEqualTo(schoolPlan.getId());
        assertThat(response.get(1).getCode()).isEqualTo("SCHOOL");
    }

    private SubscriptionPlan buildPlan(String code, String displayName, BigDecimal priceMonthly) {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setId(UUID.randomUUID());
        plan.setCode(code);
        plan.setDisplayName(displayName);
        plan.setMaxUsers(100);
        plan.setMaxClassrooms(5);
        plan.setMaxCourses(20);
        plan.setHasPremiumFeatures(true);
        plan.setPriceMonthly(priceMonthly);
        plan.setCurrency("EUR");
        plan.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        plan.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        return plan;
    }
}
