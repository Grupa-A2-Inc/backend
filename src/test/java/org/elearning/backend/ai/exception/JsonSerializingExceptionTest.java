package org.elearning.backend.ai.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonSerializingExceptionTest {

    @Test
    void storesProvidedMessage() {
        JsonSerializingException exception = new JsonSerializingException("serialization failed");

        assertThat(exception).hasMessage("serialization failed");
    }
}
