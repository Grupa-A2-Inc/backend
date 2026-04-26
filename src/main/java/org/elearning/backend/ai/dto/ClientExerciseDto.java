package org.elearning.backend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.elearning.backend.assessment.model.QuestionType;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientExerciseDto {
    private String exerciseId;
    private String text;
    private QuestionType type;
    private List<String> answers;
}
