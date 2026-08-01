package com.school.model;

import java.time.LocalDate;

public class AcademicYear {

    private int       id;
    private String    yearLabel;   // e.g. "2025/2026"
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean   current;

    public AcademicYear() {}

    public AcademicYear(String yearLabel, LocalDate startDate, LocalDate endDate) {
        this.yearLabel = yearLabel;
        this.startDate = startDate;
        this.endDate   = endDate;
    }

    /** Used by ComboBox to display the year label automatically. */
    @Override
    public String toString() { return yearLabel; }

    // Getters / Setters
    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public String getYearLabel()                { return yearLabel; }
    public void setYearLabel(String v)          { this.yearLabel = v; }

    public LocalDate getStartDate()             { return startDate; }
    public void setStartDate(LocalDate v)       { this.startDate = v; }

    public LocalDate getEndDate()               { return endDate; }
    public void setEndDate(LocalDate v)         { this.endDate = v; }

    public boolean isCurrent()                  { return current; }
    public void setCurrent(boolean v)           { this.current = v; }

    public String getCurrentDisplay()           { return current ? "✔ Current" : ""; }
}