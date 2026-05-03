package org.elearning.backend.classroom.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@org.springframework.test.context.ActiveProfiles("test")
class CourseNotEligibleExceptionTest {

    @Test
    void constructor_setsMessage() {
        CourseNotEligibleException ex = new CourseNotEligibleException("not eligible");
        assertThat(ex.getMessage()).isEqualTo("not eligible");
    }
}
