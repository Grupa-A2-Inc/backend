package com.testifyai.crypto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class StudentRewardRequest {

    @NotBlank
    private String studentWalletAddress;

    @NotNull
    @Positive
    private BigDecimal amount;

    public String getStudentWalletAddress() {
        return studentWalletAddress;
    }

    public void setStudentWalletAddress(String studentWalletAddress) {
        this.studentWalletAddress = studentWalletAddress;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}