package org.elearning.backend.reward.funding;

import org.elearning.backend.reward.exception.RewardBadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Service
public class CircleFaucetClient {

    private final StablecoinProviderProperties properties;
    private final RestClient.Builder restClientBuilder;

    public CircleFaucetClient(
            StablecoinProviderProperties properties,
            RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;
        this.restClientBuilder = restClientBuilder;
    }

    public void requestSepoliaEurc(String walletAddress) {
        validateConfigured(walletAddress);

        restClientBuilder
                .baseUrl(properties.getCircle().getApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getCircle().getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("X-Request-Id", UUID.randomUUID().toString())
                .build()
                .post()
                .uri("/v1/faucet/drips")
                .body(Map.of(
                        "address", walletAddress,
                        "blockchain", properties.getCircle().getFaucetBlockchain(),
                        "native", true,
                        "usdc", false,
                        "eurc", true
                ))
                .retrieve()
                .toBodilessEntity();
    }

    private void validateConfigured(String walletAddress) {
        if (!properties.getCircle().isFaucetEnabled()) {
            throw new RewardBadRequestException("Circle faucet is disabled");
        }
        if (!StringUtils.hasText(properties.getCircle().getApiKey())) {
            throw new RewardBadRequestException("Circle API key is required for Sepolia faucet funding");
        }
        if (!StringUtils.hasText(walletAddress)) {
            throw new RewardBadRequestException("Platform wallet address is required for Sepolia faucet funding");
        }
        if (!"ETH-SEPOLIA".equalsIgnoreCase(properties.getCircle().getFaucetBlockchain())) {
            throw new RewardBadRequestException("Only ETH-SEPOLIA faucet funding is supported for this demo flow");
        }
    }
}
