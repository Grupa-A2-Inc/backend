package org.elearning.backend.assessment.exception;

// Exemplu pentru 410 Gone
public class TimerExpiredException extends RuntimeException {
    public TimerExpiredException(String message) { super(message); }
}