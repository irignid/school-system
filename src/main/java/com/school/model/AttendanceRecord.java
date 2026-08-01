package com.school.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * UI-only model for one row in the attendance marking table.
 * Uses a JavaFX StringProperty for status so the summary bar
 * can react to changes in real time.
 */
public class AttendanceRecord {

    private final int            studentId;
    private final String         studentName;
    private final String         studentNameSi;
    private final StringProperty status = new SimpleStringProperty("P");

    public AttendanceRecord(int studentId, String studentName,
                            String studentNameSi, String initialStatus) {
        this.studentId     = studentId;
        this.studentName   = studentName;
        this.studentNameSi = studentNameSi != null ? studentNameSi : "";
        this.status.set(initialStatus != null ? initialStatus : "P");
    }

    // ── Getters ───────────────────────────────────────────────
    public int    getStudentId()    { return studentId; }
    public String getStudentName()  { return studentName; }
    public String getStudentNameSi(){ return studentNameSi; }

    public StringProperty statusProperty() { return status; }
    public String  getStatus()             { return status.get(); }
    public void    setStatus(String v)     { status.set(v); }
}