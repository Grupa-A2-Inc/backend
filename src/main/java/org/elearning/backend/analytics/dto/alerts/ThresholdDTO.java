package org.elearning.backend.analytics.dto.alerts;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Setter
@Getter
public class ThresholdDTO {
    private Double failureThreshold;
}
