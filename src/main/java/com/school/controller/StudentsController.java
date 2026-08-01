package com.school.controller;

import com.school.dao.GuardianDAO;
import com.school.dao.StudentDAO;
import com.school.model.Guardian;
import com.school.model.Student;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Controller for views/students.fxml.
 *
 * Layout:
 *   Top half  – searchable TableView of all students.
 *   Bottom half – guardian TableView for the selected student.
 *
 * Dialogs for add/edit are built programmatically (no extra FXML needed).
 */
public class StudentsController {

    // ── FXML injections ───────────────────────────────────────

    /* Student table */
    @FXML private TextField   searchField;
    @FXML private CheckBox    showInactiveCheck;
    @FXML private TableView<Student> studentTable;
    @FXML private TableColumn<Student, Integer>    colId;
    @FXML private TableColumn<Student, String>     colName;
    @FXML private TableColumn<Student, String>     colNameSi;
    @FXML private TableColumn<Student, LocalDate>  colDob;
    @FXML private TableColumn<Student, String>     colGender;
    @FXML private TableColumn<Student, String>     colNic;
    @FXML private TableColumn<Student, LocalDate>  colEnrolled;
    @FXML private TableColumn<Student, String>     colStatus;
    @FXML private TableColumn<Student, Void>       colActions;

    /* Guardian panel */
    @FXML private Label    guardianTitle;
    @FXML private Button   btnAddGuardian;
    @FXML private TableView<Guardian> guardianTable;
    @FXML private TableColumn<Guardian, String> gColName;
    @FXML private TableColumn<Guardian, String> gColRelationship;
    @FXML private TableColumn<Guardian, String> gColPhone;
    @FXML private TableColumn<Guardian, String> gColEmail;
    @FXML private TableColumn<Guardian, Void>   gColActions;

    // ── State ─────────────────────────────────────────────────
    private final StudentDAO  studentDAO  = new StudentDAO();
    private final GuardianDAO guardianDAO = new GuardianDAO();

    private final ObservableList<Student>  allStudents  = FXCollections.observableArrayList();
    private final FilteredList<Student>    filtered     = new FilteredList<>(allStudents, s -> true);
    private final ObservableList<Guardian> guardianList = FXCollections.observableArrayList();

    private Student selectedStudent = null;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Init ──────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupStudentTable();
        setupGuardianTable();
        loadStudents();

