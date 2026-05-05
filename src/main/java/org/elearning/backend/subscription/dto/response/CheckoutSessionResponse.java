package org.elearning.backend.subscription.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CheckoutSessionResponse {
    private String checkoutUrl;
    private String sessionId;
}