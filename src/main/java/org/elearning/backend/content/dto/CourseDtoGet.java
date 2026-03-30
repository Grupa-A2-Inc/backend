package org.elearning.backend.content.dto;


import lombok.Data;
import java.util.UUID;
import org.elearning.backend.content.model.CourseStatus;
import org.elearning.backend.content.model.CourseVisibility;

@Data

public class CourseDtoGet {
    private UUID id;
    private String title;
    private String description;
    private String category;
    private CourseStatus status;
    private CourseVisibility visibility;
    private UUID createdBy;

}