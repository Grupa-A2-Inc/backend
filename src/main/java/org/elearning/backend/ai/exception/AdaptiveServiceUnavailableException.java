package org.elearning.backend.ai.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class AdaptiveServiceUnavailableException extends RuntimeException{
    /**
     * Constructs an AdaptiveServiceUnavailableException with the specified detail message.
     *
     * @param message the detail message describing the cause of the service unavailability
     */
    public AdaptiveServiceUnavailableException(String message) {
        super(message);
    }
}
