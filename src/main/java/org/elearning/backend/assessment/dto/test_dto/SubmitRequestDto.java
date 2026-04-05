// dto/SubmitRequestDTO.java
package org.elearning.backend.assessment.dto.test_dto;

import lombok.*;
import java.util.List;

@Getter @Setter
public class SubmitRequestDto {
    private List<SubmitAnswerDto> answers;
}