package com.school.model;

public class SchoolClass {

    private int    id;
    private int    academicYearId;
    private int    gradeLevel;
    private String section;              // "A", "B", etc.
    private int    homeroomTeacherId;    // 0 if none

    // Display-only (populated by DAO JOIN)
    private String yearLabel;
    private String teacherName;

    public SchoolClass() {}

    public SchoolClass(int academicYearId, int gradeLevel, String section, int homeroomTeacherId) {
        this.academicYearId    = academicYearId;
        this.gradeLevel        = gradeLevel;
        this.section           = section;
        this.homeroomTeacherId = homeroomTeacherId;
    }

    /** e.g. "Grade 10 - A" */
    public String getDisplayName() {
        return "Grade " + gradeLevel + " - " + section;
    }

    @Override
    public String toString() { return getDisplayName(); }

    // Getters / Setters
    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public int getAcademicYearId()              { return academicYearId; }
    public void setAcademicYearId(int v)        { this.academicYearId = v; }

    public int getGradeLevel()                  { return gradeLevel; }
    public void setGradeLevel(int v)            { this.gradeLevel = v; }

    public String getSection()                  { return section; }
    public void setSection(String v)            { this.section = v; }

    public int getHomeroomTeacherId()           { return homeroomTeacherId; }
    public void setHomeroomTeacherId(int v)     { this.homeroomTeacherId = v; }

    public String getYearLabel()                { return yearLabel; }
    public void setYearLabel(String v)          { this.yearLabel = v; }

    public String getTeacherName()              { return teacherName != null ? teacherName : "—"; }
    public void setTeacherName(String v)        { this.teacherName = v; }
}