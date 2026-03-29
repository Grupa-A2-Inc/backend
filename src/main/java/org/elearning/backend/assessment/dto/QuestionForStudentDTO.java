// dto/QuestionForStudentDTO.java
package org.elearning.backend.assessment.dto;

import lombok.*;
import java.util.List;

/** Întrebare trimisă elevului — FĂRĂ is_correct */
@Getter @Setter @Builder
public class QuestionForStudentDTO {
    private int questionId;
    private String questionType;
    private String content;
    private List<OptionForStudentDTO> options;
}