package com.school.model;

import java.time.LocalDate;

public class Invoice {

    private int       id;
    private int       studentId;
    private String    studentName;
    private int       feeStructureId;
    private LocalDate issuedDate;
    private LocalDate dueDate;
    private double    discountAmount;
    private String    discountReason;
    private String    status;           // unpaid | partial | paid | overdue

    // Calculated from v_invoice_summary
    private double totalFee;
    private double netPayable;
    private double totalPaid;
    private double outstanding;

    // Display helpers
    public String getStatusDisplay() {
        if (status == null) return "";
        return switch (status) {
            case "paid"    -> "Paid";
            case "partial" -> "Partial";
            case "overdue" -> "Overdue";
            default        -> "Unpaid";
        };
    }

    public boolean isFullyPaid() { return "paid".equals(status); }

    // Getters / Setters
    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public int getStudentId()                   { return studentId; }
    public void setStudentId(int v)             { this.studentId = v; }

    public String getStudentName()              { return studentName != null ? studentName : ""; }
    public void setStudentName(String v)        { this.studentName = v; }

    public int getFeeStructureId()              { return feeStructureId; }
    public void setFeeStructureId(int v)        { this.feeStructureId = v; }

    public LocalDate getIssuedDate()            { return issuedDate; }
    public void setIssuedDate(LocalDate v)      { this.issuedDate = v; }

    public LocalDate getDueDate()               { return dueDate; }
    public void setDueDate(LocalDate v)         { this.dueDate = v; }

    public double getDiscountAmount()           { return discountAmount; }
    public void setDiscountAmount(double v)     { this.discountAmount = v; }

    public String getDiscountReason()           { return discountReason != null ? discountReason : ""; }
    public void setDiscountReason(String v)     { this.discountReason = v; }

    public String getStatus()                   { return status; }
    public void setStatus(String v)             { this.status = v; }

    public double getTotalFee()                 { return totalFee; }
    public void setTotalFee(double v)           { this.totalFee = v; }

    public double getNetPayable()               { return netPayable; }
    public void setNetPayable(double v)         { this.netPayable = v; }

    public double getTotalPaid()                { return totalPaid; }
    public void setTotalPaid(double v)          { this.totalPaid = v; }

    public double getOutstanding()              { return outstanding; }
    public void setOutstanding(double v)        { this.outstanding = v; }
}
