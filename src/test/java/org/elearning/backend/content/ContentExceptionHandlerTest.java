package org.elearning.backend.content;

import org.elearning.backend.content.exception.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@org.springframework.test.context.ActiveProfiles("test")
class ContentExceptionHandlerTest {

    private final ContentExceptionHandler handler = new ContentExceptionHandler();

    @Test
    void handlesCourseNotFound() {
        var response = handler.handleCourseNotFound(new CourseNotFoundException(UUID.randomUUID()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsKey("message");
    }

    @Test
    void handlesChapterNotFound() {
        var response = handler.handleChapterNotFound(new ChapterNotFoundException(UUID.randomUUID()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsKey("message");
    }

    @Test
    void handlesLessonNotFound() {
        var response = handler.handleLessonNotFound(new LessonNotFoundException(UUID.randomUUID()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsKey("message");
    }

    @Test
    void handlesLessonResourceNotFound() {
        var response = handler.handleLessonResourceNotFound(new LessonResourceNotFoundException(UUID.randomUUID()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsKey("message");
    }

    @Test
    void handlesInvalidOrderIndex() {
        var response = handler.handleInvalidOrderIndex(new InvalidOrderIndexException("bad order"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "bad order");
    }

    @Test
    void handlesInvalidResourceData() {
        var response = handler.handleInvalidResourceData(new InvalidResourceDataException("bad resource"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "bad resource");
    }
}
