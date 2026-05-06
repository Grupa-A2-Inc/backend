-- ============================================================
-- SEED V19: Admin + 15 cursuri publice
-- Fix: UUID-uri valide (doar hex: 0-9, a-f)
-- Admin fara organizatie (organization_id NULL)
-- ============================================================

-- ==================== ADMIN ====================
INSERT INTO roles (name)
VALUES ('ADMIN')
ON CONFLICT (name) DO NOTHING;

INSERT INTO users (
    id, email, password_hash, first_name, last_name,
    role_id, organization_id, status
)
VALUES (
           '00000000-0000-0000-0000-000000000001',
           'admin@platforma.ro',
           '$2a$12$placeholderHashPentruAdmin',
           'Admin', 'Platforma',
           (SELECT id FROM roles WHERE name = 'ADMIN'),
           NULL,
           'ACTIVE'
       )
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- CURSURI (a = course, b = chapter, c = lesson)
-- Prefixe numerice valide hex: a0-af, b0-bf, c0-cf
-- ============================================================

-- ==================== MATEMATICA 1 — Algebra ====================
INSERT INTO courses (id, title, description, category, status, visibility, created_by) VALUES
    ('a0000001-0000-0000-0000-000000000001', 'Algebra pentru gimnaziu',
     'Curs complet de algebră: ecuații, inecuații, expresii algebrice și sisteme de ecuații.',
     'Matematică', 'PUBLISHED', 'PUBLIC', '00000000-0000-0000-0000-000000000001');

INSERT INTO chapters (id, course_id, title, order_index) VALUES
                                                             ('b0000001-0000-0000-0000-000000000001', 'a0000001-0000-0000-0000-000000000001', 'Mulțimi de numere', 1),
                                                             ('b0000001-0000-0000-0000-000000000002', 'a0000001-0000-0000-0000-000000000001', 'Expresii algebrice', 2),
                                                             ('b0000001-0000-0000-0000-000000000003', 'a0000001-0000-0000-0000-000000000001', 'Ecuații de gradul I', 3);

INSERT INTO lessons (id, chapter_id, title, content_md, order_index) VALUES
                                                                         ('c0000001-0000-0000-0000-000000000001', 'b0000001-0000-0000-0000-000000000001',
                                                                          'Numere naturale și întregi',
                                                                          '## Numere naturale și întregi' || chr(10) ||
                                                                          'N = {0, 1, 2, 3, ...} — numere naturale' || chr(10) ||
                                                                          'Z = {..., -2, -1, 0, 1, 2, ...} — numere întregi' || chr(10) ||
                                                                          '**Exercițiu:** Scrie 5 numere întregi negative mai mari decât -10.', 1),

                                                                         ('c0000001-0000-0000-0000-000000000002', 'b0000001-0000-0000-0000-000000000001',
                                                                          'Numere raționale și iraționale',
                                                                          '## Numere raționale și iraționale' || chr(10) ||
                                                                          'O fracție p/q cu q ≠ 0 este număr rațional.' || chr(10) ||
                                                                          '- Raționale: 1/2, -3/4, 7' || chr(10) ||
                                                                          '- Iraționale: √2, π' || chr(10) ||
                                                                          '**Exercițiu:** Este √9 rațional?', 2),

                                                                         ('c0000001-0000-0000-0000-000000000003', 'b0000001-0000-0000-0000-000000000002',
                                                                          'Monomul și operații cu monoame',
                                                                          '## Monomul' || chr(10) ||
                                                                          'Un monom = coeficient × puteri ale variabilelor.' || chr(10) ||
                                                                          'Exemple: 3x², -5xy' || chr(10) ||
                                                                          '**Regulă:** Se adună doar monomele asemănătoare.', 1),

                                                                         ('c0000001-0000-0000-0000-000000000004', 'b0000001-0000-0000-0000-000000000002',
                                                                          'Polinomul și operații',
                                                                          '## Polinomul' || chr(10) ||
                                                                          'Suma mai multor monoame: P(x) = 3x² + 2x - 5' || chr(10) ||
                                                                          '**Exercițiu:** Calculează (2x + 3)(x - 1).', 2),

                                                                         ('c0000001-0000-0000-0000-000000000005', 'b0000001-0000-0000-0000-000000000003',
                                                                          'Rezolvarea ecuațiilor de gradul I',
                                                                          '## Ecuații de gradul I' || chr(10) ||
                                                                          'Forma: ax + b = 0, a ≠ 0' || chr(10) ||
                                                                          'Metoda: izolăm x împărțind prin coeficient.' || chr(10) ||
                                                                          '**Exemplu:** 3x + 6 = 0 → x = -2', 1);

