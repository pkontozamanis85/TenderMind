-- ==========================================
-- 1. SEEDING PROFESSIONAL USERS
-- ==========================================

-- User 1: Senior Civil Engineer (Infrastructure Expert)
INSERT INTO users (full_name, age, sex, profession, preferred_cost_min, preferred_cost_max, nuts_code, cpv_code, version)
VALUES ('Κωνσταντίνος Γεωργίου', 52, 'MALE', 'Senior Civil Engineer', 100000.0, 10000000.0, 'EL30', '45000000', 0);

-- User 2: IT & Energy Solutions Consultant
INSERT INTO users (full_name, age, sex, profession, preferred_cost_min, preferred_cost_max, nuts_code, cpv_code, version)
VALUES ('Μαρία Αγγελίδη', 31, 'FEMALE', 'IT Project Manager', 10000.0, 500000.0, 'EL52', '72000000', 0);

-- ==========================================
-- 2. SEEDING INTERESTS (SEMANTIC TAGS)
-- ==========================================

INSERT INTO interests (name) VALUES
                                 ('Γέφυρες'), ('Οδοποιία'), ('Λιμάνια'), ('Φωτοβολταϊκά'), ('Λογισμικό'), ('Κυβερνοασφάλεια'), ('Ψηφιακός Μετασχηματισμός');

-- ==========================================
-- 3. LINKING USERS WITH INTERESTS
-- ==========================================

-- Konstantinos: Infrastructure Focus
INSERT INTO user_interests (user_id, interest_id) VALUES (1, 1), (1, 2), (1, 3);

-- Maria: Tech & Energy Focus
INSERT INTO user_interests (user_id, interest_id) VALUES (2, 4), (2, 5), (2, 6), (2, 7);