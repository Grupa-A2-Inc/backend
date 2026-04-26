package org.elearning.backend.assessment.exception;

public class TestMustBeDraftException extends RuntimeException {
    /**
     * Constructs a TestMustBeDraftException with the specified detail message.
     *
     * @param message detail message explaining why the test must be in draft state
     */
    public TestMustBeDraftException(String message) {
        super(message);
    }
}