-- ==================== MATEMATICA 2 — Geometrie ====================
INSERT INTO courses (id, title, description, category, status, visibility, created_by) VALUES
    ('a0000002-0000-0000-0000-000000000001', 'Geometrie plană și în spațiu',
     'Figuri geometrice, arii, volume și teoreme esențiale.',
     'Matematică', 'PUBLISHED', 'PUBLIC', '00000000-0000-0000-0000-000000000001');

INSERT INTO chapters (id, course_id, title, order_index) VALUES
                                                             ('b0000002-0000-0000-0000-000000000001', 'a0000002-0000-0000-0000-000000000001', 'Figuri geometrice plane', 1),
                                                             ('b0000002-0000-0000-0000-000000000002', 'a0000002-0000-0000-0000-000000000001', 'Arii și perimetre', 2),
                                                             ('b0000002-0000-0000-0000-000000000003', 'a0000002-0000-0000-0000-000000000001', 'Corpuri geometrice și volume', 3);

INSERT INTO lessons (id, chapter_id, title, content_md, order_index) VALUES
                                                                         ('c0000002-0000-0000-0000-000000000001', 'b0000002-0000-0000-0000-000000000001',
                                                                          'Triunghiul și proprietățile sale',
                                                                          '## Triunghiul' || chr(10) ||
                                                                          'Suma unghiurilor = 180°.' || chr(10) ||
                                                                          'Tipuri: echilateral (3 laturi egale), isoscel (2 laturi egale), dreptunghic (unghi 90°).', 1),

                                                                         ('c0000002-0000-0000-0000-000000000002', 'b0000002-0000-0000-0000-000000000001',
                                                                          'Cercul și elementele sale',
                                                                          '## Cercul' || chr(10) ||
                                                                          'Mulțimea punctelor egal depărtate de centru (raza r).' || chr(10) ||
                                                                          'Circumferința: C = 2πr | Aria: A = πr²', 2),

                                                                         ('c0000002-0000-0000-0000-000000000003', 'b0000002-0000-0000-0000-000000000002',
                                                                          'Aria triunghiului și dreptunghiului',
                                                                          '## Formule de arie' || chr(10) ||
                                                                          'Dreptunghi: A = l × L' || chr(10) ||
                                                                          'Triunghi: A = (b × h) / 2' || chr(10) ||
                                                                          '**Exercițiu:** Triunghi cu b=8cm, h=5cm. Aria = ?', 1),

                                                                         ('c0000002-0000-0000-0000-000000000004', 'b0000002-0000-0000-0000-000000000003',
                                                                          'Cubul și paralelipipedul',
                                                                          '## Cubul' || chr(10) ||
                                                                          'Volum: V = a³ | Arie totală: A = 6a²' || chr(10) ||
                                                                          '## Paralelipipedul dreptunghic' || chr(10) ||
                                                                          'Volum: V = l × L × h', 1);

-- ==================== BIOLOGIE 1 — Celula ====================
INSERT INTO courses (id, title, description, category, status, visibility, created_by) VALUES
    ('a0000003-0000-0000-0000-000000000001', 'Biologie celulară',
     'Structura, funcțiile și procesele vitale ale celulei.',
     'Biologie', 'PUBLISHED', 'PUBLIC', '00000000-0000-0000-0000-000000000001');

INSERT INTO chapters (id, course_id, title, order_index) VALUES
                                                             ('b0000003-0000-0000-0000-000000000001', 'a0000003-0000-0000-0000-000000000001', 'Structura celulei', 1),
                                                             ('b0000003-0000-0000-0000-000000000002', 'a0000003-0000-0000-0000-000000000001', 'Funcțiile celulei', 2),
                                                             ('b0000003-0000-0000-0000-000000000003', 'a0000003-0000-0000-0000-000000000001', 'Diviziunea celulară', 3);

INSERT INTO lessons (id, chapter_id, title, content_md, order_index) VALUES
                                                                         ('c0000003-0000-0000-0000-000000000001', 'b0000003-0000-0000-0000-000000000001',
                                                                          'Membrana celulară',
                                                                          '## Membrana celulară' || chr(10) ||
                                                                          'Dublu strat lipidic cu proteine intercalate.' || chr(10) ||
                                                                          'Funcții: delimitare, permeabilitate selectivă, recepție semnale.', 1),

                                                                         ('c0000003-0000-0000-0000-000000000002', 'b0000003-0000-0000-0000-000000000001',
                                                                          'Nucleul celular',
                                                                          '## Nucleul' || chr(10) ||
                                                                          'Centrul de control al celulei.' || chr(10) ||
                                                                          'Componente: înveliș nuclear, nucleol, cromatină (ADN + histone).', 2),

                                                                         ('c0000003-0000-0000-0000-000000000003', 'b0000003-0000-0000-0000-000000000002',
                                                                          'Fotosinteza',
                                                                          '## Fotosinteza' || chr(10) ||
                                                                          '6CO₂ + 6H₂O + lumină → C₆H₁₂O₆ + 6O₂' || chr(10) ||
                                                                          'Etape: faza luminoasă (tilacoide) + Ciclul Calvin (stromă).', 1),

                                                                         ('c0000003-0000-0000-0000-000000000004', 'b0000003-0000-0000-0000-000000000003',
                                                                          'Mitoza',
                                                                          '## Mitoza' || chr(10) ||
                                                                          'Celulă mamă → 2 celule fiice identice.' || chr(10) ||
                                                                          'Faze: Profaza → Metafaza → Anafaza → Telofaza.', 1);

