package org.elearning.backend.classroom.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomCourseResponse {
    private UUID id;
    private UUID classroomId;
    private UUID courseId;
    private LocalDateTime assignedAt;
}