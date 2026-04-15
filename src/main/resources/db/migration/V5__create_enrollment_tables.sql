
CREATE TABLE course_enrollments (
    id           UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id    UUID      NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    student_id   UUID      NOT NULL,
    enrolled_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,                         -- NULL = în progres
    CONSTRAINT uq_enrollment UNIQUE (course_id, student_id)
);

CREATE TABLE lesson_progress (
    id            UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id     UUID      NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    student_id    UUID      NOT NULL,
    enrollment_id UUID      NOT NULL REFERENCES course_enrollments(id) ON DELETE CASCADE,
    visited_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_lesson_visited UNIQUE (lesson_id, student_id)
);

CREATE INDEX idx_enrollments_student_id     ON course_enrollments(student_id);
CREATE INDEX idx_enrollments_course_id      ON course_enrollments(course_id);
CREATE INDEX idx_lesson_progress_enrollment ON lesson_progress(enrollment_id);
CREATE INDEX idx_lesson_progress_student    ON lesson_progress(student_id);
CREATE INDEX idx_lesson_progress_lesson     ON lesson_progress(lesson_id);