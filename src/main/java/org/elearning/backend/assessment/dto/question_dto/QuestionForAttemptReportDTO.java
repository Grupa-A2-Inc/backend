package org.elearning.backend.assessment.dto.question_dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.elearning.backend.assessment.model.QuestionType;

import java.util.List;

@Setter
@Getter
@Builder
public class QuestionForAttemptReportDTO {
    private int questionId;
    private QuestionType questionType;
    private String content;
    private List<Integer> selectedOptionIds;
    private List<Integer> correctOptionIds;
}
