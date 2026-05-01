package org.elearning.backend.feedback.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class RateLessonDto {
    @NotNull(message = "The rating field is required.")
    @Min(value = 1, message = "The rating must be at least 1 star.")
    @Max(value = 5, message = "The rating cannot exceed 5 stars.")
    private Integer rating;

    @Size(max = 1000, message = "The comment cannot exceed 1000 characters.")
    private String comment;
}
