package org.elearning.backend.feedback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.common.GlobalHttpStatusCodes;
import org.elearning.backend.feedback.dto.ErrorReportDto;
import org.elearning.backend.feedback.dto.GetErrorReportDto;
import org.elearning.backend.feedback.exception.DifferentIdException;
import org.elearning.backend.feedback.model.ReportStatus;
import org.elearning.backend.feedback.service.ErrorReportManagementService;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ErrorReportManagementController extends GlobalHttpStatusCodes {

    private final ErrorReportManagementService errorReportManagementService;

    @Operation(summary = "Get paginated error reports",
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
        return ResponseEntity.ok(errorReportManagementService.getReports(userId, status, courseId, pageable));
    }

    @Operation(summary = "Resolve an error report",
            description = "Marks the specified error report as resolved. Only the teacher who owns the course can resolve the report.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Error report resolved successfully"),
            @ApiResponse(responseCode = FORBIDDEN, description = "User does not have permission to resolve the report"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Error report not found"),
            @ApiResponse(responseCode = CONFLICT, description = "Report is already resolved")
    })
    @PatchMapping("/error-reports/{reportId}/resolve")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ErrorReportDto> resolveReport(
            @Parameter(description = "The ID of the report to resolve") @PathVariable UUID reportId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        return ResponseEntity.ok(errorReportManagementService.resolveReport(reportId, customUserDetails.getUserId()));
    }
}
