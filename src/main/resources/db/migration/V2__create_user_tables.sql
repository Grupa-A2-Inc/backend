-- ENUM-uri
CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE', 'BLOCKED');
CREATE TYPE role_name AS ENUM ('ADMIN', 'ORGANIZATION_ADMIN', 'TEACHER', 'STUDENT', 'PARENT');

-- =====================
-- ROLES
-- =====================
CREATE TABLE roles (
                       id    BIGSERIAL    PRIMARY KEY,
                       name  role_name    NOT NULL UNIQUE
);

INSERT INTO roles (name) VALUES
                             ('ADMIN'),
                             ('ORGANIZATION_ADMIN'),
                             ('TEACHER'),
                             ('STUDENT'),
                             ('PARENT');

-- =====================
-- USERS
-- =====================
CREATE TABLE users (
                       id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                       email         VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       first_name    VARCHAR(100) NOT NULL,
                       last_name     VARCHAR(100) NOT NULL,
                       role_id       BIGINT       NOT NULL REFERENCES roles(id),
                       status        user_status  NOT NULL DEFAULT 'ACTIVE',
                       created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
                       updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- =====================
-- INDEXES
-- =====================
CREATE INDEX idx_users_email   ON users(email);
CREATE INDEX idx_users_role_id ON users(role_id);
CREATE INDEX idx_users_status  ON users(status);