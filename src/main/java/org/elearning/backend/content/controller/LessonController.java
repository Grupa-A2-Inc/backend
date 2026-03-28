package org.elearning.backend.content.controller;


import jakarta.validation.Valid;
import org.elearning.backend.content.dto.LessonDTOEntity;
import org.elearning.backend.content.dto.LessonDTOMetadata;
import org.elearning.backend.content.dto.LessonDTOPost;
import org.elearning.backend.content.model.Lesson;
import org.elearning.backend.content.service.LessonService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class LessonController {

    //Equivalent expression: @Autowired. IntelliJ says it's safer to implement it this way, however.
    //JPA makes sure to create the service automatically

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    // Generic entrypoint purely for testing

    @GetMapping("/api/lesson-test")
    public String testMe() {
        return "LessonController functioneaza!";
    }

    // POST /api/chapters/{chapterID}/lessons Entrypoint
    // @Request Body - a JSON file with multiple fields for each lesson content
    // @PathVariable - extracts dynamic value directly from the URI
    // Creates a new lesson with a given chapter ID
    // Returns 201 if successfully created

    @PostMapping("/api/chapters/{chapterID}/lessons")
    public ResponseEntity<LessonDTOEntity> createNewLesson(
            @RequestBody @Valid LessonDTOPost modifiableLessonContent,
            @PathVariable UUID chapterID){
            return ResponseEntity.status(HttpStatus.CREATED).body(lessonService.createNewLesson(modifiableLessonContent, chapterID));
    }

    // DELETE /api/lessons/{id}  Entrypoint
    // @PathVariable - extracts dynamic value directly from the URI
    // Deletes a specific given lesson
    // Returns 204 if deletion took place

    @DeleteMapping("/api/lessons/{id}")
    public ResponseEntity<Void> deleteLesson(@PathVariable UUID id){
        lessonService.deleteLesson(id);
        return ResponseEntity.noContent().build();
    }


    // GET /api/chapters/{chapterID}/lessons Entrypoint
    // @PathVariable - extracts dynamic value directly from the URI
    // Returns every single lesson from a given chapter
    // Returns HTTP Status = 200 if everything went fine

    @GetMapping("/api/chapters/{chapterID}/lessons")
    public ResponseEntity<List<LessonDTOEntity>> getAllLessonsFromChapterID(@PathVariable UUID chapterID){
        return ResponseEntity.ok(lessonService.getAllLessonsFromChapter(chapterID));
    }

    // GET /api/chapters/{chapterID}/lessons Entrypoint
    // @PathVariable - extracts dynamic value directly from the URI
    // Returns every single lesson from a given chapter
    // Returns HTTP Status = 200 if everything went fine

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

    @PatchMapping("/api/lessons/{id}/metadata")
    public ResponseEntity<LessonDTOEntity> updateLessonMetadata(
            @PathVariable UUID id,
            @RequestBody LessonDTOMetadata lessonDTOMetadata){
        return ResponseEntity.ok(lessonService.updateLessonMetadata(id, lessonDTOMetadata));
    }

    // PATCH "/api/lessons/{id}/content" Entrypoint
    // @PathVariable - extracts dynamic value directly from the URI
    // @Request Body - a json file with the content markdown information
    // Updates the content markdown of the lesson
    // Returns HTTP Status = 200 if everything went fine

    @PatchMapping("/api/lessons/{id}/content")
    public ResponseEntity<LessonDTOEntity> updateLessonContent(
            @PathVariable UUID id,
            @RequestBody String markdownContent){
        return ResponseEntity.ok(lessonService.updateLessonMarkdownContent(id, markdownContent));
    }



}
