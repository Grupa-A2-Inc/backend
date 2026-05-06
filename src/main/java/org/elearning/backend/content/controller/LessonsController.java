package org.elearning.backend.content.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.common.GlobalHttpStatusCodes;
import org.elearning.backend.content.dto.LessonDtoEntity;
import org.elearning.backend.content.dto.LessonDtoMetadata;
import org.elearning.backend.content.dto.LessonDtoPost;
import org.elearning.backend.content.service.LessonService;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Lessons", description = "Lesson administration")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LessonsController extends GlobalHttpStatusCodes {

    private final LessonService lessonService;

    @Operation(summary = "Create a new lesson", description = "Creates a new lesson associated with a chapter given by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = CREATED, description = "Lesson successfully created"),
            @ApiResponse(responseCode = BAD_REQUEST, description = "Title cannot be null"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Chapter not found")
    })

    @PostMapping("/chapters/{chapterID}/lessons")
    @PreAuthorize("@accessService.canCreateLessons(authentication,#id)")
    public ResponseEntity<LessonDtoEntity> createNewLesson(
            @RequestBody @Valid LessonDtoPost modifiableLessonContent,
            @P("id") @PathVariable UUID chapterID) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lessonService.createNewLesson(modifiableLessonContent, chapterID));
    }

    @Operation(summary = "Delete a lesson", description = "Deletes a lesson given by its ID, repairing the order index of remaining lessons")
    @ApiResponses(value = {
            @ApiResponse(responseCode = NO_CONTENT, description = "Lesson successfully deleted"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Lesson not found")
    })
    @DeleteMapping("/lessons/{id}")
    @PreAuthorize("@accessService.canDeleteLesson(authentication,#id)")
    public ResponseEntity<Void> deleteLesson(@P("id") @PathVariable UUID id) {
        lessonService.deleteLesson(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all lessons", description = "Returns all lessons from a chapter given by its ID, ordered by order index")
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Lessons successfully returned"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Chapter not found")
    })
    @GetMapping("/chapters/{chapterID}/lessons")
    @PreAuthorize("@accessService.canViewChapterLessons(authentication,#id)")
    public ResponseEntity<List<LessonDtoEntity>> getAllLessonsFromChapterID(@P("id") @PathVariable UUID chapterID) {
        return ResponseEntity.ok(lessonService.getAllLessonsFromChapter(chapterID));
    }

    @Operation(summary = "Get lesson content", description = "Returns the markdown content of a lesson given by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Content successfully returned"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Lesson not found")
    })
    @GetMapping("/lessons/{id}/content")
    @PreAuthorize("@accessService.canViewLessonContent(authentication,#id)")
    public ResponseEntity<String> getLessonContent(@P("id") @PathVariable UUID id) {
        return ResponseEntity.ok(lessonService.getLessonContent(id));
    }

    @Operation(summary = "Update lesson metadata", description = "Updates the title and/or order index of a lesson given by its ID, repairing the order of remaining lessons if needed")
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Metadata successfully updated"),
            @ApiResponse(responseCode = BAD_REQUEST, description = "Order index out of range"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Lesson not found")
    })
    @PatchMapping("/lessons/{id}/metadata")
    @PreAuthorize("@accessService.canEditLessonMetaData(authentication,#id)")
    public ResponseEntity<LessonDtoEntity> updateLessonMetadata(
            @P("id") @PathVariable UUID id,
            @RequestBody LessonDtoMetadata lessonDTOMetadata) {
        return ResponseEntity.ok(lessonService.updateLessonMetadata(id, lessonDTOMetadata));
    }

    @Operation(summary = "Update lesson content", description = "Updates the markdown content of a lesson given by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Content successfully updated"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Lesson not found")
    })
    @PatchMapping("/lessons/{id}/content")
    @PreAuthorize("@accessService.canEditLessonContent(authentication,#id)")
    public ResponseEntity<LessonDtoEntity> updateLessonContent(
            @P("id") @PathVariable UUID id,
            @RequestBody String markdownContent) {
        return ResponseEntity.ok(lessonService.updateLessonMarkdownContent(id, markdownContent));
    }

    @Operation(summary = "Get lesson by ID", description = "Returns a lesson given by its ID. If the user is a student, it will mark it as visited")
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Lesson successfully returned"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Lesson not found")
    })

    @GetMapping("/lessons/{id}")
    @PreAuthorize("@accessService.canMarkViewedLesson(authentication,#id)")
    public ResponseEntity<LessonDtoEntity> getLessonByID(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID id) {
        return ResponseEntity.ok(lessonService.getLessonById(userDetails.getUserId(), userDetails.getRoleName(), id));
    }
}