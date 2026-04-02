// dto/SubmitRequestDTO.java
package org.elearning.backend.assessment.dto;

import lombok.*;
import java.util.List;

@Getter @Setter
public class SubmitRequestDto {
    private List<SubmitAnswerDto> answers;
}