package org.elearning.backend.ai.dto;

import lombok.Data;
import java.util.List;
import org.elearning.backend.assessment.model.QuestionType;

@Data
public class AiAdaptiveExerciseDto {
    private String exerciseId;
    private String text;
    private QuestionType type;
    private List<String> answers;

    private List<String> correctAnswers;

    private Double difficulty;
}