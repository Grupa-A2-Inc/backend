package org.elearning.backend.subscription.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.Generated;
import org.springframework.stereotype.Component;

@Component
@Generated
public class StripeSdkClient {

    public Session createSession(SessionCreateParams params) throws StripeException {
        return Session.create(params);
    }

    public Event createWebhookEvent(String payload, String sigHeader, String secret)
            throws SignatureVerificationException {
        return Webhook.constructEvent(payload, sigHeader, secret);
    }
}
