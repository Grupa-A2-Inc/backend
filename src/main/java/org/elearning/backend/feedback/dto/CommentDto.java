package org.elearning.backend.feedback.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class CommentDto {
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
