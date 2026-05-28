package org.elearning.backend.reward.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.BigInteger;

@Getter
@Setter
public class StudentRedeemRequest {
    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private BigInteger deadline;

    @NotBlank
    private String signature;
}
