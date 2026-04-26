package org.elearning.backend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class ClientResultDto {
    private String mlExerciseId;
    private boolean correct;
    private double score;
    private List<String> correctAnswers;
    private List<String> givenAnswers;
}
