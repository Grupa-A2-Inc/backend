package org.elearning.backend.ai.dto;

public record AiStudentRegistrationResponse(
        String requestId,
        String status,
        String message
) {
}
