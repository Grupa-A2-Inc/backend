package org.elearning.backend.feedback.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LessonRatingSummaryDto {
    private UUID lessonId;
    private String lessonTitle;
    private Double avgRating;
    private Integer totalRatings;

    // --- For professor ---
    private Boolean belowThreshold;
    private Map<Integer, Long> distribution;
    private List<CommentDto> recentComments;

    // --- For student ---
    private Integer myRating;
    private String myComment;
}
