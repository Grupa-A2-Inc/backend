package org.elearning.backend.subscription.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class SubscriptionPlanResponse {
    private UUID id;
    private String code;
    private String displayName;
    private Integer maxUsers;
    private Integer maxClassrooms;
    private Integer maxCourses;
    private Boolean hasPremiumFeatures;
    private BigDecimal priceMonthly;
    private String currency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
