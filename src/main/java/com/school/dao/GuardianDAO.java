package com.school.dao;

import com.school.model.Guardian;
import com.school.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for the {@code guardians} table.
 * Guardians are always accessed through their parent student.
 */
public class GuardianDAO {

    // ── Read ──────────────────────────────────────────────────

    /**
     * Returns all guardians linked to a given student.
     */
    public List<Guardian> getByStudentId(int studentId) throws SQLException {
        String sql = """
                SELECT id, student_id, full_name, relationship, phone, email
                FROM   guardians
                WHERE  student_id = ?
                ORDER  BY id
                """;

        List<Guardian> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    // ── Write ─────────────────────────────────────────────────

    /**
     * Inserts a new guardian and sets {@code guardian.id} to the generated key.
     *
     * @return true if the row was inserted
     */
    public boolean insert(Guardian g) throws SQLException {
        String sql = """
                INSERT INTO guardians (student_id, full_name, relationship, phone, email)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt   (1, g.getStudentId());
            ps.setString(2, g.getFullName());
            ps.setString(3, g.getRelationship());
            ps.setString(4, nullIfBlank(g.getPhone()));
            ps.setString(5, nullIfBlank(g.getEmail()));

            int affected = ps.executeUpdate();
            if (affected == 0) return false;

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) g.setId(keys.getInt(1));
            }
            return true;
        }
    }

    /**
     * Updates an existing guardian record.
     *
     * @return true if the row was updated
     */
    public boolean update(Guardian g) throws SQLException {
        String sql = """
                UPDATE guardians
                SET    full_name     = ?,
                       relationship  = ?,
                       phone         = ?,
                       email         = ?
                WHERE  id = ?
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, g.getFullName());
            ps.setString(2, g.getRelationship());
            ps.setString(3, nullIfBlank(g.getPhone()));
            ps.setString(4, nullIfBlank(g.getEmail()));
            ps.setInt   (5, g.getId());

            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Hard-deletes a guardian by primary key.
     * (Guardians cascade-delete when the student is deleted, but we allow
     *  individual removal here too.)
     *
     * @return true if the row was deleted
     */
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM guardians WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private Guardian map(ResultSet rs) throws SQLException {
        Guardian g = new Guardian();
        g.setId           (rs.getInt   ("id"));
        g.setStudentId    (rs.getInt   ("student_id"));
        g.setFullName     (rs.getString("full_name"));
        g.setRelationship (rs.getString("relationship"));
        g.setPhone        (rs.getString("phone"));
        g.setEmail        (rs.getString("email"));
        return g;
    }

    private String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}