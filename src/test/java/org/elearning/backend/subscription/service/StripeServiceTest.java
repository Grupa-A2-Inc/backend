package org.elearning.backend.subscription.service;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.elearning.backend.subscription.config.StripeConfig;
import org.elearning.backend.subscription.dto.request.CheckoutRequest;
import org.elearning.backend.subscription.dto.response.CheckoutSessionResponse;
import org.elearning.backend.subscription.entity.SubscriptionPlan;
import org.elearning.backend.subscription.repository.SubscriptionPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class StripeServiceTest {

    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Mock
    private StripeConfig stripeConfig;

    @Mock
    private StripeClientWrapper stripeClientWrapper;

    @InjectMocks
    private StripeService stripeService;

    private SubscriptionPlan buildPlan(String stripePriceId) {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setId(UUID.randomUUID());
        plan.setCode("STARTER");
        plan.setDisplayName("Starter");
        plan.setMaxUsers(100);
        plan.setMaxClassrooms(5);
        plan.setHasPremiumFeatures(false);
        plan.setStripePriceId(stripePriceId);
        return plan;
    }

    private CheckoutRequest buildRequest(UUID planId) {
        CheckoutRequest request = new CheckoutRequest();
        request.setPlanId(planId);
        request.setSuccessUrl("https://example.com/success");
        request.setCancelUrl("https://example.com/cancel");
        return request;
    }

    @Test
    void createCheckoutSession_success() throws StripeException {
        SubscriptionPlan plan = buildPlan("price_123");
        CheckoutRequest request = buildRequest(plan.getId());
        UUID organizationId = UUID.randomUUID();

        when(subscriptionPlanRepository.findById(plan.getId())).thenReturn(Optional.of(plan));

        Session mockSession = mock(Session.class);
        when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/cs_test_123");
        when(mockSession.getId()).thenReturn("cs_test_123");
        when(stripeClientWrapper.createCheckoutSession(any(SessionCreateParams.class))).thenReturn(mockSession);

        CheckoutSessionResponse response = stripeService.createCheckoutSession(organizationId, request);

        assertThat(response.getCheckoutUrl()).isEqualTo("https://checkout.stripe.com/pay/cs_test_123");
        assertThat(response.getSessionId()).isEqualTo("cs_test_123");
    }

    @Test
    void createCheckoutSession_planNotFound_throwsException() {
        UUID planId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        CheckoutRequest request = buildRequest(planId);

        when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stripeService.createCheckoutSession(organizationId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Subscription plan not found");
    }

    @Test
    void createCheckoutSession_planHasNoStripePriceId_throwsException() {
        SubscriptionPlan plan = buildPlan(null);
        CheckoutRequest request = buildRequest(plan.getId());
        UUID organizationId = UUID.randomUUID();

        when(subscriptionPlanRepository.findById(plan.getId())).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> stripeService.createCheckoutSession(organizationId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("has no Stripe price configured");
    }

    @Test
    void createCheckoutSession_stripeThrowsException_throwsRuntimeException() throws StripeException {
        SubscriptionPlan plan = buildPlan("price_123");
        CheckoutRequest request = buildRequest(plan.getId());
        UUID organizationId = UUID.randomUUID();

        when(subscriptionPlanRepository.findById(plan.getId())).thenReturn(Optional.of(plan));
        when(stripeClientWrapper.createCheckoutSession(any(SessionCreateParams.class)))
                .thenThrow(mock(StripeException.class));

        assertThatThrownBy(() -> stripeService.createCheckoutSession(organizationId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create Stripe checkout session");
    }
}