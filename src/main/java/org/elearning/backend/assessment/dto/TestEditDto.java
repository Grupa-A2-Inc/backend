package org.elearning.backend.assessment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = false)
public class TestEditDto {
    private String title;
    private String description;
    private Integer timeLimitSec;
    private Boolean aiEnabled;
}
