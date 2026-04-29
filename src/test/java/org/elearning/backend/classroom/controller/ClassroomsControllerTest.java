package org.elearning.backend.classroom.controller;

import org.elearning.backend.classroom.dto.request.AssignCoursesToClassroomRequest;
import org.elearning.backend.classroom.dto.request.CreateClassroomRequest;
import org.elearning.backend.classroom.dto.request.ModifyClassroomMembersRequest;
import org.elearning.backend.classroom.dto.request.UpdateClassroomRequest;
import org.elearning.backend.classroom.dto.response.ClassroomCourseDetailsResponse;
import org.elearning.backend.classroom.dto.response.ClassroomMemberResponse;
import org.elearning.backend.classroom.dto.response.ClassroomCourseResponse;
import org.elearning.backend.classroom.dto.response.ClassroomResponse;
import org.elearning.backend.classroom.entity.MembershipType;
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
import java.util.Set;
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

    @Test
    void getClassroomCourses_returns200Ok() {
        UUID classroomId = UUID.randomUUID();

        ClassroomCourseDetailsResponse details = new ClassroomCourseDetailsResponse();
        details.setCourseId(UUID.randomUUID());
        details.setTitle("Math 101");
        details.setAssignedAt(LocalDateTime.of(2026, 4, 28, 10, 0));

        List<ClassroomCourseDetailsResponse> responseBody = List.of(details);
        when(classroomCourseService.getClassroomCourses(classroomId)).thenReturn(responseBody);

        ResponseEntity<List<ClassroomCourseDetailsResponse>> response =
                classroomsController.getClassroomCourses(classroomId);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(responseBody);
    }

    @Test
    void addClassroomMembers_returns200Ok() {
        UUID userId = UUID.randomUUID();
        UUID classroomId = UUID.randomUUID();
        ModifyClassroomMembersRequest request = new ModifyClassroomMembersRequest(Set.of(UUID.randomUUID()));
        ClassroomResponse responseBody = makeResponse();
        when(classroomService.addClassroomMembers(classroomId, request, userId)).thenReturn(responseBody);

        ResponseEntity<ClassroomResponse> response =
                classroomsController.addClassroomMembers(classroomId, request, userDetails(userId));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(responseBody);
    }

    @Test
    void deleteClassroomMembers_returns200Ok() {
        UUID userId = UUID.randomUUID();
        UUID classroomId = UUID.randomUUID();
        ModifyClassroomMembersRequest request = new ModifyClassroomMembersRequest(Set.of(UUID.randomUUID()));
        ClassroomResponse responseBody = makeResponse();
        when(classroomService.deleteClassroomMembers(classroomId, request, userId)).thenReturn(responseBody);

        ResponseEntity<ClassroomResponse> response =
                classroomsController.deleteClassroomMembers(classroomId, request, userDetails(userId));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(responseBody);
    }

    @Test
    void listClassroomMembers_returns200Ok() {
        UUID classroomId = UUID.randomUUID();
        ClassroomMemberResponse member = new ClassroomMemberResponse(
                UUID.randomUUID(),
                "teacher@example.com",
                MembershipType.TEACHER
        );
        List<ClassroomMemberResponse> responseBody = List.of(member);
        when(classroomService.listClassroomMembers(classroomId, MembershipType.TEACHER)).thenReturn(responseBody);

        ResponseEntity<List<ClassroomMemberResponse>> response =
                classroomsController.listClassroomMembers(classroomId, MembershipType.TEACHER);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(responseBody);
    }

    @Test
    void getClassroomCourses_returnsEmptyList_whenServiceReturnsEmpty() {
        UUID classroomId = UUID.randomUUID();
        when(classroomCourseService.getClassroomCourses(classroomId)).thenReturn(List.of());

        ResponseEntity<List<ClassroomCourseDetailsResponse>> response =
                classroomsController.getClassroomCourses(classroomId);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull().isEmpty();
    }

    @Test
    void addClassroomMembers_delegatesCorrectArgumentsToService() {
        UUID userId = UUID.randomUUID();
        UUID classroomId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        ModifyClassroomMembersRequest request = new ModifyClassroomMembersRequest(Set.of(memberId));
        ClassroomResponse responseBody = makeResponse();
        when(classroomService.addClassroomMembers(classroomId, request, userId)).thenReturn(responseBody);

        classroomsController.addClassroomMembers(classroomId, request, userDetails(userId));

        verify(classroomService).addClassroomMembers(classroomId, request, userId);
    }

    @Test
    void deleteClassroomMembers_delegatesCorrectArgumentsToService() {
        UUID userId = UUID.randomUUID();
        UUID classroomId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        ModifyClassroomMembersRequest request = new ModifyClassroomMembersRequest(Set.of(memberId));
        ClassroomResponse responseBody = makeResponse();
        when(classroomService.deleteClassroomMembers(classroomId, request, userId)).thenReturn(responseBody);

        classroomsController.deleteClassroomMembers(classroomId, request, userDetails(userId));

        verify(classroomService).deleteClassroomMembers(classroomId, request, userId);
    }

    @Test
    void listClassroomMembers_returnsEmptyList_whenNoMembers() {
        UUID classroomId = UUID.randomUUID();
        when(classroomService.listClassroomMembers(classroomId, null)).thenReturn(List.of());

        ResponseEntity<List<ClassroomMemberResponse>> response =
                classroomsController.listClassroomMembers(classroomId, null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull().isEmpty();
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
