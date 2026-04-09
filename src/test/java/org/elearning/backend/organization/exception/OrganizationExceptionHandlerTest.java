package org.elearning.backend.organization.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class OrganizationExceptionHandlerTest {

    private final OrganizationExceptionHandler handler = new OrganizationExceptionHandler();

    @Test
    void handlesOrganizationNotFound() {
        var response = handler.handleNotFound(new OrganizationNotFoundException("missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("message", "missing");
    }
}
