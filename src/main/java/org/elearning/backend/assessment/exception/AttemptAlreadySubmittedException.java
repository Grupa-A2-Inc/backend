package org.elearning.backend.assessment.exception;

// Exemplu pentru 409 Conflict
public class AttemptAlreadySubmittedException extends RuntimeException {
    public AttemptAlreadySubmittedException(String message) { super(message); }
}
