CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE', 'BLOCKED', 'PENDING');
CREATE TYPE role_name AS ENUM ('ADMIN', 'ORGANIZATION_ADMIN', 'TEACHER', 'STUDENT', 'PARENT');

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

CREATE TABLE users (
   id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
   email         VARCHAR(255) NOT NULL UNIQUE,
   password_hash VARCHAR(255) ,
   first_name    VARCHAR(100) NOT NULL,
   last_name     VARCHAR(100) NOT NULL,
   role_id       BIGINT       NOT NULL REFERENCES roles(id),
   role_type VARCHAR(50),
   organization_id UUID,
   status        user_status  NOT NULL DEFAULT 'ACTIVE',
   created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
   updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
   failed_login_attempts INTEGER NOT NULL DEFAULT 0,
   locked_until     TIMESTAMP NULL
);

CREATE TABLE parent_student (
    id_parent    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    id_student   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (id_parent, id_student)
);

CREATE INDEX idx_users_email   ON users(email);
CREATE INDEX idx_users_role_id ON users(role_id);
CREATE INDEX idx_users_status  ON users(status);
CREATE INDEX idx_users_organization_id ON users(organization_id);
CREATE INDEX idx_parent_student_parent_id ON parent_student(id_parent);
CREATE INDEX idx_parent_student_student_id ON parent_student(id_student);
