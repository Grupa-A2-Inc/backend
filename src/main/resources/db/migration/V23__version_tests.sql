ALTER TYPE test_status ADD VALUE IF NOT EXISTS 'SUPERSEDED';

ALTER TABLE tests
    DROP CONSTRAINT IF EXISTS tests_lesson_id_key;

ALTER TABLE tests
    ADD COLUMN IF NOT EXISTS version INT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS previous_version_id UUID NULL REFERENCES tests(id);

ALTER TABLE tests
    ADD CONSTRAINT uq_tests_lesson_version UNIQUE (lesson_id, version);

CREATE UNIQUE INDEX IF NOT EXISTS uq_tests_lesson_draft
    ON tests(lesson_id)
    WHERE status = 'DRAFT';

CREATE UNIQUE INDEX IF NOT EXISTS uq_tests_lesson_published
    ON tests(lesson_id)
    WHERE status = 'PUBLISHED';
