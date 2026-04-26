package org.elearning.backend.ai.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class AdaptiveServiceUnavailableException extends RuntimeException{
    public AdaptiveServiceUnavailableException(String message) {
        super(message);
    }
}
