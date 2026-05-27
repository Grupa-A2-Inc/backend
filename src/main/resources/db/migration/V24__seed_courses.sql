-- =============================================================
-- SEED: Courses, Chapters, Lessons, Lesson Resources
-- Based on Romanian high school & university subject structure
-- All content in English
-- =============================================================

-- ========================
-- HELPER: fixed UUIDs
-- ========================

-- COURSES
-- c1 = Romanian Language & Literature
-- c2 = Mathematics
-- c3 = Physics
-- c4 = Chemistry
-- c5 = Biology
-- c6 = History
-- c7 = Geography
-- c8 = Philosophy
-- c9 = Psychology
-- c10 = Computer Science & IT
-- c11 = [UNI] Anatomy (Medicine)
-- c12 = [UNI] Data Structures & Algorithms (IT)
-- c13 = [UNI] Civil Law (Law)
-- c14 = [UNI] Macroeconomics (Economics)
-- c15 = [UNI] Architectural Design (Architecture)

-- =====================================================================
-- COURSES
-- =====================================================================
INSERT INTO courses (id, title, description, category, status, visibility, created_by) VALUES

-- HIGH SCHOOL
('00000000-0000-0000-0000-000000000001',
 'Romanian Language & Literature',
 'A comprehensive exploration of the Romanian language — grammar, syntax, stylistics — combined with the study of canonical literary texts from all major periods: classical, modern, and contemporary. Students develop reading comprehension, critical thinking, and expressive writing skills.',
 'Language & Literature',
 'PUBLISHED', 'PUBLIC',
 '00000000-0000-0000-0000-000000000099'),

('00000000-0000-0000-0000-000000000002',
 'Mathematics',
 'From the fundamentals of algebra and geometry to advanced topics in calculus, trigonometry, and combinatorics. This course builds rigorous logical reasoning and problem-solving skills essential for STEM fields and everyday analytical thinking.',
 'Mathematics',
 'PUBLISHED', 'PUBLIC',
 '00000000-0000-0000-0000-000000000099'),

('00000000-0000-0000-0000-000000000003',
 'Physics',
 'An in-depth study of the physical world — classical mechanics, thermodynamics, electricity, magnetism, optics, and modern physics. Emphasis on experimental reasoning and mathematical modeling of natural phenomena.',
 'Natural Sciences',
 'PUBLISHED', 'PUBLIC',
 '00000000-0000-0000-0000-000000000099'),

('00000000-0000-0000-0000-000000000004',
 'Chemistry',
 'Covering atomic theory, chemical bonding, stoichiometry, organic and inorganic chemistry, and electrochemistry. Students learn to interpret the molecular world through equations, experiments, and real-world applications.',
 'Natural Sciences',
 'PUBLISHED', 'PUBLIC',
 '00000000-0000-0000-0000-000000000099'),

('00000000-0000-0000-0000-000000000005',
 'Biology',
 'A journey through life itself — from cell biology and genetics to ecosystems and human physiology. This course connects molecular mechanisms to large-scale ecological processes, preparing students for health, environmental, and life-science careers.',
 'Natural Sciences',
 'PUBLISHED', 'PUBLIC',
 '00000000-0000-0000-0000-000000000099'),

('00000000-0000-0000-0000-000000000006',
 'History',
 'A chronological and thematic exploration of world history — ancient civilizations, medieval empires, early modern revolutions, and the 20th-century conflicts that shaped the present. Critical analysis of sources and historical causation is central.',
 'Humanities',
 'PUBLISHED', 'PUBLIC',
 '00000000-0000-0000-0000-000000000099'),

('00000000-0000-0000-0000-000000000007',
 'Geography',
 'Physical and human geography explored together: plate tectonics, climate systems, river dynamics, urbanization, globalization, and geopolitical regions. Includes map interpretation and spatial thinking skills.',
 'Humanities',
 'PUBLISHED', 'PUBLIC',
 '00000000-0000-0000-0000-000000000099'),

('00000000-0000-0000-0000-000000000008',
 'Philosophy',
 'An introduction to the great questions of existence, knowledge, ethics, and logic. Students engage with primary texts from Plato to Kant to contemporary analytic and continental thinkers, developing arguments and evaluating worldviews.',
 'Humanities',
 'PUBLISHED', 'PUBLIC',
 '00000000-0000-0000-0000-000000000099'),

('00000000-0000-0000-0000-000000000009',
 'Psychology',
 'Exploring the science of mind and behavior: neuroscience foundations, perception, memory, learning, emotion, personality, social influence, and mental health. Builds both self-awareness and scientific literacy.',
 'Social Sciences',
 'PUBLISHED', 'PUBLIC',
 '00000000-0000-0000-0000-000000000099'),

('00000000-0000-0000-0000-000000000010',
 'Computer Science & Information Technology',
 'Foundational CS concepts — algorithms, data structures, programming paradigms, networking, databases, and cybersecurity basics — taught through hands-on coding projects in Python and SQL.',
 'Technology',
 'PUBLISHED', 'PUBLIC',
 '00000000-0000-0000-0000-000000000099'),

-- UNIVERSITY
('00000000-0000-0000-0000-000000000011',
 'Human Anatomy',
 'A university-level medical course covering the macroscopic and microscopic structure of the human body. Organ systems are studied in detail — skeletal, muscular, nervous, cardiovascular, respiratory, digestive, urinary, and reproductive — with clinical correlations throughout.',
 'Medicine',
 'PUBLISHED', 'PUBLIC',
 '00000000-0000-0000-0000-000000000099'),

('00000000-0000-0000-0000-000000000012',
 'Algorithms & Data Structures',
 'A rigorous university course on computational thinking: arrays, linked lists, trees, graphs, hash tables, sorting algorithms, searching strategies, and complexity analysis (Big O). Programming in Java/Python with real-world problem sets.',
 'Computer Science',
 'PUBLISHED', 'PUBLIC',
 '00000000-0000-0000-0000-000000000099'),

('00000000-0000-0000-0000-000000000013',
 'Civil Law',
 'An introduction to private law for law faculty students: legal persons, property rights, obligations, contracts, torts, family law, and inheritance. Focus on the Romanian Civil Code and European legal traditions.',
 'Law',
 'PUBLISHED', 'PUBLIC',
 '00000000-0000-0000-0000-000000000099'),

('00000000-0000-0000-0000-000000000014',
 'Macroeconomics',
 'University-level analysis of national and global economies: GDP measurement, aggregate demand and supply, fiscal and monetary policy, inflation, unemployment, international trade, and economic growth models (Solow, Keynesian, neoclassical).',
 'Economics',
 'PUBLISHED', 'PUBLIC',
 '00000000-0000-0000-0000-000000000099'),

('00000000-0000-0000-0000-000000000015',
 'Architectural Design Studio',
 'A project-based studio course for architecture students covering design process, spatial composition, structural logic, building materials, environmental performance, and presentation techniques. Projects escalate from single-room to multi-use building scale.',
 'Architecture',
 'PUBLISHED', 'PUBLIC',
 '00000000-0000-0000-0000-000000000099');


-- =====================================================================
-- CHAPTERS
-- =====================================================================
INSERT INTO chapters (id, course_id, title, order_index) VALUES

