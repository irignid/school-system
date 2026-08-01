package com.school.dao;

import com.school.model.SchoolClass;
import com.school.model.TeacherProfile;
import com.school.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClassDAO {

    // ── Read ──────────────────────────────────────────────────

    public List<SchoolClass> getByYear(int academicYearId) throws SQLException {
        String sql = """
                SELECT c.id, c.academic_year_id, c.grade_level, c.section,
                       c.homeroom_teacher_id,
                       ay.year_label,
                       CONCAT(tp.first_name, ' ', tp.last_name) AS teacher_name
                FROM   classes c
                JOIN   academic_years ay ON ay.id = c.academic_year_id
                LEFT JOIN teacher_profiles tp ON tp.id = c.homeroom_teacher_id
                WHERE  c.academic_year_id = ?
                ORDER  BY c.grade_level, c.section
                """;
        List<SchoolClass> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, academicYearId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** Returns all classes across all years — used by other modules' dropdowns. */
    public List<SchoolClass> getAll() throws SQLException {
        String sql = """
                SELECT c.id, c.academic_year_id, c.grade_level, c.section,
                       c.homeroom_teacher_id,
                       ay.year_label,
                       CONCAT(tp.first_name, ' ', tp.last_name) AS teacher_name
                FROM   classes c
                JOIN   academic_years ay ON ay.id = c.academic_year_id
                LEFT JOIN teacher_profiles tp ON tp.id = c.homeroom_teacher_id
                ORDER  BY ay.start_date DESC, c.grade_level, c.section
                """;
        List<SchoolClass> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /** All teacher profiles — used by homeroom teacher and class-subject dropdowns. */
    public List<TeacherProfile> getAllTeachers() throws SQLException {
        String sql = """
                SELECT id, user_id, first_name, last_name, phone
                FROM   teacher_profiles
                ORDER  BY last_name, first_name
                """;
        List<TeacherProfile> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                TeacherProfile tp = new TeacherProfile();
                tp.setId       (rs.getInt   ("id"));
                tp.setUserId   (rs.getInt   ("user_id"));
                tp.setFirstName(rs.getString("first_name"));
                tp.setLastName (rs.getString("last_name"));
                tp.setPhone    (rs.getString("phone"));
                list.add(tp);
            }
        }
        return list;
    }

    // ── Write ─────────────────────────────────────────────────

    public boolean insert(SchoolClass c) throws SQLException {
        String sql = """
                INSERT INTO classes (academic_year_id, grade_level, section, homeroom_teacher_id)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, c.getAcademicYearId());
            ps.setInt(2, c.getGradeLevel());
            ps.setString(3, c.getSection());
            if (c.getHomeroomTeacherId() > 0) ps.setInt(4, c.getHomeroomTeacherId());
            else                               ps.setNull(4, Types.INTEGER);
            int rows = ps.executeUpdate();
            if (rows == 0) return false;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) c.setId(keys.getInt(1));
            }
            return true;
        }
    }

    public boolean update(SchoolClass c) throws SQLException {
        String sql = """
                UPDATE classes
                SET grade_level = ?, section = ?, homeroom_teacher_id = ?
                WHERE id = ?
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt   (1, c.getGradeLevel());
            ps.setString(2, c.getSection());
            if (c.getHomeroomTeacherId() > 0) ps.setInt (3, c.getHomeroomTeacherId());
            else                               ps.setNull(3, Types.INTEGER);
            ps.setInt(4, c.getId());
            return ps.executeUpdate() > 0;
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private SchoolClass map(ResultSet rs) throws SQLException {
        SchoolClass c = new SchoolClass();
        c.setId               (rs.getInt   ("id"));
        c.setAcademicYearId   (rs.getInt   ("academic_year_id"));
        c.setGradeLevel       (rs.getInt   ("grade_level"));
        c.setSection          (rs.getString("section"));
        c.setHomeroomTeacherId(rs.getInt   ("homeroom_teacher_id")); // 0 if NULL
        c.setYearLabel        (rs.getString("year_label"));
        c.setTeacherName      (rs.getString("teacher_name"));
        return c;
    }
}