-- ==================== BIOLOGIE 2 — Corpul uman ====================
INSERT INTO courses (id, title, description, category, status, visibility, created_by) VALUES
    ('a0000004-0000-0000-0000-000000000001', 'Corpul uman — sisteme și organe',
     'Sistemele corpului uman: digestiv, circulator, respirator, nervos.',
     'Biologie', 'PUBLISHED', 'PUBLIC', '00000000-0000-0000-0000-000000000001');

INSERT INTO chapters (id, course_id, title, order_index) VALUES
                                                             ('b0000004-0000-0000-0000-000000000001', 'a0000004-0000-0000-0000-000000000001', 'Sistemul digestiv', 1),
                                                             ('b0000004-0000-0000-0000-000000000002', 'a0000004-0000-0000-0000-000000000001', 'Sistemul circulator', 2);

INSERT INTO lessons (id, chapter_id, title, content_md, order_index) VALUES
                                                                         ('c0000004-0000-0000-0000-000000000001', 'b0000004-0000-0000-0000-000000000001',
                                                                          'Digestia în cavitatea bucală și stomac',
                                                                          '## Digestia' || chr(10) ||
                                                                          'Cavitatea bucală: dinții + amilaza salivară (descompune amidonul).' || chr(10) ||
                                                                          'Stomacul: HCl + pepsină → descompune proteinele → chim gastric.', 1),

                                                                         ('c0000004-0000-0000-0000-000000000002', 'b0000004-0000-0000-0000-000000000002',
                                                                          'Inima și circulația sângelui',
                                                                          '## Inima' || chr(10) ||
                                                                          '4 camere: 2 atrii + 2 ventricule.' || chr(10) ||
                                                                          'Circulația mică (pulmonară): inimă → plămâni → inimă.' || chr(10) ||
                                                                          'Circulația mare (sistemică): inimă → corp → inimă.', 1);

-- ==================== ISTORIE 1 — Romania ====================
INSERT INTO courses (id, title, description, category, status, visibility, created_by) VALUES
    ('a0000005-0000-0000-0000-000000000001', 'Istoria României — origini până la Marea Unire',
     'Formarea poporului român, statele medievale și drumul spre Marea Unire din 1918.',
     'Istorie', 'PUBLISHED', 'PUBLIC', '00000000-0000-0000-0000-000000000001');

INSERT INTO chapters (id, course_id, title, order_index) VALUES
                                                             ('b0000005-0000-0000-0000-000000000001', 'a0000005-0000-0000-0000-000000000001', 'Dacii și cucerirea romană', 1),
                                                             ('b0000005-0000-0000-0000-000000000002', 'a0000005-0000-0000-0000-000000000001', 'Statele medievale românești', 2),
                                                             ('b0000005-0000-0000-0000-000000000003', 'a0000005-0000-0000-0000-000000000001', 'Unirea Principatelor și Marea Unire', 3);

