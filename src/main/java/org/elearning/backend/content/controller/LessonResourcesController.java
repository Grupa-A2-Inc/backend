package org.elearning.backend.content.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.elearning.backend.content.dto.ResponseLessonResourceDto;
import org.elearning.backend.content.dto.UpdateLessonResourceDto;
import org.elearning.backend.content.dto.CreateLessonResourceDto;
import org.elearning.backend.content.service.LessonResourceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Lesson Resources", description = "Lesson resource administration")
@RestController
public class LessonResourcesController {
    private final LessonResourceService lessonResourceService;

    public LessonResourcesController(LessonResourceService lessonResourceService) {
        this.lessonResourceService = lessonResourceService;
    }

    @Operation(summary = "Create a new lesson resource", description = "Creates a new resource associated with a lesson given by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Resource successfully created"),
            @ApiResponse(responseCode = "400", description = "Title or URL cannot be null"),
            @ApiResponse(responseCode = "404", description = "Lesson not found")
    })
    @PostMapping("/api/lessons/{lessonId}/resources")
    public ResponseEntity<ResponseLessonResourceDto> createNewLessonResource(
            @RequestBody CreateLessonResourceDto newLessonResourceDTOPost,
            @PathVariable UUID lessonId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lessonResourceService.createNewLessonResource(newLessonResourceDTOPost, lessonId));
    }

    @Operation(summary = "Get all lesson resources", description = "Returns all resources associated with a lesson given by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resources successfully returned"),
            @ApiResponse(responseCode = "404", description = "Lesson not found")
    })
    @GetMapping("/api/lessons/{lessonId}/resources")
    public ResponseEntity<List<ResponseLessonResourceDto>> getResourcesByLessonId(@PathVariable UUID lessonId) {
        return ResponseEntity.ok(lessonResourceService.getResourcesByLessonId(lessonId));
    }

    @Operation(summary = "Delete a lesson resource", description = "Deletes a resource given by its ID from a lesson given by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Resource successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Resource not found or does not belong to the specified lesson")
    })
    @DeleteMapping("/api/lessons/{lessonId}/resources/{resourceId}")
    public ResponseEntity<Void> deleteLessonResource(@PathVariable UUID resourceId, @PathVariable UUID lessonId) {
        lessonResourceService.deleteLessonResource(resourceId, lessonId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update a lesson resource", description = "Updates the title and/or URL of a resource given by its ID from a lesson given by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resource successfully updated"),
            @ApiResponse(responseCode = "404", description = "Resource not found, does not belong to the specified lesson, or lesson not found")
    })
    @PatchMapping("/api/lessons/{lessonId}/resources/{resourceId}")
    public ResponseEntity<ResponseLessonResourceDto> updateLessonMetadata(
            @PathVariable UUID lessonId,
            @PathVariable UUID resourceId,
            @RequestBody UpdateLessonResourceDto lessonResourceDTOPatch) {
        return ResponseEntity.ok(lessonResourceService.updateLessonResource(lessonId, resourceId, lessonResourceDTOPatch));
    }
}