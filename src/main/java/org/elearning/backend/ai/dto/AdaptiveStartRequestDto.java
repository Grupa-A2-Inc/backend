package org.elearning.backend.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "AdaptiveStartRequest", description = "Request payload used to generate a full adaptive exercise set for one student.")
public class AdaptiveStartRequestDto {
    @Schema(description = "Curriculum subject identifier used by the AI service.", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer subjectId;

    @Schema(description = "Curriculum topic identifier inside the selected subject.", example = "1102", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer topicId;

    @Schema(description = "Total number of exercises requested for the adaptive session or adaptive job.", example = "12", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private int count;
}
