CREATE TABLE lesson_ratings (
                                id          UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
                                lesson_id   UUID      NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
                                student_id  UUID      NOT NULL,
                                rating      SMALLINT  NOT NULL CHECK (rating BETWEEN 1 AND 5),
                                comment     TEXT,
                                created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
                                updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
                                CONSTRAINT uq_lesson_student_rating UNIQUE (lesson_id, student_id)
);
CREATE INDEX idx_ratings_lesson  ON lesson_ratings(lesson_id);
CREATE INDEX idx_ratings_student ON lesson_ratings(student_id);



CREATE TABLE question_error_reports (
                                        id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                                        question_id  INTEGER     NOT NULL REFERENCES questions(id) ON DELETE CASCADE,  -- ← INTEGER, nu UUID
                                        student_id   UUID        NOT NULL,
                                        description  TEXT        NOT NULL,
                                        status       VARCHAR(20) NOT NULL DEFAULT 'NEW',
                                        resolved_at  TIMESTAMP,
                                        resolved_by  UUID,
                                        created_at   TIMESTAMP   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_reports_question ON question_error_reports(question_id);
CREATE INDEX idx_reports_status   ON question_error_reports(status, created_at DESC);