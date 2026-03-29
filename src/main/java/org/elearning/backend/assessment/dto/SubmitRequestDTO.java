// dto/SubmitRequestDTO.java
package org.elearning.backend.assessment.dto;

import lombok.*;
import java.util.List;

/** Body-ul complet al request-ului de submit */
@Getter @Setter
public class SubmitRequestDTO {
    private List<SubmitAnswerDTO> answers;
}