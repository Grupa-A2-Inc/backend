package org.elearning.backend.subscription.service;

import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.organization.exception.OrganizationNotFoundException;
import org.elearning.backend.organization.exception.OrganizationSubscriptionNotFoundException;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.subscription.dto.request.CreateOrganizationSubscriptionRequest;
import org.elearning.backend.subscription.dto.request.UpdateOrganizationSubscriptionRequest;
import org.elearning.backend.subscription.dto.response.OrganizationSubscriptionResponse;
import org.elearning.backend.subscription.dto.response.OrganizationSubscriptionStatusResponse;
import org.elearning.backend.subscription.entity.OrganizationSubscription;
import org.elearning.backend.subscription.entity.OrganizationSubscriptionStatus;
import org.elearning.backend.subscription.entity.SubscriptionPlan;
import org.elearning.backend.subscription.entity.SubscriptionProvider;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationSubscriptionServiceTest {

    @Mock
    private OrganizationSubscriptionRepository organizationSubscriptionRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @InjectMocks
    private OrganizationSubscriptionService organizationSubscriptionService;

    @Test
    void createOrganizationSubscription_persistsAndMapsResponse() {
        UUID organizationId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(organizationId);

        SubscriptionPlan plan = buildPlan(planId);
        OrganizationSubscription savedSubscription = buildSubscription(organizationId, OrganizationSubscriptionStatus.ACTIVE);
        savedSubscription.setOrganization(organization);
        savedSubscription.setSubscriptionPlan(plan);

        CreateOrganizationSubscriptionRequest request = CreateOrganizationSubscriptionRequest.builder()
                .organizationId(organizationId)
                .subscriptionPlanId(planId)
                .status(OrganizationSubscriptionStatus.ACTIVE)
                .provider(SubscriptionProvider.STRIPE)
                .providerCustomerId("cus_123")
                .providerSubscriptionId("sub_123")
                .currentPeriodStart(LocalDateTime.of(2026, 1, 1, 0, 0))
                .currentPeriodEnd(LocalDateTime.of(2026, 2, 1, 0, 0))
                .build();

        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
        when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(organizationSubscriptionRepository.save(any(OrganizationSubscription.class))).thenReturn(savedSubscription);

        OrganizationSubscriptionResponse response = organizationSubscriptionService.createOrganizationSubscription(request);

        assertThat(response.getOrganizationId()).isEqualTo(organizationId);
        assertThat(response.getSubscriptionPlanId()).isEqualTo(planId);
        assertThat(response.getStatus()).isEqualTo(OrganizationSubscriptionStatus.ACTIVE);
        assertThat(response.getProvider()).isEqualTo(SubscriptionProvider.STRIPE);
    }

    @Test
    void getOrganizationSubscriptionById_returnsMappedResponse() {
        UUID organizationId = UUID.randomUUID();
        OrganizationSubscription subscription = buildSubscription(organizationId, OrganizationSubscriptionStatus.ACTIVE);

        when(organizationSubscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));

        OrganizationSubscriptionResponse response =
                organizationSubscriptionService.getOrganizationSubscriptionById(subscription.getId());

        assertThat(response.getId()).isEqualTo(subscription.getId());
        assertThat(response.getOrganizationId()).isEqualTo(organizationId);
        assertThat(response.getSubscriptionPlanId()).isEqualTo(subscription.getSubscriptionPlan().getId());
    }

    @Test
    void getSubscriptionsByOrganizationId_returnsMappedList() {
        UUID organizationId = UUID.randomUUID();
        OrganizationSubscription first = buildSubscription(organizationId, OrganizationSubscriptionStatus.ACTIVE);
        OrganizationSubscription second = buildSubscription(organizationId, OrganizationSubscriptionStatus.CANCELED);

        when(organizationSubscriptionRepository.findAllByOrganizationIdOrderByCreatedAtDesc(organizationId))
                .thenReturn(List.of(first, second));

        List<OrganizationSubscriptionResponse> responses =
                organizationSubscriptionService.getSubscriptionsByOrganizationId(organizationId);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(OrganizationSubscriptionResponse::getStatus)
                .containsExactly(OrganizationSubscriptionStatus.ACTIVE, OrganizationSubscriptionStatus.CANCELED);
    }

    @Test
    void getCurrentOrganizationSubscription_returnsMappedCurrentSubscription() {
        UUID organizationId = UUID.randomUUID();
        OrganizationSubscription subscription = buildSubscription(organizationId, OrganizationSubscriptionStatus.ACTIVE);

        when(organizationRepository.existsById(organizationId)).thenReturn(true);
        when(organizationSubscriptionRepository.findFirstByOrganizationIdAndStatusInOrderByCurrentPeriodEndDesc(
                organizationId,
                List.of(
                        OrganizationSubscriptionStatus.ACTIVE,
                        OrganizationSubscriptionStatus.TRIALING,
                        OrganizationSubscriptionStatus.PAST_DUE
                )
        )).thenReturn(Optional.of(subscription));

        OrganizationSubscriptionStatusResponse response =
                organizationSubscriptionService.getCurrentOrganizationSubscription(organizationId);

        assertThat(response.getOrganizationId()).isEqualTo(organizationId);
        assertThat(response.getStatus()).isEqualTo(OrganizationSubscriptionStatus.ACTIVE);
        assertThat(response.getCurrentPeriodStart()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
        assertThat(response.getCurrentPeriodEnd()).isEqualTo(LocalDateTime.of(2026, 2, 1, 0, 0));
        assertThat(response.getPlan().getCode()).isEqualTo("SCHOOL");
        assertThat(response.getPlan().getDisplayName()).isEqualTo("School");
        assertThat(response.getPlan().getMaxUsers()).isEqualTo(500);
        assertThat(response.getPlan().getMaxClassrooms()).isEqualTo(20);
        assertThat(response.getPlan().getHasPremiumFeatures()).isTrue();
    }

    @Test
    void getCurrentOrganizationSubscription_fallsBackToLatestSubscription() {
        UUID organizationId = UUID.randomUUID();
        OrganizationSubscription subscription = buildSubscription(organizationId, OrganizationSubscriptionStatus.CANCELED);

        when(organizationRepository.existsById(organizationId)).thenReturn(true);
        when(organizationSubscriptionRepository.findFirstByOrganizationIdAndStatusInOrderByCurrentPeriodEndDesc(
                organizationId,
                List.of(
                        OrganizationSubscriptionStatus.ACTIVE,
                        OrganizationSubscriptionStatus.TRIALING,
                        OrganizationSubscriptionStatus.PAST_DUE
                )
        )).thenReturn(Optional.empty());
        when(organizationSubscriptionRepository.findFirstByOrganizationIdOrderByCurrentPeriodEndDesc(organizationId))
                .thenReturn(Optional.of(subscription));

        OrganizationSubscriptionStatusResponse response =
                organizationSubscriptionService.getCurrentOrganizationSubscription(organizationId);

        assertThat(response.getStatus()).isEqualTo(OrganizationSubscriptionStatus.CANCELED);
        verify(organizationSubscriptionRepository).findFirstByOrganizationIdOrderByCurrentPeriodEndDesc(organizationId);
    }

    @Test
    void getCurrentOrganizationSubscription_throwsWhenOrganizationMissing() {
        UUID organizationId = UUID.randomUUID();
        when(organizationRepository.existsById(organizationId)).thenReturn(false);

        assertThrows(
                OrganizationNotFoundException.class,
                () -> organizationSubscriptionService.getCurrentOrganizationSubscription(organizationId)
        );
    }

    @Test
    void getCurrentOrganizationSubscription_throwsWhenSubscriptionMissing() {
        UUID organizationId = UUID.randomUUID();
        when(organizationRepository.existsById(organizationId)).thenReturn(true);
        when(organizationSubscriptionRepository.findFirstByOrganizationIdAndStatusInOrderByCurrentPeriodEndDesc(
                organizationId,
                List.of(
                        OrganizationSubscriptionStatus.ACTIVE,
                        OrganizationSubscriptionStatus.TRIALING,
                        OrganizationSubscriptionStatus.PAST_DUE
                )
        )).thenReturn(Optional.empty());
        when(organizationSubscriptionRepository.findFirstByOrganizationIdOrderByCurrentPeriodEndDesc(organizationId))
                .thenReturn(Optional.empty());

        assertThrows(
                OrganizationSubscriptionNotFoundException.class,
                () -> organizationSubscriptionService.getCurrentOrganizationSubscription(organizationId)
        );
    }

    @Test
    void updateOrganizationSubscription_updatesAndMapsResponse() {
        UUID organizationId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        UUID newPlanId = UUID.randomUUID();
        OrganizationSubscription existing = buildSubscription(organizationId, OrganizationSubscriptionStatus.ACTIVE);
        existing.setId(subscriptionId);

        SubscriptionPlan newPlan = buildPlan(newPlanId);
        UpdateOrganizationSubscriptionRequest request = UpdateOrganizationSubscriptionRequest.builder()
                .subscriptionPlanId(newPlanId)
                .status(OrganizationSubscriptionStatus.CANCELED)
                .provider(SubscriptionProvider.MANUAL)
                .providerCustomerId("cus_updated")
                .providerSubscriptionId("sub_updated")
                .currentPeriodStart(LocalDateTime.of(2026, 2, 1, 0, 0))
                .currentPeriodEnd(LocalDateTime.of(2026, 3, 1, 0, 0))
                .build();

        when(organizationSubscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(existing));
        when(subscriptionPlanRepository.findById(newPlanId)).thenReturn(Optional.of(newPlan));
        when(organizationSubscriptionRepository.save(any(OrganizationSubscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrganizationSubscriptionResponse response =
                organizationSubscriptionService.updateOrganizationSubscription(subscriptionId, request);

        assertThat(response.getSubscriptionPlanId()).isEqualTo(newPlanId);
        assertThat(response.getStatus()).isEqualTo(OrganizationSubscriptionStatus.CANCELED);
        assertThat(response.getProvider()).isEqualTo(SubscriptionProvider.MANUAL);
        assertThat(response.getProviderSubscriptionId()).isEqualTo("sub_updated");
    }

    private OrganizationSubscription buildSubscription(UUID organizationId, OrganizationSubscriptionStatus status) {
        Organization organization = new Organization();
        organization.setId(organizationId);

        SubscriptionPlan plan = buildPlan(UUID.randomUUID());

        OrganizationSubscription subscription = new OrganizationSubscription();
        subscription.setId(UUID.randomUUID());
        subscription.setOrganization(organization);
        subscription.setSubscriptionPlan(plan);
        subscription.setStatus(status);
        subscription.setProvider(SubscriptionProvider.STRIPE);
        subscription.setProviderCustomerId("cus_123");
        subscription.setProviderSubscriptionId("sub_123");
        subscription.setCurrentPeriodStart(LocalDateTime.of(2026, 1, 1, 0, 0));
        subscription.setCurrentPeriodEnd(LocalDateTime.of(2026, 2, 1, 0, 0));
        subscription.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        subscription.setUpdatedAt(LocalDateTime.of(2026, 1, 2, 0, 0));
        return subscription;
    }

    private SubscriptionPlan buildPlan(UUID planId) {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setId(planId);
        plan.setCode("SCHOOL");
        plan.setDisplayName("School");
        plan.setMaxUsers(500);
        plan.setMaxClassrooms(20);
        plan.setMaxCourses(100);
        plan.setHasPremiumFeatures(true);
        plan.setPriceMonthly(new BigDecimal("99.99"));
        plan.setCurrency("EUR");
        plan.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        plan.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        return plan;
    }
}
