package org.elearning.backend.reward.dto.funding;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class StablecoinPaymentRequest {

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    private LocalDateTime periodStart;

    private LocalDateTime periodEnd;

    private String externalPaymentReference;
}
