package org.elearning.backend.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.elearning.backend.content.model.CourseStatus;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDto {
    private String title;
    private String description;
    private String category;
    private CourseStatus status;
    private UUID createdBy;
}