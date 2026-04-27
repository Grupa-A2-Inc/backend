package org.elearning.backend.classroom.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.classroom.dto.request.AssignCoursesToClassroomRequest;
import org.elearning.backend.classroom.dto.request.ModifyClassroomMembersRequest;
import org.elearning.backend.classroom.dto.response.ClassroomMemberResponse;
import org.elearning.backend.classroom.entity.MembershipType;
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
@Tag(name = "Classrooms", description = "Endpoints for managing classrooms, members, and course assignments")
@RequestMapping("/api/v1/classrooms")
@RequiredArgsConstructor
public class ClassroomsController {

    private final ClassroomService classroomService;

    private final ClassroomCourseService classroomCourseService;

    @Operation(
            summary = "Create a classroom",
            description = "Creates a new classroom for the authenticated user's organization."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Classroom created successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ClassroomResponse.class)
            )
    )
    @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content)
    @ApiResponse(responseCode = "403", description = "User is not allowed to create classrooms", content = @Content)
    @PostMapping
    @PreAuthorize("@accessService.canCreateClassroom(authentication)")
    public ResponseEntity<ClassroomResponse> createClassroom(
            @Valid @RequestBody CreateClassroomRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        ClassroomResponse response = classroomService.createClassroom(request, currentUser.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "List organization classrooms",
            description = "Returns all classrooms from the authenticated user's organization."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Classrooms retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ClassroomResponse.class)
            )
    )
    @ApiResponse(responseCode = "403", description = "User is not allowed to view classrooms", content = @Content)
    @GetMapping
    @PreAuthorize("@accessService.canCreateClassroom(authentication)")
    public ResponseEntity<List<ClassroomResponse>> getMyOrganizationClassrooms(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        return ResponseEntity.ok(classroomService.getMyOrganizationClassrooms(currentUser.getUserId()));
    }

    @Operation(
            summary = "Get classroom by ID",
            description = "Returns the classroom identified by the given ID if the authenticated user can manage it."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Classroom retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ClassroomResponse.class)
            )
    )
    @ApiResponse(responseCode = "403", description = "User is not allowed to access this classroom", content = @Content)
    @ApiResponse(responseCode = "404", description = "Classroom not found", content = @Content)
    @GetMapping("/{id}")
    @PreAuthorize("@accessService.canManageClassroom(authentication, #id)")
    public ResponseEntity<ClassroomResponse> getClassroomById(
            @P("id") @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        return ResponseEntity.ok(classroomService.getClassroomById(id, currentUser.getUserId()));
    }

    @Operation(
            summary = "Update classroom fields",
            description = "Partially updates a classroom identified by the given ID."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Classroom updated successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ClassroomResponse.class)
            )
    )
    @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content)
    @ApiResponse(responseCode = "403", description = "User is not allowed to update this classroom", content = @Content)
    @ApiResponse(responseCode = "404", description = "Classroom not found", content = @Content)
    @PatchMapping("/{id}")
    @PreAuthorize("@accessService.canManageClassroom(authentication, #id)")
    public ResponseEntity<ClassroomResponse> patchClassroom(
            @P("id") @PathVariable UUID id,
            @RequestBody UpdateClassroomRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        return ResponseEntity.ok(classroomService.patchClassroom(id, request, currentUser.getUserId()));
    }

    @Operation(
            summary = "Delete classroom",
            description = "Deletes the classroom identified by the given ID."
    )
    @ApiResponse(responseCode = "204", description = "Classroom deleted successfully", content = @Content)
    @ApiResponse(responseCode = "403", description = "User is not allowed to delete this classroom", content = @Content)
    @ApiResponse(responseCode = "404", description = "Classroom not found", content = @Content)
    @DeleteMapping("/{id}")
    @PreAuthorize("@accessService.canManageClassroom(authentication, #id)")
    public ResponseEntity<Void> deleteClassroom(
            @P("id") @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        classroomService.deleteClassroom(id, currentUser.getUserId());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Assign courses to classroom",
            description = "Assigns one or more courses to the specified classroom."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Courses assigned successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ClassroomCourseResponse.class)
            )
    )
    @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content)
    @ApiResponse(responseCode = "403", description = "User is not allowed to manage this classroom", content = @Content)
    @ApiResponse(responseCode = "404", description = "Classroom or course not found", content = @Content)
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

    @Operation(
            summary = "Add members to classroom",
            description = "Adds the provided members to the specified classroom."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Members added successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ClassroomResponse.class)
            )
    )
    @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content)
    @ApiResponse(responseCode = "403", description = "User is not allowed to manage this classroom", content = @Content)
    @ApiResponse(responseCode = "404", description = "Classroom or member not found", content = @Content)
    @PostMapping("/{classroomId}/members")
    @PreAuthorize("@accessService.canManageClassroom(authentication, #classroomId)")
    public ResponseEntity<ClassroomResponse> addClassroomMembers(
            @P("classroomId") @PathVariable UUID classroomId,
            @Valid @RequestBody ModifyClassroomMembersRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        ClassroomResponse response =
                classroomService.addClassroomMembers(classroomId, request, currentUser.getUserId());

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Remove members from classroom",
            description = "Removes the provided members from the specified classroom."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Members removed successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ClassroomResponse.class)
            )
    )
    @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content)
    @ApiResponse(responseCode = "403", description = "User is not allowed to manage this classroom", content = @Content)
    @ApiResponse(responseCode = "404", description = "Classroom or member not found", content = @Content)
    @DeleteMapping("/{classroomId}/members")
    @PreAuthorize("@accessService.canManageClassroom(authentication, #classroomId)")
    public ResponseEntity<ClassroomResponse> deleteClassroomMembers(
            @P("classroomId") @PathVariable UUID classroomId,
            @Valid @RequestBody ModifyClassroomMembersRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        ClassroomResponse response =
                classroomService.deleteClassroomMembers(classroomId, request, currentUser.getUserId());

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "List classroom members",
            description = "Returns the members of the specified classroom. Optionally filters by membership role."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Classroom members retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ClassroomMemberResponse.class)
            )
    )
    @ApiResponse(responseCode = "403", description = "User is not allowed to view classroom members", content = @Content)
    @ApiResponse(responseCode = "404", description = "Classroom not found", content = @Content)
    @GetMapping("/{classroomId}/members")
    @PreAuthorize("@accessService.canListClassroomMembers(authentication, #classroomId)")
    public ResponseEntity<List<ClassroomMemberResponse>> listClassroomMembers(
            @P("classroomId") @PathVariable UUID classroomId,
            @RequestParam(required = false) MembershipType role) {

        return ResponseEntity.ok(classroomService.listClassroomMembers(classroomId, role));
    }
}
