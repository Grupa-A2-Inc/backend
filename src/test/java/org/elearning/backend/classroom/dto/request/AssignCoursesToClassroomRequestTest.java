package org.elearning.backend.classroom.dto.request;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@org.springframework.test.context.ActiveProfiles("test")
class AssignCoursesToClassroomRequestTest {

    @Test
    void gettersAndSetters_workCorrectly() {
        List<UUID> courseIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        AssignCoursesToClassroomRequest request = new AssignCoursesToClassroomRequest();
        request.setCourseIds(courseIds);

        assertThat(request.getCourseIds()).isEqualTo(courseIds);
    }

    @Test
    void noArgsConstructor_createsDtoWithNullFields() {
        AssignCoursesToClassroomRequest request = new AssignCoursesToClassroomRequest();
        assertThat(request.getCourseIds()).isNull();
    }
}
