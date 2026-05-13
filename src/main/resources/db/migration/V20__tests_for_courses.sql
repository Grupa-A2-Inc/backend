-- ============================================================
-- SEED V19.1: TESTE PENTRU FIECARE CURS
-- Adăugăm câte un test pentru o lecție din FIECARE dintre cele 15 cursuri.
-- UUID-uri folosite: d0000000-0000-0000-0000-0000000000xx
-- ============================================================

-- ==================== 1. TESTS ====================
INSERT INTO tests (id, lesson_id, created_by, title, description, time_limit_sec, status, ai_enabled) VALUES
                                                                                                          -- 1. Matematică 1 (c0000001...01)
                                                                                                          ('d0000000-0000-0000-0000-000000000001', 'c0000001-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001',
                                                                                                           'Test: Numere naturale și întregi', 'Verifică noțiunile de bază.', 300, 'PUBLISHED', false),
                                                                                                          -- 2. Matematică 2 (c0000002...01)
                                                                                                          ('d0000000-0000-0000-0000-000000000002', 'c0000002-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001',
                                                                                                           'Test: Triunghiul', 'Proprietățile de bază ale triunghiului.', 300, 'PUBLISHED', false),
                                                                                                          -- 3. Biologie 1 (c0000003...01)
                                                                                                          ('d0000000-0000-0000-0000-000000000003', 'c0000003-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001',
                                                                                                           'Test: Membrana celulară', 'Funcțiile membranei celulare.', 300, 'PUBLISHED', false),
                                                                                                          -- 4. Biologie 2 (c0000004...01)
                                                                                                          ('d0000000-0000-0000-0000-000000000004', 'c0000004-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001',
                                                                                                           'Test: Digestia', 'Cavitatea bucală și stomacul.', 300, 'PUBLISHED', false),
                                                                                                          -- 5. Istorie 1 (c0000005...01)
                                                                                                          ('d0000000-0000-0000-0000-000000000005', 'c0000005-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001',
                                                                                                           'Test: Dacii', 'Organizarea civilizației dacice.', 300, 'PUBLISHED', false),
                                                                                                          -- 6. Istorie 2 (c0000006...01)
                                                                                                          ('d0000000-0000-0000-0000-000000000006', 'c0000006-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001',
                                                                                                           'Test: Democrația ateniană', 'Apariția și regulile democrației în Atena.', 300, 'PUBLISHED', false),
                                                                                                          -- 7. Fizică (c0000007...01)
                                                                                                          ('d0000000-0000-0000-0000-000000000007', 'c0000007-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001',
                                                                                                           'Test: Viteza', 'Calculul vitezei medii.', 300, 'PUBLISHED', false),
                                                                                                          -- 8. Chimie (c0000008...01)
                                                                                                          ('d0000000-0000-0000-0000-000000000008', 'c0000008-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001',
                                                                                                           'Test: Tabelul periodic', 'Organizarea elementelor chimice.', 300, 'PUBLISHED', false),
                                                                                                          -- 9. Informatică (c0000009...01)
                                                                                                          ('d0000000-0000-0000-0000-000000000009', 'c0000009-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001',
                                                                                                           'Test: Variabile în Python', 'Tipuri de date de bază.', 300, 'PUBLISHED', false),
                                                                                                          -- 10. Geografie (c000000a...01)
                                                                                                          ('d0000000-0000-0000-0000-00000000000a', 'c000000a-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001',
                                                                                                           'Test: Carpații', 'Geografia Munților Carpați.', 300, 'PUBLISHED', false),
                                                                                                          -- 11. Română (c000000b...01)
                                                                                                          ('d0000000-0000-0000-0000-00000000000b', 'c000000b-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001',
                                                                                                           'Test: Luceafărul', 'Analiza poemului eminescian.', 300, 'PUBLISHED', false),
                                                                                                          -- 12. Engleză (c000000c...01)
                                                                                                          ('d0000000-0000-0000-0000-00000000000c', 'c000000c-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001',
                                                                                                           'Test: Present Simple', 'Utilizarea timpurilor prezentului.', 300, 'PUBLISHED', false),
                                                                                                          -- 13. Astronomie (c000000d...01)
                                                                                                          ('d0000000-0000-0000-0000-00000000000d', 'c000000d-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001',
                                                                                                           'Test: Planetele interioare', 'Sistemul solar interior.', 300, 'PUBLISHED', false),
                                                                                                          -- 14. Educație Civică (c000000e...01)
                                                                                                          ('d0000000-0000-0000-0000-00000000000e', 'c000000e-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001',
                                                                                                           'Test: Drepturile Omului', 'Declarația Universală a Drepturilor Omului.', 300, 'PUBLISHED', false),
                                                                                                          -- 15. Muzică (c000000f...01)
                                                                                                          ('d0000000-0000-0000-0000-00000000000f', 'c000000f-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001',
                                                                                                           'Test: Portativul', 'Notele muzicale și portativul.', 300, 'PUBLISHED', false);

