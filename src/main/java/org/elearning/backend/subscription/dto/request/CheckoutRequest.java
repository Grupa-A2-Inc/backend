package org.elearning.backend.subscription.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CheckoutRequest {

    @NotNull(message = "Plan ID is required")
    private UUID planId;

    @NotNull(message = "Success URL is required")
    private String successUrl;

    @NotNull(message = "Cancel URL is required")
    private String cancelUrl;
}