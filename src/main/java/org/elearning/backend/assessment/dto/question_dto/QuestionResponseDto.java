package org.elearning.backend.assessment.dto.question_dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.elearning.backend.assessment.dto.question_option_dto.OptionResponseDto;
import org.elearning.backend.assessment.model.QuestionType;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
public class QuestionResponseDto {
    private Long questionId; // ID-ul generat de baza de date!
    private QuestionType questionType;
    private String content;
    private BigDecimal difficulty;

    // Lista de opțiuni salvate (care au și ele ID-uri acum)
    private List<OptionResponseDto> options;
}