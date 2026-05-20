package org.elearning.backend.ai.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.elearning.backend.assessment.model.QuestionType;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "AdaptiveExercise", description = "Exercise returned to the client when an adaptive session is ready.")
public class ClientExerciseDto {
    @Schema(description = "Exercise identifier that must be sent back at submission time.", example = "ex-1")
    private String exerciseId;

    @Schema(description = "Question statement shown to the student.", example = "What is the capital of France?")
    private String text;

    @Schema(description = "Question type.", example = "SINGLE_CHOICE", allowableValues = {"SINGLE_CHOICE", "MULTIPLE_CHOICE", "TRUE_FALSE"})
    private QuestionType type;

    @ArraySchema(schema = @Schema(example = "Paris"), arraySchema = @Schema(description = "Visible answer options shown to the student."))
    private List<String> answers;
}
