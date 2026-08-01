package com.school.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class ExamSchedule {

    private int       id;
    private int       examId;
    private int       subjectId;
    private LocalDate date;
    private LocalTime startTime;
    private int       durationMinutes;

    // Display fields
    private String subjectName;
    private String subjectNameSi;

    public ExamSchedule() {}

    public ExamSchedule(int examId, int subjectId, LocalDate date,
                        LocalTime startTime, int durationMinutes) {
        this.examId          = examId;
        this.subjectId       = subjectId;
        this.date            = date;
        this.startTime       = startTime;
        this.durationMinutes = durationMinutes;
    }

    // Getters / Setters
    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }

    public int getExamId()                          { return examId; }
    public void setExamId(int v)                    { this.examId = v; }

    public int getSubjectId()                       { return subjectId; }
    public void setSubjectId(int v)                 { this.subjectId = v; }

    public LocalDate getDate()                      { return date; }
    public void setDate(LocalDate v)                { this.date = v; }

    public LocalTime getStartTime()                 { return startTime; }
    public void setStartTime(LocalTime v)           { this.startTime = v; }

    public int getDurationMinutes()                 { return durationMinutes; }
    public void setDurationMinutes(int v)           { this.durationMinutes = v; }

    public String getSubjectName()                  { return subjectName != null ? subjectName : ""; }
    public void setSubjectName(String v)            { this.subjectName = v; }

    public String getSubjectNameSi()                { return subjectNameSi != null ? subjectNameSi : ""; }
    public void setSubjectNameSi(String v)          { this.subjectNameSi = v; }
}