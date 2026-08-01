package com.school.model;

public class TimetableEntry {

    private int    id;
    private int    classId;
    private int    day;          // 1=Mon … 5=Fri
    private int    period;       // matches period_config.period
    private int    subjectId;
    private int    teacherId;    // teacher_profiles.id

    // Display fields (from JOIN)
    private String subjectName;
    private String teacherName;

    public TimetableEntry() {}

    public TimetableEntry(int classId, int day, int period, int subjectId, int teacherId) {
        this.classId   = classId;
        this.day       = day;
        this.period    = period;
        this.subjectId = subjectId;
        this.teacherId = teacherId;
    }

    /** Composite key used as map key: "day_period" */
    public String getSlotKey() { return day + "_" + period; }

    public static String slotKey(int day, int period) { return day + "_" + period; }

    public String getDayName() {
        return switch (day) {
            case 1 -> "Monday";
            case 2 -> "Tuesday";
            case 3 -> "Wednesday";
            case 4 -> "Thursday";
            case 5 -> "Friday";
            default -> "—";
        };
    }

    // Getters / Setters
    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public int getClassId()                     { return classId; }
    public void setClassId(int v)               { this.classId = v; }

    public int getDay()                         { return day; }
    public void setDay(int v)                   { this.day = v; }

    public int getPeriod()                      { return period; }
    public void setPeriod(int v)                { this.period = v; }

    public int getSubjectId()                   { return subjectId; }
    public void setSubjectId(int v)             { this.subjectId = v; }

    public int getTeacherId()                   { return teacherId; }
    public void setTeacherId(int v)             { this.teacherId = v; }

    public String getSubjectName()              { return subjectName != null ? subjectName : ""; }
    public void setSubjectName(String v)        { this.subjectName = v; }

    public String getTeacherName()              { return teacherName != null ? teacherName : ""; }
    public void setTeacherName(String v)        { this.teacherName = v; }
}
