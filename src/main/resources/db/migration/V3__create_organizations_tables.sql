-- =====================
-- ORGANIZATIONS
-- =====================
CREATE TABLE organizations (
   id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
   name       VARCHAR(255) NOT NULL,
   owner_id   UUID         NOT NULL REFERENCES users(id),
   created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
   updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- =====================
-- INDEXES
-- =====================
CREATE INDEX idx_organizations_owner_id ON organizations(owner_id);

ALTER TABLE users ADD COLUMN organization_id UUID REFERENCES organizations(id);