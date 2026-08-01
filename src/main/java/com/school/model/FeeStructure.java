package com.school.model;

public class FeeStructure {

    private int    id;
    private int    termId;
    private int    gradeLevel;
    private String description;

    // Display fields (from JOIN / calculated)
    private String termDisplay;   // e.g. "Term 1"
    private String yearLabel;
    private double totalAmount;   // sum of fee_items.amount

    public FeeStructure() {}

    public FeeStructure(int termId, int gradeLevel, String description) {
        this.termId      = termId;
        this.gradeLevel  = gradeLevel;
        this.description = description;
    }

    public String getGradeDisplay() { return "Grade " + gradeLevel; }

    @Override public String toString() {
        return "Grade " + gradeLevel + " — " + termDisplay;
    }

    // Getters / Setters
    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public int getTermId()                      { return termId; }
    public void setTermId(int v)                { this.termId = v; }

    public int getGradeLevel()                  { return gradeLevel; }
    public void setGradeLevel(int v)            { this.gradeLevel = v; }

    public String getDescription()              { return description != null ? description : ""; }
    public void setDescription(String v)        { this.description = v; }

    public String getTermDisplay()              { return termDisplay != null ? termDisplay : ""; }
    public void setTermDisplay(String v)        { this.termDisplay = v; }

    public String getYearLabel()                { return yearLabel != null ? yearLabel : ""; }
    public void setYearLabel(String v)          { this.yearLabel = v; }

    public double getTotalAmount()              { return totalAmount; }
    public void setTotalAmount(double v)        { this.totalAmount = v; }
}
