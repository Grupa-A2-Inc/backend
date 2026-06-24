package com.testifyai.crypto.dto;

public class TransactionResponse {

    private String transactionHash;
    private String status;

    public TransactionResponse(String transactionHash, String status) {
        this.transactionHash = transactionHash;
        this.status = status;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public String getStatus() {
        return status;
    }
}