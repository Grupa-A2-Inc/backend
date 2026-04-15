package org.elearning.backend.assessment.dto.question_option_dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class OptionResponseDto {
    private Long optionId;
    private String text;
    private Integer displayOrder;
    private Boolean isCorrect;
}