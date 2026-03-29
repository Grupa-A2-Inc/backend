// dto/SubmitAnswerDTO.java
package org.elearning.backend.assessment.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

/** Un răspuns din body-ul de submit */
@Getter @Setter
public class SubmitAnswerDTO {
    private Long questionId;
    private List<Long> selectedOptionIds;
    /** Timp în secunde — măsurat de frontend, stocat ca atare */
    private BigDecimal timeSpent;
}