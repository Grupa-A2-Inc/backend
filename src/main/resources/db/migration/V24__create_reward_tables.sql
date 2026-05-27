CREATE TABLE organization_reward_configs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    reward_percent      NUMERIC(5, 2) NOT NULL DEFAULT 10.00,
    minimum_score       NUMERIC(5, 2) NOT NULL,
    maximum_winners     INT NOT NULL,
    distribution_period VARCHAR(30) NOT NULL DEFAULT 'MONTHLY',
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_reward_config_organization UNIQUE (organization_id),
    CONSTRAINT chk_reward_percent_fixed CHECK (reward_percent = 10.00),
    CONSTRAINT chk_minimum_score_range CHECK (minimum_score >= 0 AND minimum_score <= 100),
    CONSTRAINT chk_maximum_winners_positive CHECK (maximum_winners > 0)
);

CREATE TABLE reward_cycles (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id        UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    period_start           TIMESTAMP NOT NULL,
    period_end             TIMESTAMP NOT NULL,
    subscription_amount    NUMERIC(12, 2) NOT NULL,
    reward_pool_amount     NUMERIC(12, 6) NOT NULL,
    eurc_deposited_amount  NUMERIC(12, 6) NOT NULL,
    status                 VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    deposit_tx_hash        VARCHAR(255),
    mint_tx_hash           VARCHAR(255),
    failure_reason         TEXT,
    created_at             TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_reward_cycle_period UNIQUE (organization_id, period_start, period_end),
    CONSTRAINT chk_reward_cycle_period CHECK (period_end > period_start),
    CONSTRAINT chk_subscription_amount_nonnegative CHECK (subscription_amount >= 0),
    CONSTRAINT chk_reward_pool_nonnegative CHECK (reward_pool_amount >= 0),
    CONSTRAINT chk_eurc_deposited_nonnegative CHECK (eurc_deposited_amount >= 0)
);

CREATE TABLE student_wallets (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    wallet_address VARCHAR(128) NOT NULL,
    verified       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_student_wallet_student UNIQUE (student_id),
    CONSTRAINT uq_student_wallet_address UNIQUE (wallet_address)
);

CREATE TABLE student_rewards (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reward_cycle_id         UUID NOT NULL REFERENCES reward_cycles(id) ON DELETE CASCADE,
    student_id              UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    student_wallet_address  VARCHAR(128) NOT NULL,
    reward_rank             INT NOT NULL,
    score                   NUMERIC(8, 4) NOT NULL,
    reward_amount           NUMERIC(12, 6) NOT NULL,
    tx_hash                 VARCHAR(255),
    status                  VARCHAR(30) NOT NULL DEFAULT 'CALCULATED',
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_student_reward_cycle_student UNIQUE (reward_cycle_id, student_id),
    CONSTRAINT chk_student_reward_rank_positive CHECK (reward_rank > 0),
    CONSTRAINT chk_student_reward_score_range CHECK (score >= 0 AND score <= 100),
    CONSTRAINT chk_student_reward_amount_positive CHECK (reward_amount > 0)
);

CREATE INDEX idx_reward_configs_organization ON organization_reward_configs(organization_id);
CREATE INDEX idx_reward_cycles_organization ON reward_cycles(organization_id);
CREATE INDEX idx_reward_cycles_status ON reward_cycles(status);
CREATE INDEX idx_student_wallets_student ON student_wallets(student_id);
CREATE INDEX idx_student_rewards_cycle ON student_rewards(reward_cycle_id);
CREATE INDEX idx_student_rewards_student ON student_rewards(student_id);
CREATE INDEX idx_student_rewards_status ON student_rewards(status);
