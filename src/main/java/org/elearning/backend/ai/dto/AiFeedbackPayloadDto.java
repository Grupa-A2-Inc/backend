package org.elearning.backend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class AiFeedbackPayloadDto {
    private UUID studentId;
    private Integer subjectId;
    private Integer topicId;
    private List<FeedbackResultDto> mlFeedbackResults;
}
