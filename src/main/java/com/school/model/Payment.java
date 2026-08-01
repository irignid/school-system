package com.school.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Payment {

    private int           id;
    private int           invoiceId;
    private double        amount;
    private LocalDate     paymentDate;
    private String        method;          // cash | cheque | bank
    private String        receiptNumber;
    private int           recordedBy;      // users.id
    private LocalDateTime recordedAt;

    public Payment() {}

    public Payment(int invoiceId, double amount, LocalDate paymentDate,
                   String method, String receiptNumber, int recordedBy) {
        this.invoiceId     = invoiceId;
        this.amount        = amount;
        this.paymentDate   = paymentDate;
        this.method        = method;
        this.receiptNumber = receiptNumber;
        this.recordedBy    = recordedBy;
    }

    public String getMethodDisplay() {
        if (method == null) return "";
        return switch (method) {
            case "cheque" -> "Cheque";
            case "bank"   -> "Bank Transfer";
            default       -> "Cash";
        };
    }

    // Getters / Setters
    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public int getInvoiceId()                   { return invoiceId; }
    public void setInvoiceId(int v)             { this.invoiceId = v; }

    public double getAmount()                   { return amount; }
    public void setAmount(double v)             { this.amount = v; }

    public LocalDate getPaymentDate()           { return paymentDate; }
    public void setPaymentDate(LocalDate v)     { this.paymentDate = v; }

    public String getMethod()                   { return method; }
    public void setMethod(String v)             { this.method = v; }

    public String getReceiptNumber()            { return receiptNumber != null ? receiptNumber : ""; }
    public void setReceiptNumber(String v)      { this.receiptNumber = v; }

    public int getRecordedBy()                  { return recordedBy; }
    public void setRecordedBy(int v)            { this.recordedBy = v; }

    public LocalDateTime getRecordedAt()        { return recordedAt; }
    public void setRecordedAt(LocalDateTime v)  { this.recordedAt = v; }
}
