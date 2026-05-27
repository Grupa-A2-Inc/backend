package com.testifyai.crypto.service;

import com.testifyai.crypto.contracts.TAIEngine;
import com.testifyai.crypto.dto.MintRewardsRequest;
import com.testifyai.crypto.dto.RewardStatsResponse;
import com.testifyai.crypto.dto.StudentRewardRequest;
import com.testifyai.crypto.util.TokenAmountConverter;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

@Service
public class TaiRewardBlockchainService {

    private final TAIEngine taiEngine;
    private final Erc20Service erc20Service;

    public TaiRewardBlockchainService(
            TAIEngine taiEngine,
            Erc20Service erc20Service
    ) {
        this.taiEngine = taiEngine;
        this.erc20Service = erc20Service;
    }

    public TransactionReceipt depositBacking(BigDecimal amount) {
        BigInteger amountInSmallestUnit = TokenAmountConverter.toSmallestUnit(amount);

        erc20Service.approveTaiEngine(amountInSmallestUnit);

        try {
            return taiEngine.depositBacking(amountInSmallestUnit).send();
        } catch (Exception exception) {
            throw new RuntimeException("Could not deposit EURC backing", exception);
        }
    }

    public TransactionReceipt mintRewards(MintRewardsRequest request) {
        List<String> studentAddresses = request.getRewards()
                .stream()
                .map(StudentRewardRequest::getStudentWalletAddress)
                .toList();

        List<BigInteger> rewardAmounts = request.getRewards()
                .stream()
                .map(StudentRewardRequest::getAmount)
                .map(TokenAmountConverter::toSmallestUnit)
                .toList();

        try {
            return taiEngine.mintRewards(studentAddresses, rewardAmounts).send();
        } catch (Exception exception) {
            throw new RuntimeException("Could not mint TAI rewards", exception);
        }
    }

    public TransactionReceipt redeemWithSignature(
            String user,
            String recipient,
            BigDecimal amount,
            BigInteger deadline,
            String signature
    ) {
        BigInteger amountInSmallestUnit = TokenAmountConverter.toSmallestUnit(amount);
        byte[] signatureBytes = Numeric.hexStringToByteArray(signature);

        try {
            return taiEngine.redeemWithSignature(
                    user,
                    recipient,
                    amountInSmallestUnit,
                    deadline,
                    signatureBytes
            ).send();
        } catch (Exception exception) {
            throw new RuntimeException("Could not redeem TAI with signature", exception);
        }
    }

    public RewardStatsResponse getRewardStats() {
        try {
            BigInteger vaultBalance = taiEngine.getVaultBalance().send();
            BigInteger taiTotalSupply = taiEngine.getTaiTotalSupply().send();
            BigInteger availableToMint = taiEngine.getAvailableToMint().send();
            Boolean fullyBacked = taiEngine.isFullyBacked().send();

            return new RewardStatsResponse(
                    TokenAmountConverter.fromSmallestUnit(vaultBalance),
                    TokenAmountConverter.fromSmallestUnit(taiTotalSupply),
                    TokenAmountConverter.fromSmallestUnit(availableToMint),
                    fullyBacked
            );
        } catch (Exception exception) {
            throw new RuntimeException("Could not read reward stats", exception);
        }
    }

    public BigInteger getNonce(String userAddress) {
        try {
            return taiEngine.getNonce(userAddress).send();
        } catch (Exception exception) {
            throw new RuntimeException("Could not read nonce", exception);
        }
    }
}