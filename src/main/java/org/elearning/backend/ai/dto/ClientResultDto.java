package org.elearning.backend.ai.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Schema(name = "AdaptiveExerciseResult", description = "Evaluation result for one adaptive exercise after submission.")
public class ClientResultDto {
    @Schema(description = "Exercise identifier.", example = "ex-1")
    private String mlExerciseId;

    @Schema(description = "True when the exercise was answered fully correctly.", example = "false")
    private boolean correct;

    @Schema(description = "Score for this exercise. Depending on type and partial scoring rules, this can be 0.0, 0.5 or 1.0.", example = "0.5")
    private double score;

    @ArraySchema(schema = @Schema(example = "Paris"), arraySchema = @Schema(description = "Correct answers stored for the exercise."))
    private List<String> correctAnswers;

    @ArraySchema(schema = @Schema(example = "Lyon"), arraySchema = @Schema(description = "Answers submitted by the student."))
    private List<String> givenAnswers;
}
