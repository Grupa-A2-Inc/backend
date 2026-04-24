CREATE OR REPLACE VIEW lesson_difficulty_by_student AS
WITH my_best AS (
    SELECT test_id, student_id, MAX(score_percent) AS my_best_score
    FROM test_results
    GROUP BY test_id, student_id
),
class_average AS (
    SELECT test_id, AVG(best_score) AS class_average_best
    FROM (
        SELECT tr.test_id, tr.student_id, MAX(tr.score_percent) AS best_score
        FROM test_results tr
        GROUP BY tr.test_id, tr.student_id
    ) student_best
    GROUP BY test_id
)
SELECT
    c.id AS course_id,
    l.id AS lesson_id,
    l.title AS lesson_title,
    mb.student_id AS student_id,
    mb.my_best_score AS my_personal_best_score,
    ca.class_average_best AS class_average_of_best,
    (ca.class_average_best - mb.my_best_score) AS gap
FROM courses c
         JOIN chapters ch ON ch.course_id = c.id
         JOIN lessons l ON l.chapter_id = ch.id
         JOIN tests t ON t.lesson_id = l.id
         JOIN my_best mb ON mb.test_id = t.id
         JOIN class_average ca ON ca.test_id = t.id;