package com.school.controller;

import com.school.dao.UserManagementDAO;
import com.school.model.UserProfile;
import com.school.util.SessionManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class UserManagementController {

    private final UserManagementDAO dao = new UserManagementDAO();
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int currentUserId = 0;

    // ── FXML ──────────────────────────────────────────────────
    @FXML private TextField          searchField;
    @FXML private ComboBox<String>   roleFilter;
    @FXML private ComboBox<String>   statusFilter;

    @FXML private TableView<UserProfile>                userTable;
    @FXML private TableColumn<UserProfile, String>      colUsername;
    @FXML private TableColumn<UserProfile, String>      colFullName;
    @FXML private TableColumn<UserProfile, String>      colRole;
    @FXML private TableColumn<UserProfile, String>      colPhone;
    @FXML private TableColumn<UserProfile, String>      colStatus;
    @FXML private TableColumn<UserProfile, String>      colLastLogin;
    @FXML private TableColumn<UserProfile, Void>        colActions;

    private final ObservableList<UserProfile> allUsers      = FXCollections.observableArrayList();
    private final FilteredList<UserProfile>   filteredUsers = new FilteredList<>(allUsers, u -> true);

    // ══════════════════════════════════════════════════════════
    //  INIT
    // ══════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        currentUserId = SessionManager.getInstance().getCurrentUser().getId();
        setupFilters();
        setupTable();
        loadUsers();
    }

    private void setupFilters() {
        roleFilter.setItems(FXCollections.observableArrayList("All", "Teacher", "Staff"));
        roleFilter.setValue("All");
        statusFilter.setItems(FXCollections.observableArrayList("All", "Active", "Inactive"));
        statusFilter.setValue("All");

        roleFilter.valueProperty().addListener((obs, old, now) -> applyFilter());
        statusFilter.valueProperty().addListener((obs, old, now) -> applyFilter());
    }

    private void setupTable() {
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));

        colRole.setCellValueFactory(new PropertyValueFactory<>("roleDisplay"));
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(switch (item) {
                    case "Principal" -> "-fx-text-fill:#7c3aed;-fx-font-weight:bold;";
                    case "Teacher"   -> "-fx-text-fill:#2563eb;-fx-font-weight:bold;";
                    default          -> "-fx-text-fill:#0891b2;-fx-font-weight:bold;";
                });
            }
        });

        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));

        colStatus.setCellValueFactory(new PropertyValueFactory<>("statusDisplay"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("Active".equals(item)
                        ? "-fx-text-fill:#16a34a;-fx-font-weight:bold;"
                        : "-fx-text-fill:#dc2626;-fx-font-weight:bold;");
            }
        });

        colLastLogin.setCellValueFactory(cd -> {
            var dt = cd.getValue().getLastLogin();
            return new javafx.beans.property.SimpleStringProperty(
                    dt != null ? DT_FMT.format(dt) : "Never");
        });

        // Actions: Edit | Reset Password | Deactivate/Activate
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("Edit");
            private final Button btnReset  = new Button("Reset Password");
            private final Button btnToggle = new Button();
            private final HBox   box       = new HBox(6, btnEdit, btnReset, btnToggle);

            {
                btnEdit.getStyleClass().add("btn-secondary");
                btnReset.getStyleClass().add("btn-secondary");
                btnToggle.getStyleClass().add("btn-danger");
                box.setAlignment(Pos.CENTER_LEFT);

                btnEdit.setOnAction(e -> handleEdit(getTableView().getItems().get(getIndex())));
                btnReset.setOnAction(e -> handleResetPassword(getTableView().getItems().get(getIndex())));
                btnToggle.setOnAction(e -> handleToggleActive(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }

                UserProfile up = getTableView().getItems().get(getIndex());
                boolean isSelf = up.getId() == currentUserId;

                // Can't edit/deactivate yourself or the principal account
                boolean isPrincipal = "principal".equals(up.getRole());
                btnEdit.setDisable(isPrincipal);
                btnReset.setDisable(isSelf);     // reset own password via OS, not here
                btnToggle.setDisable(isSelf || isPrincipal);
                btnToggle.setText(up.isActive() ? "Deactivate" : "Activate");

                setGraphic(box);
            }
        });

        userTable.setItems(filteredUsers);
    }

    // ══════════════════════════════════════════════════════════
    //  DATA
    // ══════════════════════════════════════════════════════════

    private void loadUsers() {
        try {
            allUsers.setAll(dao.getAll());
            applyFilter();
        } catch (SQLException ex) {
            showError("Database error", ex.getMessage());
        }
    }

    private void applyFilter() {
        String search = searchField.getText().trim().toLowerCase();
        String role   = roleFilter.getValue();
        String status = statusFilter.getValue();

        filteredUsers.setPredicate(up -> {
            boolean matchRole = "All".equals(role)
                    || up.getRoleDisplay().equalsIgnoreCase(role);
            boolean matchStatus = "All".equals(status)
                    || up.getStatusDisplay().equalsIgnoreCase(status);
            boolean matchSearch = search.isEmpty()
                    || up.getUsername().toLowerCase().contains(search)
                    || up.getFullName().toLowerCase().contains(search);
            return matchRole && matchStatus && matchSearch;
        });
    }

    @FXML
    private void handleSearch() { applyFilter(); }

    // ══════════════════════════════════════════════════════════
    //  HANDLERS
    // ══════════════════════════════════════════════════════════

    @FXML
    private void handleAddUser() {
        showUserDialog(null).ifPresent(data -> {
            try {
                dao.createUser(data[0], data[1], data[2], data[3], data[4], data[5]);
                loadUsers();
            } catch (SQLException ex) {
                showError("Save failed",
                        ex.getMessage().contains("Duplicate")
                                ? "Username \"" + data[0] + "\" is already taken."
                                : ex.getMessage());
            }
        });
    }

    private void handleEdit(UserProfile up) {
        showEditDialog(up).ifPresent(data -> {
            try {
                dao.updateProfile(up.getId(), up.getRole(), data[0], data[1], data[2]);
                loadUsers();
            } catch (SQLException ex) {
                showError("Save failed", ex.getMessage());
            }
        });
    }

    private void handleResetPassword(UserProfile up) {
        showResetPasswordDialog(up).ifPresent(newPass -> {
            try {
                dao.resetPassword(up.getId(), newPass);
                showInfo("Password Reset",
                        "Password for \"" + up.getUsername() + "\" has been reset successfully.");
            } catch (SQLException ex) {
                showError("Reset failed", ex.getMessage());
            }
        });
    }

    private void handleToggleActive(UserProfile up) {
        boolean newState = !up.isActive();
        String  action   = newState ? "re-activate" : "deactivate";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to " + action + " \"" + up.getUsername() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    dao.setActive(up.getId(), newState);
                    loadUsers();
                } catch (SQLException ex) {
                    showError("Update failed", ex.getMessage());
                }
            }
        });
    }

    // ══════════════════════════════════════════════════════════
    //  DIALOGS
    // ══════════════════════════════════════════════════════════

    /**
     * Add User dialog.
     *
     * @return String[6] { username, password, role, firstName, lastName, phone }
     *         or empty if cancelled.
     */
    private Optional<String[]> showUserDialog(Void unused) {
        Dialog<String[]> dlg = new Dialog<>();
        dlg.setTitle("Add User");
        dlg.setHeaderText(null);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(440);

        TextField        tfUsername  = new TextField();  tfUsername.setPromptText("Username *");
        PasswordField    pfPassword  = new PasswordField(); pfPassword.setPromptText("Password *");
        PasswordField    pfConfirm   = new PasswordField(); pfConfirm.setPromptText("Confirm password *");
        ComboBox<String> cbRole      = new ComboBox<>(
                FXCollections.observableArrayList("teacher", "staff"));
        cbRole.setPromptText("Select role *");
        cbRole.setMaxWidth(Double.MAX_VALUE);
        TextField        tfFirstName = new TextField(); tfFirstName.setPromptText("First name *");
        TextField        tfLastName  = new TextField(); tfLastName.setPromptText("Last name *");
        TextField        tfPhone     = new TextField(); tfPhone.setPromptText("Phone (optional)");

        GridPane grid = formGrid();
        grid.addRow(0, label("Username *"),         tfUsername);
        grid.addRow(1, label("Password *"),         pfPassword);
        grid.addRow(2, label("Confirm Password *"), pfConfirm);
        grid.addRow(3, label("Role *"),             cbRole);
        grid.addRow(4, label("First Name *"),       tfFirstName);
        grid.addRow(5, label("Last Name *"),        tfLastName);
        grid.addRow(6, label("Phone"),              tfPhone);
        GridPane.setHgrow(cbRole, Priority.ALWAYS);
        dlg.getDialogPane().setContent(grid);

        Button ok = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            StringBuilder err = new StringBuilder();
            if (tfUsername.getText().isBlank())          err.append("• Username is required\n");
            if (pfPassword.getText().isBlank())          err.append("• Password is required\n");
            else if (pfPassword.getText().length() < 6)  err.append("• Password must be at least 6 characters\n");
            if (!pfPassword.getText().equals(pfConfirm.getText()))
                                                         err.append("• Passwords do not match\n");
            if (cbRole.getValue() == null)               err.append("• Role is required\n");
            if (tfFirstName.getText().isBlank())         err.append("• First name is required\n");
            if (tfLastName.getText().isBlank())          err.append("• Last name is required\n");
            if (!err.isEmpty()) { ev.consume(); showError("Validation", err.toString().trim()); }
        });

        dlg.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            return new String[]{
                    tfUsername.getText().trim(),
                    pfPassword.getText(),
                    cbRole.getValue(),
                    tfFirstName.getText().trim(),
                    tfLastName.getText().trim(),
                    tfPhone.getText().trim()
            };
        });

        return dlg.showAndWait();
    }

    /**
     * Edit profile dialog (name + phone only — username and role are immutable).
     *
     * @return String[3] { firstName, lastName, phone } or empty if cancelled.
     */
    private Optional<String[]> showEditDialog(UserProfile up) {
        Dialog<String[]> dlg = new Dialog<>();
        dlg.setTitle("Edit Profile — " + up.getUsername());
        dlg.setHeaderText("Role: " + up.getRoleDisplay() + "  |  Username cannot be changed.");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(400);

        TextField tfFirst = new TextField(up.getFirstName() != null ? up.getFirstName() : "");
        TextField tfLast  = new TextField(up.getLastName()  != null ? up.getLastName()  : "");
        TextField tfPhone = new TextField(up.getPhone()     != null ? up.getPhone()     : "");

        GridPane grid = formGrid();
        grid.addRow(0, label("First Name *"), tfFirst);
        grid.addRow(1, label("Last Name *"),  tfLast);
        grid.addRow(2, label("Phone"),        tfPhone);
        dlg.getDialogPane().setContent(grid);

        Button ok = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            StringBuilder err = new StringBuilder();
            if (tfFirst.getText().isBlank()) err.append("• First name is required\n");
            if (tfLast.getText().isBlank())  err.append("• Last name is required\n");
            if (!err.isEmpty()) { ev.consume(); showError("Validation", err.toString().trim()); }
        });

        dlg.setResultConverter(btn -> btn != ButtonType.OK ? null
                : new String[]{ tfFirst.getText().trim(), tfLast.getText().trim(), tfPhone.getText().trim() });

        return dlg.showAndWait();
    }

    /** Reset password dialog — returns the new plain-text password. */
    private Optional<String> showResetPasswordDialog(UserProfile up) {
        Dialog<String> dlg = new Dialog<>();
        dlg.setTitle("Reset Password — " + up.getUsername());
        dlg.setHeaderText(null);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(380);

        PasswordField pfNew     = new PasswordField(); pfNew.setPromptText("New password *");
        PasswordField pfConfirm = new PasswordField(); pfConfirm.setPromptText("Confirm password *");

        GridPane grid = formGrid();
        grid.addRow(0, label("New Password *"),     pfNew);
        grid.addRow(1, label("Confirm Password *"), pfConfirm);
        dlg.getDialogPane().setContent(grid);

        Button ok = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            StringBuilder err = new StringBuilder();
            if (pfNew.getText().isBlank())                        err.append("• Password is required\n");
            else if (pfNew.getText().length() < 6)                err.append("• Password must be at least 6 characters\n");
            if (!pfNew.getText().equals(pfConfirm.getText()))     err.append("• Passwords do not match\n");
            if (!err.isEmpty()) { ev.consume(); showError("Validation", err.toString().trim()); }
        });

        dlg.setResultConverter(btn -> btn == ButtonType.OK ? pfNew.getText() : null);
        return dlg.showAndWait();
    }

    // ══════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════

    private GridPane formGrid() {
        GridPane g = new GridPane();
        g.setHgap(12); g.setVgap(10); g.setPadding(new Insets(16));
        ColumnConstraints lc = new ColumnConstraints(160);
        ColumnConstraints fc = new ColumnConstraints(200, 200, Double.MAX_VALUE);
        fc.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(lc, fc);
        return g;
    }

    private Label label(String text) {
        Label l = new Label(text); l.getStyleClass().add("form-label"); return l;
    }

    private void showError(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}