package com.testifyai.crypto.controller;

import com.testifyai.crypto.dto.DepositBackingRequest;
import com.testifyai.crypto.dto.MintRewardsRequest;
import com.testifyai.crypto.dto.RedeemRequest;
import com.testifyai.crypto.dto.RewardStatsResponse;
import com.testifyai.crypto.dto.TransactionResponse;
import com.testifyai.crypto.service.TaiRewardBlockchainService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.Map;

@RestController
@RequestMapping("/api/crypto/rewards")
public class TaiRewardController {

    private final TaiRewardBlockchainService taiRewardBlockchainService;

    public TaiRewardController(TaiRewardBlockchainService taiRewardBlockchainService) {
        this.taiRewardBlockchainService = taiRewardBlockchainService;
    }

    @PostMapping("/backing/deposit")
    public TransactionResponse depositBacking(@Valid @RequestBody DepositBackingRequest request) {
        TransactionReceipt receipt = taiRewardBlockchainService.depositBacking(request.getAmount());

        return new TransactionResponse(
                receipt.getTransactionHash(),
                receipt.getStatus()
        );
    }

    @PostMapping("/mint")
    public TransactionResponse mintRewards(@Valid @RequestBody MintRewardsRequest request) {
        TransactionReceipt receipt = taiRewardBlockchainService.mintRewards(request);

        return new TransactionResponse(
                receipt.getTransactionHash(),
                receipt.getStatus()
        );
    }

    @PostMapping("/redeem")
    public TransactionResponse redeem(@Valid @RequestBody RedeemRequest request) {
        TransactionReceipt receipt = taiRewardBlockchainService.redeemWithSignature(
                request.getUser(),
                request.getRecipient(),
                request.getAmount(),
                request.getDeadline(),
                request.getSignature()
        );

        return new TransactionResponse(
                receipt.getTransactionHash(),
                receipt.getStatus()
        );
    }

    @GetMapping("/stats")
    public RewardStatsResponse getStats() {
        return taiRewardBlockchainService.getRewardStats();
    }

    @GetMapping("/nonce/{walletAddress}")
    public Map<String, BigInteger> getNonce(@PathVariable String walletAddress) {
        return Map.of(
                "nonce",
                taiRewardBlockchainService.getNonce(walletAddress)
        );
    }
}