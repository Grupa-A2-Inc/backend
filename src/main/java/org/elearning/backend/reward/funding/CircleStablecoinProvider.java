package org.elearning.backend.reward.funding;

import org.elearning.backend.reward.exception.RewardBadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
public class CircleStablecoinProvider implements StablecoinProvider {

    private final StablecoinProviderProperties properties;
    private final RestClient.Builder restClientBuilder;

    public CircleStablecoinProvider(
            StablecoinProviderProperties properties,
            RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;
        this.restClientBuilder = restClientBuilder;
    }

    @Override
    public StablecoinFundingResult fundPlatformWallet(
            UUID organizationId,
            BigDecimal paymentAmount,
            BigDecimal eurcAmount,
            String idempotencyKey
    ) {
        validateConfigured();

        Map<String, Object> request = Map.of(
                "idempotencyKey", idempotencyKey,
                "source", Map.of(
                        "type", "wallet",
                        "id", properties.getCircle().getSourceWalletId()
                ),
                "destination", Map.of(
                        "type", "blockchain",
                        "address", properties.getCircle().getPlatformWalletAddress(),
                        "chain", "ETH"
                ),
                "amount", Map.of(
                        "amount", eurcAmount.toPlainString(),
                        "currency", "EURC"
                ),
                "metadata", Map.of(
                        "organizationId", organizationId.toString(),
                        "paymentAmount", paymentAmount.toPlainString()
                )
        );

        Map<?, ?> response = restClientBuilder
                .baseUrl(properties.getCircle().getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getCircle().getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build()
                .post()
                .uri("/v1/transfers")
                .body(request)
                .retrieve()
                .body(Map.class);

        String transactionHash = extractTransactionHash(response);
        return new StablecoinFundingResult(eurcAmount, "circle", transactionHash);
    }

    private void validateConfigured() {
        if (!properties.isEnabled()) {
            throw new RewardBadRequestException("Stablecoin provider is disabled");
        }
        if (!"circle".equalsIgnoreCase(properties.getProvider())) {
            throw new RewardBadRequestException("Unsupported stablecoin provider: " + properties.getProvider());
        }
        if (!StringUtils.hasText(properties.getCircle().getApiKey())
                || !StringUtils.hasText(properties.getCircle().getSourceWalletId())
                || !StringUtils.hasText(properties.getCircle().getPlatformWalletAddress())) {
            throw new RewardBadRequestException("Circle stablecoin provider is not fully configured");
        }
    }

    private String extractTransactionHash(Map<?, ?> response) {
        if (response == null) {
            return null;
        }
        Object data = response.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            Object txHash = dataMap.get("transactionHash");
            if (txHash != null) {
                return txHash.toString();
            }
            Object id = dataMap.get("id");
            if (id != null) {
                return id.toString();
            }
        }
        return null;
    }
}
