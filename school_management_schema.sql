-- ============================================================
--  School Management System — Database Schema (Complete)
--  MySQL 8.x (MariaDB 10.6+ compatible)
--
--  Encoding : utf8mb4  — supports Sinhala Unicode (mandatory)
--  Engine   : InnoDB   — required for foreign key support
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ── Drop in reverse dependency order ─────────────────────────────────────
DROP TABLE IF EXISTS timetable;
DROP TABLE IF EXISTS period_config;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS invoices;
DROP TABLE IF EXISTS fee_items;
DROP TABLE IF EXISTS fee_structures;
DROP TABLE IF EXISTS marks;
DROP TABLE IF EXISTS exam_schedules;
DROP TABLE IF EXISTS exams;
DROP TABLE IF EXISTS grade_thresholds;
DROP TABLE IF EXISTS attendance;
DROP TABLE IF EXISTS class_subjects;
DROP TABLE IF EXISTS class_enrollments;
DROP TABLE IF EXISTS classes;
DROP TABLE IF EXISTS subjects;
DROP TABLE IF EXISTS terms;
DROP TABLE IF EXISTS academic_years;
DROP TABLE IF EXISTS guardians;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS staff_profiles;
DROP TABLE IF EXISTS teacher_profiles;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;


-- ══════════════════════════════════════════════════════════════
--  AUTH & USERS
-- ══════════════════════════════════════════════════════════════

