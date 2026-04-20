package org.elearning.backend.assessment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.elearning.backend.assessment.dto.question_dto.QuestionDataForUsersDto;
import org.elearning.backend.assessment.dto.assigment_dto.TestEditDto;
import org.elearning.backend.assessment.dto.assigment_dto.TestEntityDto;
import org.elearning.backend.assessment.service.TestService;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
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



    public TestsController(TestService testService){
        this.testService = testService;
    }

    @Operation(summary = "Create new test",
            description = "Creates a new test for a given lesson associated with a teacher")
    @ApiResponse(responseCode = CREATED, description = "Test created")
    @ApiResponse(responseCode = NOT_FOUND, description = "Lesson does not exist")
    @ApiResponse(responseCode = CONFLICT, description = "Lesson already has an associated test")

    @PostMapping("/lessons/{lessonId}/test")
    @PreAuthorize("@accessService.canCreateLessonTest(authentication,#id)")
    public ResponseEntity<TestEntityDto> createTest(
            @P("id")@PathVariable UUID lessonId,
            @RequestBody TestEditDto modifiableTestData,@AuthenticationPrincipal CustomUserDetails currentUser) {
        UUID userId = currentUser.getUserId();


        return ResponseEntity.status(HttpStatus.CREATED).body(testService.createNewTest(lessonId, modifiableTestData, userId));
    }

    @Operation( summary = "Delete a test",
            description = "Deletes the test with a given ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = NO_CONTENT, description = "Test successfully deleted"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Inexistent test")
    })
    @DeleteMapping("/tests/{testId}")
    @PreAuthorize("@accessService.canDeleteTest(authentication,#id)")
    public ResponseEntity<Void> deleteTest(@P("id") @PathVariable UUID testId){
        testService.deleteTest(testId);
        return ResponseEntity.noContent().build();
    }


    @Operation(summary = "Get test",
            description = "Returns a test associated a specific lesson given via lesson id")
    @ApiResponse(responseCode = OK, description = "Test successfully returned")
    @ApiResponse(responseCode = NOT_FOUND, description = "Lesson or test does not exist")
    @GetMapping("/lessons/{lessonId}/test")
    @PreAuthorize("@accessService.canViewLessonTest(authentication,#id)")
    public ResponseEntity<TestEntityDto> getTestFromLesson(@P("id") @PathVariable UUID lessonId){
        return ResponseEntity.ok(testService.getTestFromLesson(lessonId));
    }

    @Operation(summary = "Get test details",
            description = "Returns the details of a given test given via test id")
    @ApiResponse(responseCode = OK, description = "Test details successfully returned")
    @ApiResponse(responseCode = NOT_FOUND, description = "Test does not exist")
    @GetMapping("/tests/{testId}")
    @PreAuthorize("@accessService.canViewTest(authentication,#id)")
    public ResponseEntity<TestEntityDto> getTestDetails(@P("id") @PathVariable UUID testId){
        return ResponseEntity.ok(testService.getTestDetails(testId));
    }

    @Operation( summary = "Update test data",
            description = "Updates the title, description, time duration and toggles the Ai usage of a given test via its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Test successfully updated"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Inexistent test")

    })
    @PatchMapping("/tests/{testId}")
    @PreAuthorize("@accessService.canEditTest(authentication,#id)")
    public ResponseEntity<TestEntityDto> editTestContent(
            @P("id") @PathVariable UUID testId,
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
    @PreAuthorize("@accessService.canPublishTest(authentication,#id)")
    @PatchMapping("/tests/{testId}/publish")
    public ResponseEntity<TestEntityDto> publishTest(@P("id") @PathVariable UUID testId){
        return ResponseEntity.ok(testService.publishTest(testId));
    }


    @Operation( summary = "Get questions",
            description = "Returns the list of each question associated with a given test. Includes the correct options" +
                    "if the user is a teacher")
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Questions successfully returned"),
            @ApiResponse(responseCode = FORBIDDEN, description = "Access denied"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Inexistent test")

    })

    @GetMapping("/tests/{testId}/questions")
    @PreAuthorize("@accessService.canViewTestQuestions(authentication,#id)")
    public ResponseEntity<List<QuestionDataForUsersDto>> getQuestions(@P("id") @PathVariable UUID testId,@AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(testService.getListOfQuestions(testId, currentUser.getRoleName()));
    }






}
