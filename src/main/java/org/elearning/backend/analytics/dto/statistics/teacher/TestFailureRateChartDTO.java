package org.elearning.backend.analytics.dto.statistics.teacher;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Setter
@Getter
public class TestFailureRateChartDTO {
    List<FailureRateChartPointDTO> failureRatePoints;
}
