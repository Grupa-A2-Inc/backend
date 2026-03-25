package org.elearning.backend.content.controller;

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
    @PostMapping("/api/lessons/{lessonId}/resources")
    public ResponseEntity<Object> createNewLessonResource(
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
}