CREATE TABLE users (
    id            INT          NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          ENUM('principal','teacher','staff') NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login    TIMESTAMP    NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE teacher_profiles (
    id         INT          NOT NULL AUTO_INCREMENT,
    user_id    INT          NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    phone      VARCHAR(20)  NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_teacher_user (user_id),
    CONSTRAINT fk_teacher_user
        FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE staff_profiles (
    id         INT          NOT NULL AUTO_INCREMENT,
    user_id    INT          NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    phone      VARCHAR(20)  NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_staff_user (user_id),
    CONSTRAINT fk_staff_user
        FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ══════════════════════════════════════════════════════════════
--  PEOPLE  (students & guardians — no login)
-- ══════════════════════════════════════════════════════════════

CREATE TABLE students (
    id              INT           NOT NULL AUTO_INCREMENT,
    first_name      VARCHAR(100)  NOT NULL,
    last_name       VARCHAR(100)  NOT NULL,
    first_name_si   VARCHAR(100)  NULL     COMMENT 'First name in Sinhala script',
    date_of_birth   DATE          NOT NULL,
    nic             VARCHAR(20)   NULL,
    gender          ENUM('M','F') NOT NULL,
    religion        VARCHAR(50)   NULL,
    address         TEXT          NULL,
    enrollment_date DATE          NOT NULL,
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY  uq_students_nic  (nic),
    KEY         idx_student_name (last_name, first_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE guardians (
    id           INT          NOT NULL AUTO_INCREMENT,
    student_id   INT          NOT NULL,
    full_name    VARCHAR(100) NOT NULL,
    relationship VARCHAR(50)  NOT NULL,
    phone        VARCHAR(20)  NULL,
    email        VARCHAR(100) NULL,

    PRIMARY KEY (id),
    KEY idx_guardian_student (student_id),
    CONSTRAINT fk_guardian_student
        FOREIGN KEY (student_id) REFERENCES students(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ══════════════════════════════════════════════════════════════
--  ACADEMIC STRUCTURE
-- ══════════════════════════════════════════════════════════════

CREATE TABLE academic_years (
    id         INT         NOT NULL AUTO_INCREMENT,
    year_label VARCHAR(10) NOT NULL,
    start_date DATE        NOT NULL,
    end_date   DATE        NOT NULL,
    is_current BOOLEAN     NOT NULL DEFAULT FALSE,

    PRIMARY KEY (id),
    UNIQUE KEY uq_year_label (year_label)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE terms (
    id               INT  NOT NULL AUTO_INCREMENT,
    academic_year_id INT  NOT NULL,
    term_number      INT  NOT NULL,
    start_date       DATE NOT NULL,
    end_date         DATE NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_term_per_year (academic_year_id, term_number),
    CONSTRAINT chk_term_number CHECK (term_number BETWEEN 1 AND 3),
    CONSTRAINT fk_term_year
        FOREIGN KEY (academic_year_id) REFERENCES academic_years(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE subjects (
    id          INT          NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    name_si     VARCHAR(100) NULL     COMMENT 'Subject name in Sinhala',
    grade_level INT          NOT NULL,

    PRIMARY KEY (id),
    KEY idx_subject_grade (grade_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE classes (
    id                  INT        NOT NULL AUTO_INCREMENT,
    academic_year_id    INT        NOT NULL,
    grade_level         INT        NOT NULL,
    section             VARCHAR(5) NOT NULL,
    homeroom_teacher_id INT        NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_class_slot (academic_year_id, grade_level, section),
    KEY idx_class_teacher (homeroom_teacher_id),
    CONSTRAINT fk_class_year
        FOREIGN KEY (academic_year_id) REFERENCES academic_years(id),
    CONSTRAINT fk_class_teacher
        FOREIGN KEY (homeroom_teacher_id) REFERENCES teacher_profiles(id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ══════════════════════════════════════════════════════════════
--  JUNCTION TABLES
-- ══════════════════════════════════════════════════════════════

CREATE TABLE class_enrollments (
    id         INT NOT NULL AUTO_INCREMENT,
    student_id INT NOT NULL,
    class_id   INT NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_enrollment (student_id, class_id),
    KEY idx_enrollment_class (class_id),
    CONSTRAINT fk_enrol_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_enrol_class   FOREIGN KEY (class_id)   REFERENCES classes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE class_subjects (
    id         INT NOT NULL AUTO_INCREMENT,
    class_id   INT NOT NULL,
    subject_id INT NOT NULL,
    teacher_id INT NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_class_subject (class_id, subject_id),
    KEY idx_cs_teacher (teacher_id),
    CONSTRAINT fk_cs_class   FOREIGN KEY (class_id)   REFERENCES classes(id),
    CONSTRAINT fk_cs_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
    CONSTRAINT fk_cs_teacher FOREIGN KEY (teacher_id) REFERENCES teacher_profiles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ══════════════════════════════════════════════════════════════
--  ATTENDANCE
-- ══════════════════════════════════════════════════════════════

CREATE TABLE attendance (
    id         INT  NOT NULL AUTO_INCREMENT,
    student_id INT  NOT NULL,
    class_id   INT  NOT NULL,
    date       DATE NOT NULL,
    period     INT  NOT NULL,
    status     ENUM('P','A','L','ML') NOT NULL DEFAULT 'P',
    marked_by  INT  NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_attendance_slot (student_id, class_id, date, period),
    KEY idx_att_class_date (class_id, date),
    KEY idx_att_student    (student_id),
    CONSTRAINT fk_att_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_att_class   FOREIGN KEY (class_id)   REFERENCES classes(id),
    CONSTRAINT fk_att_teacher FOREIGN KEY (marked_by)  REFERENCES teacher_profiles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ══════════════════════════════════════════════════════════════
--  TIMETABLE
-- ══════════════════════════════════════════════════════════════

CREATE TABLE period_config (
    period      INT          NOT NULL,
    label       VARCHAR(20)  NOT NULL,
    start_time  TIME         NOT NULL,
    end_time    TIME         NOT NULL,
    is_interval BOOLEAN      NOT NULL DEFAULT FALSE,

    PRIMARY KEY (period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE timetable (
    id         INT NOT NULL AUTO_INCREMENT,
    class_id   INT NOT NULL,
    day        TINYINT NOT NULL,      -- 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri
    period     INT NOT NULL,
    subject_id INT NOT NULL,
    teacher_id INT NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_slot (class_id, day, period),
    CONSTRAINT fk_tt_class   FOREIGN KEY (class_id)   REFERENCES classes(id),
    CONSTRAINT fk_tt_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
    CONSTRAINT fk_tt_teacher FOREIGN KEY (teacher_id) REFERENCES teacher_profiles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ══════════════════════════════════════════════════════════════
--  EXAMS & GRADING
-- ══════════════════════════════════════════════════════════════

CREATE TABLE grade_thresholds (
    id           INT          NOT NULL AUTO_INCREMENT,
    grade_symbol VARCHAR(5)   NOT NULL,
    min_mark     DECIMAL(5,2) NOT NULL,
    max_mark     DECIMAL(5,2) NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_grade_symbol (grade_symbol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE exams (
    id         INT          NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL,
    class_id   INT          NOT NULL,
    term_id    INT          NOT NULL,
    created_by INT          NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    KEY idx_exam_class (class_id),
    KEY idx_exam_term  (term_id),
    CONSTRAINT fk_exam_class FOREIGN KEY (class_id)   REFERENCES classes(id),
    CONSTRAINT fk_exam_term  FOREIGN KEY (term_id)    REFERENCES terms(id),
    CONSTRAINT fk_exam_user  FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE exam_schedules (
    id               INT  NOT NULL AUTO_INCREMENT,
    exam_id          INT  NOT NULL,
    subject_id       INT  NOT NULL,
    date             DATE NOT NULL,
    start_time       TIME NOT NULL,
    duration_minutes INT  NOT NULL DEFAULT 180,

    PRIMARY KEY (id),
    UNIQUE KEY uq_exam_subject (exam_id, subject_id),
    KEY idx_es_exam (exam_id),
    CONSTRAINT fk_es_exam    FOREIGN KEY (exam_id)    REFERENCES exams(id) ON DELETE CASCADE,
    CONSTRAINT fk_es_subject FOREIGN KEY (subject_id) REFERENCES subjects(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE marks (
    id            INT          NOT NULL AUTO_INCREMENT,
    student_id    INT          NOT NULL,
    exam_id       INT          NOT NULL,
    subject_id    INT          NOT NULL,
    mark_obtained DECIMAL(5,2) NOT NULL,
    max_mark      DECIMAL(5,2) NOT NULL DEFAULT 100.00,
    entered_by    INT          NOT NULL,
    entered_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_mark (student_id, exam_id, subject_id),
    KEY idx_mark_exam    (exam_id),
    KEY idx_mark_student (student_id),
    CONSTRAINT fk_mark_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_mark_exam    FOREIGN KEY (exam_id)    REFERENCES exams(id),
    CONSTRAINT fk_mark_subject FOREIGN KEY (subject_id) REFERENCES subjects(id),
    CONSTRAINT fk_mark_user    FOREIGN KEY (entered_by) REFERENCES users(id),
    CONSTRAINT chk_mark_range  CHECK (mark_obtained >= 0 AND mark_obtained <= max_mark)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ══════════════════════════════════════════════════════════════
--  FEES
-- ══════════════════════════════════════════════════════════════

CREATE TABLE fee_structures (
    id          INT          NOT NULL AUTO_INCREMENT,
    term_id     INT          NOT NULL,
    grade_level INT          NOT NULL,
    description VARCHAR(255) NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_fee_structure (term_id, grade_level),
    CONSTRAINT fk_fs_term FOREIGN KEY (term_id) REFERENCES terms(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE fee_items (
    id               INT           NOT NULL AUTO_INCREMENT,
    fee_structure_id INT           NOT NULL,
    item_name        VARCHAR(100)  NOT NULL,
    amount           DECIMAL(10,2) NOT NULL,

    PRIMARY KEY (id),
    KEY idx_fee_items_structure (fee_structure_id),
    CONSTRAINT fk_fi_structure
        FOREIGN KEY (fee_structure_id) REFERENCES fee_structures(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE invoices (
    id               INT           NOT NULL AUTO_INCREMENT,
    student_id       INT           NOT NULL,
    fee_structure_id INT           NOT NULL,
    issued_date      DATE          NOT NULL,
    due_date         DATE          NOT NULL,
    discount_amount  DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    discount_reason  VARCHAR(255)  NULL,
    status           ENUM('unpaid','partial','paid','overdue') NOT NULL DEFAULT 'unpaid',
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_invoice (student_id, fee_structure_id),
    KEY idx_invoice_student   (student_id),
    KEY idx_invoice_status    (status),
    CONSTRAINT fk_inv_student   FOREIGN KEY (student_id)       REFERENCES students(id),
    CONSTRAINT fk_inv_structure FOREIGN KEY (fee_structure_id) REFERENCES fee_structures(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE payments (
    id             INT           NOT NULL AUTO_INCREMENT,
    invoice_id     INT           NOT NULL,
    amount         DECIMAL(10,2) NOT NULL,
    payment_date   DATE          NOT NULL,
    method         ENUM('cash','cheque','bank') NOT NULL,
    receipt_number VARCHAR(50)   NULL,
    recorded_by    INT           NOT NULL,
    recorded_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    KEY idx_payments_invoice (invoice_id),
    CONSTRAINT fk_pay_invoice FOREIGN KEY (invoice_id)  REFERENCES invoices(id),
    CONSTRAINT fk_pay_user    FOREIGN KEY (recorded_by) REFERENCES users(id),
    CONSTRAINT chk_pay_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ══════════════════════════════════════════════════════════════
--  SEED DATA
-- ══════════════════════════════════════════════════════════════

INSERT IGNORE INTO grade_thresholds (grade_symbol, min_mark, max_mark) VALUES
    ('A+', 90.00, 100.00),
    ('A',  75.00,  89.99),
    ('B',  65.00,  74.99),
    ('C',  55.00,  64.99),
    ('D',  35.00,  54.99),
    ('E',   0.00,  34.99);

INSERT IGNORE INTO academic_years (year_label, start_date, end_date, is_current) VALUES
    ('2025/2026', '2025-01-06', '2025-11-28', TRUE);

INSERT IGNORE INTO terms (academic_year_id, term_number, start_date, end_date) VALUES
    (1, 1, '2025-01-06', '2025-04-11'),
    (1, 2, '2025-05-05', '2025-08-08'),
    (1, 3, '2025-09-01', '2025-11-28');

INSERT IGNORE INTO subjects (name, name_si, grade_level) VALUES
    ('Mathematics',   'ගණිතය',    10),
    ('Science',       'විද්‍යාව',   10),
    ('English',       'ඉංග්‍රීසි',  10),
    ('Sinhala',       'සිංහල',     10),
    ('History',       'ඉතිහාසය',   10),
    ('Mathematics',   'ගණිතය',    11),
    ('Science',       'විද්‍යාව',   11),
    ('English',       'ඉංග්‍රීසි',  11),
    ('Sinhala',       'සිංහල',     11),
    ('History',       'ඉතිහාසය',   11);

INSERT IGNORE INTO period_config (period, label, start_time, end_time, is_interval) VALUES
    (1, 'Period 1',  '08:00', '08:45', FALSE),
    (2, 'Period 2',  '08:45', '09:30', FALSE),
    (3, 'Period 3',  '09:30', '10:15', FALSE),
    (4, 'Period 4',  '10:15', '11:00', FALSE),
    (5, 'Interval',  '11:00', '11:20', TRUE),
    (6, 'Period 5',  '11:20', '12:05', FALSE),
    (7, 'Period 6',  '12:05', '12:50', FALSE),
    (8, 'Period 7',  '12:50', '13:30', FALSE),
    (9, 'Period 8',  '13:30', '14:15', FALSE);

-- ⚠ Replace with real BCrypt hashes generated via:
--   BCrypt.hashpw("YourPassword", BCrypt.gensalt(12))
INSERT IGNORE INTO users (username, password_hash, role, is_active) VALUES
    ('principal', '$2a$12$REPLACEME_PRINCIPAL_HASH_HERE_xxxxxxxxxxxxxxxx', 'principal', TRUE),
    ('teacher1',  '$2a$12$REPLACEME_TEACHER1_HASH_HERE_xxxxxxxxxxxxxxxxx', 'teacher',   TRUE),
    ('staff1',    '$2a$12$REPLACEME_STAFF1_HASH_HERE_xxxxxxxxxxxxxxxxxxx', 'staff',     TRUE);

INSERT IGNORE INTO teacher_profiles (user_id, first_name, last_name, phone) VALUES
    (2, 'Nimal',  'Perera', '0771234567');

INSERT IGNORE INTO staff_profiles (user_id, first_name, last_name, phone) VALUES
    (3, 'Kamani', 'Silva',  '0777654321');


-- ══════════════════════════════════════════════════════════════
--  VIEWS
-- ══════════════════════════════════════════════════════════════

CREATE OR REPLACE VIEW v_student_class AS
SELECT
    s.id           AS student_id,
    s.first_name,
    s.last_name,
    s.first_name_si,
    c.id           AS class_id,
    c.grade_level,
    c.section,
    ay.year_label
FROM students s
JOIN class_enrollments ce ON ce.student_id = s.id
JOIN classes           c  ON c.id = ce.class_id
JOIN academic_years    ay ON ay.id = c.academic_year_id
WHERE ay.is_current = TRUE
  AND s.is_active   = TRUE;


-- Fixed version — avoids cartesian product between fee_items and payments
CREATE OR REPLACE VIEW v_invoice_summary AS
SELECT
    i.id                                                           AS invoice_id,
    i.student_id,
    CONCAT(s.first_name, ' ', s.last_name)                        AS student_name,
    i.fee_structure_id,
    i.issued_date,
    i.due_date,
    i.discount_amount,
    CASE
        WHEN COALESCE(p_sum.total_paid, 0) = 0
            THEN 'unpaid'
        WHEN COALESCE(p_sum.total_paid, 0) >= COALESCE(fi_sum.total_fee, 0) - i.discount_amount
            THEN 'paid'
        ELSE 'partial'
    END                                                            AS status,
    COALESCE(fi_sum.total_fee, 0)                                  AS total_fee,
    COALESCE(fi_sum.total_fee, 0) - i.discount_amount              AS net_payable,
    COALESCE(p_sum.total_paid,  0)                                 AS total_paid,
    COALESCE(fi_sum.total_fee, 0) - i.discount_amount
        - COALESCE(p_sum.total_paid, 0)                            AS outstanding
FROM invoices i
JOIN students s ON s.id = i.student_id
LEFT JOIN (
    SELECT fee_structure_id, SUM(amount) AS total_fee
    FROM   fee_items
    GROUP  BY fee_structure_id
) fi_sum ON fi_sum.fee_structure_id = i.fee_structure_id
LEFT JOIN (
    SELECT invoice_id, SUM(amount) AS total_paid
    FROM   payments
    GROUP  BY invoice_id
) p_sum ON p_sum.invoice_id = i.id;


CREATE OR REPLACE VIEW v_attendance_summary AS
SELECT
    a.student_id,
    CONCAT(s.first_name, ' ', s.last_name)   AS student_name,
    a.class_id,
    COUNT(*)                                  AS total_periods,
    SUM(a.status = 'P')                       AS present,
    SUM(a.status = 'A')                       AS absent,
    SUM(a.status = 'L')                       AS late,
    SUM(a.status = 'ML')                      AS medical_leave,
    ROUND(SUM(a.status = 'P') / COUNT(*) * 100, 1) AS attendance_pct
FROM attendance a
JOIN students   s ON s.id = a.student_id
GROUP BY a.student_id, s.first_name, s.last_name, a.class_id;


CREATE OR REPLACE VIEW v_marks_with_grade AS
SELECT
    m.id,
    m.student_id,
    CONCAT(s.first_name, ' ', s.last_name) AS student_name,
    m.exam_id,
    e.name                                  AS exam_name,
    m.subject_id,
    sub.name                                AS subject_name,
    m.mark_obtained,
    m.max_mark,
    ROUND(m.mark_obtained / m.max_mark * 100, 2) AS percentage,
    g.grade_symbol
FROM marks  m
JOIN students s  ON s.id   = m.student_id
JOIN exams    e  ON e.id   = m.exam_id
JOIN subjects sub ON sub.id = m.subject_id
LEFT JOIN grade_thresholds g
       ON (m.mark_obtained / m.max_mark * 100) BETWEEN g.min_mark AND g.max_mark;