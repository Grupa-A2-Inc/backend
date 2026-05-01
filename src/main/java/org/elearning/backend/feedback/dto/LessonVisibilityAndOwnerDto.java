package org.elearning.backend.feedback.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.elearning.backend.content.model.CourseVisibility;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class LessonVisibilityAndOwnerDto {
    private CourseVisibility courseVisibility;
    private UUID createdBy;
    private String title;
}
