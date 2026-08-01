package com.school.dao;

import com.school.model.Exam;
import com.school.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExamDAO {

    public List<Exam> getAll() throws SQLException {
        String sql = """
                SELECT e.id, e.name, e.class_id, e.term_id, e.created_by, e.created_at,
                       CONCAT('Grade ', c.grade_level, ' - ', c.section) AS class_display,
                       CONCAT('Term ', t.term_number)                     AS term_display,
                       ay.year_label
                FROM   exams e
                JOIN   classes       c  ON c.id  = e.class_id
                JOIN   terms         t  ON t.id  = e.term_id
                JOIN   academic_years ay ON ay.id = t.academic_year_id
                ORDER  BY e.created_at DESC
                """;
        List<Exam> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /** Exams belonging to a specific class. */
    public List<Exam> getByClass(int classId) throws SQLException {
        String sql = """
                SELECT e.id, e.name, e.class_id, e.term_id, e.created_by, e.created_at,
                       CONCAT('Grade ', c.grade_level, ' - ', c.section) AS class_display,
                       CONCAT('Term ', t.term_number)                     AS term_display,
                       ay.year_label
                FROM   exams e
                JOIN   classes       c  ON c.id  = e.class_id
                JOIN   terms         t  ON t.id  = e.term_id
                JOIN   academic_years ay ON ay.id = t.academic_year_id
                WHERE  e.class_id = ?
                ORDER  BY e.created_at DESC
                """;
        List<Exam> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, classId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public boolean insert(Exam e) throws SQLException {
        String sql = "INSERT INTO exams (name, class_id, term_id, created_by) VALUES (?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, e.getName());
            ps.setInt   (2, e.getClassId());
            ps.setInt   (3, e.getTermId());
            ps.setInt   (4, e.getCreatedBy());
            int rows = ps.executeUpdate();
            if (rows == 0) return false;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) e.setId(keys.getInt(1));
            }
            return true;
        }
    }

    public boolean update(Exam e) throws SQLException {
        String sql = "UPDATE exams SET name = ?, term_id = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, e.getName());
            ps.setInt   (2, e.getTermId());
            ps.setInt   (3, e.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM exams WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Exam map(ResultSet rs) throws SQLException {
        Exam e = new Exam();
        e.setId          (rs.getInt      ("id"));
        e.setName        (rs.getString   ("name"));
        e.setClassId     (rs.getInt      ("class_id"));
        e.setTermId      (rs.getInt      ("term_id"));
        e.setCreatedBy   (rs.getInt      ("created_by"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) e.setCreatedAt(ts.toLocalDateTime());
        e.setClassDisplay(rs.getString   ("class_display"));
        e.setTermDisplay (rs.getString   ("term_display"));
        e.setYearLabel   (rs.getString   ("year_label"));
        return e;
    }
}