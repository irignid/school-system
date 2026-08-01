package com.school.dao;

import com.school.model.AcademicYear;
import com.school.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AcademicYearDAO {

    public List<AcademicYear> getAll() throws SQLException {
        String sql = "SELECT id, year_label, start_date, end_date, is_current FROM academic_years ORDER BY start_date DESC";
        List<AcademicYear> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public AcademicYear getCurrent() throws SQLException {
        String sql = "SELECT id, year_label, start_date, end_date, is_current FROM academic_years WHERE is_current = TRUE LIMIT 1";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? map(rs) : null;
        }
    }

    /** Inserts a new academic year. Sets the generated id on the object. */
    public boolean insert(AcademicYear ay) throws SQLException {
        String sql = "INSERT INTO academic_years (year_label, start_date, end_date, is_current) VALUES (?, ?, ?, FALSE)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ay.getYearLabel());
            ps.setDate  (2, Date.valueOf(ay.getStartDate()));
            ps.setDate  (3, Date.valueOf(ay.getEndDate()));
            int rows = ps.executeUpdate();
            if (rows == 0) return false;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) ay.setId(keys.getInt(1));
            }
            return true;
        }
    }

    /**
     * Makes one year the current year.
     * Clears all other rows first (only one can be current at a time).
     * Runs both updates in a single transaction.
     */
    public boolean setCurrent(int id) throws SQLException {
        String clearSql = "UPDATE academic_years SET is_current = FALSE";
        String setSql   = "UPDATE academic_years SET is_current = TRUE WHERE id = ?";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(clearSql)) { ps.executeUpdate(); }
                try (PreparedStatement ps = con.prepareStatement(setSql))   { ps.setInt(1, id); ps.executeUpdate(); }
                con.commit();
                return true;
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    private AcademicYear map(ResultSet rs) throws SQLException {
        AcademicYear ay = new AcademicYear();
        ay.setId        (rs.getInt    ("id"));
        ay.setYearLabel (rs.getString ("year_label"));
        ay.setStartDate (rs.getDate   ("start_date").toLocalDate());
        ay.setEndDate   (rs.getDate   ("end_date").toLocalDate());
        ay.setCurrent   (rs.getBoolean("is_current"));
        return ay;
    }
}