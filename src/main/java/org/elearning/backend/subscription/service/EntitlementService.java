package org.elearning.backend.subscription.service;

import lombok.RequiredArgsConstructor;
import org.elearning.backend.classroom.repository.ClassroomRepository;
import org.elearning.backend.subscription.entity.OrganizationSubscription;
import org.elearning.backend.subscription.entity.OrganizationSubscriptionStatus;
import org.elearning.backend.subscription.entity.SubscriptionPlan;
import org.elearning.backend.subscription.exception.ClassroomLimitExceededException;
import org.elearning.backend.subscription.exception.SubscriptionNotActiveException;
import org.elearning.backend.subscription.exception.UserLimitExceededException;
import org.elearning.backend.subscription.repository.OrganizationSubscriptionRepository;
import org.elearning.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EntitlementService {

    private static final List<OrganizationSubscriptionStatus> ACTIVE_STATUSES = List.of(
            OrganizationSubscriptionStatus.ACTIVE,
            OrganizationSubscriptionStatus.TRIALING,
            OrganizationSubscriptionStatus.PAST_DUE
    );

    private final OrganizationSubscriptionRepository organizationSubscriptionRepository;
    private final UserRepository userRepository;
    private final ClassroomRepository classroomRepository;

    public boolean hasFeatureAccess(UUID organizationId) {
        return resolveActiveSubscription(organizationId)
                .map(sub -> sub.getSubscriptionPlan().getHasPremiumFeatures())
                .orElse(false);
    }

    public void canCreateUser(UUID organizationId) {
        canCreateUsers(organizationId, 1);
    }

    public void canCreateUsers(UUID organizationId, int count) {
        OrganizationSubscription subscription = requireActiveSubscription(organizationId);
        SubscriptionPlan plan = subscription.getSubscriptionPlan();

        int currentUsers = userRepository.findByOrganizationId(organizationId).size();

        if (currentUsers + count > plan.getMaxUsers()) {
            throw new UserLimitExceededException(organizationId, plan.getMaxUsers());
        }
    }

    public void canCreateClassroom(UUID organizationId) {
        OrganizationSubscription subscription = requireActiveSubscription(organizationId);
        SubscriptionPlan plan = subscription.getSubscriptionPlan();

        int currentClassrooms = classroomRepository.findAllByOrganizationIdOrderByNameAsc(organizationId).size();

        if (currentClassrooms >= plan.getMaxClassrooms()) {
            throw new ClassroomLimitExceededException(organizationId, plan.getMaxClassrooms());
        }
    }

    public boolean hasActiveSubscription(UUID organizationId) {
        return resolveActiveSubscription(organizationId).isPresent();
    }

    private OrganizationSubscription requireActiveSubscription(UUID organizationId) {
        return resolveActiveSubscription(organizationId)
                .orElseThrow(() -> new SubscriptionNotActiveException(organizationId));
    }

    private java.util.Optional<OrganizationSubscription> resolveActiveSubscription(UUID organizationId) {
        return organizationSubscriptionRepository
                .findFirstByOrganizationIdAndStatusInOrderByCurrentPeriodEndDesc(organizationId, ACTIVE_STATUSES);
    }
}
