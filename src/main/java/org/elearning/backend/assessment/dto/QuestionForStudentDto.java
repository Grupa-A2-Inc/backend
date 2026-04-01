// dto/QuestionForStudentDTO.java
package org.elearning.backend.assessment.dto;

import lombok.*;
import org.elearning.backend.assessment.model.QuestionType;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
public class QuestionForStudentDto {
    private int questionId;
    private QuestionType questionType;
    private String content;
    private BigDecimal difficulty;
    private List<OptionForStudentDto> options;
}