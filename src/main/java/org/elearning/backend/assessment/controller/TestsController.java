package org.elearning.backend.assessment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.elearning.backend.assessment.dto.TestEditDto;
import org.elearning.backend.assessment.dto.TestEntityDto;
import org.elearning.backend.assessment.service.TestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class TestsController {

    private final TestService testService;

    public TestsController(TestService testService){
        this.testService = testService;
    }

    @Operation(summary = "Create new test",
            description = "Creates a new test for a given lesson associated with a teacher")
    @ApiResponse(responseCode = "201", description = "Test created")
    @ApiResponse(responseCode = "404", description = "Lesson does not exist")
    @ApiResponse(responseCode = "409", description = "Lesson already has an associated test")

    @PostMapping("/api/lessons/{lessonId}/test")
    public ResponseEntity<TestEntityDto> createTest(
            @PathVariable UUID lessonId,
            @RequestBody TestEditDto modifiableTestData){

        // Placeholder for JWT - to be replaced with actual authentication logic
        UUID professorId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        return ResponseEntity.status(HttpStatus.CREATED).body(testService.createNewTest(lessonId, modifiableTestData, professorId));
    }

    @Operation( summary = "Delete a test",
            description = "Deletes the test with a given ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Test successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Inexistent test")
    })
    @DeleteMapping("/api/tests/{testId}")
    public ResponseEntity<Void> deleteTest(@PathVariable UUID testId){
        testService.deleteTest(testId);
        return ResponseEntity.noContent().build();
    }


    @Operation(summary = "Get test",
            description = "Returns a test associated a specific lesson given via lesson id")
    @ApiResponse(responseCode = "200", description = "Test successfully returned")
    @ApiResponse(responseCode = "404", description = "Lesson or test does not exist")
    @GetMapping("/api/lessons/{lessonId}/test")
    public ResponseEntity<TestEntityDto> getTestFromLesson(@PathVariable UUID lessonId){
        return ResponseEntity.ok(testService.getTestFromLesson(lessonId));
    }

    @Operation(summary = "Get test details",
            description = "Returns the details of a given test given via test id")
    @ApiResponse(responseCode = "200", description = "Test details successfully returned")
    @ApiResponse(responseCode = "404", description = "Test does not exist")
    @GetMapping("/api/tests/{testId}")
    public ResponseEntity<TestEntityDto> getTestDetails(@PathVariable UUID testId){
        return ResponseEntity.ok(testService.getTestDetails(testId));
    }

    @Operation( summary = "Update test data",
            description = "Updates the title, description, time duration and toggles the Ai usage of a given test via its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Test successfully updated"),
            @ApiResponse(responseCode = "404", description = "Inexistent test")

    })
    @PatchMapping("/api/tests/{testId}")
    public ResponseEntity<TestEntityDto> editTestContent(
            @PathVariable UUID testId,
            @RequestBody TestEditDto editableTestContent){
        return ResponseEntity.ok(testService.updateTest(editableTestContent, testId));
    }

    @Operation( summary = "Publish test",
            description = "Makes a test public as long has it has at least one active question")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Test successfully published"),
            @ApiResponse(responseCode = "404", description = "Inexistent test"),
            @ApiResponse(responseCode = "409", description = "Insufficient questions")

    })
    @PatchMapping("/api/tests/{testId}/publish")
    public ResponseEntity<TestEntityDto> publishTest(@PathVariable UUID testId){
        return ResponseEntity.ok(testService.publishTest(testId));
    }






}
