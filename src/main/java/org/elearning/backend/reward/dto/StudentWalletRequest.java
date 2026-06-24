package org.elearning.backend.reward.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentWalletRequest {
    @NotBlank
    private String walletAddress;
}
