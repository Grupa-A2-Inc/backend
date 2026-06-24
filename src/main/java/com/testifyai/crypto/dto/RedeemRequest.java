package com.testifyai.crypto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.math.BigInteger;

public class RedeemRequest {

    @NotBlank
    private String user;

    @NotBlank
    private String recipient;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private BigInteger deadline;

    @NotBlank
    private String signature;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigInteger getDeadline() {
        return deadline;
    }

    public void setDeadline(BigInteger deadline) {
        this.deadline = deadline;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}