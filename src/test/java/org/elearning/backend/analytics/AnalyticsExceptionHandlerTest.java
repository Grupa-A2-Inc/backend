package org.elearning.backend.analytics;

import org.elearning.backend.analytics.exception.AnalyticsExceptionHandler;
import org.elearning.backend.analytics.exception.StudentNotEnrolledInCourseException;
import org.elearning.backend.analytics.exception.WithoutAccessException;
import org.elearning.backend.assessment.exception.DoesNotExistException;
import org.elearning.backend.content.exception.CourseNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@org.springframework.test.context.ActiveProfiles("test")
class AnalyticsExceptionHandlerTest {

    private final AnalyticsExceptionHandler handler = new AnalyticsExceptionHandler();

    @Test
    void handlesWithoutAccess() {
        UUID userId = UUID.randomUUID();

        var response = handler.handleNotFound(new WithoutAccessException(userId));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("message", "User " + userId + " has no access to this field");
    }

    @Test
    void handlesDoesNotExist() {
        var response = handler.handleDoesNotExist(new DoesNotExistException("missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("message", "missing");
    }

    @Test
    void handlesStudentNotEnrolled() {
        var response = handler.handleValidationException(new StudentNotEnrolledInCourseException("not enrolled"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("message", "not enrolled");
    }

    @Test
    void handlesCourseNotFound() {
        var response = handler.handleCourseNotFound(new CourseNotFoundException(UUID.randomUUID()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsKey("message");
    }
}
