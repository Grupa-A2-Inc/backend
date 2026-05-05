package org.elearning.backend.subscription.service;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.subscription.dto.request.CheckoutRequest;
import org.elearning.backend.subscription.dto.response.CheckoutSessionResponse;
import org.elearning.backend.subscription.entity.SubscriptionPlan;
import org.elearning.backend.subscription.exception.StripeCheckoutException;
import org.elearning.backend.subscription.repository.SubscriptionPlanRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StripeService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final StripeClientWrapper stripeClientWrapper;

    public CheckoutSessionResponse createCheckoutSession(UUID organizationId, CheckoutRequest request) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(request.getPlanId())
                .orElseThrow(() -> new IllegalArgumentException("Subscription plan not found: " + request.getPlanId()));

        if (plan.getStripePriceId() == null) {
            throw new IllegalArgumentException("Plan " + plan.getCode() + " has no Stripe price configured");
        }

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(plan.getStripePriceId())
                                    .setQuantity(1L)
                                    .build()
                    )
                    .putMetadata("organizationId", organizationId.toString())
                    .putMetadata("planId", request.getPlanId().toString())
                    .setSuccessUrl(request.getSuccessUrl())
                    .setCancelUrl(request.getCancelUrl())
                    .build();

            Session session = stripeClientWrapper.createCheckoutSession(params);

            return new CheckoutSessionResponse(session.getUrl(), session.getId());

        } catch (StripeException e) {
            throw new StripeCheckoutException("Failed to create Stripe checkout session", e);
        }
    }
}
