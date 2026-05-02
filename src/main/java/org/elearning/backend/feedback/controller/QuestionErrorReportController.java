package org.elearning.backend.feedback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.elearning.backend.feedback.dto.DescriptionRequestDto;
import org.elearning.backend.feedback.dto.ErrorReportDto;
import org.elearning.backend.feedback.dto.GetErrorReportDto;
import org.elearning.backend.feedback.exception.DifferentIdException;
import org.elearning.backend.feedback.model.ReportStatus;
import org.elearning.backend.feedback.service.QuestionErrorReportService;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/v1")
public class QuestionErrorReportController {

    private static final String OK = "200";
    private static final String CREATED = "201";

    private static final String BAD_REQUEST = "400";
    private static final String FORBIDDEN = "403";
    private static final String NOT_FOUND = "404";

    private final QuestionErrorReportService questionErrorReportService;

    public QuestionErrorReportController(QuestionErrorReportService questionErrorReportService) {
        this.questionErrorReportService = questionErrorReportService;
    }

    @Operation(summary = "Create a new report", description = "Allows the student to report issues they found at a given" +
            "question given via question id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = CREATED, description = "Report successfully created"),
            @ApiResponse(responseCode = BAD_REQUEST, description = "Description is empty, too short or too long"),
            @ApiResponse(responseCode = FORBIDDEN, description = "User doesn't have access to the question"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Question not found")
    })
    @PostMapping("/questions/{questionId}/error-reports")
    @PreAuthorize("@questionAccessValidatorService.hasStudentAccessToQuestion(authentication,#id)")
    public ResponseEntity<ErrorReportDto> createNewReport(
            @RequestBody @Valid DescriptionRequestDto description,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @P("id") @PathVariable Integer questionId) {


        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(questionErrorReportService.createReport(questionId, customUserDetails.getUserId(), description));
    }

    //-------- Dev4 --------
    @Operation(
            summary = "Get paginated error reports",
            description = "Retrieves a paginated list of error reports for courses owned by the authenticated teacher. " +
                    "Can be filtered by status and course ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Paginated list of error reports retrieved successfully"),
            @ApiResponse(responseCode = FORBIDDEN, description = "User does not have permission to view the reports")
    })
    @GetMapping("/professors/{professorId}/error-reports")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Page<GetErrorReportDto>> getReportsForProfessor(
            @Parameter(description = "The ID of the professor") @PathVariable UUID professorId,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) UUID courseId,
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        UUID userId = customUserDetails.getUserId();
        if (!userId.equals(professorId)) {
            throw new DifferentIdException("Authenticated user ID does not match the professor ID in the path");
        }
        return ResponseEntity.ok(questionErrorReportService.getReports(userId, status, courseId, pageable));
    }
}
