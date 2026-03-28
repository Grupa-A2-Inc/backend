package org.elearning.backend.content.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.persistence.EntityNotFoundException;
import org.elearning.backend.content.dto.ChapterDTOPost;
import org.elearning.backend.content.dto.ChapterDTOResponse;
import org.elearning.backend.content.model.Chapter;
import org.elearning.backend.content.service.ChapterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class ChapterController {
    private final ChapterService chapterService;
    public ChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    /**
     * POST /api/courses/{courseId}/chapters Entrypoint
     * Creates a new chapter inside a course.
     * Returns HTTP 201 if successfully created, along with the created chapter's info in the response body.
     * Returns HTTP 404 if no course with the given id exists.
     * @param courseId the course's id
     * @param newChapterTitle the new chapter's title
     * @return ResponseEntity with the created chapter's info and HTTP status code 201 if successful, or HTTP 404 if the course is not found.
     */
    @PostMapping("/api/courses/{courseId}/chapters")
    @Operation(summary = "Creates a new chapter.", description = "API endpoint for creating a new chapter inside a course.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Chapter created successfully"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    public ResponseEntity<ChapterDTOResponse> createNewChapter(@PathVariable UUID courseId,
                                                               @RequestBody String newChapterTitle) {
        ChapterDTOResponse chapterInfo;
        try {
            chapterInfo = new ChapterDTOResponse(chapterService.createNewChapter(courseId, newChapterTitle));
        }
        catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(chapterInfo);
    }

    /**
     * GET /api/courses/{courseId}/chapters
     * Retrieves a list of all chapters from a course.
     * Returns HTTP 200 if successfully retrieved, along with the list of chapters in the response body.
     * Returns HTTP 404 if no course with the given id exists.
     * @param courseId the course's id
     * @return ResponseEntity with the list of chapters and HTTP status code 200 if successful, or HTTP 404 if the course is not found.
     */
    @GetMapping("/api/courses/{courseId}/chapters")
    @Operation(summary = "Retrieves all chapters from a course.", description = "API endpoint for retrieving all chapters from a course.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chapters retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    public ResponseEntity<List<ChapterDTOResponse>> getChaptersByCourseId(@PathVariable UUID courseId) {
        List<Chapter> chapters;
        List<ChapterDTOResponse> chapterDTOMetadataList;
        try {
            chapters = chapterService.getAllChaptersFromCourse(courseId);
            chapterDTOMetadataList = chapters.stream().map(ChapterDTOResponse::new).toList();
        }
        catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(chapterDTOMetadataList);

    }

    /**
     * DELETE /api/chapters/{id}
     * Deletes a chapter with the given id.
     * Returns HTTP 204 if successfully deleted.
     * Returns HTTP 404 if no chapter with the given id exists.
     * @param id the chapter's id
     * @return ResponseEntity with HTTP status code 204 if successful, or HTTP 404 if the chapter is not found.
     */
    @DeleteMapping("/api/chapters/{id}")
    @Operation(summary = "Deletes a chapter.", description = "API endpoint for deleting a chapter.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Chapter deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Chapter not found")
    })
    public ResponseEntity<Void> deleteChapter(@PathVariable UUID id) {
        try {
            chapterService.deleteChapter(id);
        }
        catch (IllegalArgumentException | EntityNotFoundException exception) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/chapers/{id}
     * Updates a chapter's title and/or order index.
     * Returns HTTP 200 if successfully updated, along with the updated chapter's info in the response body.
     * Returns HTTP 404 if no chapter with the given id exists, or if the new order index goes out of bounds.
     * @param id the chapter's id
     * @param chapterDTOPost an object containing the new title and/or order index
     * @return ResponseEntity with the updated chapter's info and HTTP status code 200 if successful, or HTTP 404 if the chapter is not found or if the new order index is out of bounds.
     */
    @PatchMapping("/api/chapters/{id}")
    @Operation(summary = "Updates a chapter's metadata.", description = "API endpoint for updating a chapter's title and/or order index.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chapter updated successfully"),
            @ApiResponse(responseCode = "404", description = "Chapter not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<ChapterDTOResponse> updateChapter(@PathVariable UUID id,
                                                        @RequestBody ChapterDTOPost chapterDTOPost) {
        ChapterDTOResponse updatedChapterInfo;
        try {
            updatedChapterInfo = new ChapterDTOResponse(chapterService.updateChapterMetadata(id, chapterDTOPost));
        }
        catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updatedChapterInfo);
    }

}
