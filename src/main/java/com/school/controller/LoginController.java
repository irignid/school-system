package com.school.controller;

import com.school.dao.UserDAO;
import com.school.model.User;
import com.school.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;
    @FXML private Button        loginButton;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }

        loginButton.setDisable(true);
        loginButton.setText("Signing in...");

        User user = userDAO.findByUsername(username);

        if (user == null || !user.isActive()) {
            showError("Invalid username or password.");
            resetButton();
            return;
        }

        if (!userDAO.verifyPassword(password, user.getPasswordHash())) {
            showError("Invalid username or password.");
            passwordField.clear();
            resetButton();
            return;
        }

        userDAO.updateLastLogin(user.getId());
        SessionManager.getInstance().setCurrentUser(user);
        loadMainDashboard();
    }

    private void loadMainDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/school/fxml/main.fxml")
            );
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setResizable(true);
        } catch (IOException e) {
            showError("Failed to load dashboard: " + e.getMessage());
            e.printStackTrace();
            resetButton();
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void resetButton() {
        loginButton.setDisable(false);
        loginButton.setText("Sign In");
    }
}