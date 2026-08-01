package com.school.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Student {

    private int         id;
    private String      firstName;
    private String      lastName;
    private String      firstNameSi;      // Sinhala script
    private LocalDate   dateOfBirth;
    private String      nic;
    private String      gender;           // "M" or "F"
    private String      religion;
    private String      address;
    private LocalDate   enrollmentDate;
    private boolean     active;
    private LocalDateTime createdAt;

    // ── Constructors ───────────────────────────────────────────
    public Student() {}

    public Student(String firstName, String lastName, String firstNameSi,
                   LocalDate dateOfBirth, String nic, String gender,
                   String religion, String address, LocalDate enrollmentDate) {
        this.firstName      = firstName;
        this.lastName       = lastName;
        this.firstNameSi    = firstNameSi;
        this.dateOfBirth    = dateOfBirth;
        this.nic            = nic;
        this.gender         = gender;
        this.religion       = religion;
        this.address        = address;
        this.enrollmentDate = enrollmentDate;
        this.active         = true;
    }

    // ── Convenience ────────────────────────────────────────────
    /** "John Silva" */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /** "Male" / "Female" */
    public String getGenderDisplay() {
        return "M".equals(gender) ? "Male" : "Female";
    }

    /** "Active" / "Inactive" */
    public String getStatusDisplay() {
        return active ? "Active" : "Inactive";
    }

    // ── Getters / Setters ──────────────────────────────────────
    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public String getFirstName()                { return firstName; }
    public void setFirstName(String v)          { this.firstName = v; }

    public String getLastName()                 { return lastName; }
    public void setLastName(String v)           { this.lastName = v; }

    public String getFirstNameSi()              { return firstNameSi; }
    public void setFirstNameSi(String v)        { this.firstNameSi = v; }

    public LocalDate getDateOfBirth()           { return dateOfBirth; }
    public void setDateOfBirth(LocalDate v)     { this.dateOfBirth = v; }

    public String getNic()                      { return nic; }
    public void setNic(String v)                { this.nic = v; }

    public String getGender()                   { return gender; }
    public void setGender(String v)             { this.gender = v; }

    public String getReligion()                 { return religion; }
    public void setReligion(String v)           { this.religion = v; }

    public String getAddress()                  { return address; }
    public void setAddress(String v)            { this.address = v; }

    public LocalDate getEnrollmentDate()        { return enrollmentDate; }
    public void setEnrollmentDate(LocalDate v)  { this.enrollmentDate = v; }

    public boolean isActive()                   { return active; }
    public void setActive(boolean v)            { this.active = v; }

    public LocalDateTime getCreatedAt()         { return createdAt; }
    public void setCreatedAt(LocalDateTime v)   { this.createdAt = v; }
}