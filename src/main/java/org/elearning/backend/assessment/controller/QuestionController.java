package org.elearning.backend.assessment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.assessment.dto.QuestionRequestDto;
import org.elearning.backend.assessment.dto.QuestionResponseDto;
import org.elearning.backend.assessment.service.QuestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tests/{testId}/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @Operation(summary = "Add a question to a test.",
            description = "Creates a question (Single, Multi, T/F) for a test in DRAFT state.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Question created successfully."),
            @ApiResponse(responseCode = "400", description = "Error validating options."),
            @ApiResponse(responseCode = "403", description = "You are not the test owner or the test is not in DRAFT state."),
            @ApiResponse(responseCode = "404", description = "Test was not found.")
    })
    @PostMapping
    public ResponseEntity<QuestionResponseDto> createQuestion(
            @PathVariable UUID testId,
            @RequestBody QuestionRequestDto requestDto,
            @AuthenticationPrincipal UserDetails user
    ) {
        UUID professorId = UUID.fromString(user.getUsername());
        QuestionResponseDto createdQuestion = questionService.createQuestion(testId, requestDto, professorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdQuestion);
    }

    @Operation(summary = "Get all questions for a test", description = "Retrieves a list of all questions associated with the specified test ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of questions")
    })
    @GetMapping
    public ResponseEntity<List<QuestionResponseDto>> getAllQuestions(
            @PathVariable UUID testId
    ) {
        return ResponseEntity.ok(questionService.getAllQuestionsForTest(testId));
    }

    @Operation(summary = "Get a specific question", description = "Retrieves the details of a specific question by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the question"),
            @ApiResponse(responseCode = "400", description = "Validation error (the question does not belong to the specified test)"),
            @ApiResponse(responseCode = "404", description = "Question not found")
    })
    @GetMapping("/{questionId}")
    public ResponseEntity<QuestionResponseDto> getQuestionById(
            @PathVariable UUID testId,
            @PathVariable Integer questionId
    ) {
        return ResponseEntity.ok(questionService.getQuestionById(testId, questionId));
    }

    @Operation(summary = "Update an existing question", description = "Fully updates a question and its options. The test must be in DRAFT state and owned by the authenticated professor.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Question updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error (e.g., test is not DRAFT, options are invalid, or ID mismatch)"),
            @ApiResponse(responseCode = "403", description = "Access denied (you are not the owner of the test)"),
            @ApiResponse(responseCode = "404", description = "Question or test not found")
    })
    @PutMapping("/{questionId}")
    public ResponseEntity<QuestionResponseDto> updateQuestion(
            @PathVariable UUID testId,
            @PathVariable Integer questionId,
            @RequestBody QuestionRequestDto requestDto,
            @AuthenticationPrincipal UserDetails user
    ) {
        UUID professorId = UUID.fromString(user.getUsername());
        QuestionResponseDto updatedQuestion = questionService.updateQuestion(testId, questionId, requestDto, professorId);
        return ResponseEntity.ok(updatedQuestion);
    }

    @Operation(summary = "Delete a question", description = "Deletes a question and its associated options. The test must be in DRAFT state and owned by the authenticated professor.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Question successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Validation error (e.g., test is not DRAFT or ID mismatch)"),
            @ApiResponse(responseCode = "403", description = "Access denied (you are not the owner of the test)"),
            @ApiResponse(responseCode = "404", description = "Question or test not found")
    })
    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable UUID testId,
            @PathVariable Integer questionId,
            @AuthenticationPrincipal UserDetails user
    ) {
        UUID professorId = UUID.fromString(user.getUsername());
        questionService.deleteQuestion(testId, questionId, professorId);
        return ResponseEntity.noContent().build();
    }
}