-- ── Romanian Language & Literature ──────────────────────────────────
('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'Grammar Foundations', 0),
('10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'Classical Literature', 1),
('10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'Modern & Contemporary Literature', 2),
('10000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000001', 'Essay Writing & Argumentation', 3),

-- ── Mathematics ─────────────────────────────────────────────────────
('10000000-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000002', 'Algebra & Equations', 0),
('10000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000002', 'Geometry & Trigonometry', 1),
('10000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000002', 'Functions & Analysis', 2),
('10000000-0000-0000-0000-000000000013', '00000000-0000-0000-0000-000000000002', 'Probability & Statistics', 3),

-- ── Physics ─────────────────────────────────────────────────────────
('10000000-0000-0000-0000-000000000020', '00000000-0000-0000-0000-000000000003', 'Classical Mechanics', 0),
('10000000-0000-0000-0000-000000000021', '00000000-0000-0000-0000-000000000003', 'Thermodynamics', 1),
('10000000-0000-0000-0000-000000000022', '00000000-0000-0000-0000-000000000003', 'Electricity & Magnetism', 2),
('10000000-0000-0000-0000-000000000023', '00000000-0000-0000-0000-000000000003', 'Optics & Modern Physics', 3),

-- ── Chemistry ───────────────────────────────────────────────────────
('10000000-0000-0000-0000-000000000030', '00000000-0000-0000-0000-000000000004', 'Atomic Structure & Periodic Table', 0),
('10000000-0000-0000-0000-000000000031', '00000000-0000-0000-0000-000000000004', 'Chemical Bonding & Reactions', 1),
('10000000-0000-0000-0000-000000000032', '00000000-0000-0000-0000-000000000004', 'Organic Chemistry', 2),
('10000000-0000-0000-0000-000000000033', '00000000-0000-0000-0000-000000000004', 'Electrochemistry & Applied Chemistry', 3),

-- ── Biology ─────────────────────────────────────────────────────────
('10000000-0000-0000-0000-000000000040', '00000000-0000-0000-0000-000000000005', 'Cell Biology', 0),
('10000000-0000-0000-0000-000000000041', '00000000-0000-0000-0000-000000000005', 'Genetics & Heredity', 1),
('10000000-0000-0000-0000-000000000042', '00000000-0000-0000-0000-000000000005', 'Human Physiology', 2),
('10000000-0000-0000-0000-000000000043', '00000000-0000-0000-0000-000000000005', 'Ecology & Evolution', 3),

-- ── History ─────────────────────────────────────────────────────────
('10000000-0000-0000-0000-000000000050', '00000000-0000-0000-0000-000000000006', 'Ancient Civilizations', 0),
('10000000-0000-0000-0000-000000000051', '00000000-0000-0000-0000-000000000006', 'Medieval & Renaissance World', 1),
('10000000-0000-0000-0000-000000000052', '00000000-0000-0000-0000-000000000006', 'Modern Revolutions & Nation States', 2),
('10000000-0000-0000-0000-000000000053', '00000000-0000-0000-0000-000000000006', '20th Century: Wars & Globalization', 3),

-- ── Geography ───────────────────────────────────────────────────────
('10000000-0000-0000-0000-000000000060', '00000000-0000-0000-0000-000000000007', 'Physical Geography', 0),
('10000000-0000-0000-0000-000000000061', '00000000-0000-0000-0000-000000000007', 'Climatology & Hydrology', 1),
('10000000-0000-0000-0000-000000000062', '00000000-0000-0000-0000-000000000007', 'Human & Economic Geography', 2),
('10000000-0000-0000-0000-000000000063', '00000000-0000-0000-0000-000000000007', 'Geopolitics & World Regions', 3),

-- ── Philosophy ──────────────────────────────────────────────────────
('10000000-0000-0000-0000-000000000070', '00000000-0000-0000-0000-000000000008', 'Logic & Critical Thinking', 0),
('10000000-0000-0000-0000-000000000071', '00000000-0000-0000-0000-000000000008', 'Epistemology: Theory of Knowledge', 1),
('10000000-0000-0000-0000-000000000072', '00000000-0000-0000-0000-000000000008', 'Ethics & Moral Philosophy', 2),
('10000000-0000-0000-0000-000000000073', '00000000-0000-0000-0000-000000000008', 'Metaphysics & Philosophy of Mind', 3),

-- ── Psychology ──────────────────────────────────────────────────────
('10000000-0000-0000-0000-000000000080', '00000000-0000-0000-0000-000000000009', 'Neuroscience & Biological Bases of Behavior', 0),
('10000000-0000-0000-0000-000000000081', '00000000-0000-0000-0000-000000000009', 'Perception, Memory & Cognition', 1),
('10000000-0000-0000-0000-000000000082', '00000000-0000-0000-0000-000000000009', 'Emotion, Motivation & Personality', 2),
('10000000-0000-0000-0000-000000000083', '00000000-0000-0000-0000-000000000009', 'Social Psychology & Mental Health', 3),

-- ── Computer Science ────────────────────────────────────────────────
('10000000-0000-0000-0000-000000000090', '00000000-0000-0000-0000-000000000010', 'Programming Fundamentals', 0),
('10000000-0000-0000-0000-000000000091', '00000000-0000-0000-0000-000000000010', 'Data Structures', 1),
('10000000-0000-0000-0000-000000000092', '00000000-0000-0000-0000-000000000010', 'Networking & Internet', 2),
('10000000-0000-0000-0000-000000000093', '00000000-0000-0000-0000-000000000010', 'Databases & SQL', 3),

-- ── Human Anatomy (UNI) ─────────────────────────────────────────────
('10000000-0000-0000-0000-000000000100', '00000000-0000-0000-0000-000000000011', 'Musculoskeletal System', 0),
('10000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000011', 'Nervous System', 1),
('10000000-0000-0000-0000-000000000102', '00000000-0000-0000-0000-000000000011', 'Cardiovascular & Respiratory Systems', 2),
('10000000-0000-0000-0000-000000000103', '00000000-0000-0000-0000-000000000011', 'Digestive, Urinary & Reproductive Systems', 3),

-- ── Algorithms & DS (UNI) ───────────────────────────────────────────
('10000000-0000-0000-0000-000000000110', '00000000-0000-0000-0000-000000000012', 'Complexity Analysis', 0),
('10000000-0000-0000-0000-000000000111', '00000000-0000-0000-0000-000000000012', 'Linear Data Structures', 1),
('10000000-0000-0000-0000-000000000112', '00000000-0000-0000-0000-000000000012', 'Trees & Graphs', 2),
('10000000-0000-0000-0000-000000000113', '00000000-0000-0000-0000-000000000012', 'Sorting, Searching & Hashing', 3),

-- ── Civil Law (UNI) ─────────────────────────────────────────────────
('10000000-0000-0000-0000-000000000120', '00000000-0000-0000-0000-000000000013', 'Legal Persons & Rights', 0),
('10000000-0000-0000-0000-000000000121', '00000000-0000-0000-0000-000000000013', 'Property & Real Rights', 1),
('10000000-0000-0000-0000-000000000122', '00000000-0000-0000-0000-000000000013', 'Contracts & Obligations', 2),
('10000000-0000-0000-0000-000000000123', '00000000-0000-0000-0000-000000000013', 'Family Law & Inheritance', 3),

-- ── Macroeconomics (UNI) ────────────────────────────────────────────
('10000000-0000-0000-0000-000000000130', '00000000-0000-0000-0000-000000000014', 'National Income & GDP', 0),
('10000000-0000-0000-0000-000000000131', '00000000-0000-0000-0000-000000000014', 'Aggregate Demand & Supply', 1),
('10000000-0000-0000-0000-000000000132', '00000000-0000-0000-0000-000000000014', 'Fiscal & Monetary Policy', 2),
('10000000-0000-0000-0000-000000000133', '00000000-0000-0000-0000-000000000014', 'Growth Models & International Trade', 3),

-- ── Architecture Studio (UNI) ───────────────────────────────────────
('10000000-0000-0000-0000-000000000140', '00000000-0000-0000-0000-000000000015', 'Design Process & Methods', 0),
('10000000-0000-0000-0000-000000000141', '00000000-0000-0000-0000-000000000015', 'Structural Systems & Materials', 1),
('10000000-0000-0000-0000-000000000142', '00000000-0000-0000-0000-000000000015', 'Environmental Design & Sustainability', 2),
('10000000-0000-0000-0000-000000000143', '00000000-0000-0000-0000-000000000015', 'Urban Scale & Presentation', 3);


-- =====================================================================
-- LESSONS  (3 per chapter × 60 chapters = 180 lessons)
-- Naming: L[course_idx][chapter_idx][lesson_idx]
-- =====================================================================
INSERT INTO lessons (id, chapter_id, title, content_md, order_index) VALUES

-- ===== ROMANIAN LANGUAGE & LITERATURE =====
-- Ch1: Grammar Foundations
('20000000-0000-0000-0001-000000000001', '10000000-0000-0000-0000-000000000001',
 'Parts of Speech & Morphology',
 E'## Parts of Speech & Morphology\n\nMorphology is the branch of linguistics that studies the internal structure of words and the rules by which words are formed. In Romanian — a Romance language — word forms change systematically based on gender, number, case, and tense.\n\n### The Eight Parts of Speech\n\n| Part of Speech | Function | Example (RO → EN) |\n|---|---|---|\n| Noun (*substantiv*) | Names persons, places, things | *carte* → book |\n| Pronoun (*pronume*) | Replaces nouns | *el* → he |\n| Adjective (*adjectiv*) | Modifies nouns | *mare* → big |\n| Verb (*verb*) | Expresses actions/states | *a citi* → to read |\n| Adverb (*adverb*) | Modifies verbs/adjectives | *repede* → quickly |\n| Preposition (*prepoziție*) | Connects elements | *pe* → on |\n| Conjunction (*conjuncție*) | Joins clauses | *și* → and |\n| Interjection (*interjecție*) | Expresses emotion | *vai!* → oh no! |\n\n### Romanian Nominal Declension\n\nRomanian has **three grammatical genders**: masculine, feminine, and neuter (behaves like masculine in singular, feminine in plural). Nouns decline in four cases:\n\n- **Nominative/Accusative** — subject and direct object\n- **Genitive/Dative** — possession and indirect object\n- **Vocative** — direct address\n\n### Verb Conjugation Overview\n\nVerbs are grouped into four conjugation classes based on their infinitive ending: *-a*, *-ea*, *-e*, *-i/-î*. Each class has distinct patterns for indicative, subjunctive, conditional, and imperative moods.\n\n> **Key insight:** Unlike French or Spanish, Romanian retains a definite article that is suffixed directly onto the noun: *om* (man) → *omul* (the man).\n\n### Practice Exercise\n\nIdentify the part of speech and grammatical case for each underlined word in:\n*"**Elevul** citește **cartea** **interesantă** cu **atenție**."*\n\n**Answer key:**\n- *Elevul* — noun, nominative\n- *cartea* — noun, accusative\n- *interesantă* — adjective, accusative feminine\n- *atenție* — noun, accusative (after preposition)',
 0),

('20000000-0000-0000-0001-000000000002', '10000000-0000-0000-0000-000000000001',
 'Syntax: Sentence Structure & Clause Types',
 E'## Syntax: Sentence Structure & Clause Types\n\nSyntax governs how words combine into phrases, clauses, and sentences. Mastery of syntax is essential for both literary analysis and persuasive writing.\n\n### Simple vs. Compound vs. Complex Sentences\n\n**Simple sentence** (*propoziție simplă*): one subject–predicate pair.\n> *Vântul bate.* — The wind blows.\n\n**Compound sentence** (*frază prin coordonare*): two independent clauses joined by a coordinating conjunction (*și, dar, sau, însă, ci*).\n> *Vântul bate, **dar** soarele strălucește.* — The wind blows, **but** the sun shines.\n\n**Complex sentence** (*frază prin subordonare*): a main clause + one or more subordinate clauses.\n> *Știu **că** va ploua.* — I know **that** it will rain.\n\n### Subordinate Clause Types\n\n| Clause | Introduced by | Answers |\n|---|---|---|\n| Subject (*subiectivă*) | *că, cine, ce* | What/who is the subject? |\n| Predicative (*predicativă*) | *că, cum, cine* | What is the predicate? |\n| Direct Object (*completivă directă*) | *că, să* | Whom/what? |\n| Causal (*cauzală*) | *deoarece, fiindcă* | Why? |\n| Final (*finală*) | *ca să, pentru ca* | For what purpose? |\n| Temporal (*temporală*) | *când, după ce* | When? |\n| Conditional (*condițională*) | *dacă, de* | Under what condition? |\n\n### Syntactic Analysis Method\n\n1. Identify the **predicate** first (the verb).\n2. Find the **subject** (who/what performs the action).\n3. Identify **complements** (direct, indirect, circumstantial).\n4. Determine whether each clause is independent or subordinate.\n5. Draw the **dependency tree** to visualize hierarchical relationships.\n\n### Common Errors to Avoid\n\n- **Dangling modifiers**: a participial phrase whose implied subject differs from the main clause subject.\n- **Agreement errors**: adjective or verb not agreeing with noun in gender, number, or case.\n- **Run-on sentences**: two independent clauses joined without punctuation or conjunction.',
 1),

('20000000-0000-0000-0001-000000000003', '10000000-0000-0000-0000-000000000001',
 'Stylistic Devices & Figures of Speech',
 E'## Stylistic Devices & Figures of Speech\n\nFigures of speech are the toolkit of literary expression. Recognizing and analyzing them is central to the Romanian Baccalaureate examination, where students must comment on a poem or prose excerpt.\n\n### Tropes (Semantic Figures)\n\n**Metaphor** (*metaforă*): an implicit comparison that identifies one thing with another.\n> *"Viața este o călătorie."* — Life is a journey.\n\n**Simile** (*comparație*): an explicit comparison using *ca* or *precum*.\n> *"Ochii ei sunt ca stelele."* — Her eyes are like stars.\n\n**Personification** (*personificare*): attributing human qualities to non-human entities.\n> *"Luna plânge printre nori."* — The moon cries through the clouds.\n\n**Metonymy** (*metonimie*): substituting the name of one thing for another closely associated thing.\n> *"A citit tot Eminescu."* — He read all of Eminescu. (= all of Eminescu''s works)\n\n**Hyperbole** (*hiperbolă*): deliberate exaggeration for emphasis.\n> *"Aștept de o veșnicie."* — I''ve been waiting for an eternity.\n\n**Irony** (*ironie*): saying the opposite of what one means.\n\n**Oxymoron** (*oximoron*): juxtaposing contradictory terms.\n> *"dulce amărăciune"* — sweet bitterness\n\n### Figures of Sound\n\n- **Alliteration**: repetition of initial consonants — *"Sus, în slăvi, stelele strălucesc"*\n- **Assonance**: repetition of vowel sounds\n- **Onomatopoeia**: words that phonetically imitate sounds — *fâșâit*, *murmur*\n\n### Figures of Construction\n\n- **Anaphora**: repetition of a word/phrase at the start of successive clauses\n- **Chiasmus**: ABBA syntactic reversal\n- **Ellipsis**: deliberate omission of words recoverable from context\n\n### How to Write a Stylistic Comment\n\n> 1. **Name** the figure.\n> 2. **Quote** the relevant text.\n> 3. **Interpret** the semantic/emotional effect.\n> 4. **Connect** to the overall theme of the work.\n\nExample: *"The metaphor ''inima de piatră'' (heart of stone) conveys emotional detachment and moral indifference, reinforcing the character''s role as the antagonist."*',
 2),

-- Ch2: Classical Literature
('20000000-0000-0000-0001-000000000004', '10000000-0000-0000-0000-000000000002',
 'Mihai Eminescu: Romania''s National Poet',
 E'## Mihai Eminescu: Romania''s National Poet\n\nMihai Eminescu (1850–1889) is considered the greatest Romanian poet and one of the most significant voices of European Romanticism. His work synthesizes German idealism (especially Kant and Schopenhauer), Sanskrit philosophy, Romanian folklore, and personal lyricism.\n\n### Biographical Context\n\nEminescu was born in Ipotești, Moldavia. He studied in Vienna and Berlin, absorbing Romantic and Idealist philosophy. He worked as a journalist and librarian, dying young at 39. His manuscripts — over 14,000 pages — were edited posthumously and continue to be studied.\n\n### Major Works\n\n| Work | Year | Theme |\n|---|---|---|\n| *Luceafărul* (The Evening Star) | 1883 | The alienation of genius; ideal vs. earthly love |\n| *Floare albastră* (The Blue Flower) | 1873 | Romantic nostalgia; nature vs. civilization |\n| *Odă (în metru antic)* | 1883 | Stoic acceptance of fate; classical meter |\n| *Scrisoarea I* | 1881 | Cosmogony; the vanity of human ambition |\n| *Sara pe deal* | 1876 | Pastoral idyll; synesthetic imagery |\n\n### *Luceafărul* — Close Reading\n\n*Luceafărul* (98 stanzas) narrates the story of a celestial being (the Evening Star) who twice descends to Earth out of love for a princess, only to be rejected for a mortal boy. He then petitions the Demiurge for release from immortality, which is refused.\n\n**Themes:**\n- **Incompatibility of genius and ordinary humanity**: The star cannot become mortal; the princess cannot transcend her mortality.\n- **Schopenhauerian pessimism**: Desire leads to suffering; transcendence lies in renunciation.\n- **Myth and folklore**: Draws on Romanian fairy-tale motifs (*Zburătorul*, the night-spirit).\n\n**Form:** Alternating 8-/7-syllable lines with ABAB rhyme scheme — a classical Romanian ballad structure.\n\n### Eminescu''s Stylistic Signature\n\n- Cosmic imagery (stars, sea, sky, eternity)\n- Neologisms blended with archaic Romanian\n- Philosophical meditation within lyric form\n- Oxymorons expressing existential tension (*"nemuritor și rece"* — immortal and cold)',
 0),

('20000000-0000-0000-0001-000000000005', '10000000-0000-0000-0000-000000000002',
 'Ion Creangă & the Oral Tradition',
 E'## Ion Creangă & the Oral Tradition\n\nIon Creangă (1837–1889) represents the oral and popular strand of 19th-century Romanian literature. His work is characterized by authentic Moldavian dialect, humor, and a vivid recreation of peasant life.\n\n### *Amintiri din copilărie* (Memories of Childhood)\n\nThis autobiographical prose work recounts episodes from Creangă''s childhood in Humulești. It is celebrated for:\n\n- **Authentic regional voice**: Moldavian sayings, idioms, and proverbs embedded naturally in the narrative.\n- **Humor and irony**: The child narrator''s perspective creates comic distance from adult absurdity.\n- **Sensory richness**: Food, sounds, textures of village life are evoked with precision.\n\n### Narrative Technique\n\nCreangă employs a **first-person retrospective narrator** who oscillates between naive child perspective and knowing adult commentary. This dual focalization creates dramatic irony — the reader understands more than the child narrator pretends to.\n\n### *Harap-Alb* — The Fairy Tale as Social Text\n\nThis fairy tale (*basm cult*) stages the moral education of a prince who must earn his nobility through trials. Key features:\n\n- Helpers represent folk virtues (loyalty, ingenuity, strength)\n- The villain (Spânul) embodies trickery and usurpation of identity\n- The ending restores moral order through community judgment\n\n### Creangă vs. Eminescu — A Comparison\n\n| | Eminescu | Creangă |\n|---|---|---|\n| Register | Elevated, philosophical | Colloquial, popular |\n| Themes | Cosmic, existential | Earthly, social, moral |\n| Form | Lyric poetry | Prose, fairy tales, memoirs |\n| Language | Neologisms + archaisms | Regional dialect |\n| Influence | Romanticism, Idealism | Oral folk tradition |',
 1),

('20000000-0000-0000-0001-000000000006', '10000000-0000-0000-0000-000000000002',
 'Ioan Slavici & Psychological Realism',
 E'## Ioan Slavici & Psychological Realism\n\nIoan Slavici (1848–1925) is a pivotal figure in the transition from Romanticism to Realism in Romanian literature. His fiction explores the moral and psychological consequences of greed, ambition, and social transgression in Transylvanian village communities.\n\n### *Mara* — Novel Summary\n\n*Mara* (1906) follows a strong-willed widow who sacrifices her children''s happiness in pursuit of wealth and social standing. The novel is remarkable for its psychological depth and its critique of the emerging bourgeoisie in 19th-century Transylvania.\n\n### *Moara cu noroc* (The Lucky Mill) — Close Reading\n\nThis novella is Slavici''s masterpiece and one of the most analyzed texts in the Romanian curriculum.\n\n**Plot:** Ghiță, a poor man, rents a roadside inn ("the lucky mill") and achieves quick prosperity by entering a corrupt arrangement with the outlaw Lică Sămădăul. His moral degradation destroys his family.\n\n**Themes:**\n- **The corrupting power of money**: Ghiță''s initial integrity erodes as greed overrides his conscience.\n- **Moral determinism**: Slavici suggests that crossing certain ethical lines triggers irreversible consequences.\n- **Gender and agency**: Ana (Ghiță''s wife) observes her husband''s corruption with lucid anguish — she is both victim and moral judge.\n\n**Narrative structure:** Linear chronology with an omniscient narrator who occasionally uses free indirect discourse to access characters'' inner states.\n\n**Opening motif:** The grandmother''s warning — *"omul să fie mulțumit cu sărăcia sa"* (a man should be content with his poverty) — functions as a tragic prologue, establishing the moral framework the protagonist will violate.\n\n### Slavici''s Realist Technique\n\n- Detailed social milieu (inns, markets, seasonal labor)\n- Dialogue that reveals character indirectly\n- Absence of authorial moralizing — the consequences speak for themselves\n- Regional Transylvanian color (German and Hungarian influences on vocabulary)',
 2),

-- Ch3: Modern & Contemporary Literature
('20000000-0000-0000-0001-000000000007', '10000000-0000-0000-0000-000000000003',
 'Modernism in Romanian Poetry: Arghezi, Blaga, Bacovia',
 E'## Modernism in Romanian Poetry: Arghezi, Blaga, Bacovia\n\nThe early 20th century saw a radical transformation of Romanian poetry under the influence of French Symbolism, German Expressionism, and domestic philosophical traditions.\n\n### Tudor Arghezi (1880–1967)\n\nArghezi''s poetry is defined by **aesthetic paradox**: he celebrates the beautiful within the ugly (*estetica urâtului*), the sacred within the profane.\n\n**Key works:** *Testament*, *Flori de mucigai* (Mold Flowers)\n\n*Testament* opens with the famous lines establishing poetry as a form of inheritance — transforming suffering and poverty into art that outlasts its creator.\n\n**Style:** Invented neologisms, raw physiological imagery, theological questioning, and craftsmanship (*meșteșug*) as a form of prayer.\n\n### Lucian Blaga (1895–1961)\n\nBlaga was both poet and philosopher. His poetry enacts his philosophy of the **Luciferic unconscious** — the idea that human creativity consists in deepening, not resolving, the mysteries of existence.\n\n**Key works:** *Eu nu strivesc corola de minuni a lumii* (I Do Not Crush the World''s Wonder-Wreath), *Izvorul nopții*\n\n**Style:** Mythological imagery, cosmic scope, philosophical density, village archetypes.\n\n### George Bacovia (1881–1957)\n\nBacovia is Romania''s most distinctly **Symbolist** poet. His verse is saturated with decay, monotony, provincial ennui, and synesthetic imagery.\n\n**Key works:** *Plumb* (Lead), *Lacustră* (Lacustrian), *Decor*\n\n*Plumb* (1916): the image of lead coffins in a leaden autumn enacts a synesthetic collapse — visual, tactile, and emotional lead-heaviness merge into existential suffocation.\n\n**Style:** Color symbolism (violet, gray, black), sound imagery (rain, bells), repetition as psychological entrapment.\n\n### Comparative Framework\n\n| | Arghezi | Blaga | Bacovia |\n|---|---|---|---|\n| Current | Modernism | Expressionism | Symbolism |\n| Core tension | Sacred / profane | Mystery / knowledge | Life / death |\n| Dominant image | Workshop, mud, wound | Village, cosmos, night | Rain, lead, provincial town |\n| Tone | Provocative | Solemn | Melancholic |',
 0),

('20000000-0000-0000-0001-000000000008', '10000000-0000-0000-0000-000000000003',
 'Interwar Prose: Camil Petrescu & the Authentic Novel',
 E'## Interwar Prose: Camil Petrescu & the Authentic Novel\n\nCamil Petrescu (1894–1957) is the central figure of **Romanian interwar modernist prose**. Influenced by Marcel Proust, Henri Bergson, and Edmund Husserl, he argued that the only authentic novel is one narrated by a first-person consciousness experiencing time as duration, not as external chronology.\n\n### *Ultima noapte de dragoste, întâia noapte de război*\n*(Last Night of Love, First Night of War)* — 1930\n\n**Narrator:** Ștefan Gheorghidiu, an intellectual who becomes paralyzed by jealousy over his wife Ela, then is mobilized in WWI.\n\n**Structure:** The novel has two narrative arcs:\n1. **Love as war**: The obsessive, deconstructive analysis of romantic jealousy.\n2. **War as clarity**: The brutality of the front paradoxically liberates Gheorghidiu from his private obsession.\n\n**Proustian technique:** Time is non-linear; memories intrude on present action. The narrator''s consciousness is the organizing principle — not external events.\n\n**Authenticity thesis:** Petrescu believed characters must think at the level of philosophical ideas, not just feel. Gheorghidiu quotes Kant and debates epistemology even during artillery bombardments.\n\n### *Patul lui Procust* (Procrustes'' Bed) — 1933\n\nA polyphonic novel using letters, diaries, and an authorial frame — anticipating postmodern techniques. It dissects the impossibility of knowing another person''s inner truth.\n\n### Petrescu''s Legacy\n\n- Established **subjectivism and introspection** as legitimate narrative modes in Romanian fiction\n- Critiqued the **objective, omniscient narrator** as an epistemological lie\n- Influenced all subsequent Romanian psychological fiction',
 1),

('20000000-0000-0000-0001-000000000009', '10000000-0000-0000-0000-000000000003',
 'Postwar Literature & Censorship: Writing Under Communism',
 E'## Postwar Literature & Censorship: Writing Under Communism\n\nBetween 1947 and 1989, Romanian literature operated under Communist censorship. Writers developed complex strategies to communicate subversive meaning while nominally complying with **Socialist Realism** requirements.\n\n### Socialist Realism — The Official Doctrine (1948–1964)\n\nSocialist Realism demanded literature that:\n- Depicts a **positive hero** (worker, peasant, communist militant)\n- Shows history moving toward socialist triumph\n- Uses **clear, accessible language** accessible to the masses\n- Excludes "decadent" modernist experimentation\n\nDuring this period, major writers either conformed, fell silent, or were imprisoned (e.g., Radu Gyr, Nichifor Crainic).\n\n### The Thaw & Aesthetic Liberation (1964–1971)\n\nAfter Ceaușescu''s partial break with Moscow (1964), limited liberalization allowed:\n- Rehabilitation of interwar modernists (Arghezi, Blaga)\n- The **Oneiric School** (Leonid Dimov, Dumitru Țepeneag): surrealist-influenced prose\n- The **Generation ''60** in poetry: personal, hermetic, aesthetic\n\n### Marin Preda — *Moromeții* (1955, 1967)\n\nPreda''s masterpiece traces a peasant family''s disintegration under collectivization. Volume II (1967) is a barely veiled critique of Stalinist violence, protected by its realist surface.\n\n### Nicolae Breban, Augustin Buzura, Marin Sorescu\n\n- **Breban**: psychological novels examining power and existential crisis\n- **Buzura**: hospital and psychiatric settings as metaphors for political repression (*Orgolii*, *Refugii*)\n- **Sorescu**: theater of the absurd (*Iona*) where a man trapped in a fish''s belly allegorizes totalitarian enclosure\n\n### Aesopian Language\n\nWriters used classical myths, historical disguise, fantasy, and ambiguity to encode critical content. The **implied reader** was trained to read between the lines.',
 2),

-- Ch4: Essay Writing & Argumentation
('20000000-0000-0000-0001-000000000010', '10000000-0000-0000-0000-000000000004',
 'Structure of the Argumentative Essay',
 E'## Structure of the Argumentative Essay\n\nThe argumentative essay (*eseul argumentativ*) is the central writing task in the Romanian Baccalaureate. It requires students to formulate a thesis, marshal evidence, anticipate counterarguments, and reach a reasoned conclusion.\n\n### The Five-Paragraph Model (and Its Limits)\n\nThe classical structure:\n\n1. **Introduction** — Hook, context, thesis statement\n2. **Body paragraph 1** — First argument + evidence + commentary\n3. **Body paragraph 2** — Second argument + evidence + commentary\n4. **Counterargument paragraph** — Acknowledge and refute the opposition\n5. **Conclusion** — Restate thesis + broader significance\n\n> *Note:* The five-paragraph model is a scaffold, not a straitjacket. Complex arguments require more body paragraphs; the counterargument may be integrated rather than isolated.\n\n### The Thesis Statement\n\nA strong thesis must be:\n- **Arguable** (not a statement of fact)\n- **Specific** (not vague generalization)\n- **Significant** (worth arguing)\n\n| Weak | Strong |\n|---|---|\n| *Eminescu was a great poet.* | *Eminescu''s use of cosmic imagery in* Luceafărul *enacts a Schopenhauerian critique of desire that transcends Romantic convention.* |\n\n### Evidence Hierarchy\n\n1. **Primary textual evidence** — direct quotation from the literary work\n2. **Secondary evidence** — literary critics, historical context\n3. **Analogical evidence** — comparison with other works\n4. **Logical deduction** — inference from established premises\n\n### Transitional Logic\n\nSignpost words connect ideas and show logical relationships:\n\n- **Addition**: *furthermore, in addition, moreover*\n- **Contrast**: *however, nevertheless, on the other hand*\n- **Causation**: *therefore, consequently, as a result*\n- **Concession**: *although, even though, admittedly*\n\n### Common Errors\n\n- **Circular reasoning**: restating the thesis as its own proof\n- **Straw man**: misrepresenting the opposing view to make it easier to refute\n- **Appeal to authority without analysis**: quoting a critic without explaining why the quote is relevant\n- **Summary instead of analysis**: retelling the plot rather than interpreting its significance',
 0),

('20000000-0000-0000-0001-000000000011', '10000000-0000-0000-0000-000000000004',
 'Literary Commentary: How to Analyze a Poem',
 E'## Literary Commentary: How to Analyze a Poem\n\nThe literary commentary (*comentariul literar*) is a structured essay that interprets a specific text — typically a poem or prose excerpt — using close reading techniques.\n\n### The PEEL Method (adapted for Romanian curriculum)\n\n**P** — **Point**: state the analytical claim\n**E** — **Evidence**: quote the text precisely\n**E** — **Explain**: interpret what the quotation does in context\n**L** — **Link**: connect to the poem''s overall meaning, theme, or the author''s broader oeuvre\n\n### Step-by-Step Commentary Approach\n\n**Step 1 — First Reading:** Read the poem twice without stopping to analyze. Note your immediate emotional and sensory response.\n\n**Step 2 — Contextualization:** Identify author, period, literary current, and place the poem in the author''s trajectory.\n\n**Step 3 — Thematic Analysis:**\n- What is the central theme (love, death, nature, time, identity)?\n- Is there a secondary or opposing theme?\n- How do the themes relate to the literary current?\n\n**Step 4 — Structural Analysis:**\n- How many stanzas? What shape? Uniform or irregular?\n- Is there a volta (turn in argument/tone)? Where?\n- How does the structure reinforce the meaning?\n\n**Step 5 — Prosodic Analysis:**\n- Meter (number of syllables per line)\n- Rhyme scheme (ABAB, AABB, free verse)\n- Rhythm — regular cadence creates harmony; irregular creates tension\n\n**Step 6 — Stylistic Analysis:**\n- Identify and interpret 3–5 figures of speech\n- Analyze lexical register (elevated, colloquial, archaic, neologistic)\n- Note recurring sounds (alliteration, assonance)\n\n**Step 7 — Synthesis:** Write a paragraph unifying all observations into an interpretive argument about the poem''s significance.\n\n### Model Commentary Opening\n\n> *"Scris în 1883 și publicat în ''Convorbiri literare'', ''Luceafărul'' reprezintă sinteza lirismului eminescian: o meditație cosmică asupra condiției geniului, desfășurată prin registrul basmului popular și al filosofiei schopenhaueriene."*',
 1),

('20000000-0000-0000-0001-000000000012', '10000000-0000-0000-0000-000000000004',
 'Baccalaureate Exam: Writing Strategies & Time Management',
 E'## Baccalaureate Exam: Writing Strategies & Time Management\n\nThe Romanian Baccalaureate (*Bacalaureat*) Language & Literature paper consists of three sections tested over 3 hours. Strategic preparation is as important as content knowledge.\n\n### Exam Structure (Written Paper)\n\n| Section | Task | Points |\n|---|---|---|\n| **A** | Comprehension & language questions on an unseen text | 30 |\n| **B** | Functional writing (letter, review, etc.) | 20 |\n| **C** | Extended literary essay (character analysis or thematic essay on a studied work) | 50 |\n\n**Total: 100 points (converted to grade 1–10)**\n\n### Time Allocation Strategy\n\n| Time Block | Activity |\n|---|---|\n| 0–5 min | Read entire paper; plan approach |\n| 5–25 min | Complete Section A (language questions — don''t overwrite) |\n| 25–40 min | Complete Section B (functional writing — follow format strictly) |\n| 40–45 min | Plan Section C essay (thesis + 3 arguments in bullet notes) |\n| 45–150 min | Write Section C essay (aim for 600–800 words minimum) |\n| 150–180 min | Review, correct, improve transitions |\n\n### Section C Essay: Rapid Planning Template\n\n```\nTHESIS: [Literary work] demonstrates [claim] through [technique/theme].\n\nARG 1: [Theme/character/technique]\n  - Evidence: "[exact quote]"\n  - Commentary: This shows...\n\nARG 2: [Theme/character/technique]\n  - Evidence: "[exact quote]"\n  - Commentary: This reveals...\n\nCOUNTERARG: One could argue... However...\n\nCONCLUSION: [Thesis restated + significance]\n```\n\n### Common Mistakes That Cost Points\n\n1. **Plot summary instead of analysis**: Examiners want interpretation, not retelling.\n2. **Unsupported claims**: Every analytical assertion needs textual evidence.\n3. **Ignoring formal requirements**: Section B tasks (e.g., formal letters) have strict format rules — greeting, paragraphs, sign-off.\n4. **Poor handwriting/organization**: Examiners read hundreds of papers; clarity is rewarded.\n5. **Weak conclusion**: Don''t just repeat the introduction — synthesize and add significance.',
 2),

-- ===== MATHEMATICS =====
-- Ch1: Algebra & Equations
('20000000-0000-0000-0002-000000000001', '10000000-0000-0000-0000-000000000010',
 'Sets, Relations & Functions — Foundations',
 E'## Sets, Relations & Functions — Foundations\n\n### Set Theory Basics\n\nA **set** is an unordered collection of distinct objects. Standard notation:\n- $A = \\{1, 2, 3\\}$ — roster notation\n- $B = \\{x \\in \\mathbb{R} \\mid x > 0\\}$ — set-builder notation\n\n**Key number sets:**\n- $\\mathbb{N}$ — Natural numbers $\\{0, 1, 2, 3, ...\\}$\n- $\\mathbb{Z}$ — Integers $\\{..., -2, -1, 0, 1, 2, ...\\}$\n- $\\mathbb{Q}$ — Rational numbers (fractions)\n- $\\mathbb{R}$ — Real numbers (including irrationals like $\\sqrt{2}$, $\\pi$)\n- $\\mathbb{C}$ — Complex numbers\n\n**Set operations:**\n- Union: $A \\cup B$ — elements in A or B (or both)\n- Intersection: $A \\cap B$ — elements in both A and B\n- Difference: $A \\setminus B$ — elements in A but not B\n- Complement: $\\bar{A}$ — elements not in A (relative to a universal set)\n\n### Relations\n\nA **relation** $R$ from set $A$ to set $B$ is a subset of the Cartesian product $A \\times B$.\n\nA relation on a single set can be:\n- **Reflexive**: $aRa$ for all $a$\n- **Symmetric**: $aRb \\Rightarrow bRa$\n- **Transitive**: $aRb$ and $bRc \\Rightarrow aRc$\n- **Antisymmetric**: $aRb$ and $bRa \\Rightarrow a = b$\n\nAn **equivalence relation** is reflexive + symmetric + transitive.\n\n### Functions\n\nA **function** $f: A \\to B$ assigns exactly one element of $B$ to each element of $A$.\n\n| Property | Definition |\n|---|---|\n| **Injective** (one-to-one) | $f(x_1) = f(x_2) \\Rightarrow x_1 = x_2$ |\n| **Surjective** (onto) | $\\forall y \\in B, \\exists x \\in A: f(x) = y$ |\n| **Bijective** | Both injective and surjective |\n\n### Composition & Inverse\n\nIf $f: A \\to B$ and $g: B \\to C$, then the **composition** $(g \\circ f)(x) = g(f(x))$.\n\nA bijective function has an **inverse** $f^{-1}: B \\to A$ such that $f^{-1}(f(x)) = x$.',
 0),

('20000000-0000-0000-0002-000000000002', '10000000-0000-0000-0000-000000000010',
 'Polynomial Equations & Systems',
 E'## Polynomial Equations & Systems\n\n### Quadratic Equations\n\nThe general form is $ax^2 + bx + c = 0$ where $a \\neq 0$.\n\n**Quadratic formula:** $x = \\dfrac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}$\n\n**Discriminant** $\\Delta = b^2 - 4ac$:\n- $\\Delta > 0$: two distinct real roots\n- $\\Delta = 0$: one repeated real root\n- $\\Delta < 0$: two complex conjugate roots (no real solution)\n\n**Viète''s formulas** (sum and product of roots):\n$$x_1 + x_2 = -\\frac{b}{a}, \\quad x_1 \\cdot x_2 = \\frac{c}{a}$$\n\n### Higher-Degree Polynomials\n\n**Fundamental Theorem of Algebra:** A polynomial of degree $n$ has exactly $n$ roots in $\\mathbb{C}$ (counting multiplicity).\n\n**Rational Root Theorem:** If $p/q$ is a rational root of $a_n x^n + ... + a_0 = 0$ (with integer coefficients), then $p$ divides $a_0$ and $q$ divides $a_n$.\n\n**Factor Theorem:** $(x - r)$ is a factor of $P(x)$ if and only if $P(r) = 0$.\n\n### Systems of Equations\n\n**Linear systems** can be solved by:\n1. **Substitution** — express one variable in terms of the other\n2. **Elimination (Gaussian)** — add/subtract equations to cancel a variable\n3. **Matrix method (Cramer''s Rule)** — for 2×2 and 3×3 systems\n\n**Cramer''s Rule for 2×2:**\n$$\\begin{cases} a_1 x + b_1 y = c_1 \\\\ a_2 x + b_2 y = c_2 \\end{cases}$$\n\n$$x = \\frac{\\begin{vmatrix} c_1 & b_1 \\\\ c_2 & b_2 \\end{vmatrix}}{\\begin{vmatrix} a_1 & b_1 \\\\ a_2 & b_2 \\end{vmatrix}}, \\quad y = \\frac{\\begin{vmatrix} a_1 & c_1 \\\\ a_2 & c_2 \\end{vmatrix}}{\\begin{vmatrix} a_1 & b_1 \\\\ a_2 & b_2 \\end{vmatrix}}$$\n\n### Word Problem Strategy\n\n1. **Define variables** explicitly\n2. **Translate** each condition into an equation\n3. **Solve** the system\n4. **Verify** the solution satisfies all original conditions\n5. **Interpret** in context (units, feasibility)',
 1),

('20000000-0000-0000-0002-000000000003', '10000000-0000-0000-0000-000000000010',
 'Inequalities & Absolute Value',
 E'## Inequalities & Absolute Value\n\n### Properties of Inequalities\n\nFor real numbers $a$, $b$, $c$:\n- **Addition**: $a < b \\Rightarrow a + c < b + c$\n- **Multiplication by positive**: $a < b$ and $c > 0 \\Rightarrow ac < bc$\n- **Multiplication by negative** (**direction reverses!**): $a < b$ and $c < 0 \\Rightarrow ac > bc$\n\n### Quadratic Inequalities\n\nTo solve $ax^2 + bx + c > 0$:\n1. Find roots $x_1 \\leq x_2$ (if they exist)\n2. Determine the parabola''s opening direction (up if $a > 0$)\n3. **If $a > 0$**: solution is $(-\\infty, x_1) \\cup (x_2, +\\infty)$ for "$> 0$"; $(x_1, x_2)$ for "$< 0$"\n4. **If $a < 0$**: reverse the solution sets\n\n### Absolute Value\n\n**Definition:** $|x| = x$ if $x \\geq 0$, $|x| = -x$ if $x < 0$\n\n**Key properties:**\n- $|ab| = |a||b|$\n- $|a + b| \\leq |a| + |b|$ (**Triangle Inequality**)\n- $|x| = k \\Leftrightarrow x = k$ or $x = -k$ (for $k \\geq 0$)\n- $|x| < k \\Leftrightarrow -k < x < k$\n- $|x| > k \\Leftrightarrow x < -k$ or $x > k$\n\n### Solving Absolute Value Equations\n\n**Example:** $|2x - 3| = 5$\n\nCase 1: $2x - 3 = 5 \\Rightarrow x = 4$\nCase 2: $2x - 3 = -5 \\Rightarrow x = -1$\n\n**Solution:** $x \\in \\{-1, 4\\}$ ✓\n\n### Systems with Inequalities (Linear Programming Preview)\n\nThe **feasible region** of a linear system of inequalities is the intersection of half-planes. Graphically:\n1. Graph each boundary line\n2. Shade the correct half-plane for each inequality\n3. The feasible region is the intersection of all shaded areas\n4. For optimization, the extreme values occur at **vertices** of the feasible region',
 2),

-- Ch2: Geometry & Trigonometry
('20000000-0000-0000-0002-000000000004', '10000000-0000-0000-0000-000000000011',
 'Euclidean Geometry: Triangles & Circles',
 E'## Euclidean Geometry: Triangles & Circles\n\n### Triangle Properties\n\n**Sum of interior angles:** $\\alpha + \\beta + \\gamma = 180°$\n\n**Triangle inequality:** The sum of any two sides must exceed the third side.\n\n**Congruence criteria (SAS, ASA, SSS, AAS):** Two triangles are congruent if corresponding sides and angles are equal.\n\n**Similarity criteria (AA, SAS~, SSS~):** Two triangles are similar if their angles are equal or sides are proportional.\n\n**Area formulas:**\n$$S = \\frac{1}{2} \\cdot b \\cdot h = \\frac{1}{2} ab \\sin C = \\sqrt{s(s-a)(s-b)(s-c)}$$\nwhere $s = (a+b+c)/2$ is the semi-perimeter (Heron''s formula).\n\n### Key Triangle Lines & Centers\n\n| Line | Description | Intersection |\n|---|---|---|\n| Median | Connects vertex to midpoint of opposite side | Centroid G |\n| Altitude | Perpendicular from vertex to opposite side | Orthocenter H |\n| Perpendicular bisector | Bisects a side at 90° | Circumcenter O |\n| Angle bisector | Bisects an interior angle | Incenter I |\n\n**Euler line:** The centroid, circumcenter, and orthocenter are collinear.\n\n### Circle Theorems\n\n- **Inscribed angle theorem:** An inscribed angle is half the central angle subtending the same arc.\n- **Thales'' theorem:** An angle inscribed in a semicircle is a right angle.\n- **Power of a point:** For a point P and circle, $PA \\cdot PB = PC \\cdot PD$ for any two chords through P.\n- **Tangent-radius perpendicularity:** A tangent to a circle is perpendicular to the radius at the point of tangency.\n\n### Pythagorean Theorem & Extensions\n\n$$a^2 + b^2 = c^2 \\quad \\text{(right triangle, } c \\text{ hypotenuse)}$$\n\n**Generalization — Law of Cosines:**\n$$c^2 = a^2 + b^2 - 2ab\\cos C$$\n\n**Law of Sines:**\n$$\\frac{a}{\\sin A} = \\frac{b}{\\sin B} = \\frac{c}{\\sin C} = 2R$$\nwhere $R$ is the circumradius.',
 0),

('20000000-0000-0000-0002-000000000005', '10000000-0000-0000-0000-000000000011',
 'Trigonometric Functions & Identities',
 E'## Trigonometric Functions & Identities\n\n### Unit Circle Definition\n\nFor an angle $\\theta$ measured counterclockwise from the positive x-axis:\n$$\\cos\\theta = x, \\quad \\sin\\theta = y, \\quad \\tan\\theta = \\frac{y}{x}$$\nwhere $(x, y)$ is the point on the unit circle ($r = 1$).\n\n### Fundamental Values\n\n| $\\theta$ | $0°$ | $30°$ | $45°$ | $60°$ | $90°$ |\n|---|---|---|---|---|---|\n| $\\sin$ | 0 | $\\frac{1}{2}$ | $\\frac{\\sqrt{2}}{2}$ | $\\frac{\\sqrt{3}}{2}$ | 1 |\n| $\\cos$ | 1 | $\\frac{\\sqrt{3}}{2}$ | $\\frac{\\sqrt{2}}{2}$ | $\\frac{1}{2}$ | 0 |\n| $\\tan$ | 0 | $\\frac{1}{\\sqrt{3}}$ | 1 | $\\sqrt{3}$ | undef. |\n\n### Fundamental Identities\n\n**Pythagorean identities:**\n$$\\sin^2\\theta + \\cos^2\\theta = 1$$\n$$1 + \\tan^2\\theta = \\sec^2\\theta$$\n$$1 + \\cot^2\\theta = \\csc^2\\theta$$\n\n**Addition formulas:**\n$$\\sin(A \\pm B) = \\sin A \\cos B \\pm \\cos A \\sin B$$\n$$\\cos(A \\pm B) = \\cos A \\cos B \\mp \\sin A \\sin B$$\n$$\\tan(A \\pm B) = \\frac{\\tan A \\pm \\tan B}{1 \\mp \\tan A \\tan B}$$\n\n**Double angle formulas:**\n$$\\sin 2A = 2\\sin A \\cos A$$\n$$\\cos 2A = \\cos^2 A - \\sin^2 A = 2\\cos^2 A - 1 = 1 - 2\\sin^2 A$$\n\n### Trigonometric Equations\n\nGeneral solutions:\n- $\\sin x = k \\Rightarrow x = \\arcsin k + 2k\\pi$ or $x = \\pi - \\arcsin k + 2k\\pi$\n- $\\cos x = k \\Rightarrow x = \\pm\\arccos k + 2k\\pi$\n- $\\tan x = k \\Rightarrow x = \\arctan k + k\\pi$',
 1),

('20000000-0000-0000-0002-000000000006', '10000000-0000-0000-0000-000000000011',
 'Analytic Geometry: Lines, Circles & Conics',
 E'## Analytic Geometry: Lines, Circles & Conics\n\n### Lines in the Plane\n\nForms of a line equation:\n- **Slope-intercept**: $y = mx + b$ ($m$ = slope, $b$ = y-intercept)\n- **Point-slope**: $y - y_1 = m(x - x_1)$\n- **General form**: $ax + by + c = 0$\n- **Two-intercept form**: $\\frac{x}{a} + \\frac{y}{b} = 1$\n\n**Slope from two points:** $m = \\frac{y_2 - y_1}{x_2 - x_1}$\n\n**Parallel lines:** equal slopes ($m_1 = m_2$)\n**Perpendicular lines:** $m_1 \\cdot m_2 = -1$\n\n**Distance from point $(x_0, y_0)$ to line $ax + by + c = 0$:**\n$$d = \\frac{|ax_0 + by_0 + c|}{\\sqrt{a^2 + b^2}}$$\n\n### Circles\n\n**Standard form**: $(x - h)^2 + (y - k)^2 = r^2$ (center $(h,k)$, radius $r$)\n\n**General form**: $x^2 + y^2 + Dx + Ey + F = 0$ — complete the square to find center and radius.\n\n### Conic Sections\n\nAll conics are intersections of a plane with a double cone:\n\n| Conic | Standard equation | Key parameters |\n|---|---|---|\n| Ellipse | $\\frac{x^2}{a^2} + \\frac{y^2}{b^2} = 1$ | $a > b > 0$; foci at $(\\pm c, 0)$, $c^2 = a^2 - b^2$ |\n| Hyperbola | $\\frac{x^2}{a^2} - \\frac{y^2}{b^2} = 1$ | Asymptotes $y = \\pm \\frac{b}{a}x$; $c^2 = a^2 + b^2$ |\n| Parabola | $y^2 = 4px$ | Focus at $(p, 0)$; directrix $x = -p$ |\n\n### Applications\n\n- GPS uses **ellipse** geometry (signal time differences trace hyperbolas)\n- Satellite dish antennas are **paraboloids** (parabola of revolution)\n- Planetary orbits are **ellipses** (Kepler''s First Law)',
 2),

-- Ch3: Functions & Analysis
('20000000-0000-0000-0002-000000000007', '10000000-0000-0000-0000-000000000012',
 'Limits & Continuity',
 E'## Limits & Continuity\n\n### Intuitive Definition of Limit\n\n$\\lim_{x \\to a} f(x) = L$ means: as $x$ approaches $a$ (but never equals $a$), $f(x)$ gets arbitrarily close to $L$.\n\n**Formal (ε-δ) definition:** $\\forall \\varepsilon > 0, \\exists \\delta > 0$ such that $0 < |x - a| < \\delta \\Rightarrow |f(x) - L| < \\varepsilon$.\n\n### Limit Laws\n\nIf $\\lim_{x \\to a} f(x) = L$ and $\\lim_{x \\to a} g(x) = M$:\n- Sum: $\\lim(f + g) = L + M$\n- Product: $\\lim(fg) = LM$\n- Quotient: $\\lim(f/g) = L/M$ (if $M \\neq 0$)\n- Composition: $\\lim_{x \\to a} f(g(x)) = f(M)$ (if $f$ is continuous at $M$)\n\n### Important Limits\n\n$$\\lim_{x \\to 0} \\frac{\\sin x}{x} = 1 \\qquad \\lim_{x \\to \\infty} \\left(1 + \\frac{1}{x}\\right)^x = e \\qquad \\lim_{x \\to 0} \\frac{e^x - 1}{x} = 1$$\n\n### Indeterminate Forms\n\nForms like $\\frac{0}{0}$, $\\frac{\\infty}{\\infty}$, $0 \\cdot \\infty$, $\\infty - \\infty$, $0^0$, $1^\\infty$ require special techniques:\n\n- **Factoring and canceling** (for $0/0$ polynomial cases)\n- **L''Hôpital''s Rule**: if $\\frac{f(a)}{g(a)} = \\frac{0}{0}$ or $\\frac{\\infty}{\\infty}$, then $\\lim \\frac{f}{g} = \\lim \\frac{f''}{g''}$\n- **Rationalization** (for limits involving square roots)\n\n### Continuity\n\n$f$ is **continuous at $a$** if:\n1. $f(a)$ is defined\n2. $\\lim_{x \\to a} f(x)$ exists\n3. $\\lim_{x \\to a} f(x) = f(a)$\n\n**Types of discontinuity:**\n- **Removable**: limit exists but $\\neq f(a)$ (or $f(a)$ undefined) — "hole" in graph\n- **Jump**: left and right limits exist but differ — piecewise functions\n- **Infinite**: limit is $\\pm \\infty$ — vertical asymptote\n\n**Intermediate Value Theorem:** If $f$ is continuous on $[a, b]$ and $f(a) < 0 < f(b)$, there exists $c \\in (a, b)$ with $f(c) = 0$.',
 0),

('20000000-0000-0000-0002-000000000008', '10000000-0000-0000-0000-000000000012',
 'Differentiation: Rules & Applications',
 E'## Differentiation: Rules & Applications\n\n### The Derivative\n\n$$f''(x) = \\lim_{h \\to 0} \\frac{f(x+h) - f(x)}{h}$$\n\nGeometrically: the slope of the tangent line to the graph at $x$.\n\n### Differentiation Rules\n\n| Rule | Formula |\n|---|---|\n| Constant | $(c)'' = 0$ |\n| Power | $(x^n)'' = nx^{n-1}$ |\n| Sum | $(f + g)'' = f'' + g''$ |\n| Product | $(fg)'' = f''g + fg''$ |\n| Quotient | $(f/g)'' = \\frac{f''g - fg''}{g^2}$ |\n| Chain | $(f(g(x)))'' = f''(g(x)) \\cdot g''(x)$ |\n\n### Derivatives of Key Functions\n\n| $f(x)$ | $f''(x)$ |\n|---|---|\n| $\\sin x$ | $\\cos x$ |\n| $\\cos x$ | $-\\sin x$ |\n| $e^x$ | $e^x$ |\n| $\\ln x$ | $1/x$ |\n| $\\arctan x$ | $1/(1+x^2)$ |\n\n### Applications\n\n**Monotonicity:** $f'' > 0$ on $(a,b) \\Rightarrow f$ increasing; $f'' < 0 \\Rightarrow$ decreasing.\n\n**Local extrema:** At critical points where $f''(c) = 0$ or undefined:\n- $f''$ changes from $+$ to $-$ → local maximum\n- $f''$ changes from $-$ to $+$ → local minimum\n\n**Concavity:** $f'''' > 0 \\Rightarrow$ concave up; $f'''' < 0 \\Rightarrow$ concave down.\n**Inflection point:** where $f''''$ changes sign.\n\n**Optimization algorithm:**\n1. Find domain\n2. Compute $f''(x)$ and solve $f''(x) = 0$\n3. Evaluate $f$ at critical points and endpoints\n4. Compare values to identify global max/min',
 1),

('20000000-0000-0000-0002-000000000009', '10000000-0000-0000-0000-000000000012',
 'Integration: Techniques & Applications',
 E'## Integration: Techniques & Applications\n\n### The Integral as Area\n\nThe **definite integral** $\\int_a^b f(x)\\,dx$ represents the signed area between the graph of $f$ and the x-axis over $[a, b]$.\n\n### Fundamental Theorem of Calculus\n\n**Part I:** If $F''(x) = f(x)$, then $\\int_a^b f(x)\\,dx = F(b) - F(a)$.\n\n**Part II:** $\\frac{d}{dx}\\int_a^x f(t)\\,dt = f(x)$.\n\n### Basic Antiderivatives\n\n$$\\int x^n\\,dx = \\frac{x^{n+1}}{n+1} + C \\qquad (n \\neq -1)$$\n$$\\int \\frac{1}{x}\\,dx = \\ln|x| + C \\qquad \\int e^x\\,dx = e^x + C$$\n$$\\int \\sin x\\,dx = -\\cos x + C \\qquad \\int \\cos x\\,dx = \\sin x + C$$\n\n### Integration Techniques\n\n**Substitution (reverse chain rule):**\nSet $u = g(x)$, then $\\int f(g(x))g''(x)\\,dx = \\int f(u)\\,du$.\n\n**Integration by parts (reverse product rule):**\n$$\\int u\\,dv = uv - \\int v\\,du$$\n*Mnemonic LIATE for choosing $u$: Logarithm, Inverse trig, Algebraic, Trig, Exponential.*\n\n**Partial fractions:** Decompose a rational function before integrating.\n\n### Applications of Integration\n\n- **Area between curves:** $\\int_a^b [f(x) - g(x)]\\,dx$ when $f \\geq g$ on $[a,b]$\n- **Volume of revolution (disk method):** $\\pi \\int_a^b [f(x)]^2\\,dx$\n- **Arc length:** $\\int_a^b \\sqrt{1 + [f''(x)]^2}\\,dx$\n- **Average value of $f$ on $[a,b]$:** $\\frac{1}{b-a}\\int_a^b f(x)\\,dx$',
 2),

-- Ch4: Probability & Statistics
('20000000-0000-0000-0002-000000000010', '10000000-0000-0000-0000-000000000013',
 'Combinatorics: Permutations & Combinations',
 E'## Combinatorics: Permutations & Combinations\n\n### The Fundamental Counting Principle\n\nIf task A can be performed in $m$ ways and task B in $n$ ways (independently), then both can be performed in $m \\times n$ ways.\n\n### Permutations\n\n**Permutation of $n$ objects:** $n! = n \\cdot (n-1) \\cdot ... \\cdot 2 \\cdot 1$\n\n**Permutation of $r$ from $n$ (without repetition):**\n$$P(n, r) = \\frac{n!}{(n-r)!}$$\n\n**Permutation with repetition:** $n^r$ (choosing $r$ from $n$ with repetition allowed)\n\n**Permutations of a multiset** (some elements identical):\n$$\\frac{n!}{n_1! \\cdot n_2! \\cdots n_k!}$$\n\n### Combinations\n\n**Choosing $r$ from $n$ (order does not matter):**\n$$C(n, r) = \\binom{n}{r} = \\frac{n!}{r!(n-r)!}$$\n\n**Pascal''s identity:** $\\binom{n}{r} = \\binom{n-1}{r-1} + \\binom{n-1}{r}$\n\n**Symmetry:** $\\binom{n}{r} = \\binom{n}{n-r}$\n\n### Binomial Theorem\n\n$$(a + b)^n = \\sum_{k=0}^{n} \\binom{n}{k} a^{n-k} b^k$$\n\n### Applications\n\n- How many 5-card poker hands from a 52-card deck? $\\binom{52}{5} = 2{,}598{,}960$\n- How many ways to arrange the letters in "MISSISSIPPI"? $\\frac{11!}{4! \\cdot 4! \\cdot 2! \\cdot 1!} = 34{,}650$\n- How many 4-digit PINs with no repeated digits? $P(10,4) = 5{,}040$',
 0),

('20000000-0000-0000-0002-000000000011', '10000000-0000-0000-0000-000000000013',
 'Probability Theory',
 E'## Probability Theory\n\n### Sample Space & Events\n\n- **Sample space** $\\Omega$: the set of all possible outcomes\n- **Event** $A \\subseteq \\Omega$: a subset of outcomes\n- **Probability** $P(A) \\in [0, 1]$: $P(\\Omega) = 1$, $P(\\emptyset) = 0$\n\n### Axioms of Probability\n\n1. $P(A) \\geq 0$ for all events $A$\n2. $P(\\Omega) = 1$\n3. For mutually exclusive events: $P(A \\cup B) = P(A) + P(B)$\n\n**General addition rule:** $P(A \\cup B) = P(A) + P(B) - P(A \\cap B)$\n\n### Conditional Probability & Independence\n\n$$P(A|B) = \\frac{P(A \\cap B)}{P(B)} \\quad (P(B) > 0)$$\n\n**Independence:** $A$ and $B$ are independent iff $P(A \\cap B) = P(A) \\cdot P(B)$.\n\n### Bayes'' Theorem\n\n$$P(A|B) = \\frac{P(B|A) \\cdot P(A)}{P(B)}$$\n\nThis is foundational for: medical diagnostics, spam filters, machine learning classifiers.\n\n**Example:** A test for a disease is 99% sensitive and 95% specific. The disease affects 1% of the population. If you test positive, what is the probability you actually have the disease?\n\n$$P(\\text{disease}|\\text{positive}) = \\frac{0.99 \\times 0.01}{0.99 \\times 0.01 + 0.05 \\times 0.99} \\approx 16.7\\%$$\n\n> **Insight:** Rare diseases + imperfect tests = many false positives. This is the **base rate fallacy**.\n\n### Distributions\n\n| Distribution | Use case | Formula |\n|---|---|---|\n| Binomial $B(n,p)$ | $k$ successes in $n$ trials | $\\binom{n}{k}p^k(1-p)^{n-k}$ |\n| Geometric | Trials until first success | $P(X=k) = (1-p)^{k-1}p$ |\n| Normal $N(\\mu, \\sigma^2)$ | Continuous, symmetric | Bell curve; use z-scores |',
 1),

('20000000-0000-0000-0002-000000000012', '10000000-0000-0000-0000-000000000013',
 'Descriptive Statistics & Data Analysis',
 E'## Descriptive Statistics & Data Analysis\n\n### Measures of Central Tendency\n\n**Mean (arithmetic average):**\n$$\\bar{x} = \\frac{1}{n}\\sum_{i=1}^n x_i$$\n\n**Median:** The middle value when data is sorted. For even $n$, average the two middle values.\n\n**Mode:** The most frequently occurring value. A dataset can be unimodal, bimodal, or multimodal.\n\n### Measures of Dispersion\n\n**Range:** $\\max - \\min$\n\n**Variance:**\n$$\\sigma^2 = \\frac{1}{n}\\sum_{i=1}^n (x_i - \\bar{x})^2$$\n\n**Standard deviation:** $\\sigma = \\sqrt{\\sigma^2}$ — same units as the data.\n\n**Interquartile Range (IQR):** $Q_3 - Q_1$ — resistant to outliers.\n\n### Data Visualization\n\n| Chart type | Best for |\n|---|---|\n| Histogram | Distribution of continuous data |\n| Box plot | Five-number summary + outliers |\n| Scatter plot | Relationship between two variables |\n| Bar chart | Comparing categorical groups |\n| Pie chart | Part-to-whole (use sparingly) |\n\n### Correlation vs. Causation\n\n**Pearson correlation coefficient** $r \\in [-1, 1]$:\n- $r \\approx 1$: strong positive linear relationship\n- $r \\approx -1$: strong negative linear relationship\n- $r \\approx 0$: no linear relationship\n\n> **Critical principle:** Correlation does not imply causation. Both variables may be driven by a common lurking variable (*confounder*).\n\n**Example of confounding:** Ice cream sales and drowning rates are positively correlated — because both increase in summer (temperature is the confounder), not because ice cream causes drowning.',
 2),

-- ===== PHYSICS =====
-- Ch1: Classical Mechanics
('20000000-0000-0000-0003-000000000001', '10000000-0000-0000-0000-000000000020',
 'Kinematics: Motion in 1D and 2D',
 E'## Kinematics: Motion in 1D and 2D\n\n### Basic Quantities\n\n| Quantity | Symbol | Unit | Definition |\n|---|---|---|---|\n| Position | $x$ | m | Location relative to reference point |\n| Displacement | $\\Delta x$ | m | Change in position (vector) |\n| Velocity | $v$ | m/s | Rate of change of position |\n| Acceleration | $a$ | m/s² | Rate of change of velocity |\n\n### Uniformly Accelerated Motion (UAM)\n\nFor constant acceleration $a$ starting from $v_0$ at $t = 0$:\n\n$$v = v_0 + at$$\n$$x = x_0 + v_0 t + \\frac{1}{2}at^2$$\n$$v^2 = v_0^2 + 2a(x - x_0)$$\n$$x = x_0 + \\frac{v + v_0}{2}t$$\n\n### Free Fall\n\nNear Earth''s surface, $a = g = 9.81\\,\\text{m/s}^2$ downward.\n\n**Time to fall from height $h$:** $t = \\sqrt{\\frac{2h}{g}}$\n\n**Final velocity:** $v = \\sqrt{2gh}$\n\n### Projectile Motion\n\nIn 2D, horizontal and vertical motions are **independent**:\n\n**Horizontal** (no acceleration): $x = v_0 \\cos\\theta \\cdot t$\n\n**Vertical** (free fall): $y = v_0 \\sin\\theta \\cdot t - \\frac{1}{2}gt^2$\n\n**Range:** $R = \\frac{v_0^2 \\sin 2\\theta}{g}$ — maximum range at $\\theta = 45°$\n\n**Maximum height:** $H = \\frac{(v_0 \\sin\\theta)^2}{2g}$\n\n### Relative Motion\n\nIf object A moves with velocity $\\vec{v}_A$ and object B with $\\vec{v}_B$ relative to the ground:\n$$\\vec{v}_{A/B} = \\vec{v}_A - \\vec{v}_B$$',
 0),

('20000000-0000-0000-0003-000000000002', '10000000-0000-0000-0000-000000000020',
 'Newton''s Laws & Dynamics',
 E'## Newton''s Laws & Dynamics\n\n### Newton''s Three Laws\n\n**First Law (Inertia):** An object remains at rest or moves with constant velocity unless acted upon by a net external force.\n\n**Second Law:** $\\vec{F}_{net} = m\\vec{a}$\n\nThe net force equals mass times acceleration. This is the central equation of classical mechanics.\n\n**Third Law (Action–Reaction):** For every action force, there is an equal and opposite reaction force. These forces act on *different* objects.\n\n### Common Forces\n\n| Force | Formula | Direction |\n|---|---|---|\n| Weight | $W = mg$ | Downward |\n| Normal | $N$ | Perpendicular to surface |\n| Friction (kinetic) | $f_k = \\mu_k N$ | Opposing motion |\n| Friction (static) | $f_s \\leq \\mu_s N$ | Opposing tendency of motion |\n| Spring (Hooke''s law) | $F = -kx$ | Opposing deformation |\n| Tension | $T$ | Along string, away from object |\n\n### Free Body Diagrams (FBD)\n\nA FBD shows all forces acting on an isolated object. Steps:\n1. Identify the object of interest\n2. Draw it as a point or simple shape\n3. Draw each force as an arrow labeled with symbol and direction\n4. Apply $\\sum F_x = ma_x$ and $\\sum F_y = ma_y$\n\n### Circular Motion\n\nFor uniform circular motion (constant speed $v$ in radius $r$):\n$$a_c = \\frac{v^2}{r} \\quad \\text{(centripetal acceleration, directed inward)}$$\n$$F_c = \\frac{mv^2}{r}$$\n\n**Important:** "Centrifugal force" is a fictitious force felt in the rotating frame — it does not appear in an inertial FBD.\n\n### Newton''s Law of Gravitation\n\n$$F_g = G\\frac{m_1 m_2}{r^2}, \\quad G = 6.674 \\times 10^{-11}\\,\\text{N m}^2/\\text{kg}^2$$',
 1),

('20000000-0000-0000-0003-000000000003', '10000000-0000-0000-0000-000000000020',
 'Work, Energy & Conservation Laws',
 E'## Work, Energy & Conservation Laws\n\n### Work\n\n$$W = \\vec{F} \\cdot \\vec{d} = Fd\\cos\\theta$$\n\nWork is a scalar. Positive when force and displacement are in the same direction; negative when opposite.\n\n**Work done by gravity:** $W_g = mgh$ (positive when moving downward)\n\n**Work–Energy Theorem:** The net work done on an object equals its change in kinetic energy:\n$$W_{net} = \\Delta KE = \\frac{1}{2}mv_f^2 - \\frac{1}{2}mv_i^2$$\n\n### Kinetic & Potential Energy\n\n$$KE = \\frac{1}{2}mv^2 \\qquad PE_{gravity} = mgh \\qquad PE_{spring} = \\frac{1}{2}kx^2$$\n\n### Conservation of Mechanical Energy\n\nIf only conservative forces act (gravity, springs — not friction):\n$$E_{mechanical} = KE + PE = \\text{constant}$$\n\n$$\\frac{1}{2}mv_1^2 + mgh_1 = \\frac{1}{2}mv_2^2 + mgh_2$$\n\n### Power\n\n$$P = \\frac{W}{t} = \\vec{F} \\cdot \\vec{v}$$\n\nUnit: Watt (W) = J/s. 1 horsepower ≈ 746 W.\n\n### Momentum & Impulse\n\n$$\\vec{p} = m\\vec{v} \\qquad \\vec{J} = \\Delta\\vec{p} = \\vec{F}_{avg}\\Delta t$$\n\n**Conservation of momentum** (no external forces):\n$$\\vec{p}_{total} = \\text{constant}$$\n\n**Elastic collision:** both momentum and KE conserved.\n**Inelastic collision:** only momentum conserved; objects may stick (perfectly inelastic).',
 2),

-- ===== ALGORITHMS & DATA STRUCTURES (UNI) =====
-- Ch1: Complexity Analysis
('20000000-0000-0000-0012-000000000001', '10000000-0000-0000-0000-000000000110',
 'Big O Notation & Asymptotic Analysis',
 E'## Big O Notation & Asymptotic Analysis\n\n### Why Complexity Analysis?\n\nWhen choosing an algorithm, we care about **scalability** — how performance changes as input size $n$ grows. Big O notation captures this growth rate without implementation details.\n\n### Formal Definition\n\n$f(n) = O(g(n))$ if there exist constants $c > 0$ and $n_0$ such that:\n$$f(n) \\leq c \\cdot g(n) \\quad \\forall n \\geq n_0$$\n\nIntuitively: $g(n)$ is an **upper bound** on $f(n)$ for large $n$.\n\n### Common Complexity Classes\n\n| Class | Name | Example |\n|---|---|---|\n| $O(1)$ | Constant | Array index lookup |\n| $O(\\log n)$ | Logarithmic | Binary search |\n| $O(n)$ | Linear | Linear search |\n| $O(n \\log n)$ | Linearithmic | Merge sort, heap sort |\n| $O(n^2)$ | Quadratic | Bubble sort, insertion sort |\n| $O(2^n)$ | Exponential | Recursive Fibonacci (naive) |\n| $O(n!)$ | Factorial | Brute-force TSP |\n\n### Analyzing Code\n\n```python\n# O(1) — constant time\ndef get_first(arr):\n    return arr[0]\n\n# O(n) — single loop\ndef linear_search(arr, target):\n    for item in arr:\n        if item == target:\n            return True\n    return False\n\n# O(n^2) — nested loops\ndef bubble_sort(arr):\n    n = len(arr)\n    for i in range(n):\n        for j in range(n - i - 1):\n            if arr[j] > arr[j+1]:\n                arr[j], arr[j+1] = arr[j+1], arr[j]\n```\n\n### Best, Average, Worst Case\n\n- **Best case $\\Omega$**: minimum operations over all inputs of size $n$\n- **Average case $\\Theta$**: expected operations over random inputs\n- **Worst case $O$**: maximum operations — the standard for guarantees\n\n### Space Complexity\n\nIn addition to time, we analyze **memory usage**:\n- Iterative algorithms: typically $O(1)$ extra space\n- Recursive algorithms: $O(\\text{recursion depth})$ stack space\n- Data copying (merge sort): $O(n)$ auxiliary space',
 0),

('20000000-0000-0000-0012-000000000002', '10000000-0000-0000-0000-000000000110',
 'Recursion & Recurrence Relations',
 E'## Recursion & Recurrence Relations\n\n### What Is Recursion?\n\nA function is **recursive** when it calls itself with a smaller/simpler input. Every recursive solution has:\n1. **Base case**: the simplest instance, solved directly\n2. **Recursive case**: reduces the problem to a smaller instance\n\n```python\ndef factorial(n):\n    if n == 0:        # base case\n        return 1\n    return n * factorial(n - 1)  # recursive case\n```\n\n### Recursive Thinking Framework\n\nAsk: *"If I had the answer for n-1 (or half the problem), how would I solve for n?"*\n\n### Classic Recursive Problems\n\n**Fibonacci:**\n```python\ndef fib(n):\n    if n <= 1: return n\n    return fib(n-1) + fib(n-2)  # O(2^n) — exponentially slow!\n```\n\n**Memoized Fibonacci:** Store computed results → $O(n)$ time, $O(n)$ space.\n\n**Tower of Hanoi:** Move $n$ disks from peg A to peg C using B.\n- Recurrence: $T(n) = 2T(n-1) + 1 \\Rightarrow T(n) = 2^n - 1$\n\n### Recurrence Relations & the Master Theorem\n\nFor divide-and-conquer algorithms of the form $T(n) = aT(n/b) + f(n)$:\n\n| Condition | Result |\n|---|---|\n| $f(n) = O(n^{\\log_b a - \\varepsilon})$ | $T(n) = \\Theta(n^{\\log_b a})$ |\n| $f(n) = \\Theta(n^{\\log_b a})$ | $T(n) = \\Theta(n^{\\log_b a} \\log n)$ |\n| $f(n) = \\Omega(n^{\\log_b a + \\varepsilon})$ | $T(n) = \\Theta(f(n))$ |\n\n**Merge sort example:** $a=2, b=2, f(n)=n \\Rightarrow T(n) = \\Theta(n \\log n)$\n\n### Stack Overflow & Tail Recursion\n\nDeep recursion can exhaust the call stack. **Tail recursion** (recursive call is the last operation) can be optimized by compilers into iteration, avoiding stack growth.',
 1),

('20000000-0000-0000-0012-000000000003', '10000000-0000-0000-0000-000000000110',
 'Algorithm Design Paradigms',
 E'## Algorithm Design Paradigms\n\n### 1. Brute Force\n\nTry all possible solutions. Simple to implement but typically $O(n^2)$, $O(2^n)$, or $O(n!)$.\n\n**When to use:** Only for small inputs or when no better approach exists.\n\n### 2. Divide & Conquer\n\n1. **Divide** the problem into smaller subproblems\n2. **Conquer** each subproblem recursively\n3. **Combine** the results\n\n**Examples:** Merge sort, quicksort, binary search, Karatsuba multiplication.\n\n### 3. Greedy Algorithms\n\nMake the **locally optimal** choice at each step, hoping for a globally optimal result.\n\n**Works when:** The problem has the **greedy choice property** and **optimal substructure**.\n\n**Examples:** Dijkstra''s shortest path, Huffman coding, activity selection, Kruskal''s MST.\n\n**Does NOT work for:** 0/1 knapsack (greedy can fail; use DP).\n\n### 4. Dynamic Programming (DP)\n\nBreak the problem into overlapping subproblems; store results to avoid recomputation.\n\n**Requirements:** Optimal substructure + overlapping subproblems.\n\n**Approaches:**\n- **Top-down (memoization):** recursive + cache\n- **Bottom-up (tabulation):** fill a table iteratively\n\n**Classic problems:**\n- Fibonacci (DP: $O(n)$ vs naive $O(2^n)$)\n- Longest Common Subsequence: $O(nm)$\n- 0/1 Knapsack: $O(nW)$\n- Coin change: $O(n \\cdot \\text{amount})$\n\n### 5. Backtracking\n\nBuild a solution incrementally, abandoning (backtracking) as soon as a constraint is violated.\n\n**Examples:** N-Queens, Sudoku solver, graph coloring.\n\n```python\ndef solve_nqueens(n, row=0, cols=set(), diag1=set(), diag2=set()):\n    if row == n:\n        return 1  # found a valid placement\n    count = 0\n    for col in range(n):\n        if col in cols or (row-col) in diag1 or (row+col) in diag2:\n            continue\n        cols.add(col); diag1.add(row-col); diag2.add(row+col)\n        count += solve_nqueens(n, row+1, cols, diag1, diag2)\n        cols.remove(col); diag1.remove(row-col); diag2.remove(row+col)\n    return count\n```',
 2),

-- Ch2: Linear Data Structures
('20000000-0000-0000-0012-000000000004', '10000000-0000-0000-0000-000000000111',
 'Arrays, Strings & Dynamic Arrays',
 E'## Arrays, Strings & Dynamic Arrays\n\n### Static Arrays\n\nAn array stores elements of the **same type** in contiguous memory. Access by index is $O(1)$.\n\n**Memory layout:** For an array starting at address `base` with element size `s`:\n$$\\text{address}(i) = \\text{base} + i \\times s$$\n\n**Operations complexity:**\n| Operation | Time |\n|---|---|\n| Access by index | $O(1)$ |\n| Search (unsorted) | $O(n)$ |\n| Search (sorted) | $O(\\log n)$ |\n| Insert at end | $O(1)$ |\n| Insert at position $i$ | $O(n)$ — must shift elements |\n| Delete | $O(n)$ — must shift |\n\n### Dynamic Arrays (ArrayList / Python list)\n\nWhen the array is full, allocate a new array with **double the capacity** and copy elements.\n\n**Amortized analysis:** Although occasional copies cost $O(n)$, the amortized cost per insertion is $O(1)$ (because copying occurs geometrically less frequently).\n\n**Python list implementation uses dynamic arrays.** Java: `ArrayList<T>`.\n\n### Strings\n\nStrings are sequences of characters. In most languages, they are **immutable** — concatenation creates a new string.\n\n**Efficient concatenation:** Use a `StringBuilder` (Java) or `"".join(list)` (Python) — concatenating $n$ strings naively is $O(n^2)$.\n\n**Classic string algorithms:**\n- Palindrome check: two-pointer, $O(n)$\n- Anagram check: frequency counter, $O(n)$\n- Substring search (Knuth-Morris-Pratt): $O(n + m)$\n\n### Two-Pointer Technique\n\n```python\ndef is_palindrome(s):\n    left, right = 0, len(s) - 1\n    while left < right:\n        if s[left] != s[right]:\n            return False\n        left += 1\n        right -= 1\n    return True  # O(n) time, O(1) space\n```',
 0),

('20000000-0000-0000-0012-000000000005', '10000000-0000-0000-0000-000000000111',
 'Linked Lists: Singly, Doubly & Circular',
 E'## Linked Lists: Singly, Doubly & Circular\n\n### Node Structure\n\n```python\nclass Node:\n    def __init__(self, data):\n        self.data = data\n        self.next = None  # pointer to next node\n```\n\n### Singly Linked List\n\nEach node points to the next. The last node''s `next` is `None`.\n\n**Operations:**\n| Operation | Time | Notes |\n|---|---|---|\n| Access by index | $O(n)$ | Must traverse from head |\n| Search | $O(n)$ | |\n| Insert at head | $O(1)$ | |\n| Insert at tail (with tail ptr) | $O(1)$ | |\n| Insert at position | $O(n)$ | Must find previous node |\n| Delete head | $O(1)$ | |\n| Delete by value | $O(n)$ | |\n\n### Doubly Linked List\n\nEach node has both `next` and `prev` pointers. Enables backward traversal and $O(1)$ deletion given a node reference.\n\n### Circular Linked List\n\nThe tail''s `next` points back to the head. Useful for round-robin scheduling.\n\n### Classic Problems\n\n**Reverse a linked list (iterative):**\n```python\ndef reverse(head):\n    prev, curr = None, head\n    while curr:\n        nxt = curr.next\n        curr.next = prev\n        prev, curr = curr, nxt\n    return prev  # new head\n```\n\n**Detect a cycle (Floyd''s algorithm — tortoise & hare):**\n```python\ndef has_cycle(head):\n    slow = fast = head\n    while fast and fast.next:\n        slow = slow.next\n        fast = fast.next.next\n        if slow == fast:\n            return True\n    return False\n```\n\n**Merge two sorted lists:** $O(m + n)$ time, $O(1)$ extra space (iterative).\n\n### Linked List vs Array: When to Choose\n\n| | Array | Linked List |\n|---|---|---|\n| Random access | $O(1)$ ✅ | $O(n)$ ❌ |\n| Insert/delete at head | $O(n)$ ❌ | $O(1)$ ✅ |\n| Memory overhead | Low | High (pointers) |\n| Cache performance | Excellent | Poor (non-contiguous) |',
 1),

('20000000-0000-0000-0012-000000000006', '10000000-0000-0000-0000-000000000111',
 'Stacks, Queues & Deques',
 E'## Stacks, Queues & Deques\n\n### Stack (LIFO — Last In, First Out)\n\n**Operations:** `push(x)`, `pop()`, `peek()`, `is_empty()` — all $O(1)$.\n\n**Implementation:** Dynamic array (simplest) or linked list.\n\n**Applications:**\n- **Function call stack** — the OS uses a stack for nested function calls\n- **Undo/redo** in text editors\n- **Balanced parentheses** check\n- **Depth-first search (DFS)**\n- **Expression evaluation** (infix → postfix)\n\n```python\n# Balanced parentheses\ndef is_balanced(s):\n    stack = []\n    pairs = {'')'': ''('', ''}'': ''{'', '']'': ''[''}\n    for c in s:\n        if c in ''([{'': stack.append(c)\n        elif c in pairs:\n            if not stack or stack[-1] != pairs[c]:\n                return False\n            stack.pop()\n    return len(stack) == 0\n```\n\n### Queue (FIFO — First In, First Out)\n\n**Operations:** `enqueue(x)`, `dequeue()`, `front()` — all $O(1)$ with a deque.\n\n**Implementations:**\n- Circular array (fixed-size queue)\n- Linked list\n- Python: `collections.deque` (doubly-ended, $O(1)$ both ends)\n\n**Applications:**\n- **BFS (breadth-first search)**\n- Task scheduling / job queues\n- Print spooling\n- Buffer for data streams\n\n### Priority Queue (Heap)\n\nElements are dequeued in order of **priority** (highest first).\n\n- **Min-heap:** `dequeue()` returns the minimum element — $O(\\log n)$\n- **Max-heap:** `dequeue()` returns the maximum — $O(\\log n)$\n\n**Applications:** Dijkstra, A*, top-k elements, event simulation.\n\n### Deque (Double-Ended Queue)\n\nSupports insertion and deletion at both ends in $O(1)$. Can simulate both stack and queue.',
 2),

-- Ch3: Trees & Graphs
('20000000-0000-0000-0012-000000000007', '10000000-0000-0000-0000-000000000112',
 'Binary Trees & Binary Search Trees',
 E'## Binary Trees & Binary Search Trees\n\n### Tree Terminology\n\n- **Node**: element with data and pointers to children\n- **Root**: the single node with no parent\n- **Leaf**: node with no children\n- **Height**: length of the longest root-to-leaf path\n- **Depth**: length of the path from root to a node\n- **Complete binary tree**: all levels fully filled except possibly the last\n- **Perfect binary tree**: all leaves at the same depth\n\n### Binary Search Tree (BST) Property\n\nFor every node $N$:\n- All values in $N$''s **left subtree** are **less than** $N$.value\n- All values in $N$''s **right subtree** are **greater than** $N$.value\n\n**Operations (average case):**\n| Operation | Average | Worst (degenerate) |\n|---|---|---|\n| Search | $O(\\log n)$ | $O(n)$ |\n| Insert | $O(\\log n)$ | $O(n)$ |\n| Delete | $O(\\log n)$ | $O(n)$ |\n\n### BST Traversals\n\n```python\ndef inorder(node):   # LEFT → ROOT → RIGHT → sorted order!\n    if node:\n        inorder(node.left)\n        print(node.val)\n        inorder(node.right)\n\ndef preorder(node):  # ROOT → LEFT → RIGHT → copy a tree\n    if node:\n        print(node.val)\n        preorder(node.left)\n        preorder(node.right)\n\ndef postorder(node): # LEFT → RIGHT → ROOT → delete a tree\n    if node:\n        postorder(node.left)\n        postorder(node.right)\n        print(node.val)\n```\n\n### Balanced BSTs\n\nA **degenerate** BST (sorted insertions) degrades to $O(n)$ operations. Self-balancing trees maintain $O(\\log n)$:\n- **AVL Tree**: strict height balance ($|h_L - h_R| \\leq 1$), rotations on insert/delete\n- **Red-Black Tree**: relaxed balance, used in Java `TreeMap` and Linux kernel\n- **B-Tree**: generalized M-ary tree, used in databases and file systems',
 0),

('20000000-0000-0000-0012-000000000008', '10000000-0000-0000-0000-000000000112',
 'Graph Representations & Traversal',
 E'## Graph Representations & Traversal\n\n### Graph Fundamentals\n\nA **graph** $G = (V, E)$ consists of:\n- $V$: set of vertices (nodes)\n- $E$: set of edges (connections between vertices)\n\n**Directed graph (digraph):** edges have direction.\n**Undirected graph:** edges have no direction.\n**Weighted graph:** edges have numerical weights.\n\n### Graph Representations\n\n**Adjacency Matrix:** $n \\times n$ matrix where $M[i][j] = 1$ if edge $(i,j)$ exists.\n- Space: $O(n^2)$\n- Check if edge exists: $O(1)$\n- Iterate all neighbors: $O(n)$\n- Best for: dense graphs\n\n**Adjacency List:** For each vertex, maintain a list of its neighbors.\n- Space: $O(n + m)$ where $m = |E|$\n- Iterate all neighbors: $O(\\deg(v))$\n- Best for: sparse graphs (typical in practice)\n\n### BFS (Breadth-First Search)\n\n```python\nfrom collections import deque\n\ndef bfs(graph, start):\n    visited = set([start])\n    queue = deque([start])\n    while queue:\n        node = queue.popleft()\n        print(node)\n        for neighbor in graph[node]:\n            if neighbor not in visited:\n                visited.add(neighbor)\n                queue.append(neighbor)\n```\n\n**Time:** $O(V + E)$ — **Space:** $O(V)$ (queue + visited set)\n\n**Use cases:** Shortest path (unweighted), level-order traversal, connected components.\n\n### DFS (Depth-First Search)\n\n```python\ndef dfs(graph, node, visited=set()):\n    visited.add(node)\n    print(node)\n    for neighbor in graph[node]:\n        if neighbor not in visited:\n            dfs(graph, neighbor, visited)\n```\n\n**Time:** $O(V + E)$ — **Space:** $O(V)$ (recursion stack)\n\n**Use cases:** Topological sort, cycle detection, maze solving, connected components, SCCs.',
 1),

('20000000-0000-0000-0012-000000000009', '10000000-0000-0000-0000-000000000112',
 'Shortest Paths & Minimum Spanning Trees',
 E'## Shortest Paths & Minimum Spanning Trees\n\n### Shortest Path Algorithms\n\n#### Dijkstra''s Algorithm (non-negative weights)\n\n**Idea:** Greedily extend the shortest known path. Use a min-heap (priority queue) for efficiency.\n\n```\nInitialize dist[source] = 0, all others = ∞\nPriority queue: (0, source)\nWhile queue not empty:\n    (d, u) = pop minimum\n    For each neighbor v of u with edge weight w:\n        if d + w < dist[v]:\n            dist[v] = d + w\n            push (dist[v], v) to queue\n```\n\n**Complexity:** $O((V + E) \\log V)$ with binary heap.\n\n**Limitation:** Fails with negative edge weights.\n\n#### Bellman-Ford (negative weights allowed)\n\nRelax all edges $V-1$ times. Detects negative cycles in one extra pass.\n**Complexity:** $O(VE)$ — slower but more general.\n\n#### Floyd-Warshall (all-pairs shortest paths)\n\n$O(V^3)$ DP algorithm for all-pairs shortest paths. Works with negative weights (not negative cycles).\n\n### Minimum Spanning Tree (MST)\n\nA **spanning tree** of an undirected connected graph is a subgraph that is a tree and includes all vertices. An **MST** minimizes the total edge weight.\n\n#### Kruskal''s Algorithm\n\n1. Sort all edges by weight\n2. Iterate through edges; add an edge if it doesn''t create a cycle (use Union-Find)\n3. Stop when $V-1$ edges are added\n\n**Complexity:** $O(E \\log E)$\n\n#### Prim''s Algorithm\n\nGreedily grow the MST from a starting vertex, always adding the cheapest edge connecting the tree to a non-tree vertex.\n\n**Complexity:** $O((V + E) \\log V)$ with priority queue.\n\n### Union-Find (Disjoint Set Union)\n\nEfficient data structure for tracking connected components. Two operations:\n- `find(x)`: which set does $x$ belong to?\n- `union(x, y)`: merge the sets of $x$ and $y$\n\nWith **path compression** + **union by rank**: nearly $O(1)$ amortized per operation.',
 2),

-- Ch4: Sorting, Searching & Hashing
('20000000-0000-0000-0012-000000000010', '10000000-0000-0000-0000-000000000113',
 'Sorting Algorithms: Comparison-Based',
 E'## Sorting Algorithms: Comparison-Based\n\n### Why Sorting Matters\n\nSorting is a fundamental operation. Many algorithms (binary search, merge step, certain DP) assume sorted input. The $\\Omega(n \\log n)$ lower bound applies to all comparison-based sorting.\n\n### Algorithm Comparison\n\n| Algorithm | Best | Average | Worst | Space | Stable? |\n|---|---|---|---|---|---|\n| Bubble sort | $O(n)$ | $O(n^2)$ | $O(n^2)$ | $O(1)$ | ✅ |\n| Selection sort | $O(n^2)$ | $O(n^2)$ | $O(n^2)$ | $O(1)$ | ❌ |\n| Insertion sort | $O(n)$ | $O(n^2)$ | $O(n^2)$ | $O(1)$ | ✅ |\n| Merge sort | $O(n\\log n)$ | $O(n\\log n)$ | $O(n\\log n)$ | $O(n)$ | ✅ |\n| Quick sort | $O(n\\log n)$ | $O(n\\log n)$ | $O(n^2)$ | $O(\\log n)$ | ❌ |\n| Heap sort | $O(n\\log n)$ | $O(n\\log n)$ | $O(n\\log n)$ | $O(1)$ | ❌ |\n| Tim sort | $O(n)$ | $O(n\\log n)$ | $O(n\\log n)$ | $O(n)$ | ✅ |\n\n### Merge Sort — Divide & Conquer\n\n```python\ndef merge_sort(arr):\n    if len(arr) <= 1:\n        return arr\n    mid = len(arr) // 2\n    left = merge_sort(arr[:mid])\n    right = merge_sort(arr[mid:])\n    return merge(left, right)\n\ndef merge(L, R):\n    result, i, j = [], 0, 0\n    while i < len(L) and j < len(R):\n        if L[i] <= R[j]: result.append(L[i]); i += 1\n        else: result.append(R[j]); j += 1\n    return result + L[i:] + R[j:]\n```\n\n### Quicksort — Partition & Conquer\n\nChoose a **pivot**, partition the array around it, recurse on both halves.\n\n**Pivot selection strategies:**\n- First/last element: $O(n^2)$ worst case on sorted input\n- Random pivot: expected $O(n \\log n)$\n- Median-of-three: practical improvement\n\n### Practical Advice\n\n- Python''s built-in `sort()` uses **Timsort** — optimized for real-world data\n- For nearly-sorted data: **insertion sort** outperforms quicksort\n- For stable sort with guaranteed $O(n \\log n)$: **merge sort**\n- For in-place with low overhead: **heapsort**',
 0),

('20000000-0000-0000-0012-000000000011', '10000000-0000-0000-0000-000000000113',
 'Binary Search & Searching Strategies',
 E'## Binary Search & Searching Strategies\n\n### Binary Search\n\n**Precondition:** The array must be sorted.\n\n**Idea:** Compare the target to the middle element. Eliminate half the remaining search space at each step.\n\n```python\ndef binary_search(arr, target):\n    left, right = 0, len(arr) - 1\n    while left <= right:\n        mid = left + (right - left) // 2  # avoids integer overflow\n        if arr[mid] == target:\n            return mid\n        elif arr[mid] < target:\n            left = mid + 1\n        else:\n            right = mid - 1\n    return -1  # not found\n```\n\n**Complexity:** $O(\\log n)$ time, $O(1)$ space.\n\n**Why `mid = left + (right - left) // 2` and not `(left + right) // 2`?**  \nThe sum `left + right` can overflow for very large arrays in languages like C/Java.\n\n### Binary Search Variants\n\n**First occurrence of target:**\n```python\n# Same as above but when arr[mid] == target:\n# result = mid; right = mid - 1  (keep searching left)\n```\n\n**Lower bound (first index ≥ target)** / **Upper bound (first index > target)** — critical for range queries.\n\n### Ternary Search\n\nSearch for a **unimodal function''s maximum** by dividing the range into thirds. $O(\\log_{3/2} n)$.\n\n### Exponential Search\n\nFor unbounded/infinite arrays: find a range $[2^k, 2^{k+1}]$ containing the target (doubling), then binary search within. $O(\\log n)$.\n\n### Interpolation Search\n\nFor uniformly distributed sorted data, estimate position:\n$$\\text{mid} = \\text{left} + \\frac{(\\text{target} - \\text{arr}[\\text{left}]) \\times (\\text{right} - \\text{left})}{\\text{arr}[\\text{right}] - \\text{arr}[\\text{left}]}$$\n\nAverage case $O(\\log \\log n)$ but degrades to $O(n)$ in worst case.',
 1),

('20000000-0000-0000-0012-000000000012', '10000000-0000-0000-0000-000000000113',
 'Hash Tables & Collision Resolution',
 E'## Hash Tables & Collision Resolution\n\n### What Is a Hash Table?\n\nA hash table stores key-value pairs, providing **average $O(1)$** insert, lookup, and delete operations.\n\n**Core idea:**\n1. Apply a **hash function** $h(k)$ to key $k$ → bucket index\n2. Store the value in that bucket\n3. Handle **collisions** (two keys mapping to the same bucket)\n\n### Hash Functions\n\nA good hash function:\n- Is **deterministic**: same input always gives same output\n- Is **fast** to compute\n- **Distributes keys uniformly** to minimize collisions\n\n**Simple integer hash:** $h(k) = k \\mod m$ (where $m$ is table size, preferably prime)\n\n**String hashing (polynomial rolling hash):**\n$$h(s) = \\sum_{i=0}^{n-1} s[i] \\cdot p^i \\mod m$$\n\n### Collision Resolution\n\n**Chaining:** Each bucket holds a linked list of all key-value pairs that hash to it.\n- Load factor $\\alpha = n/m$ — average chain length\n- Search: $O(1 + \\alpha)$ average\n- Works well for $\\alpha \\leq 1$\n\n**Open addressing:** All elements stored in the table itself.\n- **Linear probing**: check $h(k), h(k)+1, h(k)+2, ...$ — suffers from clustering\n- **Quadratic probing**: check $h(k), h(k)+1^2, h(k)+2^2, ...$\n- **Double hashing**: $h_1(k) + i \\cdot h_2(k)$ — best distribution\n\n### Dynamic Resizing\n\nWhen load factor exceeds a threshold (typically 0.75), **resize** the table (double capacity, rehash all keys). This maintains $O(1)$ amortized operations.\n\n### Applications\n\n- **Python dict** and **set** — hash table with open addressing\n- **Java HashMap** — chaining\n- **Caches** — LRU cache using hash map + doubly linked list\n- **Rabin-Karp** string matching — rolling hash\n- **Bloom filters** — probabilistic membership testing\n\n### Cryptographic Hash Functions\n\nMD5, SHA-256, etc. are designed to be one-way (not reversible) and collision-resistant. Used in digital signatures, password storage, blockchain.',
 2);


-- =====================================================================
-- LESSON RESOURCES
-- =====================================================================
INSERT INTO lesson_resources (id, lesson_id, title, url) VALUES

-- Romanian Language
('30000000-0000-0000-0001-000000000001', '20000000-0000-0000-0001-000000000001', 'Romanian Grammar Reference — Institutul de Lingvistică', 'https://www.lingv.ro/resources/grammar'),
('30000000-0000-0000-0001-000000000002', '20000000-0000-0000-0001-000000000001', 'Morfologia limbii române (DEX Online)', 'https://dexonline.ro/definitie/morfologie'),
('30000000-0000-0000-0001-000000000003', '20000000-0000-0000-0001-000000000002', 'Sintaxa propoziției — Gramatica Academiei', 'https://www.lingv.ro/resources/syntax'),
('30000000-0000-0000-0001-000000000004', '20000000-0000-0000-0001-000000000003', 'Figuri de stil — EduPedia', 'https://www.edupedia.ro/figuri-de-stil'),
('30000000-0000-0000-0001-000000000005', '20000000-0000-0000-0001-000000000004', 'Luceafărul — Eminescu (text integral)', 'https://ro.wikisource.org/wiki/Luceaf%C4%83rul_(Eminescu)'),
('30000000-0000-0000-0001-000000000006', '20000000-0000-0000-0001-000000000004', 'Eminescu — Biblioteca Digitală BCU', 'https://digitool.bibmet.ro/R/eminescu'),
('30000000-0000-0000-0001-000000000007', '20000000-0000-0000-0001-000000000005', 'Amintiri din copilărie — Wikisource', 'https://ro.wikisource.org/wiki/Amintiri_din_copil%C4%83rie'),
('30000000-0000-0000-0001-000000000008', '20000000-0000-0000-0001-000000000006', 'Moara cu noroc — Slavici, text online', 'https://ro.wikisource.org/wiki/Moara_cu_noroc'),
('30000000-0000-0000-0001-000000000009', '20000000-0000-0000-0001-000000000007', 'Tudor Arghezi — Biblioteca virtuală', 'https://www.poezie.ro/index.php/author/1450/Tudor_Arghezi'),
('30000000-0000-0000-0001-000000000010', '20000000-0000-0000-0001-000000000007', 'Lucian Blaga — Filozofie și poezie (Univ. Cluj)', 'https://www.ubbcluj.ro/ro/centre/blaga'),
('30000000-0000-0000-0001-000000000011', '20000000-0000-0000-0001-000000000010', 'How to Write a Thesis Statement — Purdue OWL', 'https://owl.purdue.edu/owl/general_writing/the_writing_process/thesis_statement_tips.html'),
('30000000-0000-0000-0001-000000000012', '20000000-0000-0000-0001-000000000010', 'Argumentative Essay Structure — Khan Academy', 'https://www.khanacademy.org/humanities/grammar/grammar-sat/writing-sat/a/argumentative-essay-structure'),

-- Mathematics
('30000000-0000-0000-0002-000000000001', '20000000-0000-0000-0002-000000000001', 'Set Theory Basics — Brilliant.org', 'https://brilliant.org/wiki/set-theory/'),
('30000000-0000-0000-0002-000000000002', '20000000-0000-0000-0002-000000000001', 'Functions & Relations — Khan Academy', 'https://www.khanacademy.org/math/precalculus/x9e81a4f98389efdf:functions'),
('30000000-0000-0000-0002-000000000003', '20000000-0000-0000-0002-000000000002', 'Quadratic Formula Derivation — 3Blue1Brown', 'https://www.youtube.com/watch?v=EBbtoFMJvFc'),
('30000000-0000-0000-0002-000000000004', '20000000-0000-0000-0002-000000000003', 'Solving Inequalities — Paul''s Online Math Notes', 'https://tutorial.math.lamar.edu/Classes/Alg/SolveLinearInequalities.aspx'),
('30000000-0000-0000-0002-000000000005', '20000000-0000-0000-0002-000000000004', 'Euclidean Geometry — GeoGebra Interactive', 'https://www.geogebra.org/geometry'),
('30000000-0000-0000-0002-000000000006', '20000000-0000-0000-0002-000000000005', 'Unit Circle & Trig Functions — Desmos', 'https://www.desmos.com/calculator/v7khau2d7c'),
('30000000-0000-0000-0002-000000000007', '20000000-0000-0000-0002-000000000006', 'Conic Sections — Khan Academy', 'https://www.khanacademy.org/math/precalculus/x9e81a4f98389efdf:conics'),
('30000000-0000-0000-0002-000000000008', '20000000-0000-0000-0002-000000000007', 'Limits & Continuity — MIT OpenCourseWare', 'https://ocw.mit.edu/courses/18-01sc-single-variable-calculus-fall-2010/'),
('30000000-0000-0000-0002-000000000009', '20000000-0000-0000-0002-000000000008', 'Differentiation Rules — Paul''s Online Math Notes', 'https://tutorial.math.lamar.edu/Classes/CalcI/DerivativeIntro.aspx'),
('30000000-0000-0000-0002-000000000010', '20000000-0000-0000-0002-000000000009', 'Integration Techniques — Khan Academy', 'https://www.khanacademy.org/math/ap-calculus-bc/bc-integration-new'),
('30000000-0000-0000-0002-000000000011', '20000000-0000-0000-0002-000000000010', 'Combinatorics — Brilliant.org', 'https://brilliant.org/wiki/combinatorics/'),
('30000000-0000-0000-0002-000000000012', '20000000-0000-0000-0002-000000000011', 'Bayes'' Theorem Explained — 3Blue1Brown', 'https://www.youtube.com/watch?v=HZGCoVF3YvM'),
('30000000-0000-0000-0002-000000000013', '20000000-0000-0000-0002-000000000012', 'Statistics & Probability — StatQuest (YouTube)', 'https://www.youtube.com/@statquest'),

-- Physics
('30000000-0000-0000-0003-000000000001', '20000000-0000-0000-0003-000000000001', 'Kinematics Simulations — PhET Colorado', 'https://phet.colorado.edu/en/simulations/projectile-motion'),
('30000000-0000-0000-0003-000000000002', '20000000-0000-0000-0003-000000000002', 'Newton''s Laws — Khan Academy AP Physics', 'https://www.khanacademy.org/science/ap-physics-1/ap-forces-newtons-laws'),
('30000000-0000-0000-0003-000000000003', '20000000-0000-0000-0003-000000000003', 'Conservation of Energy — MIT OpenCourseWare', 'https://ocw.mit.edu/courses/8-01sc-classical-mechanics-fall-2016/'),
('30000000-0000-0000-0003-000000000004', '20000000-0000-0000-0003-000000000003', 'Momentum & Collisions Simulation — PhET', 'https://phet.colorado.edu/en/simulations/collision-lab'),

-- Algorithms & Data Structures (UNI)
('30000000-0000-0000-0012-000000000001', '20000000-0000-0000-0012-000000000001', 'Big-O Cheat Sheet', 'https://www.bigocheatsheet.com/'),
('30000000-0000-0000-0012-000000000002', '20000000-0000-0000-0012-000000000001', 'Visualizing Algorithm Complexity — CS50', 'https://cs50.harvard.edu/x/2024/notes/3/'),
('30000000-0000-0000-0012-000000000003', '20000000-0000-0000-0012-000000000002', 'Recursion — Computerphile (YouTube)', 'https://www.youtube.com/watch?v=Mv9NEXX1VHc'),
('30000000-0000-0000-0012-000000000004', '20000000-0000-0000-0012-000000000003', 'Dynamic Programming Patterns — LeetCode Discuss', 'https://leetcode.com/discuss/general-discussion/458695/dynamic-programming-patterns'),
('30000000-0000-0000-0012-000000000005', '20000000-0000-0000-0012-000000000004', 'Arrays & Strings — LeetCode Study Plan', 'https://leetcode.com/studyplan/top-interview-150/'),
('30000000-0000-0000-0012-000000000006', '20000000-0000-0000-0012-000000000005', 'Visualizing Linked Lists — VisuAlgo', 'https://visualgo.net/en/list'),
('30000000-0000-0000-0012-000000000007', '20000000-0000-0000-0012-000000000006', 'Stacks & Queues — VisuAlgo', 'https://visualgo.net/en/stack'),
('30000000-0000-0000-0012-000000000008', '20000000-0000-0000-0012-000000000007', 'BST Visualizer — VisuAlgo', 'https://visualgo.net/en/bst'),
('30000000-0000-0000-0012-000000000009', '20000000-0000-0000-0012-000000000008', 'BFS & DFS — VisuAlgo Graphs', 'https://visualgo.net/en/dfsbfs'),
('30000000-0000-0000-0012-000000000010', '20000000-0000-0000-0012-000000000009', 'Dijkstra''s Algorithm Visualization', 'https://visualgo.net/en/sssp'),
('30000000-0000-0000-0012-000000000011', '20000000-0000-0000-0012-000000000010', 'Sorting Algorithm Animations — visualgo.net', 'https://visualgo.net/en/sorting'),
('30000000-0000-0000-0012-000000000012', '20000000-0000-0000-0012-000000000011', 'Binary Search Interactive — Khan Academy', 'https://www.khanacademy.org/computing/computer-science/algorithms/binary-search/a/binary-search'),
('30000000-0000-0000-0012-000000000013', '20000000-0000-0000-0012-000000000012', 'Hash Tables — CS50 Lecture (Harvard)', 'https://cs50.harvard.edu/x/2024/notes/5/');
