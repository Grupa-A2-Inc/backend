CREATE TABLE organization_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    subscription_plan_id UUID NOT NULL REFERENCES subscription_plans(id),
    status VARCHAR(50) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_customer_id VARCHAR(255),
    provider_subscription_id VARCHAR(255),
    current_period_start TIMESTAMP NOT NULL,
    current_period_end TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_organization_subscriptions_period_range
        CHECK (current_period_end >= current_period_start)
);

CREATE INDEX idx_organization_subscriptions_organization_id
    ON organization_subscriptions (organization_id);

CREATE UNIQUE INDEX uq_organization_subscriptions_provider_subscription_id
    ON organization_subscriptions (provider_subscription_id)
    WHERE provider_subscription_id IS NOT NULL;

CREATE INDEX idx_organization_subscriptions_status
    ON organization_subscriptions (status);