INSERT INTO lessons (id, chapter_id, title, content_md, order_index) VALUES
                                                                         ('c0000005-0000-0000-0000-000000000001', 'b0000005-0000-0000-0000-000000000001',
                                                                          'Dacii — civilizație și organizare',
                                                                          '## Dacii' || chr(10) ||
                                                                          'Apogeul sub Burebista (82-44 î.Hr.), capitala Sarmizegetusa Regia.' || chr(10) ||
                                                                          'Zeu principal: Zamolxis. Meșteșuguri avansate: metalurgie, ceramică.', 1),

                                                                         ('c0000005-0000-0000-0000-000000000002', 'b0000005-0000-0000-0000-000000000001',
                                                                          'Războaiele daco-romane',
                                                                          '## Războaiele daco-romane (101-102 și 105-106 d.Hr.)' || chr(10) ||
                                                                          'Împăratul Traian vs. regele Decebal.' || chr(10) ||
                                                                          'Dacia devine provincie romană în 106 d.Hr. → romanizare.', 2),

                                                                         ('c0000005-0000-0000-0000-000000000003', 'b0000005-0000-0000-0000-000000000002',
                                                                          'Întemeierea Țării Românești',
                                                                          '## Basarab I (1310-1352)' || chr(10) ||
                                                                          'Primul domn atestat documentar.' || chr(10) ||
                                                                          'Bătălia de la Posada (1330) — înfrânge armata maghiară.' || chr(10) ||
                                                                          'Consolidează independența Țării Românești.', 1),

                                                                         ('c0000005-0000-0000-0000-000000000004', 'b0000005-0000-0000-0000-000000000003',
                                                                          'Unirea Principatelor 1859',
                                                                          '## Alexandru Ioan Cuza' || chr(10) ||
                                                                          'Ales domn în Moldova (5 ian.) și Muntenia (24 ian. 1859) — Mica Unire.' || chr(10) ||
                                                                          'Reforme: agrară, secularizare, educație obligatorie.', 1),

                                                                         ('c0000005-0000-0000-0000-000000000005', 'b0000005-0000-0000-0000-000000000003',
                                                                          'Marea Unire — 1 Decembrie 1918',
                                                                          '## Marea Unire' || chr(10) ||
                                                                          'Unirea Transilvaniei cu România la 1 Decembrie 1918.' || chr(10) ||
                                                                          'Adunarea de la Alba Iulia: 100.000+ participanți, vot unanim.' || chr(10) ||
                                                                          '1 Decembrie = Ziua Națională a României.', 2);

-- ==================== ISTORIE 2 — Antichitate ====================
INSERT INTO courses (id, title, description, category, status, visibility, created_by) VALUES
    ('a0000006-0000-0000-0000-000000000001', 'Civilizații antice — Grecia și Roma',
     'Democrația ateniană, cultura greacă, republica și imperiul roman.',
     'Istorie', 'PUBLISHED', 'PUBLIC', '00000000-0000-0000-0000-000000000001');

INSERT INTO chapters (id, course_id, title, order_index) VALUES
                                                             ('b0000006-0000-0000-0000-000000000001', 'a0000006-0000-0000-0000-000000000001', 'Grecia Antică', 1),
                                                             ('b0000006-0000-0000-0000-000000000002', 'a0000006-0000-0000-0000-000000000001', 'Roma Antică', 2);

INSERT INTO lessons (id, chapter_id, title, content_md, order_index) VALUES
                                                                         ('c0000006-0000-0000-0000-000000000001', 'b0000006-0000-0000-0000-000000000001',
                                                                          'Democrația ateniană',
                                                                          '## Democrația ateniană' || chr(10) ||
                                                                          'Reformele lui Clistene (508 î.Hr.): Consiliul celor 500, Eclesia.' || chr(10) ||
                                                                          'Notă: sclavii, femeile și străinii nu aveau drepturi cetățenești.', 1),

                                                                         ('c0000006-0000-0000-0000-000000000002', 'b0000006-0000-0000-0000-000000000002',
                                                                          'De la Republică la Imperiu Roman',
                                                                          '## Roma Antică' || chr(10) ||
                                                                          'Republica (509-27 î.Hr.): 2 consuli + Senat.' || chr(10) ||
                                                                          'Augustus = primul împărat (27 î.Hr.). Pax Romana: pace și prosperitate.', 1);

-- ==================== FIZICA — Mecanica ====================
INSERT INTO courses (id, title, description, category, status, visibility, created_by) VALUES
    ('a0000007-0000-0000-0000-000000000001', 'Mecanică și mișcare',
     'Mișcarea corpurilor, forțe, legile lui Newton și energia mecanică.',
     'Fizică', 'PUBLISHED', 'PUBLIC', '00000000-0000-0000-0000-000000000001');

INSERT INTO chapters (id, course_id, title, order_index) VALUES
                                                             ('b0000007-0000-0000-0000-000000000001', 'a0000007-0000-0000-0000-000000000001', 'Mișcarea și viteza', 1),
                                                             ('b0000007-0000-0000-0000-000000000002', 'a0000007-0000-0000-0000-000000000001', 'Forțe și legile lui Newton', 2),
                                                             ('b0000007-0000-0000-0000-000000000003', 'a0000007-0000-0000-0000-000000000001', 'Energia mecanică', 3);

