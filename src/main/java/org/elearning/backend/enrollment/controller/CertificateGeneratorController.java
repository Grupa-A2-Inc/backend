package org.elearning.backend.enrollment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
// SWAGGER ADDED
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.common.GlobalHttpStatusCodes;
import org.elearning.backend.enrollment.service.CertificateGeneratorService;
import org.elearning.backend.security.auth.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// SWAGGER ADDED
@Tag(name = "Certificates", description = "Course completion certificate generation")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CertificateGeneratorController extends GlobalHttpStatusCodes {

    private final CertificateGeneratorService certificateGeneratorService;

    @Operation(summary = "Generate certificate",
            description = "Generates a PDF file to demonstrate the completion of an official course")
    @ApiResponses(value = {
            @ApiResponse(responseCode = OK, description = "Certificate has been generated"),
            @ApiResponse(responseCode = FORBIDDEN, description = "Student cannot generate this certificate because the course was no completed, it's private or because they are not part of the enrollment"),
            @ApiResponse(responseCode = NOT_FOUND, description = "Enrollment does not exist")
    })
    @GetMapping("/enrollments/{enrollmentId}/certificat")
    ResponseEntity<byte[]> generatePdf(@PathVariable UUID enrollmentId,
                                       @AuthenticationPrincipal CustomUserDetails userDetails){
       return ResponseEntity.ok()
               .header("Content-Type", "application/pdf") // Tells the browser the bytes are of a PDF document
               .header("Content-Disposition", "attachment; filename=\"certificate.pdf\"") // Tells the browser to download the certificate
               .body(certificateGeneratorService.generateCertificatePdf(enrollmentId, userDetails.getUserId()));
    }
}
