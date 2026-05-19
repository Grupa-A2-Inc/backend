package org.elearning.backend.content.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class LessonTest {

    @Test
    void preUpdateShouldRefreshUpdatedAtTimestamp() {
        Lesson lesson = new Lesson();
        LocalDateTime initialTimestamp = LocalDateTime.of(2024, 1, 1, 12, 0);
        lesson.setUpdatedAt(initialTimestamp);

        lesson.preUpdate();

        assertThat(lesson.getUpdatedAt()).isAfter(initialTimestamp);
    }
}
