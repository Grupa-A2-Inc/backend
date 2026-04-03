package org.elearning.backend.assessment.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class QuestionOptionsDataDto {
    private int id;
    private String text;
    private Integer displayOrder;
}
