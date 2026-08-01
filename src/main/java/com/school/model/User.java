package com.school.model;

import java.time.LocalDateTime;

public class User {

    private int           id;
    private String        username;
    private String        passwordHash;
    private String        role;          // principal | teacher | staff
    private boolean       active;
    private LocalDateTime lastLogin;

    // ── Constructors ──────────────────────────────────────────
    public User() {}

    public User(int id, String username, String role) {
        this.id       = id;
        this.username = username;
        this.role     = role;
    }

    // ── Helpers ───────────────────────────────────────────────
    public String getRoleDisplayName() {
        return switch (role) {
            case "principal" -> "Principal";
            case "teacher"   -> "Teacher";
            case "staff"     -> "Office Staff";
            default          -> role;
        };
    }

    // ── Getters & Setters ─────────────────────────────────────
    public int            getId()                        { return id; }
    public void           setId(int id)                  { this.id = id; }

    public String         getUsername()                  { return username; }
    public void           setUsername(String username)   { this.username = username; }

    public String         getPasswordHash()              { return passwordHash; }
    public void           setPasswordHash(String h)      { this.passwordHash = h; }

    public String         getRole()                      { return role; }
    public void           setRole(String role)           { this.role = role; }

    public boolean        isActive()                     { return active; }
    public void           setActive(boolean active)      { this.active = active; }

    public LocalDateTime  getLastLogin()                 { return lastLogin; }
    public void           setLastLogin(LocalDateTime dt) { this.lastLogin = dt; }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', role='" + role + "'}";
    }
}