INSERT INTO lessons (id, chapter_id, title, content_md, order_index) VALUES
                                                                         ('c0000007-0000-0000-0000-000000000001', 'b0000007-0000-0000-0000-000000000001',
                                                                          'Viteza medie și instantanee',
                                                                          '## Viteza' || chr(10) ||
                                                                          'v = Δd / Δt (m/s). Conversie: 1 m/s = 3,6 km/h.' || chr(10) ||
                                                                          '**Exemplu:** 120 km în 2 ore → v = 60 km/h.', 1),

                                                                         ('c0000007-0000-0000-0000-000000000002', 'b0000007-0000-0000-0000-000000000002',
                                                                          'Cele trei legi ale lui Newton',
                                                                          '## Legile lui Newton' || chr(10) ||
                                                                          'I — Inerția: fără forță, corpul rămâne în starea sa.' || chr(10) ||
                                                                          'II — F = m × a (Fundamentala Dinamicii).' || chr(10) ||
                                                                          'III — Acțiune = Reacțiune (egale, sens opus).', 1),

                                                                         ('c0000007-0000-0000-0000-000000000003', 'b0000007-0000-0000-0000-000000000003',
                                                                          'Energie cinetică și potențială',
                                                                          '## Energia mecanică' || chr(10) ||
                                                                          'Cinetică: Ek = mv²/2 (energia mișcării).' || chr(10) ||
                                                                          'Potențială gravitațională: Ep = mgh.' || chr(10) ||
                                                                          'Conservare: Ek + Ep = constant (fără frecare).', 1);

-- ==================== CHIMIE — Anorganica ====================
INSERT INTO courses (id, title, description, category, status, visibility, created_by) VALUES
    ('a0000008-0000-0000-0000-000000000001', 'Chimie anorganică — elemente și compuși',
     'Tabelul periodic, oxizi, acizi, baze și săruri.',
     'Chimie', 'PUBLISHED', 'PUBLIC', '00000000-0000-0000-0000-000000000001');

INSERT INTO chapters (id, course_id, title, order_index) VALUES
                                                             ('b0000008-0000-0000-0000-000000000001', 'a0000008-0000-0000-0000-000000000001', 'Tabelul periodic', 1),
                                                             ('b0000008-0000-0000-0000-000000000002', 'a0000008-0000-0000-0000-000000000001', 'Oxizi, acizi și baze', 2);

INSERT INTO lessons (id, chapter_id, title, content_md, order_index) VALUES
                                                                         ('c0000008-0000-0000-0000-000000000001', 'b0000008-0000-0000-0000-000000000001',
                                                                          'Structura tabelului periodic',
                                                                          '## Tabelul periodic' || chr(10) ||
                                                                          'Elemente ordonate după numărul atomic Z.' || chr(10) ||
                                                                          '7 perioade (rânduri), 18 grupe (coloane).' || chr(10) ||
                                                                          'Metale alcaline (Gr.I): Li, Na, K | Gaze nobile (Gr.VIII): He, Ne, Ar.', 1),

                                                                         ('c0000008-0000-0000-0000-000000000002', 'b0000008-0000-0000-0000-000000000002',
                                                                          'Acizi și baze',
                                                                          '## Acizi și baze' || chr(10) ||
                                                                          'Acizi → eliberează H⁺: HCl, H₂SO₄, HNO₃.' || chr(10) ||
                                                                          'Baze → eliberează OH⁻: NaOH, Ca(OH)₂.' || chr(10) ||
                                                                          'pH < 7 acid | pH = 7 neutru | pH > 7 bază.', 1);

-- ==================== INFORMATICA — Python ====================
INSERT INTO courses (id, title, description, category, status, visibility, created_by) VALUES
    ('a0000009-0000-0000-0000-000000000001', 'Introducere în programare cu Python',
     'Variabile, condiții, bucle și funcții cu exemple practice în Python.',
     'Informatică', 'PUBLISHED', 'PUBLIC', '00000000-0000-0000-0000-000000000001');

INSERT INTO chapters (id, course_id, title, order_index) VALUES
                                                             ('b0000009-0000-0000-0000-000000000001', 'a0000009-0000-0000-0000-000000000001', 'Bazele limbajului Python', 1),
                                                             ('b0000009-0000-0000-0000-000000000002', 'a0000009-0000-0000-0000-000000000001', 'Structuri de control', 2),
                                                             ('b0000009-0000-0000-0000-000000000003', 'a0000009-0000-0000-0000-000000000001', 'Funcții și module', 3);

