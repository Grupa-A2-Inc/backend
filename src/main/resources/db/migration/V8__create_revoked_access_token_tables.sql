CREATE TABLE revoked_access_token (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      token_hash VARCHAR(64) NOT NULL UNIQUE,
      revoked_at TIMESTAMP NOT NULL DEFAULT NOW(),
      expires_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_revoked_access_token_hash ON revoked_access_token(token_hash);
CREATE INDEX idx_revoked_access_token_expires_at ON revoked_access_token(expires_at);