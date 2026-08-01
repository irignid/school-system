package com.school.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Mark {

    private int        id;
    private int        studentId;
    private int        examId;
    private int        subjectId;
    private double     markObtained;
    private double     maxMark;
    private int        enteredBy;     // users.id

    // Display fields (from JOIN / calculated)
    private String studentName;
    private String subjectName;
    private String examName;
    private String gradeSymbol;

    public Mark() {}

    public Mark(int studentId, int examId, int subjectId,
                double markObtained, double maxMark, int enteredBy) {
        this.studentId    = studentId;
        this.examId       = examId;
        this.subjectId    = subjectId;
        this.markObtained = markObtained;
        this.maxMark      = maxMark;
        this.enteredBy    = enteredBy;
    }

    /** Percentage rounded to 2 dp. */
    public double getPercentage() {
        if (maxMark == 0) return 0;
        return BigDecimal.valueOf(markObtained / maxMark * 100)
                .setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    // Getters / Setters
    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public int getStudentId()                   { return studentId; }
    public void setStudentId(int v)             { this.studentId = v; }

    public int getExamId()                      { return examId; }
    public void setExamId(int v)                { this.examId = v; }

    public int getSubjectId()                   { return subjectId; }
    public void setSubjectId(int v)             { this.subjectId = v; }

    public double getMarkObtained()             { return markObtained; }
    public void setMarkObtained(double v)       { this.markObtained = v; }

    public double getMaxMark()                  { return maxMark; }
    public void setMaxMark(double v)            { this.maxMark = v; }

    public int getEnteredBy()                   { return enteredBy; }
    public void setEnteredBy(int v)             { this.enteredBy = v; }

    public String getStudentName()              { return studentName != null ? studentName : ""; }
    public void setStudentName(String v)        { this.studentName = v; }

    public String getSubjectName()              { return subjectName != null ? subjectName : ""; }
    public void setSubjectName(String v)        { this.subjectName = v; }

    public String getExamName()                 { return examName != null ? examName : ""; }
    public void setExamName(String v)           { this.examName = v; }

    public String getGradeSymbol()              { return gradeSymbol != null ? gradeSymbol : "—"; }
    public void setGradeSymbol(String v)        { this.gradeSymbol = v; }
}