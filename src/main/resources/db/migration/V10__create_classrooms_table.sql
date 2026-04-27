CREATE TABLE classrooms (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
                            name VARCHAR(255) NOT NULL,
                            description TEXT,
                            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                            updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_classrooms_organization_id ON classrooms(organization_id);

CREATE UNIQUE INDEX uq_classrooms_org_name_lower
    ON classrooms(organization_id, lower(name));