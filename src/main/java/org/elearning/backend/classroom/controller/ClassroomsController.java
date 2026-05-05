package org.elearning.backend.classroom.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.classroom.dto.request.AssignCoursesToClassroomRequest;
import org.elearning.backend.classroom.dto.request.ModifyClassroomMembersRequest;
import org.elearning.backend.classroom.dto.response.ClassroomCourseDetailsResponse;
import org.elearning.backend.classroom.dto.response.ClassroomMemberResponse;
import org.elearning.backend.classroom.entity.MembershipType;
import org.elearning.backend.classroom.dto.request.CreateClassroomRequest;
import org.elearning.backend.classroom.dto.request.UpdateClassroomRequest;
import org.elearning.backend.classroom.dto.response.ClassroomCourseResponse;
import org.elearning.backend.classroom.dto.response.ClassroomResponse;
import org.elearning.backend.classroom.service.ClassroomCourseService;
import org.elearning.backend.classroom.service.ClassroomService;
import org.elearning.backend.common.dto.response.PaginatedResponse;
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
@Tag(
        name = "Classrooms",
        description = "Endpoints for creating classrooms, managing classroom metadata, maintaining classroom membership, and assigning courses to classrooms. " +
                "In this module, ADMIN and ORGANIZATION_ADMIN are not interchangeable: ADMIN is the platform-wide role, while ORGANIZATION_ADMIN is restricted " +
                "to resources inside their own organization. Some classroom operations are intentionally narrower and may allow teachers or students only in " +
                "specific membership-based scenarios."
)
@RequestMapping("/api/v1/classrooms")
@RequiredArgsConstructor
public class ClassroomsController {

    private final ClassroomService classroomService;

    private final ClassroomCourseService classroomCourseService;