-- ==================== 2. QUESTIONS ====================
INSERT INTO questions (id, test_id, question_type, content, difficulty) VALUES
                                                                            (1, 'd0000000-0000-0000-0000-000000000001', 'SINGLE_CHOICE', 'Care mulțime reprezintă numerele întregi?', 0.2),
                                                                            (2, 'd0000000-0000-0000-0000-000000000002', 'SINGLE_CHOICE', 'Care este suma unghiurilor unui triunghi?', 0.1),
                                                                            (3, 'd0000000-0000-0000-0000-000000000003', 'TRUE_FALSE', 'Membrana celulară permite trecerea oricărei substanțe fără nicio selecție.', 0.3),
                                                                            (4, 'd0000000-0000-0000-0000-000000000004', 'SINGLE_CHOICE', 'Ce enzimă din stomac descompune proteinele?', 0.5),
                                                                            (5, 'd0000000-0000-0000-0000-000000000005', 'SINGLE_CHOICE', 'Cum se numea capitala statului dac în timpul lui Decebal?', 0.3),
                                                                            (6, 'd0000000-0000-0000-0000-000000000006', 'MULTI_CHOICE', 'Cine NU avea drepturi cetățenești în Atena antică?', 0.4),
                                                                            (7, 'd0000000-0000-0000-0000-000000000007', 'SINGLE_CHOICE', 'Dacă parcurgi 100 km în 2 ore, care este viteza ta medie?', 0.2),
                                                                            (8, 'd0000000-0000-0000-0000-000000000008', 'TRUE_FALSE', 'Elementele în tabelul periodic sunt ordonate după numărul lor atomic.', 0.2),
                                                                            (9, 'd0000000-0000-0000-0000-000000000009', 'SINGLE_CHOICE', 'Ce tip de dată este `True` în Python?', 0.1),
                                                                            (10, 'd0000000-0000-0000-0000-00000000000a', 'SINGLE_CHOICE', 'Care este cel mai înalt vârf din Carpații României?', 0.3),
                                                                            (11, 'd0000000-0000-0000-0000-00000000000b', 'MULTI_CHOICE', 'Ce reprezintă Hyperion în poemul Luceafărul?', 0.6),
                                                                            (12, 'd0000000-0000-0000-0000-00000000000c', 'SINGLE_CHOICE', 'Care propoziție descrie o acțiune care se întâmplă frecvent?', 0.4),
                                                                            (13, 'd0000000-0000-0000-0000-00000000000d', 'SINGLE_CHOICE', 'Care dintre următoarele este considerată "Planeta Roșie"?', 0.1),
                                                                            (14, 'd0000000-0000-0000-0000-00000000000e', 'SINGLE_CHOICE', 'În ce an a fost adoptată Declarația Universală a Drepturilor Omului?', 0.3),
                                                                            (15, 'd0000000-0000-0000-0000-00000000000f', 'SINGLE_CHOICE', 'Câte linii are un portativ muzical clasic?', 0.1);

-- ==================== 3. QUESTION_OPTIONS ====================
INSERT INTO question_options (question_id, text, display_order, is_correct) VALUES
                                                                                -- Q1 (Matematică 1)
                                                                                (1, 'N (naturale)', 1, false), (1, 'Z (întregi)', 2, true), (1, 'Q (raționale)', 3, false),
                                                                                -- Q2 (Matematică 2)
                                                                                (2, '90 grade', 1, false), (2, '180 grade', 2, true), (2, '360 grade', 3, false),
                                                                                -- Q3 (Biologie 1)
                                                                                (3, 'Adevărat', 1, false), (3, 'Fals', 2, true),
                                                                                -- Q4 (Biologie 2)
                                                                                (4, 'Amilaza salivară', 1, false), (4, 'Pepsina', 2, true), (4, 'Insulina', 3, false),
                                                                                -- Q5 (Istorie 1)
                                                                                (5, 'Sarmizegetusa Regia', 1, true), (5, 'Apulum', 2, false), (5, 'Napoca', 3, false),
                                                                                -- Q6 (Istorie 2 - Multi)
                                                                                (6, 'Femeile', 1, true), (6, 'Sclavii', 2, true), (6, 'Bărbații liberi născuți în cetate', 3, false),
                                                                                -- Q7 (Fizică)
                                                                                (7, '50 km/h', 1, true), (7, '100 km/h', 2, false), (7, '200 km/h', 3, false),
                                                                                -- Q8 (Chimie)
                                                                                (8, 'Adevărat', 1, true), (8, 'Fals', 2, false),
                                                                                -- Q9 (Informatica)
                                                                                (9, 'Integer', 1, false), (9, 'String', 2, false), (9, 'Boolean', 3, true),
                                                                                -- Q10 (Geografie)
                                                                                (10, 'Vârful Pietrosu', 1, false), (10, 'Vârful Moldoveanu', 2, true), (10, 'Vârful Omu', 3, false),
                                                                                -- Q11 (Română - Multi)
                                                                                (11, 'Geniul neînțeles', 1, true), (11, 'Dorința de cunoaștere absolută', 2, true), (11, 'Omul comun, muritor', 3, false),
                                                                                -- Q12 (Engleză)
                                                                                (12, 'I am studying now.', 1, false), (12, 'I go to school every day.', 2, true), (12, 'I studied yesterday.', 3, false),
                                                                                -- Q13 (Astronomie)
                                                                                (13, 'Venus', 1, false), (13, 'Marte', 2, true), (13, 'Jupiter', 3, false),
                                                                                -- Q14 (Civică)
                                                                                (14, '1918', 1, false), (14, '1945', 2, false), (14, '1948', 3, true),
                                                                                -- Q15 (Muzică)
                                                                                (15, '4 linii', 1, false), (15, '5 linii', 2, true), (15, '6 linii', 3, false);

-- ============================================================
-- Sincronizare secvențe (pentru inserările manuale ID)
-- ============================================================
SELECT setval('questions_id_seq', (SELECT MAX(id) FROM questions));
SELECT setval('question_options_id_seq', (SELECT MAX(id) FROM question_options));