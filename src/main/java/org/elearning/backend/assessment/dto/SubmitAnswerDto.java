// dto/SubmitAnswerDTO.java
package org.elearning.backend.assessment.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
public class SubmitAnswerDto {
    private int questionId;
    private List<Integer> selectedOptionIds;
    private BigDecimal timeSpent;
}