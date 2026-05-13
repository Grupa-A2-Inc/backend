package org.elearning.backend.assessment;

import org.elearning.backend.assessment.exception.UserHasNoPermissionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserHasNoPermissionExceptionTest {

    @Test
    void storesProvidedMessage() {
        UserHasNoPermissionException exception = new UserHasNoPermissionException("forbidden");

        assertThat(exception).hasMessage("forbidden");
    }
}
