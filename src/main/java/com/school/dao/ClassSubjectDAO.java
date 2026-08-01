package com.school.dao;

import com.school.model.ClassSubject;
import com.school.model.Subject;
import com.school.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClassSubjectDAO {

    /**
     * Returns all subject-teacher assignments for a class,
     * with subject name and teacher name resolved via JOIN.
     */
    public List<ClassSubject> getByClass(int classId) throws SQLException {
        String sql = """
                SELECT cs.id, cs.class_id, cs.subject_id, cs.teacher_id,
                       s.name AS subject_name, s.name_si AS subject_name_si,
                       CONCAT(tp.first_name, ' ', tp.last_name) AS teacher_name
                FROM   class_subjects cs
                JOIN   subjects        s  ON s.id  = cs.subject_id
                JOIN   teacher_profiles tp ON tp.id = cs.teacher_id
                WHERE  cs.class_id = ?
                ORDER  BY s.name
                """;
        List<ClassSubject> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, classId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /**
     * Returns subjects for a given grade level that have NOT yet been
     * assigned to the specified class — used to populate the "assign" dialog.
     */
    public List<Subject> getUnassignedSubjects(int classId, int gradeLevel) throws SQLException {
        String sql = """
                SELECT s.id, s.name, s.name_si, s.grade_level
                FROM   subjects s
                WHERE  s.grade_level = ?
                  AND  s.id NOT IN (
                       SELECT subject_id FROM class_subjects WHERE class_id = ?
                  )
                ORDER  BY s.name
                """;
        List<Subject> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, gradeLevel);
            ps.setInt(2, classId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Subject s = new Subject();
                    s.setId        (rs.getInt   ("id"));
                    s.setName      (rs.getString("name"));
                    s.setNameSi    (rs.getString("name_si"));
                    s.setGradeLevel(rs.getInt   ("grade_level"));
                    list.add(s);
                }
            }
        }
        return list;
    }

    public boolean insert(ClassSubject cs) throws SQLException {
        String sql = "INSERT INTO class_subjects (class_id, subject_id, teacher_id) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, cs.getClassId());
            ps.setInt(2, cs.getSubjectId());
            ps.setInt(3, cs.getTeacherId());
            int rows = ps.executeUpdate();
            if (rows == 0) return false;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) cs.setId(keys.getInt(1));
            }
            return true;
        }
    }

    /** Updates only the assigned teacher (subject cannot change — delete and re-add instead). */
    public boolean update(ClassSubject cs) throws SQLException {
        String sql = "UPDATE class_subjects SET teacher_id = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cs.getTeacherId());
            ps.setInt(2, cs.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM class_subjects WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private ClassSubject map(ResultSet rs) throws SQLException {
        ClassSubject cs = new ClassSubject();
        cs.setId           (rs.getInt   ("id"));
        cs.setClassId      (rs.getInt   ("class_id"));
        cs.setSubjectId    (rs.getInt   ("subject_id"));
        cs.setTeacherId    (rs.getInt   ("teacher_id"));
        cs.setSubjectName  (rs.getString("subject_name"));
        cs.setSubjectNameSi(rs.getString("subject_name_si"));
        cs.setTeacherName  (rs.getString("teacher_name"));
        return cs;
    }
}