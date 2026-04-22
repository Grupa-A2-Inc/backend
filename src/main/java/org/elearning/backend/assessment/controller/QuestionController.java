package org.elearning.backend.assessment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.assessment.dto.question_dto.QuestionRequestDto;
import org.elearning.backend.assessment.dto.question_dto.QuestionResponseDto;
import org.elearning.backend.assessment.model.QuestionType;
import org.elearning.backend.assessment.service.QuestionService;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tests/{testId}/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    private static final String CREATED = "201";
    private static final String OK = "200";
    private static final String NO_CONTENT = "204";

    private static final String BAD_REQUEST = "400";
    private static final String FORBIDDEN = "403";
    private static final String NOT_FOUND = "404";

    @Operation(summary = "Add a question to a test.",
            description = "Creates a question (Single, Multi, T/F) for a test in DRAFT state.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = CREATED, description = "Question created successfully."),
            @ApiResponse(responseCode = BAD_REQUEST, description = "Error validating options."),
            @ApiResponse(responseCode = FORBIDDEN, description = "You are not the test owner or the test is not in DRAFT state."),
            @ApiResponse(responseCode = NOT_FOUND, description = "Test was not found.")
    })
    @PostMapping
    @PreAuthorize("@accessService.canCreateTestQuestion(authentication,#id)")
    public ResponseEntity<QuestionResponseDto> createQuestion(
            @P("id") @PathVariable UUID testId,
            @RequestBody QuestionRequestDto requestDto,
            @AuthenticationPrincipal UserDetails user
    ) {
        UUID professorId = extractUserId(user);
        QuestionResponseDto createdQuestion = questionService.createQuestion(testId, requestDto, professorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdQuestion);
    }

    @Operation(summary = "Get a specific question", description = "Retrieves the details of a specific question by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Successfully retrieved the question"),
            @ApiResponse(responseCode = BAD_REQUEST, description = "Validation error (the question does not belong to the specified test)"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Question not found")
    })
    @GetMapping("/{questionId}")
    @PreAuthorize("@accessService.canViewTestQuestion(authentication,#id1,#id2)")
    public ResponseEntity<QuestionResponseDto> getQuestionById(
            @P("id1") @PathVariable UUID testId,
            @P("id2") @PathVariable Integer questionId
    ) {
        return ResponseEntity.ok(questionService.getQuestionById(testId, questionId));
    }

    @Operation(
            summary = "Get filtered and sorted questions for a test",
            description = "Retrieves a list of questions associated with a specific test. Supports optional filtering by question type and difficulty, and dynamic sorting."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Successfully retrieved the list of questions (returns an empty list if no questions match the filters)"),
            @ApiResponse(responseCode = BAD_REQUEST, description = "Invalid request parameters (e.g., wrong sorting direction or invalid enum value for question type)")
    })
    @GetMapping
    @PreAuthorize("@accessService.canViewTestQuestions(authentication,#id)")
    public ResponseEntity<List<QuestionResponseDto>> getQuestions(
            @P("id") @PathVariable UUID testId,
            @RequestParam(required = false) QuestionType questionType,
            @RequestParam(required = false) BigDecimal difficulty,
            @RequestParam(required = false, defaultValue = "displayOrder") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir
    ) {
        return ResponseEntity.ok(
                questionService.getFilteredAndSortedQuestions(testId, questionType, difficulty, sortBy, sortDir)
        );
    }

    @Operation(summary = "Update an existing question", description = "Fully updates a question and its options. The test must be in DRAFT state and owned by the authenticated professor.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Question updated successfully"),
            @ApiResponse(responseCode = BAD_REQUEST, description = "Validation error (e.g., test is not DRAFT, options are invalid, or ID mismatch)"),
            @ApiResponse(responseCode = FORBIDDEN, description = "Access denied (you are not the owner of the test)"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Question or test not found")
    })
    @PutMapping("/{questionId}")
    @PreAuthorize("@accessService.canEditTestQuestion(authentication,#id1,#id2)")
    public ResponseEntity<QuestionResponseDto> updateQuestion(
            @P("id1") @PathVariable UUID testId,
            @P("id2") @PathVariable Integer questionId,
            @RequestBody QuestionRequestDto requestDto,
            @AuthenticationPrincipal UserDetails user
    ) {
        UUID professorId = extractUserId(user);
        QuestionResponseDto updatedQuestion = questionService.updateQuestion(testId, questionId, requestDto, professorId);
        return ResponseEntity.ok(updatedQuestion);
    }

    @Operation(summary = "Delete a question", description = "Deletes a question and its associated options. The test must be in DRAFT state and owned by the authenticated professor.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = NO_CONTENT, description = "Question successfully deleted"),
            @ApiResponse(responseCode = BAD_REQUEST, description = "Validation error (e.g., test is not DRAFT or ID mismatch)"),
            @ApiResponse(responseCode = FORBIDDEN, description = "Access denied (you are not the owner of the test)"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Question or test not found")
    })
    @DeleteMapping("/{questionId}")
    @PreAuthorize("@accessService.canDeleteTestQuestion(authentication,#id1,#id2)")
    public ResponseEntity<Void> deleteQuestion(
            @P("id1") @PathVariable UUID testId,
            @P("id2") @PathVariable Integer questionId,
            @AuthenticationPrincipal UserDetails user
    ) {
        UUID professorId = extractUserId(user);
        questionService.deleteQuestion(testId, questionId, professorId);
        return ResponseEntity.noContent().build();
    }

    private UUID extractUserId(UserDetails user) {
        if (user instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUserId();
        }

        return UUID.fromString(user.getUsername());
    }
}
