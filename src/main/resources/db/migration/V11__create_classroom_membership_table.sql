CREATE TYPE membership_type AS ENUM ('TEACHER', 'STUDENT');

CREATE TABLE classroom_memberships (
       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
       classroom_id UUID NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE,
       user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
       membership_type membership_type NOT NULL,
       created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_classroom_user
    ON classroom_memberships(classroom_id, user_id);

CREATE INDEX idx_memberships_classroom_id
    ON classroom_memberships(classroom_id);

CREATE INDEX idx_memberships_user_id
    ON classroom_memberships(user_id);

CREATE INDEX idx_memberships_user_type
    ON classroom_memberships(user_id, membership_type);