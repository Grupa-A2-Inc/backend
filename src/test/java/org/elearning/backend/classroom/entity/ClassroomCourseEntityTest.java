package org.elearning.backend.classroom.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@org.springframework.test.context.ActiveProfiles("test")
class ClassroomCourseEntityTest {

    @Test
    void gettersAndSetters_workCorrectly() {
        UUID id = UUID.randomUUID();
        UUID classroomId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        LocalDateTime assignedAt = LocalDateTime.of(2026, 4, 24, 10, 0);

        ClassroomCourse entity = new ClassroomCourse();
        entity.setId(id);
        entity.setClassroomId(classroomId);
        entity.setCourseId(courseId);
        entity.setAssignedAt(assignedAt);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getClassroomId()).isEqualTo(classroomId);
        assertThat(entity.getCourseId()).isEqualTo(courseId);
        assertThat(entity.getAssignedAt()).isEqualTo(assignedAt);
    }

    @Test
    void noArgsConstructor_createsEntityWithNullFields() {
        ClassroomCourse entity = new ClassroomCourse();

        assertThat(entity.getId()).isNull();
        assertThat(entity.getClassroomId()).isNull();
        assertThat(entity.getCourseId()).isNull();
        assertThat(entity.getAssignedAt()).isNull();
    }
}
