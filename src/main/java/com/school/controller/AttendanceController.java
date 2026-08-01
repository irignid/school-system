package com.school.controller;

import com.school.dao.AttendanceDAO;
import com.school.model.Attendance;
import com.school.model.AttendanceRecord;
import com.school.model.SchoolClass;
import com.school.model.Student;
import com.school.util.SessionManager;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controller for views/attendance.fxml.
 *
 * Role behaviour:
 *  - Teacher   → classCombo shows only their assigned classes.
 *  - Principal → classCombo shows all classes in the current year.
 *
 * The status column uses 4 colour-coded ToggleButtons (P | A | L | ML)
 * for fast one-click marking. The summary bar updates live.
 */
public class AttendanceController {

    // ── FXML ──────────────────────────────────────────────────
    @FXML private Label     pageTitle;
    @FXML private ComboBox<SchoolClass> classCombo;
    @FXML private DatePicker            datePicker;
    @FXML private ComboBox<Integer>     periodCombo;
    @FXML private HBox      summaryBar;
    @FXML private Label     lblPresent;
    @FXML private Label     lblAbsent;
    @FXML private Label     lblLate;
    @FXML private Label     lblMedical;

    @FXML private TableView<AttendanceRecord>              attendanceTable;
    @FXML private TableColumn<AttendanceRecord, Number>    colNum;
    @FXML private TableColumn<AttendanceRecord, String>    colName;
    @FXML private TableColumn<AttendanceRecord, String>    colNameSi;
    @FXML private TableColumn<AttendanceRecord, Void>      colStatus;

    @FXML private Label  statusLabel;
    @FXML private Button btnSubmit;

    // ── State ─────────────────────────────────────────────────
    private final AttendanceDAO dao = new AttendanceDAO();
    private final ObservableList<AttendanceRecord> records = FXCollections.observableArrayList();

    private int  teacherProfileId = 0;   // 0 means principal / non-teacher
    private boolean isPrincipal   = false;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Init ──────────────────────────────────────────────────

    @FXML
    public void initialize() {
        String role = SessionManager.getInstance().getCurrentUser().getRole();
        isPrincipal = "principal".equals(role);

        if ("teacher".equals(role)) {
            pageTitle.setText("Mark Attendance");
            int userId = SessionManager.getInstance().getCurrentUser().getId();
            try { teacherProfileId = dao.getTeacherProfileId(userId); }
            catch (SQLException ex) { showError("Startup error", ex.getMessage()); return; }
        }

        setupFilters();
        setupTable();
        loadClasses();
    }

    // ── Filter setup ──────────────────────────────────────────

