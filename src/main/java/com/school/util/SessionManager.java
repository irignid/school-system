package com.school.util;

import com.school.model.User;

/**
 * Singleton that holds the currently authenticated user for the session.
 * Access anywhere with SessionManager.getInstance().getCurrentUser()
 */
public class SessionManager {

    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public User getCurrentUser()            { return currentUser; }
    public void  setCurrentUser(User user)  { this.currentUser = user; }
    public boolean isLoggedIn()             { return currentUser != null; }
    public String  getRole()               { return currentUser != null ? currentUser.getRole() : null; }

    public void logout() {
        currentUser = null;
    }
}