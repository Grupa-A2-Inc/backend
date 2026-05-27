package org.elearning.backend.reward.service;

import com.testifyai.crypto.config.CryptoProperties;
import com.testifyai.crypto.service.Erc20Service;
import com.testifyai.crypto.service.TaiRewardBlockchainService;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.reward.dto.funding.StablecoinFundingResponse;
import org.elearning.backend.reward.dto.funding.StablecoinPaymentRequest;
import org.elearning.backend.reward.entity.RewardCycle;
import org.elearning.backend.reward.entity.RewardCycleStatus;
import org.elearning.backend.reward.exception.RewardBadRequestException;
import org.elearning.backend.reward.exception.RewardConflictException;
import org.elearning.backend.reward.exception.RewardNotFoundException;
import org.elearning.backend.reward.funding.StablecoinFundingResult;
import org.elearning.backend.reward.funding.StablecoinProvider;
import org.elearning.backend.reward.funding.StablecoinProviderProperties;
import org.elearning.backend.reward.funding.CircleFaucetClient;
import org.elearning.backend.reward.repository.RewardCycleRepository;
import org.elearning.backend.reward.repository.StudentRewardRepository;
import org.elearning.backend.subscription.entity.OrganizationSubscription;
import org.elearning.backend.subscription.entity.OrganizationSubscriptionStatus;
import org.elearning.backend.subscription.repository.OrganizationSubscriptionRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RewardFundingService {

    private static final long ETHEREUM_MAINNET_CHAIN_ID = 1L;
    private static final long SEPOLIA_CHAIN_ID = 11155111L;
    private static final List<OrganizationSubscriptionStatus> ACTIVE_STATUSES = List.of(
            OrganizationSubscriptionStatus.ACTIVE,
            OrganizationSubscriptionStatus.TRIALING
    );

    private final OrganizationRepository organizationRepository;
    private final OrganizationSubscriptionRepository organizationSubscriptionRepository;
    private final RewardCycleRepository rewardCycleRepository;
    private final StudentRewardRepository studentRewardRepository;
    private final RewardDistributionService rewardDistributionService;
    private final StablecoinProvider stablecoinProvider;
    private final CircleFaucetClient circleFaucetClient;
    private final StablecoinProviderProperties stablecoinProviderProperties;
    private final CryptoProperties cryptoProperties;
    private final ObjectProvider<TaiRewardBlockchainService> taiRewardBlockchainServiceProvider;
    private final ObjectProvider<Erc20Service> erc20ServiceProvider;

    @Transactional
    public StablecoinFundingResponse mockSepoliaPayment(UUID organizationId, StablecoinPaymentRequest request) {
        if (cryptoProperties.getChainId() != SEPOLIA_CHAIN_ID) {
            throw new RewardBadRequestException("Mock reward payments are only available on Sepolia");
        }
        if (!stablecoinProviderProperties.isSepoliaMockPaymentsEnabled()) {
            throw new RewardBadRequestException("Sepolia mock reward payments are disabled");
        }

        BigDecimal rewardPoolAmount = rewardDistributionService.calculateRewardPoolAmount(request.getAmount());
        if (stablecoinProviderProperties.isSepoliaRealisticFundingEnabled()) {
            StablecoinFundingResult result;
            try {
                result = fundSepoliaWithRealEurc(
                        organizationId,
                        request.getAmount(),
                        rewardPoolAmount
                );
            } catch (RuntimeException exception) {
                if (!stablecoinProviderProperties.isSepoliaFallbackToMockEnabled()) {
                    throw exception;
                }
                result = null;
            }
            if (result != null) {
                RewardCycle cycle = fundCycle(organizationId, request, rewardPoolAmount, result);
                return toResponse(cycle, request.getAmount(), result.provider());
            }
        }

        String txHash = "mock-sepolia-" + UUID.randomUUID();
        RewardCycle cycle = fundCycle(
                organizationId,
                request,
                rewardPoolAmount,
                new StablecoinFundingResult(rewardPoolAmount, "sepolia-mock-fallback", txHash)
        );
        return toResponse(cycle, request.getAmount(), "sepolia-mock-fallback");
    }

    @Transactional
    public StablecoinFundingResponse fundWithStablecoinProvider(UUID organizationId, StablecoinPaymentRequest request) {
        if (cryptoProperties.getChainId() != ETHEREUM_MAINNET_CHAIN_ID) {
            throw new RewardBadRequestException("Real stablecoin funding is only enabled for Ethereum mainnet");
        }

        BigDecimal rewardPoolAmount = rewardDistributionService.calculateRewardPoolAmount(request.getAmount());
        String idempotencyKey = StringUtils.hasText(request.getExternalPaymentReference())
                ? request.getExternalPaymentReference()
                : organizationId + "-" + UUID.randomUUID();
        StablecoinFundingResult result = stablecoinProvider.fundPlatformWallet(
                organizationId,
                request.getAmount(),
                rewardPoolAmount,
                idempotencyKey
        );
        RewardCycle cycle = fundCycle(organizationId, request, rewardPoolAmount, result);
        return toResponse(cycle, request.getAmount(), result.provider());
    }

    private StablecoinFundingResult fundSepoliaWithRealEurc(
            UUID organizationId,
            BigDecimal paymentAmount,
            BigDecimal rewardPoolAmount
    ) {
        Erc20Service erc20Service = erc20ServiceProvider.getIfAvailable();
        TaiRewardBlockchainService blockchainService = taiRewardBlockchainServiceProvider.getIfAvailable();
        if (erc20Service == null || blockchainService == null) {
            throw new RewardBadRequestException("Crypto services are not configured for Sepolia realistic funding");
        }

        String platformWalletAddress = erc20Service.getPlatformWalletAddress();
        circleFaucetClient.requestSepoliaEurc(platformWalletAddress);
        waitForEurcBalance(erc20Service, rewardPoolAmount);

        String depositTxHash = blockchainService.depositBacking(rewardPoolAmount).getTransactionHash();
        return new StablecoinFundingResult(rewardPoolAmount, "circle-sepolia-faucet", depositTxHash);
    }

    private void waitForEurcBalance(Erc20Service erc20Service, BigDecimal requiredAmount) {
        RuntimeException lastException = null;
        for (int attempt = 0; attempt < stablecoinProviderProperties.getSepoliaFundingWaitAttempts(); attempt++) {
            try {
                BigDecimal balance = erc20Service.getPlatformEurcBalance();
                if (balance.compareTo(requiredAmount) >= 0) {
                    return;
                }
            } catch (RuntimeException exception) {
                lastException = exception;
            }
            sleepBeforeNextAttempt();
        }

        if (lastException != null) {
            throw lastException;
        }
        throw new RewardBadRequestException("Circle faucet did not provide enough Sepolia EURC for the reward pool");
    }

    private void sleepBeforeNextAttempt() {
        try {
            Thread.sleep(stablecoinProviderProperties.getSepoliaFundingWaitMs());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RewardBadRequestException("Interrupted while waiting for Sepolia EURC funding");
        }
    }

    private RewardCycle fundCycle(
            UUID organizationId,
            StablecoinPaymentRequest request,
            BigDecimal rewardPoolAmount,
            StablecoinFundingResult result
    ) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new RewardNotFoundException("Organization not found: " + organizationId);
        }

        OrganizationSubscription subscription = getActiveSubscription(organizationId);
        LocalDateTime periodStart = request.getPeriodStart() != null
                ? request.getPeriodStart()
                : subscription.getCurrentPeriodStart();
        LocalDateTime periodEnd = request.getPeriodEnd() != null
                ? request.getPeriodEnd()
                : subscription.getCurrentPeriodEnd();
        validatePeriod(periodStart, periodEnd);

        RewardCycle cycle = rewardCycleRepository.findByOrganizationIdAndPeriodStartAndPeriodEnd(
                        organizationId,
                        periodStart,
                        periodEnd
                )
                .orElseGet(RewardCycle::new);
        if (RewardCycleStatus.MINTED.equals(cycle.getStatus())) {
            throw new RewardConflictException("Reward cycle was already minted");
        }
        if (cycle.getId() != null) {
            studentRewardRepository.deleteAllByRewardCycleId(cycle.getId());
        }

        cycle.setOrganizationId(organizationId);
        cycle.setPeriodStart(periodStart);
        cycle.setPeriodEnd(periodEnd);
        cycle.setSubscriptionAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
        cycle.setRewardPoolAmount(rewardPoolAmount);
        cycle.setEurcDepositedAmount(result.eurcAmount().setScale(6, RoundingMode.DOWN));
        cycle.setDepositTxHash(result.transactionHash());
        cycle.setStatus(RewardCycleStatus.FUNDED);
        cycle.setFailureReason(null);
        return rewardCycleRepository.save(cycle);
    }

    private OrganizationSubscription getActiveSubscription(UUID organizationId) {
        return organizationSubscriptionRepository
                .findFirstByOrganizationIdAndStatusInOrderByCurrentPeriodEndDesc(organizationId, ACTIVE_STATUSES)
                .orElseThrow(() -> new RewardBadRequestException("Organization does not have an active subscription"));
    }

    private void validatePeriod(LocalDateTime periodStart, LocalDateTime periodEnd) {
        if (periodStart == null || periodEnd == null) {
            throw new RewardBadRequestException("Reward period start and end are required");
        }
        if (!periodEnd.isAfter(periodStart)) {
            throw new RewardBadRequestException("Reward period end must be after start");
        }
    }

    private StablecoinFundingResponse toResponse(RewardCycle cycle, BigDecimal paymentAmount, String provider) {
        return new StablecoinFundingResponse(
                cycle.getId(),
                cycle.getOrganizationId(),
                paymentAmount.setScale(2, RoundingMode.HALF_UP),
                cycle.getRewardPoolAmount(),
                cycle.getEurcDepositedAmount(),
                provider,
                cryptoProperties.getChainId(),
                cycle.getDepositTxHash(),
                cycle.getStatus().name()
        );
    }
}
