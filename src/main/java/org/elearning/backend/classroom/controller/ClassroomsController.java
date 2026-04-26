package org.elearning.backend.classroom.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.classroom.dto.request.AssignCoursesToClassroomRequest;
import org.elearning.backend.classroom.dto.request.CreateClassroomRequest;
import org.elearning.backend.classroom.dto.request.UpdateClassroomRequest;
import org.elearning.backend.classroom.dto.response.ClassroomCourseResponse;
import org.elearning.backend.classroom.dto.response.ClassroomResponse;
import org.elearning.backend.classroom.service.ClassroomCourseService;
import org.elearning.backend.classroom.service.ClassroomService;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/classrooms")
@RequiredArgsConstructor
public class ClassroomsController {

    private final ClassroomService classroomService;

    private final ClassroomCourseService classroomCourseService;

    @PostMapping
    @PreAuthorize("@accessService.canCreateClassroom(authentication)")
    public ResponseEntity<ClassroomResponse> createClassroom(
            @Valid @RequestBody CreateClassroomRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        ClassroomResponse response = classroomService.createClassroom(request, currentUser.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("@accessService.canCreateClassroom(authentication)")
    public ResponseEntity<List<ClassroomResponse>> getMyOrganizationClassrooms(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        return ResponseEntity.ok(classroomService.getMyOrganizationClassrooms(currentUser.getUserId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@accessService.canManageClassroom(authentication, #id)")
    public ResponseEntity<ClassroomResponse> getClassroomById(
            @P("id") @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        return ResponseEntity.ok(classroomService.getClassroomById(id, currentUser.getUserId()));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@accessService.canManageClassroom(authentication, #id)")
    public ResponseEntity<ClassroomResponse> patchClassroom(
            @P("id") @PathVariable UUID id,
            @RequestBody UpdateClassroomRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        return ResponseEntity.ok(classroomService.patchClassroom(id, request, currentUser.getUserId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@accessService.canManageClassroom(authentication, #id)")
    public ResponseEntity<Void> deleteClassroom(
            @P("id") @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        classroomService.deleteClassroom(id, currentUser.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{classroomId}/courses")
    @PreAuthorize("@accessService.canManageClassroom(authentication, #classroomId)")
    public ResponseEntity<List<ClassroomCourseResponse>> assignCourses(
            @P("classroomId") @PathVariable UUID classroomId,
            @Valid @RequestBody AssignCoursesToClassroomRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        List<ClassroomCourseResponse> response =
                classroomCourseService.assignCourses(classroomId, request, currentUser.getUserId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}