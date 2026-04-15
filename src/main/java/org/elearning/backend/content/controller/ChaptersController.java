package org.elearning.backend.content.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.elearning.backend.content.dto.ChapterDtoPost;
import org.elearning.backend.content.dto.ChapterDtoResponse;
import org.elearning.backend.content.model.Chapter;
import org.elearning.backend.content.service.ChapterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Chapters", description = "Chapter administration")
@RestController
@RequestMapping("/api/v1")
public class ChaptersController {
    private final ChapterService chapterService;

    public ChaptersController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @Operation(summary = "Create a new chapter", description = "Creates a new chapter inside a course given by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Chapter successfully created"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @PostMapping("/courses/{courseId}/chapters")
    @PreAuthorize("@accessService.canCreateChapter(authentication,#id)")
    public ResponseEntity<ChapterDtoResponse> createNewChapter(@P("id") @PathVariable UUID courseId,
                                                               @RequestBody String newChapterTitle) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ChapterDtoResponse(chapterService.createNewChapter(courseId, newChapterTitle)));
    }

    @Operation(summary = "Get all chapters", description = "Returns all chapters from a course given by its ID, ordered by order index")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chapters successfully returned"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @GetMapping("/courses/{courseId}/chapters")
    @PreAuthorize("@accessService.canViewCourseChapters(authentication,#id)")
    public ResponseEntity<List<ChapterDtoResponse>> getChaptersByCourseId(@P("id") @PathVariable UUID courseId) {
        List<Chapter> chapters = chapterService.getAllChaptersFromCourse(courseId);
        return ResponseEntity.ok(chapters.stream().map(ChapterDtoResponse::new).toList());
    }

    @Operation(summary = "Delete a chapter", description = "Deletes a chapter given by its ID, repairing the order index of remaining chapters")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Chapter successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Chapter not found")
    })
    @DeleteMapping("/chapters/{id}")
    @PreAuthorize("@accessService.canDeleteChapter(authentication,#id)")
    public ResponseEntity<Void> deleteChapter(@P("id") @PathVariable UUID id) {
        chapterService.deleteChapter(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update chapter metadata", description = "Updates the title and/or order index of a chapter given by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chapter successfully updated"),
            @ApiResponse(responseCode = "400", description = "Order index out of range"),
            @ApiResponse(responseCode = "404", description = "Chapter not found")
    })
    @PatchMapping("/chapters/{id}")
    @PreAuthorize("@accessService.canEditChapter(authentication,#id)")
    public ResponseEntity<ChapterDtoResponse> updateChapter(@P("id") @PathVariable UUID id,
                                                            @RequestBody ChapterDtoPost chapterDTOPost) {
        return ResponseEntity.ok(new ChapterDtoResponse(chapterService.updateChapterMetadata(id, chapterDTOPost)));
    }
}