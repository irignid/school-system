package com.school.model;

public class Guardian {

    private int    id;
    private int    studentId;
    private String fullName;
    private String relationship;   // Father, Mother, Uncle, etc.
    private String phone;
    private String email;

    // ── Constructors ───────────────────────────────────────────
    public Guardian() {}

    public Guardian(int studentId, String fullName, String relationship,
                    String phone, String email) {
        this.studentId    = studentId;
        this.fullName     = fullName;
        this.relationship = relationship;
        this.phone        = phone;
        this.email        = email;
    }

    // ── Getters / Setters ──────────────────────────────────────
    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public int getStudentId()                   { return studentId; }
    public void setStudentId(int v)             { this.studentId = v; }

    public String getFullName()                 { return fullName; }
    public void setFullName(String v)           { this.fullName = v; }

    public String getRelationship()             { return relationship; }
    public void setRelationship(String v)       { this.relationship = v; }

    public String getPhone()                    { return phone; }
    public void setPhone(String v)              { this.phone = v; }

    public String getEmail()                    { return email; }
    public void setEmail(String v)              { this.email = v; }
}