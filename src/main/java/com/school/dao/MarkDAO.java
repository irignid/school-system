package com.school.dao;

import com.school.model.Mark;
import com.school.model.Student;
import com.school.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MarkDAO {

    /**
     * Enrolled students for a class with their existing mark for an exam+subject.
     * Students with no mark yet have markObtained = -1 as a sentinel.
     */
    public List<Mark> getMarksForEntry(int classId, int examId, int subjectId) throws SQLException {
        String sql = """
                SELECT s.id AS student_id,
                       CONCAT(s.first_name, ' ', s.last_name) AS student_name,
                       s.first_name_si,
                       COALESCE(m.id, 0)            AS mark_id,
                       COALESCE(m.mark_obtained, -1) AS mark_obtained,
                       COALESCE(m.max_mark, 100)    AS max_mark
                FROM   students s
                JOIN   class_enrollments ce ON ce.student_id = s.id AND ce.class_id = ?
                LEFT JOIN marks m ON m.student_id = s.id
                              AND m.exam_id = ? AND m.subject_id = ?
                WHERE  s.is_active = TRUE
                ORDER  BY s.last_name, s.first_name
                """;
        List<Mark> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, classId);
            ps.setInt(2, examId);
            ps.setInt(3, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Mark mk = new Mark();
                    mk.setId          (rs.getInt   ("mark_id"));
                    mk.setStudentId   (rs.getInt   ("student_id"));
                    mk.setStudentName (rs.getString("student_name"));
                    mk.setExamId      (examId);
                    mk.setSubjectId   (subjectId);
                    mk.setMarkObtained(rs.getDouble("mark_obtained"));
                    mk.setMaxMark     (rs.getDouble("max_mark"));
                    list.add(mk);
                }
            }
        }
        return list;
    }

    /**
     * All marks for an exam with auto-calculated grades (uses the DB view).
     */
    public List<Mark> getMarksWithGrades(int examId) throws SQLException {
        String sql = """
                SELECT id, student_id, student_name, exam_id, exam_name,
                       subject_id, subject_name, mark_obtained, max_mark,
                       percentage, grade_symbol
                FROM   v_marks_with_grade
                WHERE  exam_id = ?
                ORDER  BY student_name, subject_name
                """;
        List<Mark> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Mark mk = new Mark();
                    mk.setId          (rs.getInt   ("id"));
                    mk.setStudentId   (rs.getInt   ("student_id"));
                    mk.setStudentName (rs.getString("student_name"));
                    mk.setExamId      (rs.getInt   ("exam_id"));
                    mk.setExamName    (rs.getString("exam_name"));
                    mk.setSubjectId   (rs.getInt   ("subject_id"));
                    mk.setSubjectName (rs.getString("subject_name"));
                    mk.setMarkObtained(rs.getDouble("mark_obtained"));
                    mk.setMaxMark     (rs.getDouble("max_mark"));
                    mk.setGradeSymbol (rs.getString("grade_symbol"));
                    list.add(mk);
                }
            }
        }
        return list;
    }

    /**
     * Batch upsert — INSERT or UPDATE for each mark.
     *
     * @param marks       marks to save (markObtained == -1 means skip — student left blank)
     * @param enteredBy   users.id of whoever is submitting
     */
    public void saveAll(List<Mark> marks, int enteredBy) throws SQLException {
        String sql = """
                INSERT INTO marks (student_id, exam_id, subject_id, mark_obtained, max_mark, entered_by)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    mark_obtained = VALUES(mark_obtained),
                    max_mark      = VALUES(max_mark),
                    entered_by    = VALUES(entered_by),
                    entered_at    = CURRENT_TIMESTAMP
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (Mark mk : marks) {
                if (mk.getMarkObtained() < 0) continue;   // skip blank rows
                ps.setInt   (1, mk.getStudentId());
                ps.setInt   (2, mk.getExamId());
                ps.setInt   (3, mk.getSubjectId());
                ps.setDouble(4, mk.getMarkObtained());
                ps.setDouble(5, mk.getMaxMark());
                ps.setInt   (6, enteredBy);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM marks WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}