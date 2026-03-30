// dto/OptionForStudentDTO.java
package org.elearning.backend.assessment.dto;

import lombok.*;

/** Opțiune trimisă elevului — FĂRĂ is_correct */
@Getter @Setter @Builder
public class OptionForStudentDTO {
    private int optionId;
    private String text;
    private int displayOrder;
}