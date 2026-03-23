

-- ENUM-uri
CREATE TYPE course_status AS ENUM ('DRAFT', 'PUBLISHED');
CREATE TYPE course_visibility AS ENUM ('PUBLIC', 'PRIVATE');

-- =====================
-- COURSES
-- =====================
CREATE TABLE courses (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    category    VARCHAR(100),

    status      course_status     NOT NULL DEFAULT 'DRAFT',
    visibility  course_visibility NOT NULL DEFAULT 'PRIVATE',

    created_by  UUID NOT NULL,

    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =====================
-- CHAPTERS
-- =====================
CREATE TABLE chapters (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id   UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,

    title       VARCHAR(255) NOT NULL,
    order_index INT NOT NULL DEFAULT 0,

    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =====================
-- LESSONS
-- =====================
CREATE TABLE lessons (
     id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
     chapter_id  UUID NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,

     title       VARCHAR(255) NOT NULL,
     content_md  TEXT,

     order_index INT NOT NULL DEFAULT 0,
     created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
     updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =====================
-- LESSON RESOURCES
-- =====================
CREATE TABLE lesson_resources (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id   UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,

    title       VARCHAR(255) NOT NULL,
    url         VARCHAR(500) NOT NULL,

    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =====================
-- INDEXES
-- =====================
CREATE INDEX idx_chapters_course_id ON chapters(course_id);
CREATE INDEX idx_lessons_chapter_id ON lessons(chapter_id);
CREATE INDEX idx_lesson_resources_lesson_id ON lesson_resources(lesson_id);
CREATE INDEX idx_courses_status ON courses(status);
CREATE INDEX idx_courses_visibility ON courses(visibility);