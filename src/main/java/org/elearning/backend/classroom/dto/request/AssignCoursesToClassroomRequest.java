package org.elearning.backend.classroom.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class AssignCoursesToClassroomRequest {
    @NotNull(message = "Course ID list must not be null")
    @NotEmpty(message = "Course ID list must not be empty")
    private List<@NotNull(message = "Course ID must not be null") UUID> courseIds;
}