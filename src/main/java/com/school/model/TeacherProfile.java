package com.school.model;

public class TeacherProfile {

    private int    id;        // teacher_profiles.id (NOT users.id)
    private int    userId;
    private String firstName;
    private String lastName;
    private String phone;

    public TeacherProfile() {}

    public String getFullName() { return firstName + " " + lastName; }

    /** Used by ComboBox to display the teacher name automatically. */
    @Override
    public String toString() { return getFullName(); }

    // Getters / Setters
    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public int getUserId()                      { return userId; }
    public void setUserId(int v)                { this.userId = v; }

    public String getFirstName()                { return firstName; }
    public void setFirstName(String v)          { this.firstName = v; }

    public String getLastName()                 { return lastName; }
    public void setLastName(String v)           { this.lastName = v; }

    public String getPhone()                    { return phone; }
    public void setPhone(String v)              { this.phone = v; }
}