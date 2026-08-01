package com.school.model;

import java.time.LocalDate;

public class Term {

    private int       id;
    private int       academicYearId;
    private int       termNumber;     // 1, 2, or 3
    private LocalDate startDate;
    private LocalDate endDate;

    // Display-only (populated by DAO JOIN)
    private String yearLabel;

    public Term() {}

    public Term(int academicYearId, int termNumber, LocalDate startDate, LocalDate endDate) {
        this.academicYearId = academicYearId;
        this.termNumber     = termNumber;
        this.startDate      = startDate;
        this.endDate        = endDate;
    }

    public String getDisplayName() { return "Term " + termNumber; }

    @Override
    public String toString() { return getDisplayName(); }

    // Getters / Setters
    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public int getAcademicYearId()              { return academicYearId; }
    public void setAcademicYearId(int v)        { this.academicYearId = v; }

    public int getTermNumber()                  { return termNumber; }
    public void setTermNumber(int v)            { this.termNumber = v; }

    public LocalDate getStartDate()             { return startDate; }
    public void setStartDate(LocalDate v)       { this.startDate = v; }

    public LocalDate getEndDate()               { return endDate; }
    public void setEndDate(LocalDate v)         { this.endDate = v; }

    public String getYearLabel()                { return yearLabel; }
    public void setYearLabel(String v)          { this.yearLabel = v; }
}