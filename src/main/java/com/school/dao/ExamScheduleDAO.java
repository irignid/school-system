package com.school.dao;

import com.school.model.ExamSchedule;
import com.school.model.Subject;
import com.school.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExamScheduleDAO {

    public List<ExamSchedule> getByExam(int examId) throws SQLException {
        String sql = """
                SELECT es.id, es.exam_id, es.subject_id, es.date, es.start_time, es.duration_minutes,
                       s.name AS subject_name, s.name_si AS subject_name_si
                FROM   exam_schedules es
                JOIN   subjects s ON s.id = es.subject_id
                WHERE  es.exam_id = ?
                ORDER  BY es.date, es.start_time
                """;
        List<ExamSchedule> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /**
     * Subjects assigned to this exam's class that do NOT yet have a schedule entry.
     */
    public List<Subject> getUnscheduledSubjects(int examId, int classId) throws SQLException {
        String sql = """
                SELECT s.id, s.name, s.name_si, s.grade_level
                FROM   class_subjects cs
                JOIN   subjects s ON s.id = cs.subject_id
                WHERE  cs.class_id = ?
                  AND  s.id NOT IN (SELECT subject_id FROM exam_schedules WHERE exam_id = ?)
                ORDER  BY s.name
                """;
        List<Subject> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, classId);
            ps.setInt(2, examId);
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

    public boolean insert(ExamSchedule es) throws SQLException {
        String sql = """
                INSERT INTO exam_schedules (exam_id, subject_id, date, start_time, duration_minutes)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt (1, es.getExamId());
            ps.setInt (2, es.getSubjectId());
            ps.setDate(3, Date.valueOf(es.getDate()));
            ps.setTime(4, Time.valueOf(es.getStartTime()));
            ps.setInt (5, es.getDurationMinutes());
            int rows = ps.executeUpdate();
            if (rows == 0) return false;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) es.setId(keys.getInt(1));
            }
            return true;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM exam_schedules WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private ExamSchedule map(ResultSet rs) throws SQLException {
        ExamSchedule es = new ExamSchedule();
        es.setId             (rs.getInt   ("id"));
        es.setExamId         (rs.getInt   ("exam_id"));
        es.setSubjectId      (rs.getInt   ("subject_id"));
        es.setDate           (rs.getDate  ("date").toLocalDate());
        es.setStartTime      (rs.getTime  ("start_time").toLocalTime());
        es.setDurationMinutes(rs.getInt   ("duration_minutes"));
        es.setSubjectName    (rs.getString("subject_name"));
        es.setSubjectNameSi  (rs.getString("subject_name_si"));
        return es;
    }
}