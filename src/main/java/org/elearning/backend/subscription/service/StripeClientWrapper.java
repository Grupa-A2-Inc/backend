package org.elearning.backend.subscription.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Component;

@Component
public class StripeClientWrapper {

    public Session createCheckoutSession(SessionCreateParams params) throws StripeException {
        return Session.create(params);
    }

    public Event constructWebhookEvent(String payload, String sigHeader, String secret)
            throws SignatureVerificationException {
        return Webhook.constructEvent(payload, sigHeader, secret);
    }
}