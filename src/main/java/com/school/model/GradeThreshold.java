package com.school.model;

public class GradeThreshold {

    private int    id;
    private String gradeSymbol;
    private double minMark;
    private double maxMark;

    public GradeThreshold() {}

    // Getters / Setters
    public int getId()                      { return id; }
    public void setId(int id)               { this.id = id; }

    public String getGradeSymbol()          { return gradeSymbol; }
    public void setGradeSymbol(String v)    { this.gradeSymbol = v; }

    public double getMinMark()              { return minMark; }
    public void setMinMark(double v)        { this.minMark = v; }

    public double getMaxMark()              { return maxMark; }
    public void setMaxMark(double v)        { this.maxMark = v; }

    public String getRange() {
        return minMark + " – " + maxMark;
    }
}