package org.elearning.backend.assessment.dto.assigment_dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = false)
public class TestEditDto {
    private String title;
    private String description;
    private Integer timeLimitSec;
    private Boolean aiEnabled;
}
