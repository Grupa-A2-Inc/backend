package org.elearning.backend.reward;

import com.testifyai.crypto.config.CryptoProperties;
import com.testifyai.crypto.service.Erc20Service;
import com.testifyai.crypto.service.TaiRewardBlockchainService;
import org.elearning.backend.organization.repository.OrganizationRepository;
import org.elearning.backend.reward.dto.funding.StablecoinFundingResponse;
import org.elearning.backend.reward.dto.funding.StablecoinPaymentRequest;
import org.elearning.backend.reward.entity.RewardCycle;
import org.elearning.backend.reward.entity.RewardCycleStatus;
import org.elearning.backend.reward.exception.RewardBadRequestException;
import org.elearning.backend.reward.funding.CircleFaucetClient;
import org.elearning.backend.reward.funding.StablecoinProvider;
import org.elearning.backend.reward.funding.StablecoinProviderProperties;
import org.elearning.backend.reward.repository.RewardCycleRepository;
import org.elearning.backend.reward.repository.StudentRewardRepository;
import org.elearning.backend.reward.service.RewardDistributionService;
import org.elearning.backend.reward.service.RewardFundingService;
import org.elearning.backend.subscription.entity.OrganizationSubscription;
import org.elearning.backend.subscription.entity.OrganizationSubscriptionStatus;
import org.elearning.backend.subscription.repository.OrganizationSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardFundingServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationSubscriptionRepository organizationSubscriptionRepository;

    @Mock
    private RewardCycleRepository rewardCycleRepository;

    @Mock
    private StudentRewardRepository studentRewardRepository;

    @Mock
    private RewardDistributionService rewardDistributionService;

    @Mock
    private StablecoinProvider stablecoinProvider;

    @Mock
    private CircleFaucetClient circleFaucetClient;

    @Mock
    private ObjectProvider<TaiRewardBlockchainService> taiRewardBlockchainServiceProvider;

    @Mock
    private ObjectProvider<Erc20Service> erc20ServiceProvider;

    @Mock
    private TaiRewardBlockchainService taiRewardBlockchainService;

    @Mock
    private Erc20Service erc20Service;

    private StablecoinProviderProperties stablecoinProviderProperties;
    private CryptoProperties cryptoProperties;
    private RewardFundingService service;

    @BeforeEach
    void setUp() {
        stablecoinProviderProperties = new StablecoinProviderProperties();
        cryptoProperties = new CryptoProperties();
        cryptoProperties.setChainId(11155111L);
        service = new RewardFundingService(
                organizationRepository,
                organizationSubscriptionRepository,
                rewardCycleRepository,
                studentRewardRepository,
                rewardDistributionService,
                stablecoinProvider,
                circleFaucetClient,
                stablecoinProviderProperties,
                cryptoProperties,
                taiRewardBlockchainServiceProvider,
                erc20ServiceProvider
        );
    }

    @Test
    void mockSepoliaPaymentFundsTenPercentRewardPool() {
        UUID organizationId = UUID.randomUUID();
        StablecoinPaymentRequest request = new StablecoinPaymentRequest();
        request.setAmount(new BigDecimal("100.00"));

        when(rewardDistributionService.calculateRewardPoolAmount(new BigDecimal("100.00")))
                .thenReturn(new BigDecimal("10.000000"));
        when(organizationRepository.existsById(organizationId)).thenReturn(true);
        when(organizationSubscriptionRepository.findFirstByOrganizationIdAndStatusInOrderByCurrentPeriodEndDesc(eq(organizationId), any()))
                .thenReturn(Optional.of(activeSubscription()));
        when(rewardCycleRepository.findByOrganizationIdAndPeriodStartAndPeriodEnd(eq(organizationId), any(), any()))
                .thenReturn(Optional.empty());
        when(rewardCycleRepository.save(any(RewardCycle.class))).thenAnswer(invocation -> {
            RewardCycle cycle = invocation.getArgument(0);
            cycle.setId(UUID.randomUUID());
            return cycle;
        });

        StablecoinFundingResponse response = service.mockSepoliaPayment(organizationId, request);

        ArgumentCaptor<RewardCycle> cycleCaptor = ArgumentCaptor.forClass(RewardCycle.class);
        verify(rewardCycleRepository).save(cycleCaptor.capture());
        RewardCycle savedCycle = cycleCaptor.getValue();
        assertThat(savedCycle.getSubscriptionAmount()).isEqualByComparingTo("100.00");
        assertThat(savedCycle.getRewardPoolAmount()).isEqualByComparingTo("10.000000");
        assertThat(savedCycle.getEurcDepositedAmount()).isEqualByComparingTo("10.000000");
        assertThat(savedCycle.getStatus()).isEqualTo(RewardCycleStatus.FUNDED);
        assertThat(savedCycle.getDepositTxHash()).startsWith("mock-sepolia-");
        assertThat(response.getProvider()).isEqualTo("sepolia-mock-fallback");
    }

    @Test
    void mockSepoliaPaymentWithRealisticFundingRequestsFaucetAndDepositsBacking() {
        stablecoinProviderProperties.setSepoliaRealisticFundingEnabled(true);
        stablecoinProviderProperties.setSepoliaFundingWaitAttempts(1);
        stablecoinProviderProperties.setSepoliaFundingWaitMs(0);
        UUID organizationId = UUID.randomUUID();
        StablecoinPaymentRequest request = new StablecoinPaymentRequest();
        request.setAmount(new BigDecimal("100.00"));
        TransactionReceipt receipt = new TransactionReceipt();
        receipt.setTransactionHash("0xdeposit");

        when(rewardDistributionService.calculateRewardPoolAmount(new BigDecimal("100.00")))
                .thenReturn(new BigDecimal("10.000000"));
        when(erc20ServiceProvider.getIfAvailable()).thenReturn(erc20Service);
        when(taiRewardBlockchainServiceProvider.getIfAvailable()).thenReturn(taiRewardBlockchainService);
        when(erc20Service.getPlatformWalletAddress()).thenReturn("0x1111111111111111111111111111111111111111");
        when(erc20Service.getPlatformEurcBalance()).thenReturn(new BigDecimal("10.000000"));
        when(taiRewardBlockchainService.depositBacking(new BigDecimal("10.000000"))).thenReturn(receipt);
        when(organizationRepository.existsById(organizationId)).thenReturn(true);
        when(organizationSubscriptionRepository.findFirstByOrganizationIdAndStatusInOrderByCurrentPeriodEndDesc(eq(organizationId), any()))
                .thenReturn(Optional.of(activeSubscription()));
        when(rewardCycleRepository.findByOrganizationIdAndPeriodStartAndPeriodEnd(eq(organizationId), any(), any()))
                .thenReturn(Optional.empty());
        when(rewardCycleRepository.save(any(RewardCycle.class))).thenAnswer(invocation -> {
            RewardCycle cycle = invocation.getArgument(0);
            cycle.setId(UUID.randomUUID());
            return cycle;
        });

        StablecoinFundingResponse response = service.mockSepoliaPayment(organizationId, request);

        verify(circleFaucetClient).requestSepoliaEurc("0x1111111111111111111111111111111111111111");
        verify(taiRewardBlockchainService).depositBacking(new BigDecimal("10.000000"));
        assertThat(response.getProvider()).isEqualTo("circle-sepolia-faucet");
        assertThat(response.getTransactionHash()).isEqualTo("0xdeposit");
    }

    @Test
    void mockSepoliaPaymentRejectsNonSepoliaChain() {
        cryptoProperties.setChainId(1L);
        StablecoinPaymentRequest request = new StablecoinPaymentRequest();
        request.setAmount(new BigDecimal("100.00"));

        assertThatThrownBy(() -> service.mockSepoliaPayment(UUID.randomUUID(), request))
                .isInstanceOf(RewardBadRequestException.class)
                .hasMessageContaining("only available on Sepolia");
    }

    private OrganizationSubscription activeSubscription() {
        OrganizationSubscription subscription = new OrganizationSubscription();
        subscription.setStatus(OrganizationSubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodStart(LocalDateTime.of(2026, 5, 1, 0, 0));
        subscription.setCurrentPeriodEnd(LocalDateTime.of(2026, 6, 1, 0, 0));
        return subscription;
    }
}
