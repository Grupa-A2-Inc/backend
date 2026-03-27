package org.elearning.backend.content.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.elearning.backend.content.model.LessonResource;
import org.elearning.backend.content.service.LessonResourceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * The LessonResourcesController class is a REST controller responsible for handling HTTP requests related to lesson resources.
 * It provides endpoints for creating, retrieving, and deleting lesson resources associated with specific lessons.
 */
@RestController
public class LessonResourceController {
    private final LessonResourceService lessonResourceService;

    public LessonResourceController(LessonResourceService lessonResourceService) {
        this.lessonResourceService = lessonResourceService;
    }

    /**
     * POST /api/lessons/{lessonId}/resources Entrypoint
     * @RequestBody - a JSON file with multiple fields for each lesson resource content
     * @PathVariable - extracts dynamic value directly from the URI
     * Creates a new lesson resource associated with a specific lesson ID.
     * Returns HTTP 201 if successfully created, along with the created LessonResource object in the response body.
     * If no lesson with the given ID exists, returns HTTP 404 Not Found.
     */
    @Operation(summary = "Create a new lesson resource",
            description = "Creates a new lesson resource associated with a specific lesson ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Lesson resource created successfully"),
            @ApiResponse(responseCode = "404", description = "Lesson not found")
    })
    @PostMapping("/api/lessons/{lessonId}/resources")
    public ResponseEntity<LessonResource> createNewLessonResource(
            @RequestBody LessonResource newLessonResource,
            @PathVariable UUID lessonId
            ) {
        LessonResource lessonResource;
        try {
            lessonResource = lessonResourceService.createNewLessonResource(newLessonResource, lessonId);
        }
        catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(lessonResource);
    }

    /**
     * GET /api/lessons/{lessonId}/resources Entrypoint
     * @PathVariable - extracts dynamic value directly from the URI
     * Retrieves a list of lesson resources associated with a specific lesson ID.
     * Returns HTTP 200 along with the list of LessonResource objects in the response body.
     * If no lesson with the given ID exists, returns HTTP 404 Not Found.
     */
    @Operation(summary = "Get lesson resources by lesson ID",
            description = "Retrieves a list of lesson resources associated with a specific lesson ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lesson resources retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Lesson not found")
    })
    @GetMapping("/api/lessons/{lessonId}/resources")
    public ResponseEntity<List<LessonResource>> getResourcesByLessonId(@PathVariable UUID lessonId) {
        List<LessonResource> resources;
        try {
            resources = lessonResourceService.getResourcesByLessonId(lessonId);
        }
        catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(resources);
    }

    /** DELETE /api/lessons/{lessonId}/resources/{resourceId} Entrypoint
     * @PathVariable - extracts dynamic value directly from the URI
     * Deletes a specific lesson resource associated with a specific lesson ID.
     * Returns HTTP 204 No Content if the deletion was successful.
     * If no resource with the given ID exists or if the resource does not belong to the specified lesson, returns HTTP 404 Not Found.
     */
    @Operation(summary = "Delete a lesson resource",
            description = "Deletes a specific lesson resource associated with a specific lesson ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Lesson resource deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Lesson resource not found or does not belong to the specified lesson")
    })
    @DeleteMapping("/api/lessons/{lessonId}/resources/{resourceId}")
    public ResponseEntity<Void> deleteLessonResource(@PathVariable UUID resourceId, @PathVariable UUID lessonId) {
        try {
            lessonResourceService.deleteLessonResource(resourceId, lessonId);
        }
        catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    /** PATCH /api/lessons/{lessonId}/resources/{resourceId} Entrypoint
     * @PathVariable - extracts dynamic value directly from the URI
     * @RequestBody - a JSON file with multiple fields for each lesson resource content
     * Updates the title and/or URL of a specific lesson resource associated with a specific lesson ID.
     * Returns HTTP 200 along with the updated LessonResource object in the response body if the update was successful.
     * If no resource with the given ID exists, if the resource does not belong to the specified lesson, or if no lesson with the given ID exists, returns HTTP 404 Not Found.
     */
    @Operation(summary = "Update a lesson resource",
            description = "Updates the title and/or URL of a specific lesson resource associated with a specific) lesson ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lesson resource updated successfully"),
            @ApiResponse(responseCode = "404", description = "Lesson resource not found, does not belong to the specified lesson, or lesson not found")
    })
    @PatchMapping("/api/lessons/{lessonId}/resources/{resourceId}")
    public ResponseEntity<LessonResource> updateLessonMetadata(
            @PathVariable UUID lessonId,
            @PathVariable UUID resourceId,
            @RequestBody LessonResource lessonResource){
        LessonResource updatedResource;
        try {
            updatedResource = lessonResourceService.updateLessonResource(lessonId, resourceId, lessonResource);
        }
        catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(updatedResource);
    }
}
