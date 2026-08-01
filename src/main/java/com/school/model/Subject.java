package com.school.model;

public class Subject {

    private int    id;
    private String name;       // English
    private String nameSi;     // Sinhala
    private int    gradeLevel;

    public Subject() {}

    public Subject(String name, String nameSi, int gradeLevel) {
        this.name       = name;
        this.nameSi     = nameSi;
        this.gradeLevel = gradeLevel;
    }

    /** Used by ComboBox. */
    @Override
    public String toString() { return name + "  (Grade " + gradeLevel + ")"; }

    // Getters / Setters
    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public String getName()                     { return name; }
    public void setName(String v)               { this.name = v; }

    public String getNameSi()                   { return nameSi; }
    public void setNameSi(String v)             { this.nameSi = v; }

    public int getGradeLevel()                  { return gradeLevel; }
    public void setGradeLevel(int v)            { this.gradeLevel = v; }

    public String getGradeDisplay()             { return "Grade " + gradeLevel; }
}