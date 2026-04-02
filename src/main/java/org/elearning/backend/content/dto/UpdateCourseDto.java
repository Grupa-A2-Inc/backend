package org.elearning.backend.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.elearning.backend.content.model.CourseStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCourseDto {
    private String title;
    private String description;
    private String category;
    private CourseStatus status;
}