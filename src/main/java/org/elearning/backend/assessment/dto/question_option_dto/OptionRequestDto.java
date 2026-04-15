package org.elearning.backend.assessment.dto.question_option_dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class OptionRequestDto {
    private String text;
    private Integer displayOrder;
    private Boolean isCorrect; // profesorul zice care e optiunea corecta
}