package com.school.model;

import java.time.LocalDate;

public class Attendance {

    private int       id;
    private int       studentId;
    private int       classId;
    private LocalDate date;
    private int       period;
    private String    status;    // "P", "A", "L", "ML"
    private int       markedBy;  // teacher_profiles.id

    public Attendance() {}

    public Attendance(int studentId, int classId, LocalDate date,
                      int period, String status, int markedBy) {
        this.studentId = studentId;
        this.classId   = classId;
        this.date      = date;
        this.period    = period;
        this.status    = status;
        this.markedBy  = markedBy;
    }

    // Getters / Setters
    public int getId()                       { return id; }
    public void setId(int id)                { this.id = id; }

    public int getStudentId()                { return studentId; }
    public void setStudentId(int v)          { this.studentId = v; }

    public int getClassId()                  { return classId; }
    public void setClassId(int v)            { this.classId = v; }

    public LocalDate getDate()               { return date; }
    public void setDate(LocalDate v)         { this.date = v; }

    public int getPeriod()                   { return period; }
    public void setPeriod(int v)             { this.period = v; }

    public String getStatus()                { return status; }
    public void setStatus(String v)          { this.status = v; }

    public int getMarkedBy()                 { return markedBy; }
    public void setMarkedBy(int v)           { this.markedBy = v; }
}