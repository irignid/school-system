package com.school.dao;

import com.school.model.Mark;
import com.school.model.Student;
import com.school.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReportCardDAO {

    /**
     * Student info + their current class display for a given academic year.
     */
    public Student getStudentWithClass(int studentId) throws SQLException {
        String sql = """
                SELECT s.id, s.first_name, s.last_name, s.first_name_si,
                       s.date_of_birth, s.gender, s.nic,
                       CONCAT('Grade ', c.grade_level, ' - ', c.section) AS class_display,
                       ay.year_label
                FROM   students s
                LEFT JOIN class_enrollments ce ON ce.student_id = s.id
                LEFT JOIN classes c ON c.id = ce.class_id
                LEFT JOIN academic_years ay ON ay.id = c.academic_year_id AND ay.is_current = TRUE
                WHERE  s.id = ?
                LIMIT  1
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Student s = new Student();
                s.setId         (rs.getInt   ("id"));
                s.setFirstName  (rs.getString("first_name"));
                s.setLastName   (rs.getString("last_name"));
                s.setFirstNameSi(rs.getString("first_name_si"));
                Date dob = rs.getDate("date_of_birth");
                if (dob != null) s.setDateOfBirth(dob.toLocalDate());
                s.setGender(rs.getString("gender"));
                s.setNic   (rs.getString("nic"));
                // Stash class display and year in address field temporarily —
                // avoids adding extra fields to Student model
                s.setAddress(rs.getString("class_display") + "|" + rs.getString("year_label"));
                return s;
            }
        }
    }

    /**
     * All marks for a student across ALL exams in a given term,
     * with auto-calculated grade from v_marks_with_grade.
     * Groups by subject, taking the latest exam mark if multiple exams exist.
     */
    public List<Mark> getMarksForTerm(int studentId, int termId) throws SQLException {
        String sql = """
                SELECT vmg.subject_id, vmg.subject_name,
                       vmg.exam_name,
                       vmg.mark_obtained, vmg.max_mark,
                       ROUND(vmg.mark_obtained / vmg.max_mark * 100, 2) AS percentage,
                       vmg.grade_symbol
                FROM   v_marks_with_grade vmg
                JOIN   exams e ON e.id = vmg.exam_id
                WHERE  vmg.student_id = ?
                  AND  e.term_id      = ?
                ORDER  BY vmg.subject_name, e.created_at
                """;
        List<Mark> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, termId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Mark mk = new Mark();
                    mk.setStudentId   (studentId);
                    mk.setSubjectId   (rs.getInt   ("subject_id"));
                    mk.setSubjectName (rs.getString("subject_name"));
                    mk.setExamName    (rs.getString("exam_name"));
                    mk.setMarkObtained(rs.getDouble ("mark_obtained"));
                    mk.setMaxMark     (rs.getDouble ("max_mark"));
                    mk.setGradeSymbol (rs.getString("grade_symbol"));
                    list.add(mk);
                }
            }
        }
        return list;
    }

    /**
     * Attendance summary for a student in a given class.
     * Returns int[5]: { total, present, absent, late, medicalLeave }
     */
    public int[] getAttendanceSummary(int studentId, int classId) throws SQLException {
        String sql = """
                SELECT total_periods, present, absent, late, medical_leave
                FROM   v_attendance_summary
                WHERE  student_id = ? AND class_id = ?
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, classId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new int[]{
                            rs.getInt("total_periods"),
                            rs.getInt("present"),
                            rs.getInt("absent"),
                            rs.getInt("late"),
                            rs.getInt("medical_leave")
                    };
                }
            }
        }
        return new int[]{0, 0, 0, 0, 0};
    }

    /**
     * Active students enrolled in a class, for the report card student picker.
     */
    public List<Student> getEnrolledStudents(int classId) throws SQLException {
        String sql = """
                SELECT s.id, s.first_name, s.last_name, s.first_name_si
                FROM   students s
                JOIN   class_enrollments ce ON ce.student_id = s.id
                WHERE  ce.class_id = ? AND s.is_active = TRUE
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

    /** Term display label — e.g. "Term 1 — 2025/2026". */
    public String getTermLabel(int termId) throws SQLException {
        String sql = """
                SELECT CONCAT('Term ', t.term_number, ' - ', ay.year_label) AS label
                FROM   terms t
                JOIN   academic_years ay ON ay.id = t.academic_year_id
                WHERE  t.id = ?
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, termId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("label") : "Term";
            }
        }
    }
}
