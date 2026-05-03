package org.elearning.backend.subscription.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.organization.exception.OrganizationNotFoundException;
import org.elearning.backend.organization.exception.OrganizationSubscriptionNotFoundException;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.subscription.dto.request.CreateOrganizationSubscriptionRequest;
import org.elearning.backend.subscription.dto.request.UpdateOrganizationSubscriptionRequest;
import org.elearning.backend.subscription.dto.response.OrganizationSubscriptionResponse;
import org.elearning.backend.subscription.dto.response.OrganizationSubscriptionStatusResponse;
import org.elearning.backend.subscription.dto.response.SubscriptionPlanResponse;
import org.elearning.backend.subscription.entity.OrganizationSubscription;
import org.elearning.backend.subscription.entity.OrganizationSubscriptionStatus;
import org.elearning.backend.subscription.entity.SubscriptionPlan;
import org.elearning.backend.subscription.repository.OrganizationSubscriptionRepository;
import org.elearning.backend.subscription.repository.SubscriptionPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
@Transactional
public class OrganizationSubscriptionService {

    private static final List<OrganizationSubscriptionStatus> CURRENT_STATUSES = List.of(
            OrganizationSubscriptionStatus.ACTIVE,
            OrganizationSubscriptionStatus.TRIALING,
            OrganizationSubscriptionStatus.PAST_DUE
    );

    private final OrganizationSubscriptionRepository organizationSubscriptionRepository;
    private final OrganizationRepository organizationRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public OrganizationSubscriptionResponse createOrganizationSubscription(CreateOrganizationSubscriptionRequest request) {
        OrganizationSubscription organizationSubscription = new OrganizationSubscription();
        organizationSubscription.setOrganization(getOrganization(request.getOrganizationId()));
        organizationSubscription.setSubscriptionPlan(getSubscriptionPlan(request.getSubscriptionPlanId()));
        organizationSubscription.setStatus(request.getStatus());
        organizationSubscription.setProvider(request.getProvider());
        organizationSubscription.setProviderCustomerId(request.getProviderCustomerId());
        organizationSubscription.setProviderSubscriptionId(request.getProviderSubscriptionId());
        organizationSubscription.setCurrentPeriodStart(request.getCurrentPeriodStart());
        organizationSubscription.setCurrentPeriodEnd(request.getCurrentPeriodEnd());

        OrganizationSubscription saved = organizationSubscriptionRepository.save(organizationSubscription);
        return toResponse(saved);
    }

    public OrganizationSubscriptionResponse getOrganizationSubscriptionById(UUID id) {
        OrganizationSubscription subscription = organizationSubscriptionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Organization subscription not found: " + id));
        return toResponse(subscription);
    }

    @Transactional(readOnly = true)
    public List<OrganizationSubscriptionResponse> getSubscriptionsByOrganizationId(UUID organizationId) {
        return organizationSubscriptionRepository.findAllByOrganizationIdOrderByCreatedAtDesc(organizationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationSubscriptionStatusResponse getCurrentOrganizationSubscription(UUID organizationId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new OrganizationNotFoundException("Organization not found: " + organizationId);
        }

        OrganizationSubscription subscription = organizationSubscriptionRepository
                .findFirstByOrganizationIdAndStatusInOrderByCurrentPeriodEndDesc(organizationId, CURRENT_STATUSES)
                .or(() -> organizationSubscriptionRepository.findFirstByOrganizationIdOrderByCurrentPeriodEndDesc(organizationId))
                .orElseThrow(() -> new OrganizationSubscriptionNotFoundException(
                        "Organization subscription not found for organization: " + organizationId
                ));

        return toStatusResponse(subscription);
    }

    public OrganizationSubscriptionResponse updateOrganizationSubscription(UUID id, UpdateOrganizationSubscriptionRequest request) {
        OrganizationSubscription subscription = organizationSubscriptionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Organization subscription not found: " + id));

        subscription.setSubscriptionPlan(getSubscriptionPlan(request.getSubscriptionPlanId()));
        subscription.setStatus(request.getStatus());
        subscription.setProvider(request.getProvider());
        subscription.setProviderCustomerId(request.getProviderCustomerId());
        subscription.setProviderSubscriptionId(request.getProviderSubscriptionId());
        subscription.setCurrentPeriodStart(request.getCurrentPeriodStart());
        subscription.setCurrentPeriodEnd(request.getCurrentPeriodEnd());

        OrganizationSubscription saved = organizationSubscriptionRepository.save(subscription);
        return toResponse(saved);
    }

    private Organization getOrganization(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + organizationId));
    }

    private SubscriptionPlan getSubscriptionPlan(UUID subscriptionPlanId) {
        return subscriptionPlanRepository.findById(subscriptionPlanId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription plan not found: " + subscriptionPlanId));
    }

    private OrganizationSubscriptionResponse toResponse(OrganizationSubscription subscription) {
        return new OrganizationSubscriptionResponse(
                subscription.getId(),
                subscription.getOrganization().getId(),
                subscription.getSubscriptionPlan().getId(),
                subscription.getStatus(),
                subscription.getProvider(),
                subscription.getProviderCustomerId(),
                subscription.getProviderSubscriptionId(),
                subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd(),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt()
        );
    }

    private OrganizationSubscriptionStatusResponse toStatusResponse(OrganizationSubscription subscription) {
        SubscriptionPlan plan = subscription.getSubscriptionPlan();

        return new OrganizationSubscriptionStatusResponse(
                subscription.getOrganization().getId(),
                subscription.getStatus(),
                subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd(),
                new SubscriptionPlanResponse(
                        plan.getId(),
                        plan.getCode(),
                        plan.getDisplayName(),
                        plan.getMaxUsers(),
                        plan.getMaxClassrooms(),
                        plan.getMaxCourses(),
                        plan.getHasPremiumFeatures(),
                        plan.getPriceMonthly(),
                        plan.getCurrency(),
                        plan.getCreatedAt(),
                        plan.getUpdatedAt()
                )
        );
    }
}
