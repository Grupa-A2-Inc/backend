CREATE TABLE activation_token (
        id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id     UUID            NOT NULL REFERENCES users(id),
        token_hash  VARCHAR(255)    NOT NULL UNIQUE,
        created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
        expires_at  TIMESTAMP       NOT NULL,
        used_at     TIMESTAMP       NULL
);

CREATE INDEX idx_activation_token_user_id ON activation_token(user_id);
CREATE INDEX idx_activation_token_token_hash ON activation_token(token_hash);