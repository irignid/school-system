package com.school.model;

public class ClassSubject {

    private int    id;
    private int    classId;
    private int    subjectId;
    private int    teacherId;      // teacher_profiles.id

    // Display-only (from JOIN)
    private String subjectName;
    private String subjectNameSi;
    private String teacherName;

    public ClassSubject() {}

    public ClassSubject(int classId, int subjectId, int teacherId) {
        this.classId   = classId;
        this.subjectId = subjectId;
        this.teacherId = teacherId;
    }

    // Getters / Setters
    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public int getClassId()                     { return classId; }
    public void setClassId(int v)               { this.classId = v; }

    public int getSubjectId()                   { return subjectId; }
    public void setSubjectId(int v)             { this.subjectId = v; }

    public int getTeacherId()                   { return teacherId; }
    public void setTeacherId(int v)             { this.teacherId = v; }

    public String getSubjectName()              { return subjectName; }
    public void setSubjectName(String v)        { this.subjectName = v; }

    public String getSubjectNameSi()            { return subjectNameSi; }
    public void setSubjectNameSi(String v)      { this.subjectNameSi = v; }

    public String getTeacherName()              { return teacherName != null ? teacherName : "—"; }
    public void setTeacherName(String v)        { this.teacherName = v; }
}