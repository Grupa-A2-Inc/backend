CREATE TABLE subscription_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    max_users INTEGER NOT NULL,
    max_classrooms INTEGER NOT NULL,
    max_courses INTEGER,
    has_premium_features BOOLEAN NOT NULL DEFAULT FALSE,
    price_monthly NUMERIC(10, 2),
    currency VARCHAR(3),
    stripe_price_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_subscription_plans_code_not_blank CHECK (btrim(code) <> ''),
    CONSTRAINT chk_subscription_plans_display_name_not_blank CHECK (btrim(display_name) <> ''),
    CONSTRAINT chk_subscription_plans_max_users_non_negative CHECK (max_users >= 0),
    CONSTRAINT chk_subscription_plans_max_classrooms_non_negative CHECK (max_classrooms >= 0),
    CONSTRAINT chk_subscription_plans_max_courses_non_negative CHECK (max_courses IS NULL OR max_courses >= 0),
    CONSTRAINT chk_subscription_plans_price_monthly_non_negative CHECK (price_monthly IS NULL OR price_monthly >= 0),
    CONSTRAINT chk_subscription_plans_currency_length CHECK (currency IS NULL OR char_length(currency) = 3)
);

CREATE UNIQUE INDEX uq_subscription_plans_code_lower
    ON subscription_plans (lower(code));

CREATE UNIQUE INDEX uq_subscription_plans_display_name_lower
    ON subscription_plans (lower(display_name));

INSERT INTO subscription_plans (
    code,
    display_name,
    max_users,
    max_classrooms,
    max_courses,
    has_premium_features,
    price_monthly,
    currency,
    stripe_price_id
) VALUES
    ('FREE', 'Free', 31, 1, 3, FALSE, 0.00, 'EUR', NULL),
    ('STARTER', 'Starter', 100, 5, 20, FALSE, 29.99, 'EUR', 'price_1TTTdwDbgsH75lpfdjGo1LLY'),
    ('SCHOOL', 'School', 500, 20, 100, TRUE, 99.99, 'EUR', 'price_1TTTeMDbgsH75lpfNKwLA9qu'),
    ('ENTERPRISE', 'Enterprise', 5000, 200, NULL, TRUE, 399.99, 'EUR', 'price_1TTTeoDbgsH75lpfnSMvRvob');
