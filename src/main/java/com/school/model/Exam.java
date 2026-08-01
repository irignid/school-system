package com.school.model;

import java.time.LocalDateTime;

public class Exam {

    private int           id;
    private String        name;
    private int           classId;
    private int           termId;
    private int           createdBy;    // users.id
    private LocalDateTime createdAt;

    // Display fields (from JOIN)
    private String classDisplay;  // e.g. "Grade 10 - A"
    private String termDisplay;   // e.g. "Term 1"
    private String yearLabel;

    public Exam() {}

    public Exam(String name, int classId, int termId, int createdBy) {
        this.name      = name;
        this.classId   = classId;
        this.termId    = termId;
        this.createdBy = createdBy;
    }

    @Override public String toString() { return name; }

    // Getters / Setters
    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public String getName()                     { return name; }
    public void setName(String v)               { this.name = v; }

    public int getClassId()                     { return classId; }
    public void setClassId(int v)               { this.classId = v; }

    public int getTermId()                      { return termId; }
    public void setTermId(int v)                { this.termId = v; }

    public int getCreatedBy()                   { return createdBy; }
    public void setCreatedBy(int v)             { this.createdBy = v; }

    public LocalDateTime getCreatedAt()         { return createdAt; }
    public void setCreatedAt(LocalDateTime v)   { this.createdAt = v; }

    public String getClassDisplay()             { return classDisplay != null ? classDisplay : ""; }
    public void setClassDisplay(String v)       { this.classDisplay = v; }

    public String getTermDisplay()              { return termDisplay != null ? termDisplay : ""; }
    public void setTermDisplay(String v)        { this.termDisplay = v; }

    public String getYearLabel()                { return yearLabel != null ? yearLabel : ""; }
    public void setYearLabel(String v)          { this.yearLabel = v; }
}