CREATE TABLE pw_reset_token (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255)    NOT NULL UNIQUE,
    expires_at  TIMESTAMP       NOT NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    used_at     TIMESTAMP       NULL
);


CREATE INDEX idx_pw_reset_token_user_id ON pw_reset_token(user_id);
CREATE INDEX idx_pw_reset_token_token_hash ON pw_reset_token(token_hash);
