package org.elearning.backend.subscription.service;

import org.elearning.backend.classroom.entity.Classroom;
import org.elearning.backend.classroom.repository.ClassroomRepository;
import org.elearning.backend.organization.entity.Organization;
import org.elearning.backend.subscription.entity.OrganizationSubscription;
import org.elearning.backend.subscription.entity.OrganizationSubscriptionStatus;
import org.elearning.backend.subscription.entity.SubscriptionPlan;
import org.elearning.backend.subscription.exception.ClassroomLimitExceededException;
import org.elearning.backend.subscription.exception.SubscriptionNotActiveException;
import org.elearning.backend.subscription.exception.UserLimitExceededException;
import org.elearning.backend.subscription.repository.OrganizationSubscriptionRepository;
import org.elearning.backend.user.entity.User;
import org.elearning.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntitlementServiceTest {

    @Mock
    private OrganizationSubscriptionRepository organizationSubscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClassroomRepository classroomRepository;

    @InjectMocks
    private EntitlementService entitlementService;

    private UUID organizationId;
    private SubscriptionPlan plan;
    private OrganizationSubscription activeSubscription;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();

        plan = new SubscriptionPlan();
        plan.setMaxUsers(10);
        plan.setMaxClassrooms(5);
        plan.setHasPremiumFeatures(true);

        Organization organization = new Organization();
        organization.setId(organizationId);

        activeSubscription = new OrganizationSubscription();
        activeSubscription.setOrganization(organization);
        activeSubscription.setSubscriptionPlan(plan);
        activeSubscription.setStatus(OrganizationSubscriptionStatus.ACTIVE);
    }

    @Test
    void hasActiveSubscription_whenActive_returnsTrue() {
        when(organizationSubscriptionRepository
                .findFirstByOrganizationIdAndStatusInOrderByCurrentPeriodEndDesc(eq(organizationId), any()))
                .thenReturn(Optional.of(activeSubscription));

        assertThat(entitlementService.hasActiveSubscription(organizationId)).isTrue();
    }

    @Test
    void hasActiveSubscription_whenNoSubscription_returnsFalse() {
        when(organizationSubscriptionRepository
                .findFirstByOrganizationIdAndStatusInOrderByCurrentPeriodEndDesc(eq(organizationId), any()))
                .thenReturn(Optional.empty());

        assertThat(entitlementService.hasActiveSubscription(organizationId)).isFalse();
    }

    @Test
    void hasFeatureAccess_whenPremiumPlan_returnsTrue() {
        plan.setHasPremiumFeatures(true);
        when(organizationSubscriptionRepository
                .findFirstByOrganizationIdAndStatusInOrderByCurrentPeriodEndDesc(eq(organizationId), any()))
                .thenReturn(Optional.of(activeSubscription));

        assertThat(entitlementService.hasFeatureAccess(organizationId)).isTrue();
    }

    @Test
    void hasFeatureAccess_whenNonPremiumPlan_returnsFalse() {
        plan.setHasPremiumFeatures(false);
        when(organizationSubscriptionRepository
                .findFirstByOrganizationIdAndStatusInOrderByCurrentPeriodEndDesc(eq(organizationId), any()))
                .thenReturn(Optional.of(activeSubscription));

        assertThat(entitlementService.hasFeatureAccess(organizationId)).isFalse();
    }

    @Test
    void hasFeatureAccess_whenNoSubscription_returnsFalse() {
        when(organizationSubscriptionRepository
                .findFirstByOrganizationIdAndStatusInOrderByCurrentPeriodEndDesc(eq(organizationId), any()))
                .thenReturn(Optional.empty());

        assertThat(entitlementService.hasFeatureAccess(organizationId)).isFalse();
    }

    @Test
    void canCreateUser_whenBelowLimit_doesNotThrow() {
        plan.setMaxUsers(10);
        when(organizationSubscriptionRepository
                .findFirstByOrganizationIdAndStatusInOrderByCurrentPeriodEndDesc(eq(organizationId), any()))
                .thenReturn(Optional.of(activeSubscription));
        when(userRepository.findByOrganizationId(organizationId))
                .thenReturn(buildUsers(5));

        entitlementService.canCreateUser(organizationId);

        verify(userRepository).findByOrganizationId(organizationId);
    }

    @Test
    void canCreateUser_whenAtLimit_throwsUserLimitExceededException() {
        plan.setMaxUsers(5);
        when(organizationSubscriptionRepository
                .findFirstByOrganizationIdAndStatusInOrderByCurrentPeriodEndDesc(eq(organizationId), any()))
                .thenReturn(Optional.of(activeSubscription));
        when(userRepository.findByOrganizationId(organizationId))
                .thenReturn(buildUsers(5));

        assertThatThrownBy(() -> entitlementService.canCreateUser(organizationId))
                .isInstanceOf(UserLimitExceededException.class)
                .hasMessageContaining("5");
    }

    @Test
    void canCreateUser_whenNoActiveSubscription_throwsSubscriptionNotActiveException() {
        when(organizationSubscriptionRepository
                .findFirstByOrganizationIdAndStatusInOrderByCurrentPeriodEndDesc(eq(organizationId), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> entitlementService.canCreateUser(organizationId))
                .isInstanceOf(SubscriptionNotActiveException.class);
    }

    @Test
    void canCreateUsers_whenBulkFitsWithinLimit_doesNotThrow() {
        plan.setMaxUsers(10);
        when(organizationSubscriptionRepository
                .findFirstByOrganizationIdAndStatusInOrderByCurrentPeriodEndDesc(eq(organizationId), any()))
                .thenReturn(Optional.of(activeSubscription));
        when(userRepository.findByOrganizationId(organizationId))
                .thenReturn(buildUsers(3));

        entitlementService.canCreateUsers(organizationId, 5);

        verify(userRepository).findByOrganizationId(organizationId);
    }

    @Test
    void canCreateUsers_whenBulkExceedsLimit_throwsUserLimitExceededException() {
        plan.setMaxUsers(10);
        when(organizationSubscriptionRepository
                .findFirstByOrganizationIdAndStatusInOrderByCurrentPeriodEndDesc(eq(organizationId), any()))
                .thenReturn(Optional.of(activeSubscription));
        when(userRepository.findByOrganizationId(organizationId))
                .thenReturn(buildUsers(8));

        assertThatThrownBy(() -> entitlementService.canCreateUsers(organizationId, 5))
                .isInstanceOf(UserLimitExceededException.class)
                .hasMessageContaining("10");
    }

    @Test
    void canCreateClassroom_whenBelowLimit_doesNotThrow() {
        plan.setMaxClassrooms(5);
        when(organizationSubscriptionRepository
                .findFirstByOrganizationIdAndStatusInOrderByCurrentPeriodEndDesc(eq(organizationId), any()))
                .thenReturn(Optional.of(activeSubscription));
        when(classroomRepository.findAllByOrganizationIdOrderByNameAsc(organizationId))
                .thenReturn(buildClassrooms(3));

        entitlementService.canCreateClassroom(organizationId);

        verify(classroomRepository).findAllByOrganizationIdOrderByNameAsc(organizationId);
    }

    @Test
    void canCreateClassroom_whenAtLimit_throwsClassroomLimitExceededException() {
        plan.setMaxClassrooms(3);
        when(organizationSubscriptionRepository
                .findFirstByOrganizationIdAndStatusInOrderByCurrentPeriodEndDesc(eq(organizationId), any()))
                .thenReturn(Optional.of(activeSubscription));
        when(classroomRepository.findAllByOrganizationIdOrderByNameAsc(organizationId))
                .thenReturn(buildClassrooms(3));

        assertThatThrownBy(() -> entitlementService.canCreateClassroom(organizationId))
                .isInstanceOf(ClassroomLimitExceededException.class)
                .hasMessageContaining("3");
    }

    @Test
    void canCreateClassroom_whenNoActiveSubscription_throwsSubscriptionNotActiveException() {
        when(organizationSubscriptionRepository
                .findFirstByOrganizationIdAndStatusInOrderByCurrentPeriodEndDesc(eq(organizationId), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> entitlementService.canCreateClassroom(organizationId))
                .isInstanceOf(SubscriptionNotActiveException.class);
    }

    private List<User> buildUsers(int count) {
        return Collections.nCopies(count, new User());
    }

    private List<Classroom> buildClassrooms(int count) {
        return Collections.nCopies(count, new Classroom());
    }
}
