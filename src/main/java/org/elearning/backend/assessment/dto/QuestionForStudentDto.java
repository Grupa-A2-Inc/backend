// dto/QuestionForStudentDTO.java
package org.elearning.backend.assessment.dto;

import lombok.*;
import org.elearning.backend.assessment.model.QuestionType;

import java.math.BigDecimal;
import java.util.List;

@AllArgsConstructor @NoArgsConstructor
@Getter @Setter
public class QuestionForStudentDto {
    private int questionId;
    private QuestionType questionType;
    private String content;
    private BigDecimal difficulty;
    private List<QuestionForStudentDto.OptionForStudentDto> options;

    @AllArgsConstructor @NoArgsConstructor
    @Getter @Setter
    public static class OptionForStudentDto {
        private int optionId;
        private String text;
        private int displayOrder;
    }
}