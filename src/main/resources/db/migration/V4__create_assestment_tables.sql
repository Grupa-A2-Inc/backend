-- ============================================================
-- 1. ENUM TYPES
-- ============================================================

CREATE TYPE test_status AS ENUM (
    'DRAFT',
    'PUBLISHED'
);

CREATE TYPE question_type AS ENUM (
    'single_choice',
    'multi_choice',
    'true_false'
);

CREATE TYPE attempt_status AS ENUM (
    'IN_PROGRESS',
    'DONE',
    'EXPIRED'
);

-- ============================================================
-- 2. TESTS
-- lesson_id UNIQUE → o lecție = maxim un test (v4 constraint)
-- ai_enabled: false dacă profesor, true dacă platformă
-- student_id / created_by rămân UUID fără FK real până
-- backend-ul de useri livrează tabela (notă din plan)
-- ============================================================

CREATE TABLE tests (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id       UUID            NOT NULL UNIQUE,   -- 1 lecție = 1 test
    created_by      UUID            NOT NULL,          -- extras din JWT, FK ulterior
    title           VARCHAR(255)    NOT NULL,
    description     TEXT,
    time_limit_sec  INT             NOT NULL,
    status          test_status     NOT NULL DEFAULT 'DRAFT',
    ai_enabled      BOOLEAN         NOT NULL DEFAULT false,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 3. QUESTIONS
-- Aparțin direct unui test (nu mai există bancă de întrebări).
-- subject_id / topic_id sunt INT fără FK până modulul e livrat.
-- ============================================================

CREATE TABLE questions (
    id              SERIAL          PRIMARY KEY,
    test_id         UUID            NOT NULL REFERENCES tests(id) ON DELETE CASCADE,
    subject_id      INT,            -- FK adăugat ulterior
    topic_id        INT,            -- FK adăugat ulterior
    question_type   question_type   NOT NULL,
    content         TEXT            NOT NULL,
    difficulty      NUMERIC(3, 2),  -- 0.00 – 1.00
    is_active       BOOLEAN         NOT NULL DEFAULT true,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 4. QUESTION_OPTIONS
-- Înlocuiește correct_answers JSONB și tabelul test_questions.
-- is_correct marchează variantele corecte.
-- ============================================================

CREATE TABLE question_options (
    id              SERIAL          PRIMARY KEY,
    question_id     INT             NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    text            TEXT            NOT NULL,
    display_order   INT             NOT NULL DEFAULT 1,
    is_correct      BOOLEAN         NOT NULL DEFAULT false
);

-- ============================================================
-- 5. TEST_ATTEMPTS
-- Un rând per încercare a unui elev pe un test.
-- student_id fără FK real (useri livrați ulterior).
-- ============================================================

CREATE TABLE test_attempts (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    test_id         UUID            NOT NULL REFERENCES tests(id),
    student_id      UUID            NOT NULL,           -- extras din JWT, FK ulterior
    attempt_number  INT             NOT NULL DEFAULT 1,
    started_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    ended_at        TIMESTAMP,
    status          attempt_status  NOT NULL DEFAULT 'IN_PROGRESS'
);

-- ============================================================
-- 6. ATTEMPT_ANSWERS
-- Un rând per răspuns al elevului în cadrul unui attempt.
-- selected_option_ids e JSONB → array de INT-uri.
-- time_spent vine de la frontend (secunde, float) — NU calculat.
-- ============================================================

CREATE TABLE attempt_answers (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id          UUID        NOT NULL REFERENCES test_attempts(id) ON DELETE CASCADE,
    question_id         INT         NOT NULL REFERENCES questions(id),
    selected_option_ids JSONB       NOT NULL DEFAULT '[]',
    is_correct          BOOLEAN     NOT NULL DEFAULT false,
    time_spent          NUMERIC(8, 2),   -- secunde float, de la frontend
    answered_at         TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 7. TEST_RESULTS
-- Un rând per attempt finalizat. attempt_id e PK și FK.
-- student_id / test_id fără FK real deocamdată.
-- ============================================================

CREATE TABLE test_results (
    attempt_id      UUID            PRIMARY KEY REFERENCES test_attempts(id),
    student_id      UUID            NOT NULL,           -- FK ulterior
    test_id         UUID            NOT NULL REFERENCES tests(id),
    score           NUMERIC(5, 4)   NOT NULL,           -- 0.0000 – 1.0000
    score_percent   NUMERIC(5, 2)   NOT NULL,           -- 0.00 – 100.00
    passed          BOOLEAN         NOT NULL,
    completed_at    TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 8. INDEXES
-- ============================================================

-- tests: cauta teste dupa lectie sau profesor
CREATE INDEX idx_tests_lesson_id     ON tests(lesson_id);
CREATE INDEX idx_tests_created_by    ON tests(created_by);
CREATE INDEX idx_tests_status        ON tests(status);

-- questions: cauta intrebarile unui test
CREATE INDEX idx_questions_test_id   ON questions(test_id);

-- question_options: cauta optiunile unei intrebari
CREATE INDEX idx_options_question_id ON question_options(question_id);

-- test_attempts: cauta attempturile unui student sau test
CREATE INDEX idx_attempts_test_id    ON test_attempts(test_id);
CREATE INDEX idx_attempts_student_id ON test_attempts(student_id);
CREATE INDEX idx_attempts_status     ON test_attempts(status);

-- attempt_answers: cauta raspunsurile unui attempt
CREATE INDEX idx_answers_attempt_id  ON attempt_answers(attempt_id);

-- test_results: query-urile my-best / my-attempts
CREATE INDEX idx_results_student_id  ON test_results(student_id);
CREATE INDEX idx_results_test_id     ON test_results(test_id);
CREATE INDEX idx_results_student_test ON test_results(student_id, test_id);