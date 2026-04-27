
CREATE TABLE analytics_alerts (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    test_id              UUID         NOT NULL REFERENCES tests(id) ON DELETE CASCADE,
    professor_id         UUID         NOT NULL,
    failure_threshold    NUMERIC(5,2) NOT NULL,
    current_failure_rate NUMERIC(5,2),
    triggered_at         TIMESTAMP,
    is_active            BOOLEAN      NOT NULL DEFAULT true,
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_alert_test_prof UNIQUE (test_id, professor_id)
);

CREATE INDEX idx_alerts_prof ON analytics_alerts(professor_id, is_active);
CREATE INDEX idx_alerts_test ON analytics_alerts(test_id);


CREATE TABLE ai_question_requests (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id           UUID         NOT NULL,
    subject_id          INT,
    topic_id            INT,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    generated_questions TEXT,
    test_id             UUID         REFERENCES tests(id),
    error_message       TEXT,
    resolved_at         TIMESTAMP,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_requests_lesson ON ai_question_requests(lesson_id, status);

ALTER TABLE questions
    ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'MANUAL';

CREATE INDEX idx_questions_source ON questions(test_id, source);

CREATE TABLE adaptive_sessions (
     id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
     student_id       UUID        NOT NULL,
     subject_id       INT         NOT NULL,
     topic_id         INT         NOT NULL,
     status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
     expires_at       TIMESTAMP   NOT NULL,
     completed_at     TIMESTAMP,
     ai_feedback_sent BOOLEAN     NOT NULL DEFAULT false,
     created_at       TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_adaptive_sessions_student ON adaptive_sessions(student_id, status);


CREATE TABLE adaptive_session_exercises (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id          UUID         NOT NULL REFERENCES adaptive_sessions(id) ON DELETE CASCADE,
    ml_exercise_id      VARCHAR(100) NOT NULL,
    exercise_text       TEXT         NOT NULL,
    exercise_type       VARCHAR(30)  NOT NULL,
    answers_raw         JSONB        NOT NULL,
    correct_answers_raw JSONB        NOT NULL,
    difficulty          NUMERIC(4,2),
    CONSTRAINT uq_session_exercise UNIQUE (session_id, ml_exercise_id)
);

CREATE INDEX idx_adaptive_exercises_session ON adaptive_session_exercises(session_id);


CREATE TABLE adaptive_session_answers (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id    UUID        NOT NULL REFERENCES adaptive_sessions(id),
    exercise_id   UUID        NOT NULL REFERENCES adaptive_session_exercises(id),
    given_answers JSONB       NOT NULL,
    score         NUMERIC(3,2),
    time_spent    INT,
    CONSTRAINT uq_session_answer UNIQUE (session_id, exercise_id)
);