INSERT INTO lessons (id, chapter_id, title, content_md, order_index) VALUES
                                                                         ('c0000009-0000-0000-0000-000000000001', 'b0000009-0000-0000-0000-000000000001',
                                                                          'Variabile și tipuri de date',
                                                                          '## Variabile în Python' || chr(10) ||
                                                                          'nume = "Maria"  # string' || chr(10) ||
                                                                          'varsta = 15     # integer' || chr(10) ||
                                                                          'nota = 9.5      # float' || chr(10) ||
                                                                          'este_elev = True  # boolean', 1),

                                                                         ('c0000009-0000-0000-0000-000000000002', 'b0000009-0000-0000-0000-000000000001',
                                                                          'Operatori aritmetici și logici',
                                                                          '## Operatori' || chr(10) ||
                                                                          'Aritmetici: + - * / // % **' || chr(10) ||
                                                                          'Logici: and, or, not' || chr(10) ||
                                                                          'Comparație: == != < > <= >=', 2),

                                                                         ('c0000009-0000-0000-0000-000000000003', 'b0000009-0000-0000-0000-000000000002',
                                                                          'Instrucțiunea if-elif-else',
                                                                          '## Instrucțiunea if' || chr(10) ||
                                                                          'if nota >= 9: print("Foarte bine!")' || chr(10) ||
                                                                          'elif nota >= 7: print("Bine")' || chr(10) ||
                                                                          'else: print("Mai încearcă!")' || chr(10) ||
                                                                          'Python folosește indentarea pentru blocuri.', 1),

                                                                         ('c0000009-0000-0000-0000-000000000004', 'b0000009-0000-0000-0000-000000000002',
                                                                          'Bucle for și while',
                                                                          '## Bucle' || chr(10) ||
                                                                          'for i in range(1, 6): print(i)  # 1 2 3 4 5' || chr(10) ||
                                                                          'n = 1' || chr(10) ||
                                                                          'while n <= 5: print(n); n += 1', 2),

                                                                         ('c0000009-0000-0000-0000-000000000005', 'b0000009-0000-0000-0000-000000000003',
                                                                          'Definirea și apelarea funcțiilor',
                                                                          '## Funcții' || chr(10) ||
                                                                          'def saluta(nume): return f"Salut, {nume}!"' || chr(10) ||
                                                                          'def medie(note): return sum(note) / len(note)' || chr(10) ||
                                                                          'Avantaje: reutilizare, organizare, testare.', 1);

-- ==================== GEOGRAFIE — Romania ====================
INSERT INTO courses (id, title, description, category, status, visibility, created_by) VALUES
    ('a000000a-0000-0000-0000-000000000001', 'Geografia României',
     'Relief, climă, hidrografie și regiuni geografice ale României.',
     'Geografie', 'PUBLISHED', 'PUBLIC', '00000000-0000-0000-0000-000000000001');

INSERT INTO chapters (id, course_id, title, order_index) VALUES
                                                             ('b000000a-0000-0000-0000-000000000001', 'a000000a-0000-0000-0000-000000000001', 'Relieful României', 1),
                                                             ('b000000a-0000-0000-0000-000000000002', 'a000000a-0000-0000-0000-000000000001', 'Clima și hidrografia', 2);

INSERT INTO lessons (id, chapter_id, title, content_md, order_index) VALUES
                                                                         ('c000000a-0000-0000-0000-000000000001', 'b000000a-0000-0000-0000-000000000001',
                                                                          'Carpații și subdiviziunile lor',
                                                                          '## Carpații (28% din suprafața României)' || chr(10) ||
                                                                          'Orientali: vf. Pietrosu 2303 m.' || chr(10) ||
                                                                          'Meridionali: vf. Moldoveanu 2544 m (cel mai înalt).' || chr(10) ||
                                                                          'Occidentali: altitudine mai mică.', 1),

                                                                         ('c000000a-0000-0000-0000-000000000002', 'b000000a-0000-0000-0000-000000000002',
                                                                          'Dunărea și principalii afluenți',
                                                                          '## Dunărea' || chr(10) ||
                                                                          'Lungime pe teritoriul României: ~1075 km.' || chr(10) ||
                                                                          'Se varsă în Marea Neagră prin Delta Dunării (Rezervație UNESCO).' || chr(10) ||
                                                                          'Afluenți: Prut, Siret, Olt, Mureș (indirect).', 1);

-- ==================== ROMANA — Literatura ====================
INSERT INTO courses (id, title, description, category, status, visibility, created_by) VALUES
    ('a000000b-0000-0000-0000-000000000001', 'Literatură română — opere și autori esențiali',
     'Analiza marilor opere: Eminescu, Creangă, Caragiale, Rebreanu.',
     'Limba și Literatura Română', 'PUBLISHED', 'PUBLIC', '00000000-0000-0000-0000-000000000001');

INSERT INTO chapters (id, course_id, title, order_index) VALUES
                                                             ('b000000b-0000-0000-0000-000000000001', 'a000000b-0000-0000-0000-000000000001', 'Mihai Eminescu', 1),
                                                             ('b000000b-0000-0000-0000-000000000002', 'a000000b-0000-0000-0000-000000000001', 'Caragiale și Creangă', 2);

