// dto/OptionForStudentDTO.java
package org.elearning.backend.assessment.dto;

import lombok.*;

@Getter @Setter @Builder
public class OptionForStudentDto {
    private int optionId;
    private String text;
    private int displayOrder;
}