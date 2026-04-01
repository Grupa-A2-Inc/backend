// dto/SubmitAnswerDTO.java
package org.elearning.backend.assessment.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
public class SubmitAnswerDto {
    private Long questionId;
    private List<Long> selectedOptionIds;
    private BigDecimal timeSpent;
}