INSERT INTO lessons (id, chapter_id, title, content_md, order_index) VALUES
                                                                         ('c000000b-0000-0000-0000-000000000001', 'b000000b-0000-0000-0000-000000000001',
                                                                          'Luceafărul — analiză',
                                                                          '## Luceafărul (1883) — Mihai Eminescu' || chr(10) ||
                                                                          '98 de strofe în 4 cânturi.' || chr(10) ||
                                                                          'Temă: conflictul geniu vs. om comun, iubire vs. nemurire.' || chr(10) ||
                                                                          'Simboluri: Hyperion = geniul, Cătălina = omul comun.', 1),

                                                                         ('c000000b-0000-0000-0000-000000000002', 'b000000b-0000-0000-0000-000000000002',
                                                                          'O scrisoare pierdută — comedie',
                                                                          '## O scrisoare pierdută (1884) — I.L. Caragiale' || chr(10) ||
                                                                          'Comedie în 4 acte. Satirizează societatea politică românească.' || chr(10) ||
                                                                          'Personaje: Trahanache, Zoe, Tipătescu, Cațavencu, Dandanache.', 1);

-- ==================== ENGLEZA — Gramatica ====================
INSERT INTO courses (id, title, description, category, status, visibility, created_by) VALUES
    ('a000000c-0000-0000-0000-000000000001', 'Engleză — gramatică esențială',
     'Timpuri verbale, verbe modale și condiționale.',
     'Limbi Străine', 'PUBLISHED', 'PUBLIC', '00000000-0000-0000-0000-000000000001');

INSERT INTO chapters (id, course_id, title, order_index) VALUES
                                                             ('b000000c-0000-0000-0000-000000000001', 'a000000c-0000-0000-0000-000000000001', 'Timpuri verbale', 1),
                                                             ('b000000c-0000-0000-0000-000000000002', 'a000000c-0000-0000-0000-000000000001', 'Verbe modale', 2);

INSERT INTO lessons (id, chapter_id, title, content_md, order_index) VALUES
                                                                         ('c000000c-0000-0000-0000-000000000001', 'b000000c-0000-0000-0000-000000000001',
                                                                          'Present Simple vs Present Continuous',
                                                                          '## Present Simple vs Continuous' || chr(10) ||
                                                                          'Simple: acțiuni obișnuite. I go to school every day.' || chr(10) ||
                                                                          'Continuous: acțiuni în desfășurare. I am studying now.' || chr(10) ||
                                                                          'Indicatori: always/usually vs. now/at the moment.', 1),

                                                                         ('c000000c-0000-0000-0000-000000000002', 'b000000c-0000-0000-0000-000000000002',
                                                                          'Verbe modale — can, must, should',
                                                                          '## Verbe modale' || chr(10) ||
                                                                          'CAN: abilitate (I can swim) sau permisiune.' || chr(10) ||
                                                                          'MUST: obligație internă sau deducție.' || chr(10) ||
                                                                          'SHOULD: sfat/recomandare. You should eat vegetables.' || chr(10) ||
                                                                          'HAVE TO: obligație externă.', 1);

-- ==================== ASTRONOMIE ====================
INSERT INTO courses (id, title, description, category, status, visibility, created_by) VALUES
    ('a000000d-0000-0000-0000-000000000001', 'Sistemul Solar și Universul',
     'Planetele sistemului solar, stele, galaxii și marile mistere ale cosmosului.',
     'Astronomie', 'PUBLISHED', 'PUBLIC', '00000000-0000-0000-0000-000000000001');

INSERT INTO chapters (id, course_id, title, order_index) VALUES
                                                             ('b000000d-0000-0000-0000-000000000001', 'a000000d-0000-0000-0000-000000000001', 'Planetele sistemului solar', 1),
                                                             ('b000000d-0000-0000-0000-000000000002', 'a000000d-0000-0000-0000-000000000001', 'Stele și galaxii', 2);

INSERT INTO lessons (id, chapter_id, title, content_md, order_index) VALUES
                                                                         ('c000000d-0000-0000-0000-000000000001', 'b000000d-0000-0000-0000-000000000001',
                                                                          'Planetele interioare',
                                                                          '## Planetele interioare' || chr(10) ||
                                                                          'Mercur: cea mai mică, temperaturi extreme.' || chr(10) ||
                                                                          'Venus: cea mai caldă (efect de seră intens).' || chr(10) ||
                                                                          'Pământul: 71% apă, singura cu viață confirmată.' || chr(10) ||
                                                                          'Marte: Planeta Roșie, Olympus Mons (27 km).', 1),

                                                                         ('c000000d-0000-0000-0000-000000000002', 'b000000d-0000-0000-0000-000000000002',
                                                                          'Viața și moartea unei stele',
                                                                          '## Ciclul de viață al unei stele' || chr(10) ||
                                                                          'Naștere: nor molecular → contracție gravitațională.' || chr(10) ||
                                                                          'Secvența principală: fuziune H → He (Soarele: ~5 mld. ani rămași).' || chr(10) ||
                                                                          'Final mic: gigantă roșie → pitică albă.' || chr(10) ||
                                                                          'Final masiv: supernovă → stea neutronică / gaură neagră.', 1);

