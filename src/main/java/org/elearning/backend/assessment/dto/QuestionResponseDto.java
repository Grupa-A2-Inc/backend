package org.elearning.backend.assessment.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.elearning.backend.assessment.model.QuestionType;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder // Opțional, dar te va ajuta mult la mapare mai târziu
public class QuestionResponseDto {
    private Long questionId; // ID-ul generat de baza de date!
    private QuestionType questionType;
    private String content;
    private BigDecimal difficulty;

    // Lista de opțiuni salvate (care au și ele ID-uri acum)
    private List<OptionResponseDto> options;
}