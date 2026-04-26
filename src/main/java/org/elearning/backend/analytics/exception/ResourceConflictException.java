package org.elearning.backend.analytics.exception;

public class ResourceConflictException extends RuntimeException {
    /**
     * Creates a ResourceConflictException with the specified error message.
     *
     * @param message the detail message describing the resource conflict
     */
    public ResourceConflictException(String message) {
        super(message);
    }
}
