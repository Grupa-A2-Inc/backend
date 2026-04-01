package org.elearning.backend.assessment.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.elearning.backend.assessment.model.QuestionType;

import java.util.List;

@Setter
@Getter
@Builder
public class QuestionForAttemptReportDTO {
    private Long questionId;
    private QuestionType questionType;
    private String content;
    private List<Long> selectedOptionIds;
    private List<Long> correctOptionIds;
}
