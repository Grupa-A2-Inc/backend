package org.elearning.backend.ai.dto;

import lombok.Data;
import org.elearning.backend.assessment.model.QuestionType;

import java.util.List;

@Data
public class AiQuestionDto {
    private String text;
    private QuestionType type;
    private List<String> answers;
    private List<String> correctAnswers;
    private Double difficulty;
}