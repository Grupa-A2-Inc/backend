package org.elearning.backend.assessment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
// SWAGGER ADDED
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.assessment.dto.attempt_dto.AttemptReportDTO;
import org.elearning.backend.assessment.dto.attempt_dto.AttemptStatusDTO;
import org.elearning.backend.assessment.service.TestResultService;
import org.elearning.backend.common.GlobalHttpStatusCodes;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// SWAGGER ADDED
@Tag(name = "Test Results", description = "Test attempt results and history")
@RestController
@RequiredArgsConstructor
public class TestResultController extends GlobalHttpStatusCodes {
    private final TestResultService testResultService;

    /**
     * Retrieve the result of a specific test attempt for the authenticated user.
     *
     * @param attemptId  UUID of the attempt to fetch results for
     * @param currentUser  the authenticated principal used to determine the requesting user's id
     * @return  an AttemptReportDTO containing the attempt score and per-question details
     */
    @Operation(
            summary = "Get a completed attempt result",
            description = """
                    Returns the review payload for one attempt of the authenticated student.

                    Response details:
                    - attempt-level fields: attemptId, score, scorePercent, passed, completedAt
                    - per-question fields: questionId, questionType, content
                    - selectedOptionIds and correctOptionIds are preserved for backward compatibility
                    - options now contains the full option list for each question, including optionId, text,
                      displayOrder, and the review flags selected/correct so the frontend does not need to
                      resolve option text from ids on its own

                    This endpoint is available only after the attempt is finished.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Attempt result retrieved successfully"),
            @ApiResponse(responseCode = FORBIDDEN, description = "The attempt is still in progress"),
            @ApiResponse(responseCode = GONE, description = "The attempt expired before submission"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Attempt not found or no result is stored for it")
    })
    @GetMapping("/api/v1/attempts/{attemptId}/result")
    @PreAuthorize("@accessService.canViewAttemptResult(authentication,#id)")
    public ResponseEntity<AttemptReportDTO> getResult(@P("id") @PathVariable UUID attemptId,@AuthenticationPrincipal UserDetails currentUser) {
        UUID userId = extractUserId(currentUser);
        AttemptReportDTO response = testResultService.getTestResult(attemptId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve the authenticated user's attempts for a given test, including score and status for each attempt.
     *
     * @param testId the UUID of the test whose attempts to retrieve
     * @return a list of AttemptStatusDTO containing score and status for each attempt
     */
    @Operation(
            summary = "Get my attempts for one test",
            description = """
                    Returns the authenticated student's attempts for a single test version.

                    Each item contains the attempt id, attempt number, score, scorePercent, passed flag,
                    startedAt and current status. The list is ordered by attempt start time descending.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Attempts retrieved successfully"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Test not found")
    })
    @GetMapping("/api/v1/tests/{testId}/my-attempts")
    @PreAuthorize("@accessService.canViewMyTestAttempts(authentication,#id)")
    public ResponseEntity<List<AttemptStatusDTO>> getAttempts(@P("id") @PathVariable UUID testId ,@AuthenticationPrincipal UserDetails currentUser) {
        UUID userId = extractUserId(currentUser);

        List<AttemptStatusDTO> response = testResultService.getTestAttempts(testId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve the authenticated user's attempts across all test versions for a given lesson.
     *
     * @param lessonId the UUID of the lesson whose test attempts to retrieve
     * @return a list of AttemptStatusDTO containing score, status and test-version metadata for each attempt
     */
    @Operation(
            summary = "Get my attempts for all test versions of a lesson",
            description = """
                    Returns the authenticated student's attempts across every test version that belongs to the lesson.

                    In addition to the usual attempt summary fields, each item includes testId, testTitle and
                    testVersion so the frontend can group or label attempts by version.
                    The list is ordered by attempt start time descending.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Lesson attempts retrieved successfully"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Lesson not found")
    })
    @GetMapping("/api/v1/lessons/{lessonId}/my-attempts")
    @PreAuthorize("@accessService.canViewMyLessonAttempts(authentication,#id)")
    public ResponseEntity<List<AttemptStatusDTO>> getLessonAttempts(@P("id") @PathVariable UUID lessonId,
                                                                    @AuthenticationPrincipal UserDetails currentUser) {
        UUID userId = extractUserId(currentUser);

        List<AttemptStatusDTO> response = testResultService.getLessonAttempts(lessonId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve the authenticated user's best finished attempt for the specified test by score percentage.
     *
     * @param testId the UUID of the test to query
     * @return the best finished AttemptStatusDTO for the authenticated user and specified test; if no finished attempts exist for the test a 404 response is produced
     */
    @Operation(
            summary = "Get my best finished attempt for one test",
            description = """
                    Returns the authenticated student's best finished attempt for the given test.

                    Only attempts with status DONE are considered. The best attempt is selected by scorePercent
                    descending and returned as an AttemptStatusDTO.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Best finished attempt retrieved successfully"),
            @ApiResponse(responseCode = NOT_FOUND, description = "No finished attempts found for the specified test")
    })
    @GetMapping("/api/v1/tests/{testId}/my-best")
    @PreAuthorize("@accessService.canViewMyBestTestResult(authentication,#id)")
    public ResponseEntity<AttemptStatusDTO> getBestAttempt(@P("id") @PathVariable UUID testId,@AuthenticationPrincipal UserDetails currentUser) {
        UUID userId = extractUserId(currentUser);


        AttemptStatusDTO response = testResultService.getBestTestAttempt(testId, userId);
        return ResponseEntity.ok(response);
    }

    private UUID extractUserId(UserDetails currentUser) {
        if (currentUser instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUserId();
        }

        return UUID.fromString(currentUser.getUsername());
    }
}
