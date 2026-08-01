package com.school.dao;

import com.school.model.GradeThreshold;
import com.school.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GradeThresholdDAO {

    public List<GradeThreshold> getAll() throws SQLException {
        String sql = "SELECT id, grade_symbol, min_mark, max_mark FROM grade_thresholds ORDER BY min_mark DESC";
        List<GradeThreshold> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public boolean update(GradeThreshold gt) throws SQLException {
        String sql = "UPDATE grade_thresholds SET min_mark = ?, max_mark = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, gt.getMinMark());
            ps.setDouble(2, gt.getMaxMark());
            ps.setInt   (3, gt.getId());
            return ps.executeUpdate() > 0;
        }
    }

    private GradeThreshold map(ResultSet rs) throws SQLException {
        GradeThreshold gt = new GradeThreshold();
        gt.setId         (rs.getInt   ("id"));
        gt.setGradeSymbol(rs.getString("grade_symbol"));
        gt.setMinMark    (rs.getDouble("min_mark"));
        gt.setMaxMark    (rs.getDouble("max_mark"));
        return gt;
    }
}