    @Operation(
            summary = "Create a classroom",
            description = "Creates a new classroom inside the authenticated caller's organization. This operation is intended for organization-level administration. " +
                    "An ORGANIZATION_ADMIN can create classrooms only for their own organization; they do not gain cross-organization reach. " +
                    "A platform ADMIN is a broader role elsewhere in the system, but this endpoint is documented around the organization-managed classroom workflow."
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
            description = """
                    Returns the classrooms that belong to the authenticated caller's organization. The result is organization-scoped rather than platform-wide.
                    This means an ORGANIZATION_ADMIN sees only the classrooms from the organization they administer, not classrooms from other organizations.
                    This endpoint is meant for administrative overviews inside one tenant boundary.

                    Query parameters:
                    - `page` — zero-based page index
                    - `size` — number of items per page
                    - `search` — optional case-insensitive text filter for classroom name or description, depending on service implementation
                    - `sortBy` — optional field used for sorting the classroom list
                    - `sortDir` — optional sort direction; use `asc` or `desc`
                    """
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
    public ResponseEntity<PaginatedResponse<ClassroomResponse>> getMyOrganizationClassrooms(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Min(0) @Max(1000) Integer size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {

        return ResponseEntity.ok(classroomService.getMyOrganizationClassrooms(
                currentUser.getUserId(), page, size, search, sortBy, sortDir));
    }

    @Operation(
            summary = "List my classrooms",
            description = "Returns the classrooms the authenticated user belongs to, either as STUDENT or TEACHER."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Classrooms retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ClassroomResponse.class)
            )
    )
    @ApiResponse(responseCode = "403", description = "Not authenticated", content = @Content)
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    //@PreAuthorize("@accessService.extractCurrentUser(authentication) != null")
    public ResponseEntity<PaginatedResponse<ClassroomResponse>> getMyClassrooms(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {

        return ResponseEntity.ok(classroomService.getMyClassrooms(
                currentUser.getUserId(), page, size, search, sortBy, sortDir));
    }

    @Operation(
            summary = "Get classroom by ID",
            description = "Returns the classroom identified by the given ID when the caller is allowed to manage that classroom. " +
                    "The management rule is stricter than simple authentication: a platform ADMIN may manage any classroom, while an ORGANIZATION_ADMIN " +
                    "may manage only classrooms that belong to their own organization. Ordinary teachers and students do not automatically gain classroom-management access."
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
            description = "Partially updates mutable classroom fields such as name and description. This is a management-level operation, not a membership-level operation. " +
                    "A platform ADMIN can update any classroom, whereas an ORGANIZATION_ADMIN can update only classrooms inside their own organization. " +
                    "Being a teacher or student in a classroom is not enough to use this endpoint."
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
            description = "Deletes the classroom identified by the given ID. Because this is a destructive administrative action, access is limited to classroom managers. " +
                    "A platform ADMIN has global scope, while an ORGANIZATION_ADMIN is limited to classrooms within their own organization. " +
                    "Membership in the classroom alone does not authorize deletion."
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
            description = "Assigns one or more courses to the specified classroom. This endpoint is intentionally more restrictive than general classroom management. " +
                    "The caller must pass the access rule that verifies the classroom belongs to the same organization and that every requested course was created by the " +
                    "authenticated teacher. In other words, ORGANIZATION_ADMIN is not enough here: organization admins may manage the classroom generally, but they are not " +
                    "allowed to attach courses unless the course-assignment access rule explicitly permits it. The endpoint is designed to preserve teacher ownership of course content."
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
    @PreAuthorize("@accessService.canAssignCoursesToClassroom(authentication, #classroomId, #request)")
    public ResponseEntity<List<ClassroomCourseResponse>> assignCourses(
            @P("classroomId") @PathVariable UUID classroomId,
            @P("request") @Valid @RequestBody AssignCoursesToClassroomRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        List<ClassroomCourseResponse> response =
                classroomCourseService.assignCourses(classroomId, request, currentUser.getUserId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "List courses in classroom",
            description = """
                    Returns the courses assigned to the specified classroom. Visibility to this list depends on the caller's relationship to the classroom rather than
                    a single admin-only rule. A platform ADMIN may access broadly, an ORGANIZATION_ADMIN may access according to organization-scoped classroom rules,
                    and teachers or students may gain access only through classroom membership or other access-service checks.

                    Query parameters:
                    - `page` — zero-based page index
                    - `size` — number of items per page
                    - `search` — optional case-insensitive text filter applied to classroom-course results
                    - `category` — optional course-category filter
                    - `sortBy` — optional field used for sorting the classroom-course list
                    - `sortDir` — optional sort direction; use `asc` or `desc`
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Courses retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ClassroomCourseDetailsResponse.class)
            )
    )
    @ApiResponse(responseCode = "403", description = "User is not allowed to view classroom courses", content = @Content)
    @ApiResponse(responseCode = "404", description = "Classroom not found", content = @Content)
    @GetMapping("/{classroomId}/courses")
    @PreAuthorize("@accessService.canViewClassroomCourses(authentication, #classroomId)")
    public ResponseEntity<PaginatedResponse<ClassroomCourseDetailsResponse>> getClassroomCourses(
            @P("classroomId") @PathVariable UUID classroomId,
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Min(0) @Max(1000) Integer size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {

        return ResponseEntity.ok(classroomCourseService.getClassroomCourses(
                classroomId, page, size, search, category, sortBy, sortDir));
    }

    @Operation(
            summary = "Add members to classroom",
            description = "Adds the provided users to the specified classroom. This is an administrative classroom-management action. " +
                    "A platform ADMIN can perform it globally, while an ORGANIZATION_ADMIN can perform it only for classrooms within their own organization. " +
                    "The endpoint does not exist to let teachers self-manage classroom rosters unless the access rules explicitly grant them classroom-management authority."
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
            description = "Removes the provided users from the specified classroom. Like member addition, this is a management operation rather than a normal classroom-participant action. " +
                    "A platform ADMIN acts across the system, while an ORGANIZATION_ADMIN is limited to their own organization. The endpoint enforces those boundaries through the access layer."
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
            description = """
                    Returns the members of the specified classroom and optionally filters the result by membership type, such as TEACHER or STUDENT.
                    This endpoint is broader than classroom-management endpoints in some cases because access may also be granted to teachers who are actually assigned to the classroom.
                    The distinction remains important: ADMIN is platform-wide, ORGANIZATION_ADMIN is organization-wide, and teacher access is membership-based rather than administrative.

                    Query parameters:
                    - `role` — optional membership-type filter, typically `TEACHER` or `STUDENT`
                    - `page` — zero-based page index
                    - `size` — number of items per page
                    - `search` — optional case-insensitive text filter applied to member data
                    - `sortBy` — optional field used for sorting the member list
                    - `sortDir` — optional sort direction; use `asc` or `desc`
                    """
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
    public ResponseEntity<PaginatedResponse<ClassroomMemberResponse>> listClassroomMembers(
            @P("classroomId") @PathVariable UUID classroomId,
            @RequestParam(required = false) MembershipType role,
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Min(0) @Max(1000) Integer size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {

        return ResponseEntity.ok(classroomService.listClassroomMembers(
                classroomId, role, page, size, search, sortBy, sortDir));
    }
}
