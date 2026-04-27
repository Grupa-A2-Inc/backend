package org.elearning.backend.ai.dto;

import lombok.Data;

@Data
public class AdaptiveStartRequestDto {
    private Integer subjectId;
    private Integer topicId;
    private int count;
}
