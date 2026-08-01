package com.school.dao;

import com.school.model.Subject;
import com.school.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SubjectDAO {

    public List<Subject> getAll() throws SQLException {
        String sql = "SELECT id, name, name_si, grade_level FROM subjects ORDER BY grade_level, name";
        List<Subject> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Subject> getByGrade(int grade) throws SQLException {
        String sql = "SELECT id, name, name_si, grade_level FROM subjects WHERE grade_level = ? ORDER BY name";
        List<Subject> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, grade);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public boolean insert(Subject s) throws SQLException {
        String sql = "INSERT INTO subjects (name, name_si, grade_level) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getName());
            ps.setString(2, nullIfBlank(s.getNameSi()));
            ps.setInt   (3, s.getGradeLevel());
            int rows = ps.executeUpdate();
            if (rows == 0) return false;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) s.setId(keys.getInt(1));
            }
            return true;
        }
    }

    public boolean update(Subject s) throws SQLException {
        String sql = "UPDATE subjects SET name = ?, name_si = ?, grade_level = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, s.getName());
            ps.setString(2, nullIfBlank(s.getNameSi()));
            ps.setInt   (3, s.getGradeLevel());
            ps.setInt   (4, s.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM subjects WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Subject map(ResultSet rs) throws SQLException {
        Subject s = new Subject();
        s.setId        (rs.getInt   ("id"));
        s.setName      (rs.getString("name"));
        s.setNameSi    (rs.getString("name_si"));
        s.setGradeLevel(rs.getInt   ("grade_level"));
        return s;
    }

    private String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}