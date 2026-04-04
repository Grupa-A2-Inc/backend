// dto/TestResultDTO.java
package org.elearning.backend.assessment.dto;

import lombok.*;
import org.elearning.backend.assessment.model.QuestionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor @NoArgsConstructor
@Getter @Setter
public class TestResultDto {
    private UUID attemptId;
    private BigDecimal score;
    private BigDecimal scorePercent;
    private boolean passed;
    private LocalDateTime completedAt;
    private List<TestResultQuestionDto> questions;

    @AllArgsConstructor @NoArgsConstructor
    @Getter @Setter
    public static class TestResultQuestionDto {
        private int questionId;
        private QuestionType questionType;
        private String content;
        private boolean isCorrect;
        private List<Integer> selectedOptionIds;
        private List<Integer> correctOptionIds;
    }
}