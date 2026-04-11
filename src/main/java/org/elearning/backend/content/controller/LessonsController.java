package org.elearning.backend.content.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.elearning.backend.content.dto.LessonDtoEntity;
import org.elearning.backend.content.dto.LessonDtoMetadata;
import org.elearning.backend.content.dto.LessonDtoPost;
import org.elearning.backend.content.service.LessonService;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Lessons", description = "Lesson administration")
@RestController
@RequestMapping("/api/v1")
public class LessonsController {

    private final LessonService lessonService;

    public LessonsController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @Operation(summary = "Create a new lesson", description = "Creates a new lesson associated with a chapter given by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Lesson successfully created"),
            @ApiResponse(responseCode = "400", description = "Title cannot be null"),
            @ApiResponse(responseCode = "404", description = "Chapter not found")
    })
    @PostMapping("/chapters/{chapterID}/lessons")
    public ResponseEntity<LessonDtoEntity> createNewLesson(
            @RequestBody @Valid LessonDtoPost modifiableLessonContent,
            @PathVariable UUID chapterID) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lessonService.createNewLesson(modifiableLessonContent, chapterID));
    }

    @Operation(summary = "Delete a lesson", description = "Deletes a lesson given by its ID, repairing the order index of remaining lessons")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Lesson successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Lesson not found")
    })
    @DeleteMapping("/lessons/{id}")
    public ResponseEntity<Void> deleteLesson(@PathVariable UUID id) {
        lessonService.deleteLesson(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all lessons", description = "Returns all lessons from a chapter given by its ID, ordered by order index")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lessons successfully returned"),
            @ApiResponse(responseCode = "404", description = "Chapter not found")
    })
    @GetMapping("/chapters/{chapterID}/lessons")
    public ResponseEntity<List<LessonDtoEntity>> getAllLessonsFromChapterID(@PathVariable UUID chapterID) {
        return ResponseEntity.ok(lessonService.getAllLessonsFromChapter(chapterID));
    }

    @Operation(summary = "Get lesson content", description = "Returns the markdown content of a lesson given by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Content successfully returned"),
            @ApiResponse(responseCode = "404", description = "Lesson not found")
    })
    @GetMapping("/lessons/{id}/content")
    public ResponseEntity<String> getLessonContent(@PathVariable UUID id) {
        return ResponseEntity.ok(lessonService.getLessonContent(id));
    }

    @Operation(summary = "Update lesson metadata", description = "Updates the title and/or order index of a lesson given by its ID, repairing the order of remaining lessons if needed")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metadata successfully updated"),
            @ApiResponse(responseCode = "400", description = "Order index out of range"),
            @ApiResponse(responseCode = "404", description = "Lesson not found")
    })
    @PatchMapping("/lessons/{id}/metadata")
    public ResponseEntity<LessonDtoEntity> updateLessonMetadata(
            @PathVariable UUID id,
            @RequestBody LessonDtoMetadata lessonDTOMetadata) {
        return ResponseEntity.ok(lessonService.updateLessonMetadata(id, lessonDTOMetadata));
    }

    @Operation(summary = "Update lesson content", description = "Updates the markdown content of a lesson given by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Content successfully updated"),
            @ApiResponse(responseCode = "404", description = "Lesson not found")
    })
    @PatchMapping("/lessons/{id}/content")
    public ResponseEntity<LessonDtoEntity> updateLessonContent(
            @PathVariable UUID id,
            @RequestBody String markdownContent) {
        return ResponseEntity.ok(lessonService.updateLessonMarkdownContent(id, markdownContent));
    }

    @Operation(summary = "Get lesson by ID", description = "Returns a lesson given by its ID. If the user is a student, it will mark it as visited")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lesson successfully returned"),
            @ApiResponse(responseCode = "404", description = "Lesson not found")
    })

    @GetMapping("/lessons/{id}")
    public ResponseEntity<LessonDtoEntity> getLessonByID(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID id) {
        return ResponseEntity.ok(lessonService.getLessonById(userDetails.getUserId(), userDetails.getRoleName(), id));
    }
}