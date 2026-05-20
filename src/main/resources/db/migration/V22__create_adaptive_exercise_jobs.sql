CREATE TABLE adaptive_exercise_jobs (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id     UUID         NOT NULL,
    subject_id     INT          NOT NULL,
    topic_id       INT          NOT NULL,
    question_count INT          NOT NULL,
    ai_job_id      VARCHAR(100),
    session_id     UUID         REFERENCES adaptive_sessions(id),
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    error_message  TEXT,
    resolved_at    TIMESTAMP,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_adaptive_exercise_jobs_student ON adaptive_exercise_jobs(student_id, status);
CREATE INDEX idx_adaptive_exercise_jobs_ai_job_id ON adaptive_exercise_jobs(ai_job_id);
