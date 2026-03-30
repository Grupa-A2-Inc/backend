package org.elearning.backend.assessment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.elearning.backend.assessment.dto.*;
import org.elearning.backend.assessment.service.AttemptService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AttemptController {

    private final AttemptService attemptService;

    @Operation(summary = "Începe un test", description = "Creează un attempt nou și returnează întrebările fără răspunsuri corecte.")
    @ApiResponse(responseCode = "200", description = "Attempt creat cu succes")
    @ApiResponse(responseCode = "400", description = "Testul nu e PUBLISHED")
    @ApiResponse(responseCode = "404", description = "Testul nu există")
    @PostMapping("/api/tests/{testId}/start")
    public ResponseEntity<StartAttemptResponseDTO> startAttempt(@PathVariable UUID testId) {

        // Placeholder for JWT - to be replaced with actual authentication logic
        UUID studentId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        StartAttemptResponseDTO response = attemptService.startAttempt(testId, studentId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Submit răspunsuri", description = "Procesează răspunsurile elevului, calculează scorul și salvează rezultatul.")
    @ApiResponse(responseCode = "200", description = "Submit reușit")
    @ApiResponse(responseCode = "404", description = "Attempt-ul nu există")
    @ApiResponse(responseCode = "409", description = "Attempt-ul e deja DONE")
    @ApiResponse(responseCode = "410", description = "Timerul a expirat")
    @PostMapping("/api/attempts/{attemptId}/submit")
    public ResponseEntity<TestResultDTO> submitAttempt(
            @PathVariable UUID attemptId,
            @RequestBody SubmitRequestDTO request) {

        // Placeholder for JWT - to be replaced with actual authentication logic
        UUID studentId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        TestResultDTO result = attemptService.submitAttempt(attemptId, studentId, request);
        return ResponseEntity.ok(result);
    }
}