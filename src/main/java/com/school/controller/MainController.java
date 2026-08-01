package com.school.controller;

import com.school.model.User;
import com.school.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class MainController implements Initializable {

    @FXML private Label     userLabel;
    @FXML private Label     roleLabel;
    @FXML private VBox      navButtons;
    @FXML private StackPane contentArea;

    private Button activeButton = null;

    // ── Nav items per role ─────────────────────────────────────
    private static final Map<String, List<String[]>> NAV = Map.of(
        "principal", List.of(
            new String[]{"📊  Dashboard",       "dashboard"},
            new String[]{"👥  Students",         "students"},
            new String[]{"🏫  Academic Setup",   "academic"},
            new String[]{"✅  Attendance",        "attendance"},
            new String[]{"📝  Exams & Grades",   "exams"},
            new String[]{"💰  Fee Management",   "fees"},
            new String[]{"🔐  User Management",  "users"}
        ),
        "teacher", List.of(
            new String[]{"📊  Dashboard",       "dashboard"},
            new String[]{"✅  Mark Attendance",  "attendance"},
            new String[]{"📝  Enter Marks",      "exams"}
        ),
        "staff", List.of(
            new String[]{"📊  Dashboard",       "dashboard"},
            new String[]{"👥  Students",         "students"},
            new String[]{"💰  Fee Management",   "fees"}
        )
    );

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = SessionManager.getInstance().getCurrentUser();

        userLabel.setText(user.getUsername());
        roleLabel.setText(user.getRoleDisplayName());

        buildSidebar(user.getRole());
        loadView("dashboard");  // default view
    }

    private void buildSidebar(String role) {
        List<String[]> items = NAV.getOrDefault(role, List.of());
        for (String[] item : items) {
            Button btn = new Button(item[0]);
            btn.getStyleClass().add("nav-btn");
            btn.setMaxWidth(Double.MAX_VALUE);
            final String viewName = item[1];
            btn.setOnAction(e -> {
                setActiveButton(btn);
                loadView(viewName);
            });
            navButtons.getChildren().add(btn);
        }
    }

    private void setActiveButton(Button btn) {
        if (activeButton != null) activeButton.getStyleClass().remove("nav-btn-active");
        activeButton = btn;
        btn.getStyleClass().add("nav-btn-active");
    }

    public void loadView(String viewName) {
        String path = "/com/school/fxml/views/" + viewName + ".fxml";
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Node view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            // Print the actual error
            /* System.err.println("ERROR loading view: " + viewName);
            System.err.println("Full error: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Caused by: " + e.getCause().getMessage());
            }
            e.printStackTrace(); */
            
            // Placeholder until each view is built
            Label placeholder = new Label("📂  " + viewName.toUpperCase() + " — coming soon");
            placeholder.setStyle("-fx-font-size: 18px; -fx-text-fill: #94a3b8;");
            contentArea.getChildren().setAll(placeholder);
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/school/fxml/login.fxml")
            );
            Scene scene = new Scene(loader.load(), 1000, 680);
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(scene);
            stage.setMaximized(false);
            stage.setResizable(false);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}