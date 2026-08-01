package com.school.dao;

import com.school.model.Student;
import com.school.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for the {@code students} table.
 * Every method opens and closes its own connection (single-school LAN app).
 */
public class StudentDAO {

    // ── Read ──────────────────────────────────────────────────

    /**
     * Returns ALL students (active + inactive), ordered by last name then first name.
     */
    public List<Student> getAll() throws SQLException {
        String sql = """
                SELECT id, first_name, last_name, first_name_si,
                       date_of_birth, nic, gender, religion,
                       address, enrollment_date, is_active, created_at
                FROM   students
                ORDER  BY last_name, first_name
                """;

        List<Student> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /**
     * Full-text search across English name, Sinhala name, and NIC.
     * Returns active students only (typical list view).
     *
     * @param query raw search string (no leading/trailing wildcards needed)
     */
    public List<Student> search(String query) throws SQLException {
        String like = "%" + query.trim() + "%";
        String sql = """
                SELECT id, first_name, last_name, first_name_si,
                       date_of_birth, nic, gender, religion,
                       address, enrollment_date, is_active, created_at
                FROM   students
                WHERE  (first_name   LIKE ?
                    OR  last_name    LIKE ?
                    OR  first_name_si LIKE ?
                    OR  nic          LIKE ?)
                ORDER  BY last_name, first_name
                """;

        List<Student> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /**
     * Fetches a single student by primary key. Returns null if not found.
     */
    public Student getById(int id) throws SQLException {
        String sql = """
                SELECT id, first_name, last_name, first_name_si,
                       date_of_birth, nic, gender, religion,
                       address, enrollment_date, is_active, created_at
                FROM   students
                WHERE  id = ?
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    // ── Write ─────────────────────────────────────────────────

    /**
     * Inserts a new student and sets {@code student.id} to the generated key.
     *
     * @return true if the row was inserted
     */
    public boolean insert(Student s) throws SQLException {
        String sql = """
                INSERT INTO students
                    (first_name, last_name, first_name_si,
                     date_of_birth, nic, gender, religion,
                     address, enrollment_date, is_active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, s.getFirstName());
            ps.setString(2, s.getLastName());
            ps.setString(3, nullIfBlank(s.getFirstNameSi()));
            ps.setDate  (4, toSqlDate(s.getDateOfBirth()));
            ps.setString(5, nullIfBlank(s.getNic()));
            ps.setString(6, s.getGender());
            ps.setString(7, nullIfBlank(s.getReligion()));
            ps.setString(8, nullIfBlank(s.getAddress()));
            ps.setDate  (9, toSqlDate(s.getEnrollmentDate()));
            ps.setBoolean(10, true);

            int affected = ps.executeUpdate();
            if (affected == 0) return false;

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) s.setId(keys.getInt(1));
            }
            return true;
        }
    }

    /**
     * Updates all editable fields of an existing student.
     *
     * @return true if the row was updated
     */
    public boolean update(Student s) throws SQLException {
        String sql = """
                UPDATE students
                SET    first_name     = ?,
                       last_name      = ?,
                       first_name_si  = ?,
                       date_of_birth  = ?,
                       nic            = ?,
                       gender         = ?,
                       religion       = ?,
                       address        = ?,
                       enrollment_date = ?
                WHERE  id = ?
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, s.getFirstName());
            ps.setString(2, s.getLastName());
            ps.setString(3, nullIfBlank(s.getFirstNameSi()));
            ps.setDate  (4, toSqlDate(s.getDateOfBirth()));
            ps.setString(5, nullIfBlank(s.getNic()));
            ps.setString(6, s.getGender());
            ps.setString(7, nullIfBlank(s.getReligion()));
            ps.setString(8, nullIfBlank(s.getAddress()));
            ps.setDate  (9, toSqlDate(s.getEnrollmentDate()));
            ps.setInt   (10, s.getId());

            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Soft-deletes (or re-activates) a student by flipping {@code is_active}.
     *
     * @param id     student primary key
     * @param active true = active, false = deactivated
     * @return true if the row was updated
     */
    public boolean setActive(int id, boolean active) throws SQLException {
        String sql = "UPDATE students SET is_active = ? WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setBoolean(1, active);
            ps.setInt    (2, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Hard-delete a student record.
     *
     * @return true if the row was updated
     */
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM students WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    /** Maps a ResultSet row to a Student instance. */
    private Student map(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setId           (rs.getInt      ("id"));
        s.setFirstName    (rs.getString   ("first_name"));
        s.setLastName     (rs.getString   ("last_name"));
        s.setFirstNameSi  (rs.getString   ("first_name_si"));
        s.setDateOfBirth  (toLocalDate    (rs.getDate("date_of_birth")));
        s.setNic          (rs.getString   ("nic"));
        s.setGender       (rs.getString   ("gender"));
        s.setReligion     (rs.getString   ("religion"));
        s.setAddress      (rs.getString   ("address"));
        s.setEnrollmentDate(toLocalDate   (rs.getDate("enrollment_date")));
        s.setActive       (rs.getBoolean  ("is_active"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) s.setCreatedAt(ts.toLocalDateTime());
        return s;
    }

    private java.sql.Date toSqlDate(LocalDate d) {
        return d == null ? null : java.sql.Date.valueOf(d);
    }

    private LocalDate toLocalDate(java.sql.Date d) {
        return d == null ? null : d.toLocalDate();
    }

    /** Returns null for blank/empty strings so optional fields store NULL in DB. */
    private String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}