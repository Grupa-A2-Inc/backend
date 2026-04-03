package org.elearning.backend.assessment.dto;

import lombok.Getter;
import lombok.Setter;
import org.elearning.backend.assessment.model.QuestionType;
import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
public class QuestionRequestDto {
    private QuestionType questionType;
    private String content;
    private BigDecimal difficulty;

    // Lista de opțiuni trimise de profesor
    private List<OptionRequestDto> options;
}