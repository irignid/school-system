package com.school.dao;

import com.school.model.Student;
import com.school.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClassEnrollmentDAO {

    /** Students currently enrolled in a class. */
    public List<Student> getEnrolled(int classId) throws SQLException {
        String sql = """
                SELECT s.id, s.first_name, s.last_name, s.first_name_si, s.gender, s.nic
                FROM   students s
                JOIN   class_enrollments ce ON ce.student_id = s.id
                WHERE  ce.class_id = ? AND s.is_active = TRUE
                ORDER  BY s.last_name, s.first_name
                """;
        return fetchStudents(sql, classId);
    }

    /**
     * Active students NOT yet enrolled in the given class.
     * Filters by grade level of the class so the list stays relevant.
     */
    public List<Student> getAvailable(int classId, int gradeLevel) throws SQLException {
        String sql = """
                SELECT s.id, s.first_name, s.last_name, s.first_name_si, s.gender, s.nic
                FROM   students s
                WHERE  s.is_active = TRUE
                  AND  s.id NOT IN (
                       SELECT student_id FROM class_enrollments WHERE class_id = ?
                  )
                ORDER  BY s.last_name, s.first_name
                """;
        return fetchStudents(sql, classId);
    }

    /** Enroll a list of students into a class in one batch. */
    public void enroll(int classId, List<Integer> studentIds) throws SQLException {
        String sql = "INSERT IGNORE INTO class_enrollments (student_id, class_id) VALUES (?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int sid : studentIds) {
                ps.setInt(1, sid);
                ps.setInt(2, classId);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /** Remove a single student from a class. */
    public boolean unenroll(int classId, int studentId) throws SQLException {
        String sql = "DELETE FROM class_enrollments WHERE class_id = ? AND student_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, classId);
            ps.setInt(2, studentId);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Helper ────────────────────────────────────────────────

    private List<Student> fetchStudents(String sql, int classId) throws SQLException {
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
                    s.setGender     (rs.getString("gender"));
                    s.setNic        (rs.getString("nic"));
                    list.add(s);
                }
            }
        }
        return list;
    }
}