-- ==================== EDUCATIE CIVICA ====================
INSERT INTO courses (id, title, description, category, status, visibility, created_by) VALUES
    ('a000000e-0000-0000-0000-000000000001', 'Educație civică — drepturi și democrație',
     'Drepturile omului, Constituția României și rolul cetățeanului activ.',
     'Educație Civică', 'PUBLISHED', 'PUBLIC', '00000000-0000-0000-0000-000000000001');

INSERT INTO chapters (id, course_id, title, order_index) VALUES
                                                             ('b000000e-0000-0000-0000-000000000001', 'a000000e-0000-0000-0000-000000000001', 'Drepturile omului', 1),
                                                             ('b000000e-0000-0000-0000-000000000002', 'a000000e-0000-0000-0000-000000000001', 'Instituțiile democratice', 2);

INSERT INTO lessons (id, chapter_id, title, content_md, order_index) VALUES
                                                                         ('c000000e-0000-0000-0000-000000000001', 'b000000e-0000-0000-0000-000000000001',
                                                                          'Declarația Universală a Drepturilor Omului',
                                                                          '## DUDO (10 decembrie 1948)' || chr(10) ||
                                                                          'Drepturi civile: viață, libertate, interzicerea torturii, proces echitabil.' || chr(10) ||
                                                                          'Drepturi sociale: educație, muncă, sănătate.' || chr(10) ||
                                                                          '10 decembrie = Ziua Drepturilor Omului.', 1),

                                                                         ('c000000e-0000-0000-0000-000000000002', 'b000000e-0000-0000-0000-000000000002',
                                                                          'Parlamentul și Guvernul României',
                                                                          '## Instituțiile statului' || chr(10) ||
                                                                          'Parlamentul (legislativ): Senat 136 + Camera Deputaților 330.' || chr(10) ||
                                                                          'Guvernul (executiv): Prim-ministru + miniștri.' || chr(10) ||
                                                                          'Președinte: ales direct, mandat 5 ani.' || chr(10) ||
                                                                          'Justiție: Curtea Supremă + Curtea Constituțională.', 1);

-- ==================== MUZICA ====================
INSERT INTO courses (id, title, description, category, status, visibility, created_by) VALUES
    ('a000000f-0000-0000-0000-000000000001', 'Educație muzicală — teoria muzicii',
     'Note, portative, ritmuri, instrumente muzicale și mari compozitori.',
     'Muzică', 'PUBLISHED', 'PUBLIC', '00000000-0000-0000-0000-000000000001');

INSERT INTO chapters (id, course_id, title, order_index) VALUES
                                                             ('b000000f-0000-0000-0000-000000000001', 'a000000f-0000-0000-0000-000000000001', 'Notație muzicală', 1),
                                                             ('b000000f-0000-0000-0000-000000000002', 'a000000f-0000-0000-0000-000000000001', 'Mari compozitori', 2);

INSERT INTO lessons (id, chapter_id, title, content_md, order_index) VALUES
                                                                         ('c000000f-0000-0000-0000-000000000001', 'b000000f-0000-0000-0000-000000000001',
                                                                          'Portativul și cheile muzicale',
                                                                          '## Portativul' || chr(10) ||
                                                                          '5 linii și 4 spații. Note: Do Re Mi Fa Sol La Si Do.' || chr(10) ||
                                                                          'Cheia Sol (instrumente înalte) | Cheia Fa (instrumente grave).' || chr(10) ||
                                                                          'Durate: întreagă=4t, doime=2t, pătrime=1t, optime=1/2t.', 1),

                                                                         ('c000000f-0000-0000-0000-000000000002', 'b000000f-0000-0000-0000-000000000002',
                                                                          'Bach, Mozart și Beethoven',
                                                                          '## Mari compozitori clasici' || chr(10) ||
                                                                          'Bach (1685-1750): baroc, Tocata și Fuga în Re minor.' || chr(10) ||
                                                                          'Mozart (1756-1791): clasicism, Don Giovanni, Simfonia nr.40.' || chr(10) ||
                                                                          'Beethoven (1770-1827): Simfonia nr.9 (Oda Bucuriei) — imnul UE, compusă surd.', 1);

-- ============================================================
-- TOTAL: 15 cursuri PUBLIC + PUBLISHED
-- ============================================================