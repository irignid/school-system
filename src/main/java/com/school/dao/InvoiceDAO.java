package com.school.dao;

import com.school.model.Invoice;
import com.school.model.Payment;
import com.school.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InvoiceDAO {

    // ── Invoices ──────────────────────────────────────────────

    /**
     * All invoices with totals from the view, optionally filtered by status.
     *
     * @param status null = all statuses
     */
    public List<Invoice> getAll(String status) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT invoice_id, student_id, student_name, fee_structure_id,
                       issued_date, due_date, discount_amount, status,
                       total_fee, net_payable, total_paid, outstanding
                FROM   v_invoice_summary
                """);
        if (status != null) sql.append(" WHERE status = ?");
        sql.append(" ORDER BY due_date ASC, student_name");

        List<Invoice> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            if (status != null) ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapInvoice(rs));
            }
        }
        return list;
    }

    /** Invoices for a specific student. */
    public List<Invoice> getByStudent(int studentId) throws SQLException {
        String sql = """
                SELECT invoice_id, student_id, student_name, fee_structure_id,
                       issued_date, due_date, discount_amount, status,
                       total_fee, net_payable, total_paid, outstanding
                FROM   v_invoice_summary
                WHERE  student_id = ?
                ORDER  BY issued_date DESC
                """;
        List<Invoice> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapInvoice(rs));
            }
        }
        return list;
    }

    /** Search invoices by student name fragment. */
    public List<Invoice> search(String query) throws SQLException {
        String sql = """
                SELECT invoice_id, student_id, student_name, fee_structure_id,
                       issued_date, due_date, discount_amount, status,
                       total_fee, net_payable, total_paid, outstanding
                FROM   v_invoice_summary
                WHERE  student_name LIKE ?
                ORDER  BY student_name, due_date
                """;
        List<Invoice> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + query.trim() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapInvoice(rs));
            }
        }
        return list;
    }

    /**
     * Generates an invoice for a student against a fee structure.
     * Uses UNIQUE KEY uq_invoice(student_id, fee_structure_id) — safe to call
     * per student; will fail with a duplicate key error if already generated.
     */
    public boolean generateInvoice(int studentId, int feeStructureId,
                                    LocalDate issuedDate, LocalDate dueDate,
                                    double discountAmount, String discountReason) throws SQLException {
        String sql = """
                INSERT INTO invoices
                    (student_id, fee_structure_id, issued_date, due_date,
                     discount_amount, discount_reason, status)
                VALUES (?, ?, ?, ?, ?, ?, 'unpaid')
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt   (1, studentId);
            ps.setInt   (2, feeStructureId);
            ps.setDate  (3, Date.valueOf(issuedDate));
            ps.setDate  (4, Date.valueOf(dueDate));
            ps.setDouble(5, discountAmount);
            ps.setString(6, discountReason == null || discountReason.isBlank() ? null : discountReason.trim());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Generates invoices for ALL active students enrolled in classes
     * matching the grade level of a fee structure.
     *
     * @return count of invoices inserted
     */
    public int generateBulkInvoices(int feeStructureId, int gradeLevel, int termId,
                                     LocalDate issuedDate, LocalDate dueDate) throws SQLException {
        // Find eligible students: enrolled in a current-year class at this grade,
        // who do NOT yet have an invoice for this fee structure
        String findSql = """
                SELECT DISTINCT s.id
                FROM   students s
                JOIN   class_enrollments ce ON ce.student_id = s.id
                JOIN   classes c ON c.id = ce.class_id
                JOIN   academic_years ay ON ay.id = c.academic_year_id
                WHERE  ay.is_current = TRUE
                  AND  c.grade_level  = ?
                  AND  s.is_active    = TRUE
                  AND  s.id NOT IN (
                       SELECT student_id FROM invoices WHERE fee_structure_id = ?
                  )
                """;
        String insertSql = """
                INSERT INTO invoices
                    (student_id, fee_structure_id, issued_date, due_date,
                     discount_amount, status)
                VALUES (?, ?, ?, ?, 0, 'unpaid')
                """;

        int count = 0;
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                List<Integer> studentIds = new ArrayList<>();
                try (PreparedStatement ps = con.prepareStatement(findSql)) {
                    ps.setInt(1, gradeLevel);
                    ps.setInt(2, feeStructureId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) studentIds.add(rs.getInt(1));
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                    for (int sid : studentIds) {
                        ps.setInt  (1, sid);
                        ps.setInt  (2, feeStructureId);
                        ps.setDate (3, Date.valueOf(issuedDate));
                        ps.setDate (4, Date.valueOf(dueDate));
                        ps.addBatch();
                        count++;
                    }
                    ps.executeBatch();
                }
                con.commit();
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        }
        return count;
    }

    /**
     * Updates the invoice status based on payments received.
     * Recalculates: unpaid / partial / paid. Overdue must be set separately
     * (done at load time by comparing due_date with today).
     */
    public boolean refreshStatus(int invoiceId) throws SQLException {
        String sql = """
                UPDATE invoices i
                JOIN (
                    SELECT i2.id,
                        COALESCE(fi_sum.total_fee, 0) - i2.discount_amount AS net_payable,
                        COALESCE(p_sum.total_paid, 0)                       AS total_paid
                    FROM   invoices i2
                    LEFT JOIN (
                        SELECT fee_structure_id, SUM(amount) AS total_fee
                        FROM   fee_items GROUP BY fee_structure_id
                    ) fi_sum ON fi_sum.fee_structure_id = i2.fee_structure_id
                    LEFT JOIN (
                        SELECT invoice_id, SUM(amount) AS total_paid
                        FROM   payments GROUP BY invoice_id
                    ) p_sum ON p_sum.invoice_id = i2.id
                    WHERE i2.id = ?
                ) calc ON calc.id = i.id
                SET i.status = CASE
                    WHEN calc.total_paid = 0                       THEN 'unpaid'
                    WHEN calc.total_paid >= calc.net_payable       THEN 'paid'
                    ELSE 'partial'
                END
                WHERE i.id = ?
                """;
        try (Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            ps.setInt(2, invoiceId);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Payments ──────────────────────────────────────────────

    public List<Payment> getPaymentsByInvoice(int invoiceId) throws SQLException {
        String sql = """
                SELECT id, invoice_id, amount, payment_date, method, receipt_number,
                       recorded_by, recorded_at
                FROM   payments
                WHERE  invoice_id = ?
                ORDER  BY payment_date DESC, recorded_at DESC
                """;
        List<Payment> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapPayment(rs));
            }
        }
        return list;
    }

    public boolean recordPayment(Payment p) throws SQLException {
        String sql = """
                INSERT INTO payments (invoice_id, amount, payment_date, method, receipt_number, recorded_by)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1, p.getInvoiceId());
            ps.setDouble(2, p.getAmount());
            ps.setDate  (3, Date.valueOf(p.getPaymentDate()));
            ps.setString(4, p.getMethod());
            ps.setString(5, nullIfBlank(p.getReceiptNumber()));
            ps.setInt   (6, p.getRecordedBy());
            int rows = ps.executeUpdate();
            if (rows == 0) return false;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) p.setId(keys.getInt(1));
            }
            // Update invoice status after payment
            refreshStatus(p.getInvoiceId());
            return true;
        }
    }

    public boolean deletePayment(int paymentId, int invoiceId) throws SQLException {
        String sql = "DELETE FROM payments WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, paymentId);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) refreshStatus(invoiceId);
            return ok;
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private Invoice mapInvoice(ResultSet rs) throws SQLException {
        Invoice inv = new Invoice();
        inv.setId             (rs.getInt   ("invoice_id"));
        inv.setStudentId      (rs.getInt   ("student_id"));
        inv.setStudentName    (rs.getString("student_name"));
        inv.setFeeStructureId (rs.getInt   ("fee_structure_id"));
        Date issued = rs.getDate("issued_date");
        if (issued != null) inv.setIssuedDate(issued.toLocalDate());
        Date due = rs.getDate("due_date");
        if (due != null) inv.setDueDate(due.toLocalDate());
        inv.setDiscountAmount (rs.getDouble("discount_amount"));
        inv.setStatus         (rs.getString("status"));
        inv.setTotalFee       (rs.getDouble("total_fee"));
        inv.setNetPayable     (rs.getDouble("net_payable"));
        inv.setTotalPaid      (rs.getDouble("total_paid"));
        inv.setOutstanding    (rs.getDouble("outstanding"));
        return inv;
    }

    private Payment mapPayment(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setId           (rs.getInt      ("id"));
        p.setInvoiceId    (rs.getInt      ("invoice_id"));
        p.setAmount       (rs.getDouble   ("amount"));
        Date pd = rs.getDate("payment_date");
        if (pd != null) p.setPaymentDate(pd.toLocalDate());
        p.setMethod       (rs.getString   ("method"));
        p.setReceiptNumber(rs.getString   ("receipt_number"));
        p.setRecordedBy   (rs.getInt      ("recorded_by"));
        Timestamp ts = rs.getTimestamp("recorded_at");
        if (ts != null) p.setRecordedAt(ts.toLocalDateTime());
        return p;
    }

    private String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
