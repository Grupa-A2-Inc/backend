ALTER TABLE ai_question_requests
    ADD COLUMN ai_job_id VARCHAR(100);

UPDATE ai_question_requests
SET status = 'DONE'
WHERE status = 'SUCCESS';

UPDATE ai_question_requests
SET status = 'FAILED'
WHERE status = 'FALLBACK';

CREATE INDEX idx_ai_requests_ai_job_id ON ai_question_requests(ai_job_id);
