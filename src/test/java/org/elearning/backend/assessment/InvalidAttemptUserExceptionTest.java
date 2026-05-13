package org.elearning.backend.assessment;

import org.elearning.backend.assessment.exception.InvalidAttemptUserException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InvalidAttemptUserExceptionTest {

    @Test
    void storesProvidedMessage() {
        InvalidAttemptUserException exception = new InvalidAttemptUserException("invalid attempt owner");

        assertThat(exception).hasMessage("invalid attempt owner");
    }
}
