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
@Schema(name = "AdaptiveSubmitRequest", description = "Payload sent when the student submits the answers for an adaptive session.")
public class AdaptiveSubmitRequestDto {

    @ArraySchema(schema = @Schema(implementation = AnswerDto.class), arraySchema = @Schema(description = "One entry for each answered exercise. Missing exercises are treated as unanswered."))
    private List<AnswerDto> answers;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Schema(name = "AdaptiveSubmitAnswer", description = "Student answer payload for one adaptive exercise.")
    public static class AnswerDto {
        @Schema(description = "Exercise identifier received in the adaptive session payload.", example = "ex-1", requiredMode = Schema.RequiredMode.REQUIRED)
        private String exerciseId;

        @ArraySchema(schema = @Schema(example = "A"), arraySchema = @Schema(description = "Selected answers. For single choice and true/false send one value. For multiple choice send all selected values."))
        private List<String> givenAnswers;

        @Schema(description = "Time spent on this exercise, in seconds.", example = "18")
        private Integer timeSpent;
    }
}
