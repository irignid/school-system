package com.school.dao;

import com.school.model.GradeThreshold;
import com.school.model.Invoice;
import com.school.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DashboardDAO {

    // ══════════════════════════════════════════════════════════
    //  PRINCIPAL STATS
    // ══════════════════════════════════════════════════════════

    public int getTotalActiveStudents() throws SQLException {
        String sql = "SELECT COUNT(*) FROM students WHERE is_active = TRUE";
        return querySingleInt(sql);
    }

    public int getActiveClassCount() throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM classes c
                JOIN academic_years ay ON ay.id = c.academic_year_id
                WHERE ay.is_current = TRUE
                """;
        return querySingleInt(sql);
    }

    /** Attendance percentage across ALL classes for today. Returns -1 if no records. */
    public double getTodayAttendancePct() throws SQLException {
        String sql = """
                SELECT COUNT(*) AS total,
                       SUM(status = 'P') AS present
                FROM attendance
                WHERE date = ?
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(LocalDate.now()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int total = rs.getInt("total");
                    if (total == 0) return -1;
                    return rs.getDouble("present") / total * 100;
                }
            }
        }
        return -1;
    }

    public double getTotalOutstandingFees() throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(outstanding), 0)
                FROM v_invoice_summary
                WHERE status != 'paid'
                """;
        return querySingleDouble(sql);
    }

    // ══════════════════════════════════════════════════════════
    //  TEACHER STATS
    // ══════════════════════════════════════════════════════════

    /** Number of distinct classes a teacher is assigned to in the current year. */
    public int getTeacherClassCount(int teacherProfileId) throws SQLException {
        String sql = """
                SELECT COUNT(DISTINCT c.id)
                FROM classes c
                JOIN academic_years ay ON ay.id = c.academic_year_id
                LEFT JOIN class_subjects cs ON cs.class_id = c.id
                WHERE ay.is_current = TRUE
                  AND (cs.teacher_id = ? OR c.homeroom_teacher_id = ?)
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, teacherProfileId);
            ps.setInt(2, teacherProfileId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Total students across all classes a teacher is assigned to. */
    public int getTeacherStudentCount(int teacherProfileId) throws SQLException {
        String sql = """
                SELECT COUNT(DISTINCT ce.student_id)
                FROM class_enrollments ce
                JOIN classes c ON c.id = ce.class_id
                JOIN academic_years ay ON ay.id = c.academic_year_id
                LEFT JOIN class_subjects cs ON cs.class_id = c.id
                WHERE ay.is_current = TRUE
                  AND (cs.teacher_id = ? OR c.homeroom_teacher_id = ?)
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, teacherProfileId);
            ps.setInt(2, teacherProfileId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Number of periods assigned to this teacher in the timetable for today. */
    public int getTeacherPeriodsMarkedToday(int teacherProfileId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM   timetable t
                JOIN   period_config pc ON pc.period = t.period
                WHERE  t.teacher_id = ?
                AND  t.day        = DAYOFWEEK(CURDATE()) - 1
                AND  DAYOFWEEK(CURDATE()) BETWEEN 2 AND 6
                AND  pc.is_interval = FALSE
                """;
        try (Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, teacherProfileId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Classes + subject count for the teacher's assignment overview table.
     * Returns rows: grade_level, section, subject_count
     */
    public List<String[]> getTeacherClassSummary(int teacherProfileId) throws SQLException {
        String sql = """
                SELECT c.grade_level, c.section,
                       COUNT(cs.id) AS subject_count
                FROM   classes c
                JOIN   academic_years ay ON ay.id = c.academic_year_id
                LEFT JOIN class_subjects cs
                       ON cs.class_id = c.id AND cs.teacher_id = ?
                WHERE  ay.is_current = TRUE
                  AND  (cs.teacher_id = ? OR c.homeroom_teacher_id = ?)
                GROUP  BY c.id, c.grade_level, c.section
                ORDER  BY c.grade_level, c.section
                """;
        List<String[]> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, teacherProfileId);
            ps.setInt(2, teacherProfileId);
            ps.setInt(3, teacherProfileId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new String[]{
                            "Grade " + rs.getInt("grade_level") + " - " + rs.getString("section"),
                            String.valueOf(rs.getInt("subject_count"))
                    });
                }
            }
        }
        return list;
    }

    // ══════════════════════════════════════════════════════════
    //  STAFF STATS
    // ══════════════════════════════════════════════════════════

    public int getUnpaidInvoiceCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM invoices WHERE status IN ('unpaid','overdue')";
        return querySingleInt(sql);
    }

    public double getCollectedThisMonth() throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(amount), 0)
                FROM payments
                WHERE MONTH(payment_date) = MONTH(CURDATE())
                  AND YEAR(payment_date)  = YEAR(CURDATE())
                """;
        return querySingleDouble(sql);
    }

    /** Top overdue/unpaid invoices for the staff dashboard list. */
    public List<Invoice> getOverdueInvoices(int limit) throws SQLException {
        String sql = """
                SELECT invoice_id, student_id, student_name, fee_structure_id,
                       issued_date, due_date, discount_amount, status,
                       total_fee, net_payable, total_paid, outstanding
                FROM   v_invoice_summary
                WHERE  status IN ('unpaid','overdue','partial')
                  AND  outstanding > 0
                ORDER  BY due_date ASC
                LIMIT  ?
                """;
        List<Invoice> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapInvoice(rs));
            }
        }
        return list;
    }

    // ══════════════════════════════════════════════════════════
    //  GRADE THRESHOLDS (principal edits from dashboard)
    // ══════════════════════════════════════════════════════════

    public List<GradeThreshold> getThresholds() throws SQLException {
        String sql = "SELECT id, grade_symbol, min_mark, max_mark FROM grade_thresholds ORDER BY min_mark DESC";
        List<GradeThreshold> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                GradeThreshold gt = new GradeThreshold();
                gt.setId         (rs.getInt   ("id"));
                gt.setGradeSymbol(rs.getString("grade_symbol"));
                gt.setMinMark    (rs.getDouble ("min_mark"));
                gt.setMaxMark    (rs.getDouble ("max_mark"));
                list.add(gt);
            }
        }
        return list;
    }

    public boolean updateThreshold(GradeThreshold gt) throws SQLException {
        String sql = "UPDATE grade_thresholds SET min_mark = ?, max_mark = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, gt.getMinMark());
            ps.setDouble(2, gt.getMaxMark());
            ps.setInt   (3, gt.getId());
            return ps.executeUpdate() > 0;
        }
    }

    // ══════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════

    private int querySingleInt(String sql) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private double querySingleDouble(String sql) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }

    private Invoice mapInvoice(ResultSet rs) throws SQLException {
        Invoice inv = new Invoice();
        inv.setId            (rs.getInt   ("invoice_id"));
        inv.setStudentId     (rs.getInt   ("student_id"));
        inv.setStudentName   (rs.getString("student_name"));
        inv.setFeeStructureId(rs.getInt   ("fee_structure_id"));
        Date issued = rs.getDate("issued_date");
        if (issued != null) inv.setIssuedDate(issued.toLocalDate());
        Date due = rs.getDate("due_date");
        if (due != null) inv.setDueDate(due.toLocalDate());
        inv.setDiscountAmount(rs.getDouble("discount_amount"));
        inv.setStatus        (rs.getString("status"));
        inv.setTotalFee      (rs.getDouble("total_fee"));
        inv.setNetPayable    (rs.getDouble("net_payable"));
        inv.setTotalPaid     (rs.getDouble("total_paid"));
        inv.setOutstanding   (rs.getDouble("outstanding"));
        return inv;
    }
}
