package org.elearning.backend.classroom.controller;

import org.elearning.backend.classroom.dto.request.AssignCoursesToClassroomRequest;
import org.elearning.backend.classroom.dto.request.CreateClassroomRequest;
import org.elearning.backend.classroom.dto.request.UpdateClassroomRequest;
import org.elearning.backend.classroom.dto.response.ClassroomCourseResponse;
import org.elearning.backend.classroom.dto.response.ClassroomResponse;
import org.elearning.backend.classroom.service.ClassroomCourseService;
import org.elearning.backend.classroom.service.ClassroomService;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.elearning.backend.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassroomsControllerTest {

    @Mock
    private ClassroomService classroomService;

    @InjectMocks
    private ClassroomsController classroomsController;

    @Mock
    private ClassroomCourseService classroomCourseService;

    @Test
    void createClassroom_returns201Created() {
        UUID userId = UUID.randomUUID();
        CreateClassroomRequest request = new CreateClassroomRequest("Class A", "Desc");
        ClassroomResponse responseBody = makeResponse();
        when(classroomService.createClassroom(request, userId)).thenReturn(responseBody);

        ResponseEntity<ClassroomResponse> response =
                classroomsController.createClassroom(request, userDetails(userId));

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isSameAs(responseBody);
    }

    @Test
    void getMyOrganizationClassrooms_returns200Ok() {
        UUID userId = UUID.randomUUID();
        List<ClassroomResponse> responseBody = List.of(makeResponse(), makeResponse());
        when(classroomService.getMyOrganizationClassrooms(userId)).thenReturn(responseBody);

        ResponseEntity<List<ClassroomResponse>> response =
                classroomsController.getMyOrganizationClassrooms(userDetails(userId));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(responseBody);
    }

    @Test
    void getClassroomById_returns200Ok() {
        UUID userId = UUID.randomUUID();
        UUID classroomId = UUID.randomUUID();
        ClassroomResponse responseBody = makeResponse();
        when(classroomService.getClassroomById(classroomId, userId)).thenReturn(responseBody);

        ResponseEntity<ClassroomResponse> response =
                classroomsController.getClassroomById(classroomId, userDetails(userId));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(responseBody);
    }

    @Test
    void patchClassroom_returns200Ok() {
        UUID userId = UUID.randomUUID();
        UUID classroomId = UUID.randomUUID();
        UpdateClassroomRequest request = new UpdateClassroomRequest("New", "Desc");
        ClassroomResponse responseBody = makeResponse();
        when(classroomService.patchClassroom(classroomId, request, userId)).thenReturn(responseBody);

        ResponseEntity<ClassroomResponse> response =
                classroomsController.patchClassroom(classroomId, request, userDetails(userId));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(responseBody);
    }

    @Test
    void deleteClassroom_returns204NoContent() {
        UUID userId = UUID.randomUUID();
        UUID classroomId = UUID.randomUUID();

        ResponseEntity<Void> response =
                classroomsController.deleteClassroom(classroomId, userDetails(userId));

        verify(classroomService).deleteClassroom(classroomId, userId);
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void assignCourses_returns201Created() {
        UUID userId = UUID.randomUUID();
        UUID classroomId = UUID.randomUUID();
        AssignCoursesToClassroomRequest request = new AssignCoursesToClassroomRequest();
        request.setCourseIds(List.of(UUID.randomUUID()));

        List<ClassroomCourseResponse> responseBody = List.of(
                new ClassroomCourseResponse(UUID.randomUUID(), classroomId, UUID.randomUUID(),
                        LocalDateTime.of(2026, 4, 24, 10, 0))
        );
        when(classroomCourseService.assignCourses(classroomId, request, userId)).thenReturn(responseBody);

        ResponseEntity<List<ClassroomCourseResponse>> response =
                classroomsController.assignCourses(classroomId, request, userDetails(userId));

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(responseBody);
    }

    private ClassroomResponse makeResponse() {
        return new ClassroomResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Class A",
                "Desc",
                LocalDateTime.of(2026, 4, 24, 10, 0),
                LocalDateTime.of(2026, 4, 24, 10, 0)
        );
    }

    private CustomUserDetails userDetails(UUID userId) {
        User user = new User();
        user.setId(userId);
        return new CustomUserDetails(user);
    }
}
