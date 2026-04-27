package org.elearning.backend.classroom.dto.response;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ClassroomCourseResponseTest {

    @Test
    void gettersAndSetters_workCorrectly() {
        UUID id = UUID.randomUUID();
        UUID classroomId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        LocalDateTime assignedAt = LocalDateTime.of(2026, 4, 24, 10, 0);

        ClassroomCourseResponse response = new ClassroomCourseResponse(id, classroomId, courseId, assignedAt);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getClassroomId()).isEqualTo(classroomId);
        assertThat(response.getCourseId()).isEqualTo(courseId);
        assertThat(response.getAssignedAt()).isEqualTo(assignedAt);
    }

    @Test
    void noArgsConstructor_createsDtoWithNullFields() {
        ClassroomCourseResponse response = new ClassroomCourseResponse();

        assertThat(response.getId()).isNull();
        assertThat(response.getClassroomId()).isNull();
        assertThat(response.getCourseId()).isNull();
        assertThat(response.getAssignedAt()).isNull();
    }
}