package org.elearning.backend.content.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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

import java.util.List;
import java.util.UUID;

@Tag(name = "Courses", description = "Course administration")
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CoursesController {

    private final CourseService courseService;

    @Operation(summary = "Create a new course", description = "Creates a new course with its chapters, lessons and resources")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Course successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
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


    @Operation(summary = "Get public courses", description = "Returns all published and public courses")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Courses successfully returned")
    })
    @GetMapping("/public")
    @PreAuthorize("@accessService.canViewPublicCourses(authentication)")
    public ResponseEntity<List<ResponseCourseDto>> getPublicCourses() {
        return ResponseEntity.ok(courseService.getPublicCourses());
    }

    @Operation(summary = "Get my courses", description = "Returns all courses created by the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Courses successfully returned")
    })
    @GetMapping("/my-courses")
    @PreAuthorize("@accessService.canViewMyCourses(authentication)")
    public ResponseEntity<List<ResponseCourseDto>> getMyCourses(@AuthenticationPrincipal CustomUserDetails currentUser) {
        UUID userId = currentUser.getUserId();
        return ResponseEntity.ok(courseService.getMyCourses(userId));
    }

    @Operation(summary = "Update a course", description = "Fully updates a course given by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Course successfully updated"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @PutMapping("/{id}")
    @PreAuthorize("@accessService.canReplaceCourse(authentication,#id)")
    public ResponseEntity<ResponseCourseDto> updateCourse(@P("id") @PathVariable UUID id, @RequestBody UpdateCourseDto updateCourseDto) {
        return ResponseEntity.ok(courseService.updateCourse(id, updateCourseDto));
    }

    @Operation(summary = "Partially update a course", description = "Partially updates a course given by its ID, modifying only non-null fields")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Course successfully patched"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @PatchMapping("/{id}")
    @PreAuthorize("@accessService.canEditCourse(authentication,#id)")
    public ResponseEntity<ResponseCourseDto> patchCourse(@P("id") @PathVariable UUID id, @RequestBody UpdateCourseDto updateCourseDto) {
        return ResponseEntity.ok(courseService.patchCourse(id, updateCourseDto));
    }

    @Operation(summary = "Delete a course", description = "Deletes a course given by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Course successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("@accessService.canDeleteCourse(authentication,#id)")
    public ResponseEntity<Void> deleteCourse(@P("id") @PathVariable UUID id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get full course view", description = "Returns a course with all its chapters, lessons and resources given by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Course successfully returned"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @GetMapping("/{courseId}/full-view")
    @PreAuthorize("@accessService.canViewCourseFullView(authentication,#id)")
    public ResponseEntity<ResponseCourseFullViewDto> getCourseFullView(@P("id") @PathVariable UUID courseId) {
        return ResponseEntity.ok(courseService.getCourseFullView(courseId));
    }
}