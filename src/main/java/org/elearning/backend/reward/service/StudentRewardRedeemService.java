package org.elearning.backend.reward.service;

import com.testifyai.crypto.config.CryptoProperties;
import com.testifyai.crypto.service.TaiRewardBlockchainService;
import com.testifyai.crypto.util.TokenAmountConverter;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.reward.dto.StudentRedeemQuoteResponse;
import org.elearning.backend.reward.dto.StudentRedeemRequest;
import org.elearning.backend.reward.dto.StudentRedeemResponse;
import org.elearning.backend.reward.entity.StudentWallet;
import org.elearning.backend.reward.exception.RewardBadRequestException;
import org.elearning.backend.reward.exception.RewardNotFoundException;
import org.elearning.backend.reward.repository.StudentWalletRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentRewardRedeemService {

    private static final long REDEEM_DEADLINE_SECONDS = 15 * 60;

    private final StudentWalletRepository studentWalletRepository;
    private final ObjectProvider<TaiRewardBlockchainService> taiRewardBlockchainServiceProvider;
    private final CryptoProperties cryptoProperties;

    @Transactional(readOnly = true)
    public StudentRedeemQuoteResponse createRedeemAllQuote(UUID studentId) {
        StudentWallet wallet = getVerifiedWallet(studentId);
        TaiRewardBlockchainService blockchainService = getBlockchainService();

        BigDecimal taiBalance = blockchainService.getTaiBalance(wallet.getWalletAddress());
        if (taiBalance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RewardBadRequestException("Student has no TAI balance to redeem");
        }

        BigInteger amountSmallestUnit = TokenAmountConverter.toSmallestUnit(taiBalance);
        BigInteger nonce = blockchainService.getNonce(wallet.getWalletAddress());
        BigInteger deadline = BigInteger.valueOf(Instant.now().plusSeconds(REDEEM_DEADLINE_SECONDS).getEpochSecond());

        return new StudentRedeemQuoteResponse(
                wallet.getWalletAddress(),
                wallet.getWalletAddress(),
                taiBalance,
                amountSmallestUnit.toString(),
                nonce.toString(),
                deadline.toString(),
                buildTypedData(wallet.getWalletAddress(), amountSmallestUnit, nonce, deadline)
        );
    }

    @Transactional
    public StudentRedeemResponse redeemAll(UUID studentId, StudentRedeemRequest request) {
        StudentWallet wallet = getVerifiedWallet(studentId);
        TaiRewardBlockchainService blockchainService = getBlockchainService();

        if (!EvmAddressValidator.isValid(wallet.getWalletAddress())) {
            throw new RewardBadRequestException("Student wallet address must be a valid EVM address");
        }
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RewardBadRequestException("Redeem amount must be greater than zero");
        }
        if (request.getDeadline().compareTo(BigInteger.valueOf(Instant.now().getEpochSecond())) <= 0) {
            throw new RewardBadRequestException("Redeem signature is expired");
        }
        if (!request.getSignature().matches("^0x[a-fA-F0-9]{130}$")) {
            throw new RewardBadRequestException("Redeem signature must be a valid EVM signature");
        }

        BigDecimal currentBalance = blockchainService.getTaiBalance(wallet.getWalletAddress());
        if (currentBalance.compareTo(request.getAmount()) != 0) {
            throw new RewardBadRequestException("Redeem all amount must match the current TAI balance");
        }

        TransactionReceipt receipt = blockchainService.redeemWithSignature(
                wallet.getWalletAddress(),
                wallet.getWalletAddress(),
                request.getAmount(),
                request.getDeadline(),
                request.getSignature()
        );

        return new StudentRedeemResponse(
                wallet.getWalletAddress(),
                request.getAmount(),
                receipt.getTransactionHash(),
                receipt.getStatus()
        );
    }

    private StudentWallet getVerifiedWallet(UUID studentId) {
        StudentWallet wallet = studentWalletRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RewardNotFoundException("Wallet not found for student: " + studentId));
        if (!Boolean.TRUE.equals(wallet.getVerified())) {
            throw new RewardBadRequestException("Student wallet must be verified before redeeming rewards");
        }
        return wallet;
    }

    private TaiRewardBlockchainService getBlockchainService() {
        TaiRewardBlockchainService blockchainService = taiRewardBlockchainServiceProvider.getIfAvailable();
        if (blockchainService == null) {
            throw new RewardBadRequestException("Crypto reward blockchain service is not configured");
        }
        return blockchainService;
    }

    private Map<String, Object> buildTypedData(
            String walletAddress,
            BigInteger amountSmallestUnit,
            BigInteger nonce,
            BigInteger deadline
    ) {
        return Map.of(
                "types", Map.of(
                        "EIP712Domain", List.of(
                                Map.of("name", "name", "type", "string"),
                                Map.of("name", "version", "type", "string"),
                                Map.of("name", "chainId", "type", "uint256"),
                                Map.of("name", "verifyingContract", "type", "address")
                        ),
                        "Redeem", List.of(
                                Map.of("name", "user", "type", "address"),
                                Map.of("name", "recipient", "type", "address"),
                                Map.of("name", "amount", "type", "uint256"),
                                Map.of("name", "nonce", "type", "uint256"),
                                Map.of("name", "deadline", "type", "uint256")
                        )
                ),
                "primaryType", "Redeem",
                "domain", Map.of(
                        "name", "TAIEngine",
                        "version", "1",
                        "chainId", cryptoProperties.getChainId(),
                        "verifyingContract", cryptoProperties.getTaiEngineAddress()
                ),
                "message", Map.of(
                        "user", walletAddress,
                        "recipient", walletAddress,
                        "amount", amountSmallestUnit.toString(),
                        "nonce", nonce.toString(),
                        "deadline", deadline.toString()
                )
        );
    }
}
