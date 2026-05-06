package org.elearning.backend.security.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Security", description = "Endpoints for basic authenticated security checks")
@RequestMapping("/api/v1/protected")
public class ProtectedController {
    private static final String OK = "200";

    @Operation(
            summary = "Ping protected endpoint",
            description = "Returns a simple success payload when the caller has reached a protected endpoint successfully."
    )
    @ApiResponse(responseCode = OK, description = "Protected endpoint reached successfully", content = @Content)

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
}
