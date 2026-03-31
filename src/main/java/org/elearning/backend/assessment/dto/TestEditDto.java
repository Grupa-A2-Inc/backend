package org.elearning.backend.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TestEditDto {
    private String title;
    private String description;
    private Integer timeLimitSec;
    private Boolean aiEnabled;
}
