package org.elearning.backend.analytics.dto.statistics.teacher;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@AllArgsConstructor
@Setter
@Getter
public class FailureRateChartPointDTO {
    private LocalDate date;
    private Double dailyFailureRate;
}