        // Update guardian panel when student selection changes
        studentTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, now) -> onStudentSelected(now));
    }

    // ── Student table setup ───────────────────────────────────

    private void setupStudentTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        colName.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getFullName()));

        colNameSi.setCellValueFactory(new PropertyValueFactory<>("firstNameSi"));

        // Format dates as dd/MM/yyyy
        colDob.setCellValueFactory(new PropertyValueFactory<>("dateOfBirth"));
        colDob.setCellFactory(col -> dateCellFactory());

        colGender.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getGenderDisplay()));

        colNic.setCellValueFactory(new PropertyValueFactory<>("nic"));

        colEnrolled.setCellValueFactory(new PropertyValueFactory<>("enrollmentDate"));
        colEnrolled.setCellFactory(col -> dateCellFactory());

        // Colour-coded status label
        colStatus.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getStatusDisplay()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("Active".equals(item)
                        ? "-fx-text-fill: #22c55e; -fx-font-weight: bold;"
                        : "-fx-text-fill: #ef4444; -fx-font-weight: bold;");
            }
        });

        // Action buttons: Edit | Deactivate/Activate
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("Edit");
            private final Button btnToggle = new Button();
            private final Button btnDelete = new Button("Delete");
            private final HBox   box       = new HBox(6, btnEdit, btnToggle, btnDelete);

            {
                btnEdit.getStyleClass().add("btn-secondary");
                btnToggle.getStyleClass().add("btn-danger");
                box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                btnEdit.setOnAction(e -> {
                    Student s = getTableView().getItems().get(getIndex());
                    handleEditStudent(s);
                });
                btnToggle.setOnAction(e -> {
                    Student s = getTableView().getItems().get(getIndex());
                    handleToggleActive(s);
                });
                btnDelete.getStyleClass().add("btn-danger");
                btnDelete.setOnAction(e -> {
                    Student s = getTableView().getItems().get(getIndex());
                    handleDeleteStudent(s);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Student s = getTableView().getItems().get(getIndex());
                btnToggle.setText(s.isActive() ? "Disable" : "Enable");
                setGraphic(box);
            }
        });

        studentTable.setItems(filtered);
    }

    // ── Deleting Student Records ───────────────────────────────

    private void handleDeleteStudent(Student s) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Permanently delete " + s.getFullName() + "?\n" +
                "This will also delete their guardians, attendance, marks, and invoices.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    // Hard delete — cascades handle related records via FK
                    studentDAO.delete(s.getId());
                    allStudents.remove(s);
                    applyFilter();
                } catch (SQLException ex) {
                    showError("Delete failed",
                            "Could not delete student — they may still have fee invoices.\n"
                            + ex.getMessage());
                }
            }
        });
    }

    // ── Guardian table setup ──────────────────────────────────

    private void setupGuardianTable() {
        gColName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        gColRelationship.setCellValueFactory(new PropertyValueFactory<>("relationship"));
        gColPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        gColEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        gColActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox   box       = new HBox(6, btnEdit, btnDelete);

            {
                btnEdit.getStyleClass().add("btn-secondary");
                btnDelete.getStyleClass().add("btn-danger");
                box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                btnEdit.setOnAction(e -> {
                    Guardian g = getTableView().getItems().get(getIndex());
                    handleEditGuardian(g);
                });
                btnDelete.setOnAction(e -> {
                    Guardian g = getTableView().getItems().get(getIndex());
                    handleDeleteGuardian(g);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        guardianTable.setItems(guardianList);
    }

    // ── Data loading ──────────────────────────────────────────

    private void loadStudents() {
        try {
            List<Student> data = studentDAO.getAll();
            allStudents.setAll(data);
            applyFilter();
        } catch (SQLException ex) {
            showError("Database error", "Could not load students: " + ex.getMessage());
        }
    }

    private void onStudentSelected(Student s) {
        selectedStudent = s;
        if (s == null) {
            guardianTitle.setText("Guardians — select a student above");
            btnAddGuardian.setDisable(true);
            guardianList.clear();
            return;
        }

        guardianTitle.setText("Guardians for " + s.getFullName());
        btnAddGuardian.setDisable(false);
        loadGuardians(s.getId());
    }

    private void loadGuardians(int studentId) {
        try {
            guardianList.setAll(guardianDAO.getByStudentId(studentId));
        } catch (SQLException ex) {
            showError("Database error", "Could not load guardians: " + ex.getMessage());
        }
    }

    // ── FXML handlers — students ──────────────────────────────

    @FXML
    private void handleSearch() {
        applyFilter();
    }

    private void applyFilter() {
        String q           = searchField.getText().trim().toLowerCase();
        boolean showAll    = showInactiveCheck.isSelected();

        filtered.setPredicate(s -> {
            if (!showAll && !s.isActive()) return false;
            if (q.isEmpty()) return true;

            return s.getFirstName().toLowerCase().contains(q)
                    || s.getLastName().toLowerCase().contains(q)
                    || (s.getFirstNameSi() != null && s.getFirstNameSi().contains(q))
                    || (s.getNic() != null && s.getNic().toLowerCase().contains(q));
        });
    }

    @FXML
    private void handleAddStudent() {
        showStudentDialog(null).ifPresent(s -> {
            try {
                studentDAO.insert(s);
                allStudents.add(s);
                applyFilter();
                studentTable.getSelectionModel().select(s);
            } catch (SQLException ex) {
                showError("Save failed", ex.getMessage());
            }
        });
    }

    private void handleEditStudent(Student s) {
        showStudentDialog(s).ifPresent(updated -> {
            try {
                studentDAO.update(updated);
                // Refresh the observable list entry
                int idx = allStudents.indexOf(s);
                if (idx >= 0) allStudents.set(idx, updated);
                applyFilter();
            } catch (SQLException ex) {
                showError("Save failed", ex.getMessage());
            }
        });
    }

    private void handleToggleActive(Student s) {
        String action = s.isActive() ? "deactivate" : "re-activate";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to " + action + " " + s.getFullName() + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    boolean newState = !s.isActive();
                    studentDAO.setActive(s.getId(), newState);
                    s.setActive(newState);
                    // Force TableView to re-render the row
                    studentTable.refresh();
                } catch (SQLException ex) {
                    showError("Update failed", ex.getMessage());
                }
            }
        });
    }

    // ── FXML handlers — guardians ─────────────────────────────

    @FXML
    private void handleAddGuardian() {
        if (selectedStudent == null) return;
        showGuardianDialog(null, selectedStudent.getId()).ifPresent(g -> {
            try {
                guardianDAO.insert(g);
                guardianList.add(g);
            } catch (SQLException ex) {
                showError("Save failed", ex.getMessage());
            }
        });
    }

    private void handleEditGuardian(Guardian g) {
        showGuardianDialog(g, g.getStudentId()).ifPresent(updated -> {
            try {
                guardianDAO.update(updated);
                int idx = guardianList.indexOf(g);
                if (idx >= 0) guardianList.set(idx, updated);
            } catch (SQLException ex) {
                showError("Save failed", ex.getMessage());
            }
        });
    }

    private void handleDeleteGuardian(Guardian g) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete guardian \"" + g.getFullName() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    guardianDAO.delete(g.getId());
                    guardianList.remove(g);
                } catch (SQLException ex) {
                    showError("Delete failed", ex.getMessage());
                }
            }
        });
    }

    // ── Student dialog (add / edit) ───────────────────────────

    /**
     * Builds and shows an Add / Edit student dialog programmatically.
     *
     * @param existing null  → Add mode; non-null → Edit mode (fields pre-filled)
     * @return Optional with the saved Student, or empty if cancelled
     */
    private Optional<Student> showStudentDialog(Student existing) {
        boolean editing = existing != null;

        Dialog<Student> dlg = new Dialog<>();
        dlg.setTitle(editing ? "Edit Student" : "Add Student");
        dlg.setHeaderText(null);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(520);

        // ── Form fields ──
        TextField tfFirst      = new TextField();    tfFirst.setPromptText("First name *");
        TextField tfLast       = new TextField();    tfLast.setPromptText("Last name *");
        TextField tfFirstSi    = new TextField();    tfFirstSi.setPromptText("Name in Sinhala");
        DatePicker dpDob       = buildDatePicker();
        TextField tfNic        = new TextField();    tfNic.setPromptText("NIC (optional)");
        ComboBox<String> cbGender = new ComboBox<>(
                FXCollections.observableArrayList("Male", "Female"));
        cbGender.setPromptText("Select gender *");
        TextField tfReligion   = new TextField();    tfReligion.setPromptText("Religion (optional)");
        TextArea  taAddress    = new TextArea();
        taAddress.setPromptText("Address (optional)");
        taAddress.setPrefRowCount(3);
        taAddress.setWrapText(true);
        DatePicker dpEnrolled  = buildDatePicker();
        dpEnrolled.setValue(LocalDate.now());

        // Pre-fill for edit mode
        if (editing) {
            tfFirst.setText(existing.getFirstName());
            tfLast.setText(existing.getLastName());
            tfFirstSi.setText(existing.getFirstNameSi() != null ? existing.getFirstNameSi() : "");
            dpDob.setValue(existing.getDateOfBirth());
            tfNic.setText(existing.getNic() != null ? existing.getNic() : "");
            cbGender.setValue(existing.getGenderDisplay());
            tfReligion.setText(existing.getReligion() != null ? existing.getReligion() : "");
            taAddress.setText(existing.getAddress() != null ? existing.getAddress() : "");
            dpEnrolled.setValue(existing.getEnrollmentDate());
        }

        // ── Layout ──
        GridPane grid = formGrid();
        int row = 0;
        grid.addRow(row++, label("First Name (EN) *"), tfFirst);
        grid.addRow(row++, label("Last Name (EN) *"),  tfLast);
        grid.addRow(row++, label("Name in Sinhala"), tfFirstSi);
        grid.addRow(row++, label("Date of Birth *"),   dpDob);
        grid.addRow(row++, label("NIC"),               tfNic);
        grid.addRow(row++, label("Gender *"),          cbGender);
        grid.addRow(row++, label("Religion"),          tfReligion);
        grid.addRow(row++, label("Address"),           taAddress);
        grid.addRow(row,   label("Enrollment Date *"), dpEnrolled);

        GridPane.setHgrow(tfFirst,   Priority.ALWAYS);
        GridPane.setHgrow(cbGender,  Priority.ALWAYS);
        GridPane.setHgrow(taAddress, Priority.ALWAYS);

        dlg.getDialogPane().setContent(grid);

        // ── Validate & convert ──
        Button okBtn = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            StringBuilder errors = new StringBuilder();
            if (tfFirst.getText().isBlank())   errors.append("• First name is required\n");
            if (tfLast.getText().isBlank())    errors.append("• Last name is required\n");
            if (dpDob.getValue() == null)      errors.append("• Date of birth is required\n");
            if (cbGender.getValue() == null)   errors.append("• Gender is required\n");
            if (dpEnrolled.getValue() == null) errors.append("• Enrollment date is required\n");

            if (!errors.isEmpty()) {
                ev.consume();           // stop dialog from closing
                showError("Validation", errors.toString().trim());
            }
        });

        dlg.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;

            Student s = editing ? existing : new Student();
            s.setFirstName(tfFirst.getText().trim());
            s.setLastName(tfLast.getText().trim());
            s.setFirstNameSi(tfFirstSi.getText().trim());
            s.setDateOfBirth(dpDob.getValue());
            s.setNic(tfNic.getText().trim());
            s.setGender("Male".equals(cbGender.getValue()) ? "M" : "F");
            s.setReligion(tfReligion.getText().trim());
            s.setAddress(taAddress.getText().trim());
            s.setEnrollmentDate(dpEnrolled.getValue());
            if (!editing) s.setActive(true);
            return s;
        });

        return dlg.showAndWait();
    }

    // ── Guardian dialog (add / edit) ──────────────────────────

    private Optional<Guardian> showGuardianDialog(Guardian existing, int studentId) {
        boolean editing = existing != null;

        Dialog<Guardian> dlg = new Dialog<>();
        dlg.setTitle(editing ? "Edit Guardian" : "Add Guardian");
        dlg.setHeaderText(null);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(420);

        // ── Form fields ──
        TextField tfName  = new TextField(); tfName.setPromptText("Full name *");
        ComboBox<String> cbRel = new ComboBox<>(FXCollections.observableArrayList(
                "Father", "Mother", "Uncle", "Aunt", "Grandfather",
                "Grandmother", "Guardian", "Other"));
        cbRel.setEditable(true);
        cbRel.setPromptText("Relationship *");
        TextField tfPhone = new TextField(); tfPhone.setPromptText("Phone (optional)");
        TextField tfEmail = new TextField(); tfEmail.setPromptText("Email (optional)");

        if (editing) {
            tfName.setText(existing.getFullName());
            cbRel.setValue(existing.getRelationship());
            tfPhone.setText(existing.getPhone() != null ? existing.getPhone() : "");
            tfEmail.setText(existing.getEmail() != null ? existing.getEmail() : "");
        }

        GridPane grid = formGrid();
        grid.addRow(0, label("Full Name *"),    tfName);
        grid.addRow(1, label("Relationship *"), cbRel);
        grid.addRow(2, label("Phone"),          tfPhone);
        grid.addRow(3, label("Email"),          tfEmail);

        dlg.getDialogPane().setContent(grid);

        // Validate
        Button okBtn = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            StringBuilder errors = new StringBuilder();
            if (tfName.getText().isBlank())                              errors.append("• Full name is required\n");
            if (cbRel.getValue() == null || cbRel.getValue().isBlank()) errors.append("• Relationship is required\n");
            if (!errors.isEmpty()) {
                ev.consume();
                showError("Validation", errors.toString().trim());
            }
        });

        dlg.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            Guardian g = editing ? existing : new Guardian();
            g.setStudentId(studentId);
            g.setFullName(tfName.getText().trim());
            g.setRelationship(cbRel.getValue().trim());
            g.setPhone(tfPhone.getText().trim());
            g.setEmail(tfEmail.getText().trim());
            return g;
        });

        return dlg.showAndWait();
    }

    // ── Helpers ───────────────────────────────────────────────

    /** Pre-configured GridPane for all dialogs. */
    private GridPane formGrid() {
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(10);
        g.setPadding(new Insets(16));
        ColumnConstraints labelCol = new ColumnConstraints(150);
        ColumnConstraints fieldCol = new ColumnConstraints(200, 200, Double.MAX_VALUE);
        fieldCol.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(labelCol, fieldCol);
        return g;
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("form-label");
        return l;
    }

    /** DatePicker configured to use dd/MM/yyyy display format. */
    private DatePicker buildDatePicker() {
        DatePicker dp = new DatePicker();
        dp.setConverter(new StringConverter<>() {
            @Override public String toString(LocalDate d)   { return d == null ? "" : DATE_FMT.format(d); }
            @Override public LocalDate fromString(String s) {
                try { return (s == null || s.isBlank()) ? null : LocalDate.parse(s, DATE_FMT); }
                catch (Exception e) { return null; }
            }
        });
        dp.setMaxWidth(Double.MAX_VALUE);
        return dp;
    }

    /** Generic TableCell that formats a LocalDate. */
    private <T> TableCell<T, LocalDate> dateCellFactory() {
        return new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : DATE_FMT.format(item));
            }
        };
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}