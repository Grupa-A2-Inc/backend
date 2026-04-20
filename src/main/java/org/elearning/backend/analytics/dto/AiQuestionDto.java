package org.elearning.backend.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.elearning.backend.assessment.model.QuestionOption;
import org.elearning.backend.assessment.model.QuestionType;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class AiQuestionDto {
    private String text;
    private QuestionType type;
    private List<QuestionOption> options;
}
