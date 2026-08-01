package com.school.controller;

import com.school.controller.AcademicController;
import com.school.dao.AttendanceDAO;
import com.school.dao.ClassDAO;
import com.school.dao.DashboardDAO;
import com.school.dao.TimetableDAO;
import com.school.model.GradeThreshold;
import com.school.model.Invoice;
import com.school.model.PeriodConfig;
import com.school.model.SchoolClass;
import com.school.model.TimetableEntry;
import com.school.util.SessionManager;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.util.converter.DoubleStringConverter;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class DashboardController {

    @FXML private Label pageTitle;
    @FXML private VBox  dashContent;

    private final DashboardDAO  dao          = new DashboardDAO();
    private final AttendanceDAO attDAO       = new AttendanceDAO();
    private final TimetableDAO  timetableDAO = new TimetableDAO();
    private final ClassDAO      classDAO     = new ClassDAO();


    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ══════════════════════════════════════════════════════════
    //  INIT — route to role-specific builder
    // ══════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        String role = SessionManager.getInstance().getCurrentUser().getRole();
        try {
            switch (role) {
                case "principal" -> buildPrincipalDashboard();
                case "teacher"   -> buildTeacherDashboard();
                default          -> buildStaffDashboard();
            }
        } catch (SQLException ex) {
            dashContent.getChildren().add(errorLabel("Could not load dashboard: " + ex.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════
    //  PRINCIPAL
    // ══════════════════════════════════════════════════════════

    private void buildPrincipalDashboard() throws SQLException {
        pageTitle.setText("Principal Dashboard");

        // ── Stat cards ────────────────────────────────────────
        int    students    = dao.getTotalActiveStudents();
        int    classes     = dao.getActiveClassCount();
        double attPct      = dao.getTodayAttendancePct();
        double outstanding = dao.getTotalOutstandingFees();

        String attText = attPct < 0 ? "No data" : String.format("%.1f%%", attPct);

        HBox cards = statRow(
                statCard("👥  Students",     String.valueOf(students),            "#6366f1", "#ede9fe"),
                statCard("🏫  Classes",       String.valueOf(classes),             "#0891b2", "#e0f2fe"),
                statCard("✅  Attendance",    attText,                             "#16a34a", "#dcfce7"),
                statCard("💰  Outstanding",   "LKR " + String.format("%.2f", outstanding), "#dc2626", "#fee2e2")
        );
        dashContent.getChildren().add(cards);

        // ── Grade thresholds editor ───────────────────────────
        dashContent.getChildren().add(sectionTitle("📊  Grade Scale"));
        TableView<GradeThreshold> threshTable = buildThresholdsTable();
        Label hint = new Label("Double-click Min or Max to edit a threshold.");
        hint.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:12px;");
        dashContent.getChildren().addAll(threshTable, hint);

        // ── Quick info ────────────────────────────────────────
        dashContent.getChildren().add(sectionTitle("📆  Today"));
        Label dateLabel = new Label("Date: " + DATE_FMT.format(LocalDate.now()));
        dateLabel.setStyle("-fx-text-fill:#64748b; -fx-font-size:13px;");
        dashContent.getChildren().add(dateLabel);
    }

    /** Editable TableView for grade thresholds. */
    private TableView<GradeThreshold> buildThresholdsTable() throws SQLException {
        ObservableList<GradeThreshold> thresholds =
                FXCollections.observableArrayList(dao.getThresholds());

        TableView<GradeThreshold> table = new TableView<>(thresholds);
        table.setEditable(true);
        table.setPrefHeight(220);
        table.setMaxWidth(500);
        table.getStyleClass().add("table-view");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<GradeThreshold, String> colGrade = new TableColumn<>("Grade");
        colGrade.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getGradeSymbol()));
        colGrade.setEditable(false);
        colGrade.setMaxWidth(100);

        TableColumn<GradeThreshold, Double> colMin = new TableColumn<>("Min Mark");
        colMin.setCellValueFactory(cd ->
                new SimpleDoubleProperty(cd.getValue().getMinMark()).asObject());
        colMin.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colMin.setOnEditCommit(e -> {
            GradeThreshold gt = e.getRowValue();
            gt.setMinMark(e.getNewValue());
            saveThreshold(gt);
        });

        TableColumn<GradeThreshold, Double> colMax = new TableColumn<>("Max Mark");
        colMax.setCellValueFactory(cd ->
                new SimpleDoubleProperty(cd.getValue().getMaxMark()).asObject());
        colMax.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colMax.setOnEditCommit(e -> {
            GradeThreshold gt = e.getRowValue();
            gt.setMaxMark(e.getNewValue());
            saveThreshold(gt);
        });

        table.getColumns().addAll(colGrade, colMin, colMax);

        Label hint = new Label("Double-click Min or Max to edit a threshold.");
        hint.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:12px;");

        VBox box = new VBox(6, table, hint);
        box.setMaxWidth(500);

        // Wrap in a VBox so we can return a single node — but caller adds directly
        // Return the table; hint added to dashContent below
        dashContent.getChildren().add(hint);   // pre-add hint, table returned separately
        return table;
    }

    private void saveThreshold(GradeThreshold gt) {
        try {
            dao.updateThreshold(gt);
        } catch (SQLException ex) {
            showError("Save failed", ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════
    //  TEACHER
    // ══════════════════════════════════════════════════════════

    private void buildTeacherDashboard() throws SQLException {
        pageTitle.setText("Dashboard");

        int userId = SessionManager.getInstance().getCurrentUser().getId();
        int tpId   = attDAO.getTeacherProfileId(userId);

        int classCount   = dao.getTeacherClassCount(tpId);
        int studentCount = dao.getTeacherStudentCount(tpId);
        int periodsToday = dao.getTeacherPeriodsMarkedToday(tpId);

        HBox cards = statRow(
                statCard("🏫  My Classes",    String.valueOf(classCount),   "#6366f1", "#ede9fe"),
                statCard("👥  My Students",   String.valueOf(studentCount), "#0891b2", "#e0f2fe"),
                statCard("✅  Periods Today", String.valueOf(periodsToday), "#16a34a", "#dcfce7")
        );
        dashContent.getChildren().add(cards);

        // ── Class summary table ───────────────────────────────
        dashContent.getChildren().add(sectionTitle("📋  My Classes"));

        List<String[]> classSummary = dao.getTeacherClassSummary(tpId);
        if (classSummary.isEmpty()) {
            dashContent.getChildren().add(emptyLabel("No classes assigned in the current year."));
        } else {
            TableView<String[]> table = new TableView<>(
                    FXCollections.observableArrayList(classSummary));
            table.setPrefHeight(Math.min(200, 40 + classSummary.size() * 36));
            table.setMaxWidth(400);
            table.setEditable(false);
            table.getStyleClass().add("table-view");
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            TableColumn<String[], String> colClass = new TableColumn<>("Class");
            colClass.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[0]));

            TableColumn<String[], String> colSubjects = new TableColumn<>("Subjects Assigned");
            colSubjects.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[1]));
            colSubjects.setMaxWidth(160);

            table.getColumns().addAll(colClass, colSubjects);
            dashContent.getChildren().add(table);
        }

        // ── Timetable ──────────────────────────────────────────
        dashContent.getChildren().add(sectionTitle("🗓  My Timetable"));
        
        List<PeriodConfig> periodConfigs = timetableDAO.getPeriodConfigs();
        List<SchoolClass>  myClasses     = classDAO.getAll().stream()
                .filter(c -> {
                    try {
                        return new AttendanceDAO().getClassesForTeacher(tpId)
                                .stream().anyMatch(mc -> mc.getId() == c.getId());
                    } catch (SQLException e) { return false; }
                })
                .toList();
        
        if (myClasses.isEmpty() || periodConfigs.isEmpty()) {
            dashContent.getChildren().add(emptyLabel("No timetable data available."));
        } else {
            // If teacher has multiple classes, show a ComboBox to pick
            if (myClasses.size() == 1) {
                SchoolClass cls  = myClasses.get(0);
                Map<String, TimetableEntry> slots = timetableDAO.getByClass(cls.getId());
                AcademicController helper = new AcademicController();
                try {
                    GridPane grid = helper.buildTimetableGrid(cls, slots, periodConfigs, true);
                    dashContent.getChildren().add(grid);
                } catch (SQLException ex) {
                    dashContent.getChildren().add(emptyLabel("Could not load timetable."));
                }
            } else {
                // Multiple classes — picker + grid
                ComboBox<SchoolClass> clsPicker = new ComboBox<>(
                        FXCollections.observableArrayList(myClasses));
                clsPicker.setPromptText("Select class to view timetable…");
                clsPicker.setPrefWidth(200);
                dashContent.getChildren().add(clsPicker);
        
                VBox gridHolder = new VBox();
                dashContent.getChildren().add(gridHolder);

                clsPicker.valueProperty().addListener((obs, old, now) -> {
                    if (now == null) return;
                    try {
                        Map<String, TimetableEntry> slots = timetableDAO.getByClass(now.getId());
                        AcademicController helper = new AcademicController();
                        GridPane grid = helper.buildTimetableGrid(now, slots, periodConfigs, true);
                        gridHolder.getChildren().setAll(grid);
                    } catch (SQLException ex) {
                        gridHolder.getChildren().setAll(new Label("Could not load timetable."));
                    }
                });

                // Auto-select first class
                clsPicker.setValue(myClasses.get(0));
            }
        }

        // Date
        dashContent.getChildren().add(sectionTitle("📆  Today"));
        Label dateLabel = new Label("Date: " + DATE_FMT.format(LocalDate.now()));
        dateLabel.setStyle("-fx-text-fill:#64748b; -fx-font-size:13px;");
        dashContent.getChildren().add(dateLabel);
    }

    // ══════════════════════════════════════════════════════════
    //  STAFF
    // ══════════════════════════════════════════════════════════

    private void buildStaffDashboard() throws SQLException {
        pageTitle.setText("Dashboard");

        int    totalStudents  = dao.getTotalActiveStudents();
        int    unpaidCount    = dao.getUnpaidInvoiceCount();
        double outstanding    = dao.getTotalOutstandingFees();
        double collectedMonth = dao.getCollectedThisMonth();

        HBox cards = statRow(
                statCard("👥  Students",       String.valueOf(totalStudents),                   "#6366f1", "#ede9fe"),
                statCard("💵  Unpaid",          String.valueOf(unpaidCount),                     "#dc2626", "#fee2e2"),
                statCard("💰  Outstanding",     "LKR " + String.format("%.2f", outstanding),    "#ea580c", "#ffedd5"),
                statCard("✅  This Month",       "LKR " + String.format("%.2f", collectedMonth), "#16a34a", "#dcfce7")
        );
        dashContent.getChildren().add(cards);

        // ── Overdue / unpaid invoices ─────────────────────────
        dashContent.getChildren().add(sectionTitle("💳  Pending Invoices"));

        List<Invoice> overdue = dao.getOverdueInvoices(15);
        if (overdue.isEmpty()) {
            dashContent.getChildren().add(emptyLabel("No outstanding invoices. 🎉"));
        } else {
            TableView<Invoice> table = new TableView<>(FXCollections.observableArrayList(overdue));
            table.setPrefHeight(Math.min(350, 44 + overdue.size() * 36));
            table.setEditable(false);
            table.getStyleClass().add("table-view");
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            TableColumn<Invoice, String> colStudent = new TableColumn<>("Student");
            colStudent.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("studentName"));
            colStudent.setMinWidth(160);

            TableColumn<Invoice, String> colDue = new TableColumn<>("Due Date");
            colDue.setCellValueFactory(cd ->
                    new SimpleStringProperty(cd.getValue().getDueDate() != null
                            ? DATE_FMT.format(cd.getValue().getDueDate()) : "—"));
            colDue.setMaxWidth(110);

            TableColumn<Invoice, Double> colOutstanding = new TableColumn<>("Outstanding (LKR)");
            colOutstanding.setCellValueFactory(cd ->
                    new SimpleDoubleProperty(cd.getValue().getOutstanding()).asObject());
            colOutstanding.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(Double item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); setStyle(""); return; }
                    setText(String.format("%.2f", item));
                    setStyle("-fx-text-fill:#dc2626; -fx-font-weight:bold;");
                }
            });
            colOutstanding.setMaxWidth(160);

            TableColumn<Invoice, String> colStatus = new TableColumn<>("Status");
            colStatus.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("statusDisplay"));
            colStatus.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); setStyle(""); return; }
                    setText(item);
                    setStyle(switch (item) {
                        case "Overdue" -> "-fx-text-fill:#dc2626;-fx-font-weight:bold;";
                        case "Partial" -> "-fx-text-fill:#d97706;-fx-font-weight:bold;";
                        default        -> "-fx-text-fill:#64748b;-fx-font-weight:bold;";
                    });
                }
            });
            colStatus.setMaxWidth(100);

            table.getColumns().addAll(colStudent, colDue, colOutstanding, colStatus);
            dashContent.getChildren().add(table);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  UI HELPERS
    // ══════════════════════════════════════════════════════════

    /** Row of up to 4 stat cards. */
    private HBox statRow(VBox... cards) {
        HBox row = new HBox(16);
        row.getChildren().addAll(cards);
        for (VBox card : cards) HBox.setHgrow(card, Priority.ALWAYS);
        return row;
    }

    /**
     * A coloured summary card.
     *
     * @param title   card label
     * @param value   big number/text
     * @param fg      text colour hex
     * @param bg      background colour hex
     */
    private VBox statCard(String title, String value, String fg, String bg) {
        Label lValue = new Label(value);
        lValue.setStyle("-fx-font-size:28px; -fx-font-weight:bold; -fx-text-fill:" + fg + ";");

        Label lTitle = new Label(title);
        lTitle.setStyle("-fx-font-size:13px; -fx-text-fill:#64748b;");

        VBox card = new VBox(4, lValue, lTitle);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(18, 20, 18, 20));
        card.setStyle("-fx-background-color:" + bg + ";"
                + "-fx-background-radius:10;"
                + "-fx-border-color:" + fg + "33;"
                + "-fx-border-radius:10;"
                + "-fx-border-width:1;");
        card.setMinWidth(160);
        return card;
    }

    private Label sectionTitle(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#1e293b;"
                + "-fx-padding:8 0 0 0;");
        return l;
    }

    private Label emptyLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:13px;");
        return l;
    }

    private Label errorLabel(String text) {
        Label l = new Label("⚠  " + text);
        l.setStyle("-fx-text-fill:#dc2626; -fx-font-size:13px;");
        return l;
    }

    private void showError(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}
