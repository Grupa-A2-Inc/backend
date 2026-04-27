package org.elearning.backend.ai.exception;

public class AiApiException extends RuntimeException{
    /**
     * Constructs an AiApiException with the specified detail message.
     *
     * @param message the detail message describing the AI API error
     */
    public AiApiException(String message)
    {
        super(message);
    }
}
