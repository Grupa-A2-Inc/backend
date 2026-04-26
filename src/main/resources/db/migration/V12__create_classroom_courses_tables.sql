CREATE TABLE classroom_courses (
    id           UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    classroom_id UUID      NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE,
    course_id    UUID      NOT NULL REFERENCES courses(id)    ON DELETE CASCADE,
    assigned_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_classroom_course UNIQUE (classroom_id, course_id)
);

CREATE INDEX idx_classroom_courses_classroom_id ON classroom_courses(classroom_id);
CREATE INDEX idx_classroom_courses_course_id    ON classroom_courses(course_id);