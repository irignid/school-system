package com.school.model;

import java.time.LocalDateTime;

/**
 * Display model for the User Management table.
 * Combines users + teacher_profiles / staff_profiles into one object.
 */
public class UserProfile {

    private int           id;
    private String        username;
    private String        role;
    private boolean       active;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;

    // From teacher_profiles or staff_profiles
    private String firstName;
    private String lastName;
    private String phone;

    public String getFullName() {
        if (firstName == null && lastName == null) return "—";
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    public String getRoleDisplay() {
        if (role == null) return "";
        return switch (role) {
            case "principal" -> "Principal";
            case "teacher"   -> "Teacher";
            case "staff"     -> "Staff";
            default          -> role;
        };
    }

    public String getStatusDisplay() { return active ? "Active" : "Inactive"; }

    // Getters / Setters
    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public String getUsername()                 { return username; }
    public void setUsername(String v)           { this.username = v; }

    public String getRole()                     { return role; }
    public void setRole(String v)               { this.role = v; }

    public boolean isActive()                   { return active; }
    public void setActive(boolean v)            { this.active = v; }

    public LocalDateTime getLastLogin()         { return lastLogin; }
    public void setLastLogin(LocalDateTime v)   { this.lastLogin = v; }

    public LocalDateTime getCreatedAt()         { return createdAt; }
    public void setCreatedAt(LocalDateTime v)   { this.createdAt = v; }

    public String getFirstName()                { return firstName; }
    public void setFirstName(String v)          { this.firstName = v; }

    public String getLastName()                 { return lastName; }
    public void setLastName(String v)           { this.lastName = v; }

    public String getPhone()                    { return phone; }
    public void setPhone(String v)              { this.phone = v; }
}