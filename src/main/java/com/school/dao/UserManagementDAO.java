package com.school.dao;

import com.school.model.UserProfile;
import com.school.util.DBConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserManagementDAO {

    // ── Read ──────────────────────────────────────────────────

    public List<UserProfile> getAll() throws SQLException {
        String sql = """
                SELECT u.id, u.username, u.role, u.is_active, u.last_login, u.created_at,
                       COALESCE(tp.first_name, sp.first_name) AS first_name,
                       COALESCE(tp.last_name,  sp.last_name)  AS last_name,
                       COALESCE(tp.phone,       sp.phone)      AS phone
                FROM   users u
                LEFT JOIN teacher_profiles tp ON tp.user_id = u.id
                LEFT JOIN staff_profiles   sp ON sp.user_id = u.id
                ORDER  BY u.role, u.username
                """;
        List<UserProfile> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    // ── Create ────────────────────────────────────────────────

    /**
     * Creates a user row + the matching teacher_profiles or staff_profiles row
     * in a single transaction.
     *
     * @param username   login name (must be unique)
     * @param password   plain-text password — BCrypt-hashed before storing
     * @param role       "teacher" or "staff"
     * @param firstName  profile first name
     * @param lastName   profile last name
     * @param phone      profile phone (nullable)
     * @return the new user's id
     */
    public int createUser(String username, String password, String role,
                           String firstName, String lastName, String phone) throws SQLException {
        String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));

        String insertUser    = "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)";
        String insertTeacher = "INSERT INTO teacher_profiles (user_id, first_name, last_name, phone) VALUES (?, ?, ?, ?)";
        String insertStaff   = "INSERT INTO staff_profiles   (user_id, first_name, last_name, phone) VALUES (?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                int userId;
                try (PreparedStatement ps = con.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, username.trim());
                    ps.setString(2, hash);
                    ps.setString(3, role);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("User insert returned no key.");
                        userId = keys.getInt(1);
                    }
                }

                String profileSql = "teacher".equals(role) ? insertTeacher : insertStaff;
                try (PreparedStatement ps = con.prepareStatement(profileSql)) {
                    ps.setInt   (1, userId);
                    ps.setString(2, firstName.trim());
                    ps.setString(3, lastName.trim());
                    ps.setString(4, (phone == null || phone.isBlank()) ? null : phone.trim());
                    ps.executeUpdate();
                }

                con.commit();
                return userId;

            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    // ── Update ────────────────────────────────────────────────

    /**
     * Updates the profile name and phone for a user.
     * Works for both teacher and staff roles.
     */
    public boolean updateProfile(int userId, String role,
                                  String firstName, String lastName, String phone) throws SQLException {
        String sql = "teacher".equals(role)
                ? "UPDATE teacher_profiles SET first_name = ?, last_name = ?, phone = ? WHERE user_id = ?"
                : "UPDATE staff_profiles   SET first_name = ?, last_name = ?, phone = ? WHERE user_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, firstName.trim());
            ps.setString(2, lastName.trim());
            ps.setString(3, (phone == null || phone.isBlank()) ? null : phone.trim());
            ps.setInt   (4, userId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Resets a user's password. BCrypt-hashes the plain-text password before storing.
     */
    public boolean resetPassword(int userId, String newPassword) throws SQLException {
        String hash = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));
        String sql  = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setInt   (2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Activates or deactivates a user account.
     * A deactivated user cannot log in.
     */
    public boolean setActive(int userId, boolean active) throws SQLException {
        String sql = "UPDATE users SET is_active = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt    (2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Helper ────────────────────────────────────────────────

    private UserProfile map(ResultSet rs) throws SQLException {
        UserProfile up = new UserProfile();
        up.setId        (rs.getInt   ("id"));
        up.setUsername  (rs.getString("username"));
        up.setRole      (rs.getString("role"));
        up.setActive    (rs.getBoolean("is_active"));
        up.setFirstName (rs.getString("first_name"));
        up.setLastName  (rs.getString("last_name"));
        up.setPhone     (rs.getString("phone"));
        Timestamp ll = rs.getTimestamp("last_login");
        if (ll != null) up.setLastLogin(ll.toLocalDateTime());
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) up.setCreatedAt(ca.toLocalDateTime());
        return up;
    }
}