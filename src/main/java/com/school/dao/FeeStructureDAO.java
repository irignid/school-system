package com.school.dao;

import com.school.model.FeeItem;
import com.school.model.FeeStructure;
import com.school.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FeeStructureDAO {

    // ── Fee Structures ────────────────────────────────────────

    public List<FeeStructure> getAll() throws SQLException {
        String sql = """
                SELECT fs.id, fs.term_id, fs.grade_level, fs.description,
                       CONCAT('Term ', t.term_number) AS term_display,
                       ay.year_label,
                       COALESCE(SUM(fi.amount), 0)   AS total_amount
                FROM   fee_structures fs
                JOIN   terms          t  ON t.id  = fs.term_id
                JOIN   academic_years ay ON ay.id = t.academic_year_id
                LEFT JOIN fee_items   fi ON fi.fee_structure_id = fs.id
                GROUP  BY fs.id, fs.term_id, fs.grade_level, fs.description,
                          t.term_number, ay.year_label
                ORDER  BY ay.start_date DESC, t.term_number, fs.grade_level
                """;
        List<FeeStructure> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapStructure(rs));
        }
        return list;
    }

    public List<FeeStructure> getByTerm(int termId) throws SQLException {
        String sql = """
                SELECT fs.id, fs.term_id, fs.grade_level, fs.description,
                       CONCAT('Term ', t.term_number) AS term_display,
                       ay.year_label,
                       COALESCE(SUM(fi.amount), 0)   AS total_amount
                FROM   fee_structures fs
                JOIN   terms          t  ON t.id  = fs.term_id
                JOIN   academic_years ay ON ay.id = t.academic_year_id
                LEFT JOIN fee_items   fi ON fi.fee_structure_id = fs.id
                WHERE  fs.term_id = ?
                GROUP  BY fs.id, fs.term_id, fs.grade_level, fs.description,
                          t.term_number, ay.year_label
                ORDER  BY fs.grade_level
                """;
        List<FeeStructure> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, termId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapStructure(rs));
            }
        }
        return list;
    }

    public boolean insert(FeeStructure fs) throws SQLException {
        String sql = "INSERT INTO fee_structures (term_id, grade_level, description) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1, fs.getTermId());
            ps.setInt   (2, fs.getGradeLevel());
            ps.setString(3, nullIfBlank(fs.getDescription()));
            int rows = ps.executeUpdate();
            if (rows == 0) return false;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) fs.setId(keys.getInt(1));
            }
            return true;
        }
    }

    public boolean update(FeeStructure fs) throws SQLException {
        String sql = "UPDATE fee_structures SET description = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nullIfBlank(fs.getDescription()));
            ps.setInt   (2, fs.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        // fee_items cascade-delete via FK ON DELETE CASCADE
        String sql = "DELETE FROM fee_structures WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Fee Items ─────────────────────────────────────────────

    public List<FeeItem> getItemsByStructure(int structureId) throws SQLException {
        String sql = """
                SELECT id, fee_structure_id, item_name, amount
                FROM   fee_items
                WHERE  fee_structure_id = ?
                ORDER  BY id
                """;
        List<FeeItem> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, structureId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapItem(rs));
            }
        }
        return list;
    }

    public boolean insertItem(FeeItem item) throws SQLException {
        String sql = "INSERT INTO fee_items (fee_structure_id, item_name, amount) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1, item.getFeeStructureId());
            ps.setString(2, item.getItemName());
            ps.setDouble(3, item.getAmount());
            int rows = ps.executeUpdate();
            if (rows == 0) return false;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) item.setId(keys.getInt(1));
            }
            return true;
        }
    }

    public boolean updateItem(FeeItem item) throws SQLException {
        String sql = "UPDATE fee_items SET item_name = ?, amount = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, item.getItemName());
            ps.setDouble(2, item.getAmount());
            ps.setInt   (3, item.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteItem(int id) throws SQLException {
        String sql = "DELETE FROM fee_items WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private FeeStructure mapStructure(ResultSet rs) throws SQLException {
        FeeStructure fs = new FeeStructure();
        fs.setId          (rs.getInt   ("id"));
        fs.setTermId      (rs.getInt   ("term_id"));
        fs.setGradeLevel  (rs.getInt   ("grade_level"));
        fs.setDescription (rs.getString("description"));
        fs.setTermDisplay (rs.getString("term_display"));
        fs.setYearLabel   (rs.getString("year_label"));
        fs.setTotalAmount (rs.getDouble("total_amount"));
        return fs;
    }

    private FeeItem mapItem(ResultSet rs) throws SQLException {
        FeeItem fi = new FeeItem();
        fi.setId             (rs.getInt   ("id"));
        fi.setFeeStructureId (rs.getInt   ("fee_structure_id"));
        fi.setItemName       (rs.getString("item_name"));
        fi.setAmount         (rs.getDouble("amount"));
        return fi;
    }

    private String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
