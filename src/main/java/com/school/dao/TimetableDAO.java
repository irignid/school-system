package com.school.dao;

import com.school.model.PeriodConfig;
import com.school.model.TimetableEntry;
import com.school.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimetableDAO {

    // ── Period config ─────────────────────────────────────────

    public List<PeriodConfig> getPeriodConfigs() throws SQLException {
        String sql = """
                SELECT period, label, start_time, end_time, is_interval
                FROM   period_config
                ORDER  BY period
                """;
        List<PeriodConfig> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                PeriodConfig pc = new PeriodConfig();
                pc.setPeriod   (rs.getInt    ("period"));
                pc.setLabel    (rs.getString ("label"));
                pc.setStartTime(rs.getTime   ("start_time").toLocalTime());
                pc.setEndTime  (rs.getTime   ("end_time").toLocalTime());
                pc.setInterval (rs.getBoolean("is_interval"));
                list.add(pc);
            }
        }
        return list;
    }

    // ── Timetable entries ─────────────────────────────────────

    /**
     * All timetable entries for a class, keyed by "day_period"
     * for O(1) lookup when building the grid.
     */
    public Map<String, TimetableEntry> getByClass(int classId) throws SQLException {
        String sql = """
                SELECT t.id, t.class_id, t.day, t.period,
                       t.subject_id, t.teacher_id,
                       s.name  AS subject_name,
                       CONCAT(tp.first_name, ' ', tp.last_name) AS teacher_name
                FROM   timetable t
                JOIN   subjects         s  ON s.id  = t.subject_id
                JOIN   teacher_profiles tp ON tp.id = t.teacher_id
                WHERE  t.class_id = ?
                """;
        Map<String, TimetableEntry> map = new HashMap<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, classId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TimetableEntry e = map(rs);
                    map.put(e.getSlotKey(), e);
                }
            }
        }
        return map;
    }

    /**
     * All timetable entries for a specific teacher across all classes.
     * Used by the teacher dashboard read-only view.
     * Returns map keyed by "classId_day_period".
     */
    public List<TimetableEntry> getByTeacher(int teacherProfileId) throws SQLException {
        String sql = """
                SELECT t.id, t.class_id, t.day, t.period,
                       t.subject_id, t.teacher_id,
                       s.name  AS subject_name,
                       CONCAT(tp.first_name, ' ', tp.last_name) AS teacher_name
                FROM   timetable t
                JOIN   subjects         s  ON s.id  = t.subject_id
                JOIN   teacher_profiles tp ON tp.id = t.teacher_id
                WHERE  t.teacher_id = ?
                ORDER  BY t.day, t.period
                """;
        List<TimetableEntry> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, teacherProfileId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /**
     * Upserts a timetable slot.
     * INSERT … ON DUPLICATE KEY UPDATE handles re-assignment safely.
     */
    public boolean upsert(TimetableEntry e) throws SQLException {
        String sql = """
                INSERT INTO timetable (class_id, day, period, subject_id, teacher_id)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    subject_id = VALUES(subject_id),
                    teacher_id = VALUES(teacher_id)
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, e.getClassId());
            ps.setInt(2, e.getDay());
            ps.setInt(3, e.getPeriod());
            ps.setInt(4, e.getSubjectId());
            ps.setInt(5, e.getTeacherId());
            int rows = ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) e.setId(keys.getInt(1));
            }
            return rows > 0;
        }
    }

    /** Clears a single slot. */
    public boolean delete(int classId, int day, int period) throws SQLException {
        String sql = "DELETE FROM timetable WHERE class_id = ? AND day = ? AND period = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, classId);
            ps.setInt(2, day);
            ps.setInt(3, period);
            return ps.executeUpdate() > 0;
        }
    }

    /** Clears the entire timetable for a class. */
    public boolean deleteByClass(int classId) throws SQLException {
        String sql = "DELETE FROM timetable WHERE class_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, classId);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Helper ────────────────────────────────────────────────

    private TimetableEntry map(ResultSet rs) throws SQLException {
        TimetableEntry e = new TimetableEntry();
        e.setId         (rs.getInt   ("id"));
        e.setClassId    (rs.getInt   ("class_id"));
        e.setDay        (rs.getInt   ("day"));
        e.setPeriod     (rs.getInt   ("period"));
        e.setSubjectId  (rs.getInt   ("subject_id"));
        e.setTeacherId  (rs.getInt   ("teacher_id"));
        e.setSubjectName(rs.getString("subject_name"));
        e.setTeacherName(rs.getString("teacher_name"));
        return e;
    }
}
