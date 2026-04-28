package org.elearning.backend.classroom.dto.response;

import lombok.Data;
import org.elearning.backend.content.model.CourseStatus;
import org.elearning.backend.content.model.CourseVisibility;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ClassroomCourseDetailsResponse {
    private UUID courseId;
    private String title;
    private String description;
    private String category;
    private CourseStatus status;
    private CourseVisibility visibility;
    private UUID createdBy;
    private LocalDateTime assignedAt;
}