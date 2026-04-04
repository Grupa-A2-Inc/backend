package org.elearning.backend.assessment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.elearning.backend.assessment.dto.QuestionDataForUsersDto;
import org.elearning.backend.assessment.dto.TestEditDto;
import org.elearning.backend.assessment.dto.TestEntityDto;
import org.elearning.backend.assessment.service.TestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class TestsController {

    private final TestService testService;

    private static final String CREATED = "201";
    private static final String OK = "200";
    private static final String NO_CONTENT = "204";

    private static final String CONFLICT = "409";
    private static final String FORBIDDEN = "403";
    private static final String NOT_FOUND = "404";

    // Placeholder for JWT - to be replaced with actual authentication logic
    private final UUID professorId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    public TestsController(TestService testService){
        this.testService = testService;
    }

    @Operation(summary = "Create new test",
            description = "Creates a new test for a given lesson associated with a teacher")
    @ApiResponse(responseCode = CREATED, description = "Test created")
    @ApiResponse(responseCode = NOT_FOUND, description = "Lesson does not exist")
    @ApiResponse(responseCode = CONFLICT, description = "Lesson already has an associated test")

    @PostMapping("/lessons/{lessonId}/test")
    public ResponseEntity<TestEntityDto> createTest(
            @PathVariable UUID lessonId,
            @RequestBody TestEditDto modifiableTestData){


        return ResponseEntity.status(HttpStatus.CREATED).body(testService.createNewTest(lessonId, modifiableTestData, professorId));
    }

    @Operation( summary = "Delete a test",
            description = "Deletes the test with a given ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = NO_CONTENT, description = "Test successfully deleted"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Inexistent test")
    })
    @DeleteMapping("/tests/{testId}")
    public ResponseEntity<Void> deleteTest(@PathVariable UUID testId){
        testService.deleteTest(testId);
        return ResponseEntity.noContent().build();
    }


    @Operation(summary = "Get test",
            description = "Returns a test associated a specific lesson given via lesson id")
    @ApiResponse(responseCode = OK, description = "Test successfully returned")
    @ApiResponse(responseCode = NOT_FOUND, description = "Lesson or test does not exist")
    @GetMapping("/lessons/{lessonId}/test")
    public ResponseEntity<TestEntityDto> getTestFromLesson(@PathVariable UUID lessonId){
        return ResponseEntity.ok(testService.getTestFromLesson(lessonId));
    }

    @Operation(summary = "Get test details",
            description = "Returns the details of a given test given via test id")
    @ApiResponse(responseCode = OK, description = "Test details successfully returned")
    @ApiResponse(responseCode = NOT_FOUND, description = "Test does not exist")
    @GetMapping("/tests/{testId}")
    public ResponseEntity<TestEntityDto> getTestDetails(@PathVariable UUID testId){
        return ResponseEntity.ok(testService.getTestDetails(testId));
    }

    @Operation( summary = "Update test data",
            description = "Updates the title, description, time duration and toggles the Ai usage of a given test via its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Test successfully updated"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Inexistent test")

    })
    @PatchMapping("/tests/{testId}")
    public ResponseEntity<TestEntityDto> editTestContent(
            @PathVariable UUID testId,
            @RequestBody TestEditDto editableTestContent){
        return ResponseEntity.ok(testService.updateTest(editableTestContent, testId));
    }

    @Operation( summary = "Publish test",
            description = "Makes a test public as long has it has at least one active question")
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Test successfully published"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Inexistent test"),
            @ApiResponse(responseCode = CONFLICT, description = "Insufficient questions")

    })
    @PatchMapping("/tests/{testId}/publish")
    public ResponseEntity<TestEntityDto> publishTest(@PathVariable UUID testId){
        return ResponseEntity.ok(testService.publishTest(testId));
    }


    @Operation( summary = "Get questions",
            description = "Returns the list of each question associated with a given test. Includes the correct options" +
                    "if the user is a teacher (WORK IN PROGRESS)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Questions successfully returned"),
            @ApiResponse(responseCode = FORBIDDEN, description = "Access denied"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Inexistent test")

    })
    @GetMapping("/tests/{testId}/questions")
    public ResponseEntity<List<QuestionDataForUsersDto>> getQuestions(@PathVariable UUID testId){
        return ResponseEntity.ok(testService.getListOfQuestions(testId, professorId));
    }






}
