package org.elearning.backend.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Setter
@NoArgsConstructor
public class MyClassTestBestResultsDto {
    private UUID studentId;
    private BigDecimal bestScorePercentage;
}
