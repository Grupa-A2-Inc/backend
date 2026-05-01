package org.elearning.backend.feedback.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class RateLessonResponseDto {
    private UUID lessonId;
    private String lessonTitle;
    private int myRating;
    private String myComment;
    private double avgRating;
    private int totalRatings;
}
