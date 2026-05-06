package org.elearning.backend.content.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.common.GlobalHttpStatusCodes;
import org.elearning.backend.content.dto.UpdateCourseDto;
import org.elearning.backend.content.dto.ResponseCourseDto;
import org.elearning.backend.content.dto.ResponseCourseFullViewDto;
import org.elearning.backend.content.dto.CreateCourseDto;
import org.elearning.backend.content.service.CourseService;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import java.util.UUID;

@Tag(name = "Courses", description = "Course administration — create, read, update and delete courses. Some endpoints require specific roles (TEACHER, ADMIN).")
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CoursesController extends GlobalHttpStatusCodes {

    private final CourseService courseService;

    @Operation(
            summary = "Create a new course",
            description = """
                    Creates a new course with its chapters, lessons and resources.
                    Only users with the TEACHER role can create courses.
                    If `status` is omitted it defaults to DRAFT.
                    If `visibility` defaults to PRIVATE .  Public is only for officeal courses
                    The course is automatically assigned to the authenticated user as creator.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = CREATED, description = "Course successfully created"),
            @ApiResponse(responseCode = BAD_REQUEST, description = "Invalid input data — missing required fields or malformed JSON"),
            @ApiResponse(responseCode = UNAUTHORIZED, description = "Not authenticated"),
            @ApiResponse(responseCode = FORBIDDEN, description = "Authenticated user does not have permission to create courses")
    })
    @PostMapping
    @PreAuthorize("@accessService.canCreateCourse(authentication)")
    public ResponseEntity<ResponseCourseFullViewDto> createCourse(
            @RequestBody CreateCourseDto courseDto,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        UUID userId = currentUser.getUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courseService.createCourse(courseDto, userId));
    }

    @Operation(
            summary = "Get public courses (paginated)",
            description = """
                    Returns all courses that are both  PUBLIC.
                    OFICIAL CREATED BY US
                    Results are paginated. Use the query parameters to control pagination and sorting.
                    
                    Query parameters:
                    - `page` — zero-based page index (default: 0)
                    - `size` — number of items per page (default: 10)
                    - `sort` — field to sort by, optionally followed by `,asc` or `,desc` (default: title,asc)
                    
                    Example: `GET /api/v1/courses/public?page=0&size=10&sort=title,asc`
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Page of public courses successfully returned"),
            @ApiResponse(responseCode = UNAUTHORIZED, description = "Not authenticated")
    })
    @GetMapping("/public")
    @PreAuthorize("@accessService.canViewPublicCourses(authentication)")
    public ResponseEntity<Page<ResponseCourseDto>> getPublicCourses(
            @Parameter(hidden = true)
            @PageableDefault(size = 10, sort = "title") Pageable pageable) {
        return ResponseEntity.ok(courseService.getPublicCourses(pageable));
    }

    @Operation(
            summary = "Get my courses (paginated)",
            description = """
                    Returns all courses creaed by TEACHER(current user)
                    Results are paginated. Use the query parameters to control pagination and sorting.
                    
                    Query parameters:
                    - `page` — zero-based page index (default: 0)
                    - `size` — number of items per page (default: 10)
                    - `sort` — field to sort by, optionally followed by `,asc` or `,desc` (default: title,asc)
                    
                    Example: `GET /api/v1/courses/my-courses?page=0&size=5&sort=createdAt,desc`
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Page of user's courses successfully returned"),
            @ApiResponse(responseCode = UNAUTHORIZED, description = "Not authenticated"),
            @ApiResponse(responseCode = FORBIDDEN, description = "Authenticated user does not have permission to view their courses")
    })
    @GetMapping("/my-courses")
    @PreAuthorize("@accessService.canViewMyCourses(authentication)")
    public ResponseEntity<Page<ResponseCourseDto>> getMyCourses(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Parameter(hidden = true)
            @PageableDefault(size = 10, sort = "title") Pageable pageable) {
        UUID userId = currentUser.getUserId();
        return ResponseEntity.ok(courseService.getMyCourses(userId, pageable));
    }

    @Operation(
            summary = "Fully update a course",
            description = """
                    Fully replaces an existing course with the provided data.
                    All fields are overwritten — omitted fields will be set to null or their defaults.
                    Only the course creator (TEACHER) can perform this operation.
                    Returns 404 if the course does not exist.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Course successfully updated"),
            @ApiResponse(responseCode = BAD_REQUEST, description = "Invalid input data"),
            @ApiResponse(responseCode = UNAUTHORIZED, description = "Not authenticated"),
            @ApiResponse(responseCode = FORBIDDEN, description = "Authenticated user does not own this course"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Course not found")
    })
    @PutMapping("/{id}")
    @PreAuthorize("@accessService.canReplaceCourse(authentication,#id)")
    public ResponseEntity<ResponseCourseDto> updateCourse(
            @Parameter(description = "UUID of the course to update", required = true)
            @P("id") @PathVariable UUID id,
            @RequestBody UpdateCourseDto updateCourseDto) {
        return ResponseEntity.ok(courseService.updateCourse(id, updateCourseDto));
    }

    @Operation(
            summary = "Partially update a course",
            description = """
                    Partially updates a course — only non-null fields in the request body are applied.
                    Fields not included in the request body remain unchanged.
                    Only the course creator  can perform this operation.
                    Returns 404 if the course does not exist.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Course successfully patched"),
            @ApiResponse(responseCode = BAD_REQUEST, description = "Invalid input data"),
            @ApiResponse(responseCode = UNAUTHORIZED, description = "Not authenticated"),
            @ApiResponse(responseCode = FORBIDDEN, description = "Authenticated user does not own this course"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Course not found")
    })
    @PatchMapping("/{id}")
    @PreAuthorize("@accessService.canEditCourse(authentication,#id)")
    public ResponseEntity<ResponseCourseDto> patchCourse(
            @Parameter(description = "UUID of the course to patch", required = true)
            @P("id") @PathVariable UUID id,
            @RequestBody UpdateCourseDto updateCourseDto) {
        return ResponseEntity.ok(courseService.patchCourse(id, updateCourseDto));
    }

    @Operation(
            summary = "Delete a course",
            description = """
                    Permanently deletes a course by its ID, including all its chapters, lessons and resources.
                    This action is irreversible.
                    Only the course creator can perform this operation.
                    Returns 404 if the course does not exist.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = NO_CONTENT, description = "Course successfully deleted — no content returned"),
            @ApiResponse(responseCode = UNAUTHORIZED, description = "Not authenticated"),
            @ApiResponse(responseCode = FORBIDDEN, description = "Authenticated user does not own this course"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Course not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("@accessService.canDeleteCourse(authentication,#id)")
    public ResponseEntity<Void> deleteCourse(
            @Parameter(description = "UUID of the course to delete", required = true)
            @P("id") @PathVariable UUID id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get full course view",
            description = """
                    Returns a course with its complete structure: all chapters, lessons and resources.
                    Also includes the associated test ID for each lesson (if one exists).
                    Only users with access to the course (enrolled students, the creator, or admins) can view this.
                    Returns 404 if the course does not exist.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Full course view successfully returned"),
            @ApiResponse(responseCode = UNAUTHORIZED, description = "Not authenticated"),
            @ApiResponse(responseCode = FORBIDDEN, description = "Authenticated user does not have access to this course"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Course not found")
    })
    @GetMapping("/{courseId}/full-view")
    @PreAuthorize("@accessService.canViewCourseFullView(authentication,#id)")
    public ResponseEntity<ResponseCourseFullViewDto> getCourseFullView(
            @Parameter(description = "UUID of the course to retrieve", required = true)
            @P("id") @PathVariable UUID courseId) {
        return ResponseEntity.ok(courseService.getCourseFullView(courseId));
    }
}