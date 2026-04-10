package org.elearning.backend.enrollment.exception;

import java.util.UUID;

public class CertificateGenerationException extends RuntimeException {
    public CertificateGenerationException(UUID enrollmentId, Throwable cause) {
        super("Failed to generate certificate for enrollment: " + enrollmentId, cause);
    }
}