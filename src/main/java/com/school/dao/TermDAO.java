package com.school.dao;

import com.school.model.Term;
import com.school.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TermDAO {

    public List<Term> getByYear(int academicYearId) throws SQLException {
        String sql = """
                SELECT t.id, t.academic_year_id, t.term_number, t.start_date, t.end_date,
                       ay.year_label
                FROM   terms t
                JOIN   academic_years ay ON ay.id = t.academic_year_id
                WHERE  t.academic_year_id = ?
                ORDER  BY t.term_number
                """;
        List<Term> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, academicYearId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** Returns how many terms already exist for a given year (max 3). */
    public int countByYear(int academicYearId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM terms WHERE academic_year_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, academicYearId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public boolean insert(Term t) throws SQLException {
        String sql = "INSERT INTO terms (academic_year_id, term_number, start_date, end_date) VALUES (?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt (1, t.getAcademicYearId());
            ps.setInt (2, t.getTermNumber());
            ps.setDate(3, Date.valueOf(t.getStartDate()));
            ps.setDate(4, Date.valueOf(t.getEndDate()));
            int rows = ps.executeUpdate();
            if (rows == 0) return false;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) t.setId(keys.getInt(1));
            }
            return true;
        }
    }

    public boolean update(Term t) throws SQLException {
        String sql = "UPDATE terms SET start_date = ?, end_date = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(t.getStartDate()));
            ps.setDate(2, Date.valueOf(t.getEndDate()));
            ps.setInt (3, t.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM terms WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Term map(ResultSet rs) throws SQLException {
        Term t = new Term();
        t.setId             (rs.getInt ("id"));
        t.setAcademicYearId (rs.getInt ("academic_year_id"));
        t.setTermNumber     (rs.getInt ("term_number"));
        t.setStartDate      (rs.getDate("start_date").toLocalDate());
        t.setEndDate        (rs.getDate("end_date").toLocalDate());
        t.setYearLabel      (rs.getString("year_label"));
        return t;
    }
}