package com.school.dao;

import com.school.model.Attendance;
import com.school.model.SchoolClass;
import com.school.model.Student;
import com.school.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttendanceDAO {

    // ── Classes ───────────────────────────────────────────────

    /**
     * Classes the given teacher is assigned to in the current academic year.
     * Includes both homeroom teacher role AND any class they teach a subject in.
     */
    public List<SchoolClass> getClassesForTeacher(int teacherProfileId) throws SQLException {
        String sql = """
                SELECT DISTINCT c.id, c.academic_year_id, c.grade_level, c.section,
                       c.homeroom_teacher_id, ay.year_label
                FROM   classes c
                JOIN   academic_years ay ON ay.id = c.academic_year_id
                LEFT JOIN class_subjects cs ON cs.class_id = c.id
                WHERE  ay.is_current = TRUE
                  AND  (cs.teacher_id = ? OR c.homeroom_teacher_id = ?)
                ORDER  BY c.grade_level, c.section
                """;
        List<SchoolClass> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, teacherProfileId);
            ps.setInt(2, teacherProfileId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapClass(rs));
            }
        }
        return list;
    }

    /** All classes in the current academic year — used by the principal view. */
    public List<SchoolClass> getAllCurrentClasses() throws SQLException {
        String sql = """
                SELECT c.id, c.academic_year_id, c.grade_level, c.section,
                       c.homeroom_teacher_id, ay.year_label
                FROM   classes c
                JOIN   academic_years ay ON ay.id = c.academic_year_id
                WHERE  ay.is_current = TRUE
                ORDER  BY c.grade_level, c.section
                """;
        List<SchoolClass> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapClass(rs));
        }
        return list;
    }

    // ── Students ──────────────────────────────────────────────

    /** Active students enrolled in a class, ordered by name. */
    public List<Student> getEnrolledStudents(int classId) throws SQLException {
        String sql = """
                SELECT s.id, s.first_name, s.last_name, s.first_name_si
                FROM   students s
                JOIN   class_enrollments ce ON ce.student_id = s.id
                WHERE  ce.class_id = ?
                  AND  s.is_active = TRUE
                ORDER  BY s.last_name, s.first_name
                """;
        List<Student> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, classId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Student s = new Student();
                    s.setId         (rs.getInt   ("id"));
                    s.setFirstName  (rs.getString("first_name"));
                    s.setLastName   (rs.getString("last_name"));
                    s.setFirstNameSi(rs.getString("first_name_si"));
                    list.add(s);
                }
            }
        }
        return list;
    }

    // ── Attendance records ────────────────────────────────────

    /**
     * Existing attendance for a class/date/period slot.
     *
     * @return map of student_id → status code ("P","A","L","ML")
     */
    public Map<Integer, String> getExisting(int classId, LocalDate date, int period) throws SQLException {
        String sql = """
                SELECT student_id, status
                FROM   attendance
                WHERE  class_id = ? AND date = ? AND period = ?
                """;
        Map<Integer, String> map = new HashMap<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt (1, classId);
            ps.setDate(2, Date.valueOf(date));
            ps.setInt (3, period);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) map.put(rs.getInt("student_id"), rs.getString("status"));
            }
        }
        return map;
    }

    /**
     * Saves (or re-saves) a full period's attendance in one batch.
     * Uses INSERT … ON DUPLICATE KEY UPDATE so re-submission is safe.
     *
     * @param records          list of Attendance objects (one per student)
     * @param teacherProfileId teacher_profiles.id of whoever is submitting
     */
    public void saveAll(List<Attendance> records, int teacherProfileId) throws SQLException {
        String sql = """
                INSERT INTO attendance (student_id, class_id, date, period, status, marked_by)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    status    = VALUES(status),
                    marked_by = VALUES(marked_by)
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            for (Attendance a : records) {
                ps.setInt   (1, a.getStudentId());
                ps.setInt   (2, a.getClassId());
                ps.setDate  (3, Date.valueOf(a.getDate()));
                ps.setInt   (4, a.getPeriod());
                ps.setString(5, a.getStatus());
                ps.setInt   (6, teacherProfileId);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ── Profile lookup ────────────────────────────────────────

    /**
     * Resolves a users.id → teacher_profiles.id.
     *
     * @return teacher_profiles.id, or 0 if the user has no teacher profile
     */
    public int getTeacherProfileId(int userId) throws SQLException {
        String sql = "SELECT id FROM teacher_profiles WHERE user_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id") : 0;
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private SchoolClass mapClass(ResultSet rs) throws SQLException {
        SchoolClass c = new SchoolClass();
        c.setId             (rs.getInt   ("id"));
        c.setAcademicYearId (rs.getInt   ("academic_year_id"));
        c.setGradeLevel     (rs.getInt   ("grade_level"));
        c.setSection        (rs.getString("section"));
        c.setHomeroomTeacherId(rs.getInt ("homeroom_teacher_id"));
        c.setYearLabel      (rs.getString("year_label"));
        return c;
    }
}