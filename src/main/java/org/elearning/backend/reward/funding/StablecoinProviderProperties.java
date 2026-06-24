package org.elearning.backend.reward.funding;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "stablecoin")
public class StablecoinProviderProperties {

    private String provider = "circle";
    private boolean enabled = false;
    private boolean sepoliaMockPaymentsEnabled = true;
    private boolean sepoliaRealisticFundingEnabled = false;
    private boolean sepoliaFallbackToMockEnabled = true;
    private int sepoliaFundingWaitAttempts = 6;
    private long sepoliaFundingWaitMs = 5000;

    private final Circle circle = new Circle();

    @Getter
    @Setter
    public static class Circle {
        private String apiBaseUrl = "https://api.circle.com";
        private String apiKey;
        private String sourceWalletId;
        private String eurcTokenId;
        private String platformWalletAddress;
        private boolean faucetEnabled = false;
        private String faucetBlockchain = "ETH-SEPOLIA";
    }
}
