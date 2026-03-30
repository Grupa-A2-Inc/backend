// dto/QuestionForStudentDTO.java
package org.elearning.backend.assessment.dto;

import lombok.*;
import org.elearning.backend.assessment.model.QuestionType;

import java.util.List;

@Getter @Setter
public class QuestionForStudentDTO {
    private int questionId;
    private QuestionType questionType;
    private String content;
    private List<OptionForStudentDTO> options;
}