    private void setupFilters() {
        // Date picker defaults to today, formatted dd/MM/yyyy
        datePicker.setValue(LocalDate.now());
        datePicker.setConverter(new StringConverter<>() {
            @Override public String toString(LocalDate d)   { return d == null ? "" : DATE_FMT.format(d); }
            @Override public LocalDate fromString(String s) {
                try { return (s == null || s.isBlank()) ? null : LocalDate.parse(s, DATE_FMT); }
                catch (Exception e) { return null; }
            }
        });

        // Periods 1–8
        periodCombo.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5, 6, 7, 8));
        periodCombo.setValue(1);

        // Class combo shows either all classes (principal) or only assigned classes (teacher)
        classCombo.setConverter(new StringConverter<SchoolClass>() {
            @Override public String toString(SchoolClass c) { 
                return c == null ? "" : "Grade " + c.getGradeLevel() + " - " + c.getSection(); 
            }
            @Override public SchoolClass fromString(String s) { return null; }
        });
    }

    private void loadClasses() {
        try {
            List<SchoolClass> classes = isPrincipal
                    ? dao.getAllCurrentClasses()
                    : dao.getClassesForTeacher(teacherProfileId);
            classCombo.setItems(FXCollections.observableArrayList(classes));
        } catch (SQLException ex) {
            showError("Could not load classes", ex.getMessage());
        }
    }

    // ── Table setup ───────────────────────────────────────────

    private void setupTable() {
        // Row number
        colNum.setCellValueFactory(cd ->
                new SimpleIntegerProperty(attendanceTable.getItems().indexOf(cd.getValue()) + 1));

        colName.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getStudentName()));

        colNameSi.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getStudentNameSi()));

        // Status column — 4 colour-coded ToggleButtons per row
        colStatus.setCellFactory(col -> new StatusCell());

        attendanceTable.setItems(records);
    }

    // ── Load handler ──────────────────────────────────────────

    @FXML
    private void handleLoad() {
        SchoolClass cls = classCombo.getValue();
        LocalDate   date = datePicker.getValue();
        Integer     period = periodCombo.getValue();

        if (cls == null || date == null || period == null) {
            showError("Missing selection", "Please select a class, date, and period.");
            return;
        }

        try {
            List<Student>       students = dao.getEnrolledStudents(cls.getId());
            Map<Integer,String> existing = dao.getExisting(cls.getId(), date, period);

            if (students.isEmpty()) {
                records.clear();
                btnSubmit.setDisable(true);
                statusLabel.setText("No enrolled students found for this class.");
                hideSummary();
                return;
            }

            records.clear();
            for (Student s : students) {
                String status = existing.getOrDefault(s.getId(), "P");
                AttendanceRecord rec = new AttendanceRecord(
                        s.getId(), s.getFullName(), s.getFirstNameSi(), status);
                // Update summary whenever any status changes
                rec.statusProperty().addListener((obs, old, now) -> updateSummary());
                records.add(rec);
            }

            btnSubmit.setDisable(false);
            showSummary();
            updateSummary();

            boolean alreadyMarked = !existing.isEmpty();
            statusLabel.setText(alreadyMarked
                    ? "⚠  Already submitted — editing will overwrite."
                    : "Loaded " + students.size() + " students. Mark attendance and submit.");

        } catch (SQLException ex) {
            showError("Load failed", ex.getMessage());
        }
    }

    // ── Submit handler ────────────────────────────────────────

    @FXML
    private void handleSubmit() {
        SchoolClass cls    = classCombo.getValue();
        LocalDate   date   = datePicker.getValue();
        Integer     period = periodCombo.getValue();

        if (cls == null || date == null || period == null || records.isEmpty()) return;

        // Resolve who is submitting
        int markerId = teacherProfileId;
        if (isPrincipal && markerId == 0) {
            // Principal doesn't have a teacher profile; use 0 is not allowed
            // by FK constraint. We prevent submission from principal for now.
            showError("Not permitted",
                    "Attendance must be submitted by the class teacher.");
            return;
        }

        List<Attendance> batch = new ArrayList<>();
        for (AttendanceRecord rec : records) {
            batch.add(new Attendance(rec.getStudentId(), cls.getId(),
                    date, period, rec.getStatus(), markerId));
        }

        try {
            dao.saveAll(batch, markerId);
            statusLabel.setText("✔  Saved successfully at "
                    + java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        } catch (SQLException ex) {
            showError("Save failed", ex.getMessage());
        }
    }

    // ── Summary bar ───────────────────────────────────────────

    private void updateSummary() {
        long p  = records.stream().filter(r -> "P" .equals(r.getStatus())).count();
        long a  = records.stream().filter(r -> "A" .equals(r.getStatus())).count();
        long l  = records.stream().filter(r -> "L" .equals(r.getStatus())).count();
        long ml = records.stream().filter(r -> "ML".equals(r.getStatus())).count();

        lblPresent.setText("Present  " + p);
        lblAbsent .setText("Absent  "  + a);
        lblLate   .setText("Late  "    + l);
        lblMedical.setText("Medical  " + ml);
    }

    private void showSummary() {
        summaryBar.setVisible(true);
        summaryBar.setManaged(true);
    }

    private void hideSummary() {
        summaryBar.setVisible(false);
        summaryBar.setManaged(false);
    }

    // ── Helpers ───────────────────────────────────────────────

    private void showError(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }

    // ══════════════════════════════════════════════════════════
    //  INNER: Status cell with 4 ToggleButtons
    // ══════════════════════════════════════════════════════════

    /**
     * A table cell that shows 4 ToggleButtons: P | A | L | ML.
     * The active button is colour-coded. Clicking updates the record's
     * status property immediately (which triggers the summary bar update).
     *
     * The {@code updating} flag prevents the ToggleGroup listener from
     * accidentally writing back to the wrong record during cell recycling.
     */
    private static class StatusCell extends TableCell<AttendanceRecord, Void> {

        private final ToggleGroup  group  = new ToggleGroup();
        private final ToggleButton btnP   = makeBtn("Present",       "P",  "#16a34a", "#dcfce7");
        private final ToggleButton btnA   = makeBtn("Absent",        "A",  "#dc2626", "#fee2e2");
        private final ToggleButton btnL   = makeBtn("Late",          "L",  "#ea580c", "#ffedd5");
        private final ToggleButton btnML  = makeBtn("Medical Leave", "ML", "#2563eb", "#dbeafe");
        private final HBox         box    = new HBox(6, btnP, btnA, btnL, btnML);

        private boolean updating = false;

        StatusCell() {
            btnP .setToggleGroup(group);
            btnA .setToggleGroup(group);
            btnL .setToggleGroup(group);
            btnML.setToggleGroup(group);
            box.setAlignment(Pos.CENTER_LEFT);

            group.selectedToggleProperty().addListener((obs, old, now) -> {
                if (updating) return;
                // Prevent full de-selection
                if (now == null) { updating = true; group.selectToggle(old); updating = false; return; }

                AttendanceRecord rec = currentRecord();
                if (rec == null) return;
                if      (now == btnP)  rec.setStatus("P");
                else if (now == btnA)  rec.setStatus("A");
                else if (now == btnL)  rec.setStatus("L");
                else if (now == btnML) rec.setStatus("ML");

                // Refresh button colours
                applyStyles(now);
            });
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                setGraphic(null); return;
            }
            AttendanceRecord rec = currentRecord();
            if (rec == null) { setGraphic(null); return; }

            updating = true;
            switch (rec.getStatus()) {
                case "A"  -> { group.selectToggle(btnA);  applyStyles(btnA); }
                case "L"  -> { group.selectToggle(btnL);  applyStyles(btnL); }
                case "ML" -> { group.selectToggle(btnML); applyStyles(btnML); }
                default   -> { group.selectToggle(btnP);  applyStyles(btnP); }
            }
            updating = false;

            setGraphic(box);
        }

        // ── Private helpers ────────────────────────────────────

        private AttendanceRecord currentRecord() {
            int idx = getIndex();
            if (idx < 0 || idx >= getTableView().getItems().size()) return null;
            return getTableView().getItems().get(idx);
        }

        private void applyStyles(Toggle selected) {
            resetStyle(btnP,  "#16a34a", "#dcfce7");
            resetStyle(btnA,  "#dc2626", "#fee2e2");
            resetStyle(btnL,  "#ea580c", "#ffedd5");
            resetStyle(btnML, "#2563eb", "#dbeafe");

            if (selected instanceof ToggleButton tb) {
                String[] colours = colourFor(tb);
                tb.setStyle("-fx-background-color:" + colours[0]
                        + ";-fx-text-fill:white;-fx-font-weight:bold;"
                        + "-fx-background-radius:6;-fx-cursor:hand;");
            }
        }

        private void resetStyle(ToggleButton btn, String fg, String bg) {
            btn.setStyle("-fx-background-color:" + bg
                    + ";-fx-text-fill:" + fg
                    + ";-fx-font-weight:bold;-fx-background-radius:6;-fx-cursor:hand;");
        }

        private String[] colourFor(ToggleButton tb) {
            if (tb == btnP)  return new String[]{"#16a34a", "#dcfce7"};
            if (tb == btnA)  return new String[]{"#dc2626", "#fee2e2"};
            if (tb == btnL)  return new String[]{"#ea580c", "#ffedd5"};
            if (tb == btnML) return new String[]{"#2563eb", "#dbeafe"};
            return new String[]{"#334155", "#e2e8f0"};
        }

        private static ToggleButton makeBtn(String text, String code,
                                            String fg, String bg) {
            ToggleButton btn = new ToggleButton(text);
            btn.setPrefWidth(code.equals("ML") ? 120 : 80);
            btn.setStyle("-fx-background-color:" + bg
                    + ";-fx-text-fill:" + fg
                    + ";-fx-font-weight:bold;-fx-background-radius:6;-fx-cursor:hand;");
            return btn;
        }
    }
}