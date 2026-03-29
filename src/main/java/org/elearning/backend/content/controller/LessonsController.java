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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Lessons", description = "Lesson administration")
@RestController
public class LessonsController {

    //Equivalent expression: @Autowired. IntelliJ says it's safer to implement it this way, however.
    //JPA makes sure to create the service automatically

    private final LessonService lessonService;

    public LessonsController(LessonService lessonService) {
        this.lessonService = lessonService;
    }


    // POST /api/chapters/{chapterID}/lessons Entrypoint
    // @Request Body - a JSON file with multiple fields for each lesson content
    // @PathVariable - extracts dynamic value directly from the URI
    // Creates a new lesson with a given chapter ID
    // Returns 201 if successfully created

    @Operation(
            summary = "Creates new lesson",
            description = "Create a new lesson associated with with a chapter via its ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Lesson successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid data (For example, missing title)"),
            @ApiResponse(responseCode = "404", description = "Inexistent chapter")
    })
    @PostMapping("/api/chapters/{chapterID}/lessons")
    public ResponseEntity<LessonDtoEntity> createNewLesson(
            @RequestBody @Valid LessonDtoPost modifiableLessonContent,
            @PathVariable UUID chapterID){
            return ResponseEntity.status(HttpStatus.CREATED).body(lessonService.createNewLesson(modifiableLessonContent, chapterID));
    }

    // DELETE /api/lessons/{id}  Entrypoint
    // @PathVariable - extracts dynamic value directly from the URI
    // Deletes a specific given lesson
    // Returns 204 if deletion took place

    @Operation( summary = "Delete a lesson",
                description = "Deletes the lesson with a given ID, making sure to also repair the order index")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Lesson successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Inexistent lesson")
    })
    @DeleteMapping("/api/lessons/{id}")
    public ResponseEntity<Void> deleteLesson(@PathVariable UUID id){
        lessonService.deleteLesson(id);
        return ResponseEntity.noContent().build();
    }


    // GET /api/chapters/{chapterID}/lessons Entrypoint
    // @PathVariable - extracts dynamic value directly from the URI
    // Returns every single lesson from a given chapter
    // Returns HTTP Status = 200 if everything went fine

    @Operation( summary = "Get all lessons",
            description = "Returns every single lesson from a specified chapter given by its ID, in order of order index")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lessons successfully returned"),
            @ApiResponse(responseCode = "404", description = "Inexistent chapter")
    })
    @GetMapping("/api/chapters/{chapterID}/lessons")
    public ResponseEntity<List<LessonDtoEntity>> getAllLessonsFromChapterID(@PathVariable UUID chapterID){
        return ResponseEntity.ok(lessonService.getAllLessonsFromChapter(chapterID));
    }

    // GET /api/chapters/{chapterID}/lessons Entrypoint
    // @PathVariable - extracts dynamic value directly from the URI
    // Returns the markdown content of a given lesson
    // Returns HTTP Status = 200 if everything went fine

    @Operation( summary = "Get lesson content ",
            description = "Returns the markdown content of a given lesson from its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Markdown successfully returned"),
            @ApiResponse(responseCode = "404", description = "Inexistent lesson or content")
    })
    @GetMapping("/api/lessons/{id}/content")
    public ResponseEntity<String> getLessonContent(
            @PathVariable UUID id){
        return ResponseEntity.ok(lessonService.getLessonContent(id));
    }

    // PATCH /api/lessons/{id}/metadata Entrypoint
    // @PathVariable - extracts dynamic value directly from the URI
    // @Request Body - a json file with a fields for both the new title and order index
    // Updates the title and/or order index of a lesson
    // Returns HTTP Status = 200 if everything went fine

    @Operation( summary = "Update lesson metadata ",
            description = "Updates either the title and order index of a given lesson from its id. " +
                    "If the order index is changed, it makes sure to also repair the order of every lesson from that chapter")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metadata successfully updated"),
            @ApiResponse(responseCode = "400", description = "Out of range order index; It cannot be lesser than 1 or bigger than the total amount of lessons inside the chapter"),
            @ApiResponse(responseCode = "404", description = "Inexistent content (lesson or chapter associated with it)")

    })
    @PatchMapping("/api/lessons/{id}/metadata")
    public ResponseEntity<LessonDtoEntity> updateLessonMetadata(
            @PathVariable UUID id,
            @RequestBody LessonDtoMetadata lessonDTOMetadata){
        return ResponseEntity.ok(lessonService.updateLessonMetadata(id, lessonDTOMetadata));
    }

    // PATCH "/api/lessons/{id}/content" Entrypoint
    // @PathVariable - extracts dynamic value directly from the URI
    // @Request Body - a json file with the content markdown information
    // Updates the content markdown of the lesson
    // Returns HTTP Status = 200 if everything went fine

    @Operation( summary = "Update lesson content",
            description = "Updates the content markdown of a lesson given by its id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Content successfully updated"),
            @ApiResponse(responseCode = "404", description = "Inexistent lesson")

    })
    @PatchMapping("/api/lessons/{id}/content")
    public ResponseEntity<LessonDtoEntity> updateLessonContent(
            @PathVariable UUID id,
            @RequestBody String markdownContent){
        return ResponseEntity.ok(lessonService.updateLessonMarkdownContent(id, markdownContent));
    }
}
