package com.school.controller;

import com.school.dao.*;
import com.school.model.*;
import com.school.service.ReportCardService;
import com.school.util.SessionManager;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Controller for views/exams.fxml.
 *
 * Principal  → all 3 tabs visible, classCombo shows all classes.
 * Teacher    → Tab 1 (Exams) hidden, Tab 3 (View Grades) hidden,
 *              classCombo shows only their assigned classes.
 */
public class ExamsController {

    // ── DAOs ──────────────────────────────────────────────────
    private final ExamDAO             examDAO           = new ExamDAO();
    private final ExamScheduleDAO     scheduleDAO       = new ExamScheduleDAO();
    private final MarkDAO             markDAO           = new MarkDAO();
    private final GradeThresholdDAO   gradeDAO          = new GradeThresholdDAO();
    private final AttendanceDAO       attDAO            = new AttendanceDAO();  // reuse getClassesForTeacher
    private final TermDAO             termDAO           = new TermDAO();
    private final AcademicYearDAO     yearDAO           = new AcademicYearDAO();
    private final ReportCardDAO       reportCardDAO     = new ReportCardDAO();
    private final ReportCardService   reportCardService = new ReportCardService();

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // ── Session info ──────────────────────────────────────────
    private int     currentUserId       = 0;
    private int     teacherProfileId    = 0;
    private boolean isPrincipal         = false;

    private List<SchoolClass> availableClasses;

    // ── FXML: general ─────────────────────────────────────────
    @FXML private Label    pageTitle;
    @FXML private TabPane  tabPane;
    @FXML private Tab      tabExams;
    @FXML private Tab      tabMarks;
    @FXML private Tab      tabGrades;
    @FXML private Tab      tabReportCards;

    // ── FXML: Tab 1 — Exams ───────────────────────────────────
    @FXML private ComboBox<SchoolClass>         examClassFilter;
    @FXML private TableView<Exam>               examTable;
    @FXML private TableColumn<Exam, String>     examColName;
    @FXML private TableColumn<Exam, String>     examColClass;
    @FXML private TableColumn<Exam, String>     examColTerm;
    @FXML private TableColumn<Exam, String>     examColYear;
    @FXML private TableColumn<Exam, Void>       examColActions;

    @FXML private Label                              scheduleTitle;
    @FXML private Button                             btnAddSchedule;
    @FXML private TableView<ExamSchedule>           scheduleTable;
    @FXML private TableColumn<ExamSchedule, String> schColSubject;
    @FXML private TableColumn<ExamSchedule, String> schColDate;
    @FXML private TableColumn<ExamSchedule, String> schColTime;
    @FXML private TableColumn<ExamSchedule, String> schColDuration;
    @FXML private TableColumn<ExamSchedule, Void>   schColActions;

    private Exam selectedExam = null;

    // ── FXML: Tab 2 — Enter Marks ─────────────────────────────
    @FXML private ComboBox<SchoolClass>  marksClassCombo;
    @FXML private ComboBox<Exam>         marksExamCombo;
    @FXML private ComboBox<ExamSchedule> marksSubjectCombo;
    @FXML private Label                  marksMaxLabel;
    @FXML private TextField              marksMaxField;

    @FXML private TableView<Mark>               marksTable;
    @FXML private TableColumn<Mark, Number>     mkColNum;
    @FXML private TableColumn<Mark, String>     mkColName;
    @FXML private TableColumn<Mark, String>     mkColNameSi;
    @FXML private TableColumn<Mark, Void>       mkColMark;
    @FXML private TableColumn<Mark, String>     mkColGrade;

    @FXML private Label  marksStatusLabel;
    @FXML private Button btnSaveMarks;

    private final ObservableList<Mark> marksList = FXCollections.observableArrayList();

    // ── FXML: Tab 3 — View Grades ─────────────────────────────
    @FXML private ComboBox<SchoolClass> gradesClassCombo;
    @FXML private ComboBox<Exam>        gradesExamCombo;

    @FXML private TableView<Mark>               gradesTable;
    @FXML private TableColumn<Mark, String>     grColStudent;
    @FXML private TableColumn<Mark, String>     grColSubject;
    @FXML private TableColumn<Mark, String>     grColMark;
    @FXML private TableColumn<Mark, String>     grColMax;
    @FXML private TableColumn<Mark, Number>     grColPct;
    @FXML private TableColumn<Mark, String>     grColGrade;

    @FXML private TableView<GradeThreshold>               thresholdTable;
    @FXML private TableColumn<GradeThreshold, String>     thrColGrade;
    @FXML private TableColumn<GradeThreshold, String>     thrColRange;

    // ── FXML: Tab 4 — Report Cards ─────────────────────────────
    @FXML private ComboBox<SchoolClass>            rcClassCombo;
    @FXML private ComboBox<Term>                   rcTermCombo;
    @FXML private TableView<Student>               rcStudentTable;
    @FXML private TableColumn<Student, String>     rcColName;
    @FXML private TableColumn<Student, String>     rcColNameSi;
    @FXML private TableColumn<Student, Void>       rcColActions;
    @FXML private Label                            rcStatusLabel;

    // ══════════════════════════════════════════════════════════
    //  INIT
    // ══════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        currentUserId = user.getId();
        isPrincipal   = "principal".equals(user.getRole());

        if (!isPrincipal) {
            pageTitle.setText("Enter Marks");
            tabPane.getTabs().remove(tabGrades);
            tabPane.getTabs().remove(tabExams);
            tabPane.getTabs().remove(tabReportCards);
            try { teacherProfileId = attDAO.getTeacherProfileId(currentUserId); }
            catch (SQLException ex) { showError("Startup error", ex.getMessage()); return; }
        }

        try {
            availableClasses = isPrincipal
                    ? new AttendanceDAO().getAllCurrentClasses()
                    : attDAO.getClassesForTeacher(teacherProfileId);

            if (isPrincipal) {
                setupExamTab();
                setupGradesTab();
                setupReportCardsTab();
            }
            setupMarksTab();

        } catch (SQLException ex) {
            showError("Startup error", ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════
    //  TAB 1 — EXAMS
    // ══════════════════════════════════════════════════════════

    private void setupExamTab() throws SQLException {
        // Class filter — "All classes" option plus each class
        ObservableList<SchoolClass> filterItems = FXCollections.observableArrayList();
        filterItems.addAll(availableClasses);
        examClassFilter.setItems(filterItems);
        examClassFilter.setPromptText("All classes");
        examClassFilter.valueProperty().addListener((obs, old, now) -> loadExams());

        examColName.setCellValueFactory(new PropertyValueFactory<>("name"));
        examColClass.setCellValueFactory(new PropertyValueFactory<>("classDisplay"));
        examColTerm.setCellValueFactory(new PropertyValueFactory<>("termDisplay"));
        examColYear.setCellValueFactory(new PropertyValueFactory<>("yearLabel"));

        examColActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox   box       = new HBox(6, btnEdit, btnDelete);
            {
                btnEdit.getStyleClass().add("btn-secondary");
                btnDelete.getStyleClass().add("btn-danger");
                box.setAlignment(Pos.CENTER_LEFT);
                btnEdit.setOnAction(e -> handleEditExam(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDeleteExam(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        // Schedule columns
        schColSubject.setCellValueFactory(new PropertyValueFactory<>("subjectName"));
        schColDate.setCellValueFactory(cd ->
                new SimpleStringProperty(DATE_FMT.format(cd.getValue().getDate())));
        schColTime.setCellValueFactory(cd ->
                new SimpleStringProperty(TIME_FMT.format(cd.getValue().getStartTime())));
        schColDuration.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getDurationMinutes() + " min"));
        schColActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnRemove = new Button("Remove");
            {
                btnRemove.getStyleClass().add("btn-danger");
                btnRemove.setOnAction(e -> handleDeleteSchedule(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnRemove);
            }
        });

        // Exam selection → load schedule
        examTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, now) -> onExamSelected(now));

        loadExams();
    }

    private void loadExams() {
        try {
            SchoolClass cls = examClassFilter != null ? examClassFilter.getValue() : null;
            List<Exam> exams = cls != null
                    ? examDAO.getByClass(cls.getId())
                    : examDAO.getAll();
            examTable.setItems(FXCollections.observableArrayList(exams));
        } catch (SQLException ex) {
            showError("Database error", ex.getMessage());
        }
    }

    private void onExamSelected(Exam exam) {
        selectedExam = exam;
        if (exam == null) {
            scheduleTitle.setText("Schedule — select an exam above");
            btnAddSchedule.setDisable(true);
            scheduleTable.getItems().clear();
            return;
        }
        scheduleTitle.setText("Schedule — " + exam.getName());
        btnAddSchedule.setDisable(false);
        loadSchedule(exam.getId());
    }

    private void loadSchedule(int examId) {
        try {
            scheduleTable.setItems(FXCollections.observableArrayList(scheduleDAO.getByExam(examId)));
        } catch (SQLException ex) {
            showError("Database error", ex.getMessage());
        }
    }

    @FXML
    private void handleCreateExam() {
        showExamDialog(null).ifPresent(e -> {
            try {
                examDAO.insert(e);
                loadExams();
            } catch (SQLException ex) {
                showError("Save failed", ex.getMessage());
            }
        });
    }

    private void handleEditExam(Exam e) {
        showExamDialog(e).ifPresent(updated -> {
            try {
                examDAO.update(updated);
                loadExams();
            } catch (SQLException ex) {
                showError("Save failed", ex.getMessage());
            }
        });
    }

    private void handleDeleteExam(Exam e) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete exam \"" + e.getName() + "\"?\nAll marks and schedules will also be deleted.",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try { examDAO.delete(e.getId()); loadExams(); onExamSelected(null); }
                catch (SQLException ex) { showError("Delete failed", ex.getMessage()); }
            }
        });
    }

    @FXML
    private void handleAddSchedule() {
        if (selectedExam == null) return;
        try {
            List<Subject> unscheduled = scheduleDAO.getUnscheduledSubjects(
                    selectedExam.getId(), selectedExam.getClassId());
            if (unscheduled.isEmpty()) {
                showError("Nothing to add", "All subjects for this class already have a schedule entry.");
                return;
            }
            showScheduleDialog(selectedExam, unscheduled).ifPresent(es -> {
                try {
                    scheduleDAO.insert(es);
                    loadSchedule(selectedExam.getId());
                } catch (SQLException ex) { showError("Save failed", ex.getMessage()); }
            });
        } catch (SQLException ex) { showError("Database error", ex.getMessage()); }
    }

    private void handleDeleteSchedule(ExamSchedule es) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove \"" + es.getSubjectName() + "\" from the schedule?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try { scheduleDAO.delete(es.getId()); loadSchedule(selectedExam.getId()); }
                catch (SQLException ex) { showError("Delete failed", ex.getMessage()); }
            }
        });
    }

    // ── Exam dialog ───────────────────────────────────────────

    private Optional<Exam> showExamDialog(Exam existing) {
        boolean editing = existing != null;
        Dialog<Exam> dlg = new Dialog<>();
        dlg.setTitle(editing ? "Edit Exam" : "Create Exam");
        dlg.setHeaderText(null);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(440);

        TextField tfName = new TextField();
        tfName.setPromptText("Exam name *");

        ComboBox<SchoolClass> cbClass = new ComboBox<>(FXCollections.observableArrayList(availableClasses));
        cbClass.setPromptText("Select class *");
        cbClass.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Term> cbTerm = new ComboBox<>();
        cbTerm.setPromptText("Select term *");
        cbTerm.setMaxWidth(Double.MAX_VALUE);

        // Load terms when class changes (need year from class)
        cbClass.valueProperty().addListener((obs, old, now) -> {
            if (now == null) { cbTerm.getItems().clear(); return; }
            try {
                AcademicYear ay = yearDAO.getCurrent();
                if (ay != null) cbTerm.setItems(
                        FXCollections.observableArrayList(termDAO.getByYear(ay.getId())));
            } catch (SQLException ex) { showError("Database error", ex.getMessage()); }
        });

        if (editing) {
            tfName.setText(existing.getName());
            availableClasses.stream()
                    .filter(c -> c.getId() == existing.getClassId())
                    .findFirst().ifPresent(cbClass::setValue);
            // Term loaded via listener above; set after
        }

        GridPane grid = formGrid();
        grid.addRow(0, label("Exam Name *"), tfName);
        grid.addRow(1, label("Class *"),     cbClass);
        grid.addRow(2, label("Term *"),      cbTerm);
        GridPane.setHgrow(cbClass, Priority.ALWAYS);
        GridPane.setHgrow(cbTerm,  Priority.ALWAYS);
        dlg.getDialogPane().setContent(grid);

        // Pre-select term for edit (after items populated)
        if (editing) {
            cbClass.fireEvent(new javafx.event.ActionEvent());
            cbTerm.getItems().stream()
                    .filter(t -> t.getId() == existing.getTermId())
                    .findFirst().ifPresent(cbTerm::setValue);
        }

        Button ok = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            StringBuilder err = new StringBuilder();
            if (tfName.getText().isBlank())  err.append("• Exam name is required\n");
            if (cbClass.getValue() == null)  err.append("• Class is required\n");
            if (cbTerm.getValue() == null)   err.append("• Term is required\n");
            if (!err.isEmpty()) { ev.consume(); showError("Validation", err.toString().trim()); }
        });

        dlg.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            Exam e = editing ? existing : new Exam();
            e.setName(tfName.getText().trim());
            e.setClassId(cbClass.getValue().getId());
            e.setTermId(cbTerm.getValue().getId());
            e.setCreatedBy(currentUserId);
            e.setClassDisplay(cbClass.getValue().getDisplayName());
            e.setTermDisplay(cbTerm.getValue().getDisplayName());
            return e;
        });

        return dlg.showAndWait();
    }

    // ── Schedule dialog ───────────────────────────────────────

    private Optional<ExamSchedule> showScheduleDialog(Exam exam, List<Subject> subjects) {
        Dialog<ExamSchedule> dlg = new Dialog<>();
        dlg.setTitle("Add Subject — " + exam.getName());
        dlg.setHeaderText(null);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(420);

        ComboBox<Subject> cbSubject = new ComboBox<>(FXCollections.observableArrayList(subjects));
        cbSubject.setPromptText("Select subject *");
        cbSubject.setMaxWidth(Double.MAX_VALUE);

        DatePicker dpDate  = buildDatePicker();
        TextField  tfTime  = new TextField(); tfTime.setPromptText("HH:mm  e.g. 09:00 *");
        TextField  tfDur   = new TextField("180"); tfDur.setPromptText("Minutes *");

        GridPane grid = formGrid();
        grid.addRow(0, label("Subject *"),         cbSubject);
        grid.addRow(1, label("Exam Date *"),       dpDate);
        grid.addRow(2, label("Start Time *"),      tfTime);
        grid.addRow(3, label("Duration (min) *"),  tfDur);
        GridPane.setHgrow(cbSubject, Priority.ALWAYS);
        dlg.getDialogPane().setContent(grid);

        Button ok = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            StringBuilder err = new StringBuilder();
            if (cbSubject.getValue() == null) err.append("• Subject is required\n");
            if (dpDate.getValue() == null)    err.append("• Date is required\n");
            LocalTime parsedTime = parseTime(tfTime.getText());
            if (parsedTime == null)           err.append("• Valid start time required (HH:mm)\n");
            int dur = parseDuration(tfDur.getText());
            if (dur <= 0)                     err.append("• Duration must be a positive number\n");
            if (!err.isEmpty()) { ev.consume(); showError("Validation", err.toString().trim()); }
        });

        dlg.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            return new ExamSchedule(exam.getId(), cbSubject.getValue().getId(),
                    dpDate.getValue(), parseTime(tfTime.getText()),
                    parseDuration(tfDur.getText()));
        });

        return dlg.showAndWait();
    }

    // ══════════════════════════════════════════════════════════
    //  TAB 2 — ENTER MARKS
    // ══════════════════════════════════════════════════════════

    private void setupMarksTab() {
        marksClassCombo.setItems(FXCollections.observableArrayList(availableClasses));
        marksClassCombo.valueProperty().addListener((obs, old, now) -> {
            marksExamCombo.getItems().clear();
            marksSubjectCombo.getItems().clear();
            if (now == null) return;
            try {
                marksExamCombo.setItems(FXCollections.observableArrayList(examDAO.getByClass(now.getId())));
            } catch (SQLException ex) { showError("Database error", ex.getMessage()); }
        });

        marksExamCombo.valueProperty().addListener((obs, old, now) -> {
            marksSubjectCombo.getItems().clear();
            if (now == null) return;
            try {
                List<ExamSchedule> sch = scheduleDAO.getByExam(now.getId());
                marksSubjectCombo.setItems(FXCollections.observableArrayList(sch));
            } catch (SQLException ex) { showError("Database error", ex.getMessage()); }
        });

        // Give ComboBox a nice display for ExamSchedule
        marksSubjectCombo.setConverter(new StringConverter<>() {
            @Override public String toString(ExamSchedule es) {
                return es == null ? "" : es.getSubjectName();
            }
            @Override public ExamSchedule fromString(String s) { return null; }
        });

        // Columns
        mkColNum.setCellValueFactory(cd ->
                new SimpleIntegerProperty(marksTable.getItems().indexOf(cd.getValue()) + 1));
        mkColName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        mkColNameSi.setCellValueFactory(cd ->
                new SimpleStringProperty(""));   // populated via Mark.studentName only; si not in marks model

        // Editable mark column — TextField spinner inside cell
        mkColMark.setCellFactory(col -> new MarkEntryCell());

        // Live grade preview
        mkColGrade.setCellValueFactory(new PropertyValueFactory<>("gradeSymbol"));
        mkColGrade.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(gradeStyle(item));
            }
        });

        marksTable.setItems(marksList);
    }

    @FXML
    private void handleLoadMarks() {
        SchoolClass  cls     = marksClassCombo.getValue();
        Exam         exam    = marksExamCombo.getValue();
        ExamSchedule subject = marksSubjectCombo.getValue();

        if (cls == null || exam == null || subject == null) {
            showError("Missing selection", "Please select class, exam, and subject.");
            return;
        }

        try {
            List<Mark> loaded = markDAO.getMarksForEntry(cls.getId(), exam.getId(), subject.getSubjectId());

            // Resolve live grades for already-entered marks
            List<GradeThreshold> thresholds = gradeDAO.getAll();
            for (Mark mk : loaded) {
                if (mk.getMarkObtained() >= 0) {
                    mk.setGradeSymbol(calcGrade(mk.getPercentage(), thresholds));
                } else {
                    mk.setGradeSymbol("—");
                }
            }

            marksList.setAll(loaded);
            btnSaveMarks.setDisable(false);

            // Show max mark field
            marksMaxLabel.setVisible(true);  marksMaxLabel.setManaged(true);
            marksMaxField.setVisible(true);  marksMaxField.setManaged(true);
            marksMaxField.setText("100");

            marksStatusLabel.setText("Loaded " + loaded.size() + " students.");

        } catch (SQLException ex) {
            showError("Load failed", ex.getMessage());
        }
    }

    @FXML
    private void handleSaveMarks() {
        ExamSchedule subject = marksSubjectCombo.getValue();
        if (subject == null || marksList.isEmpty()) return;

        // Apply any max mark override
        double maxMark = 100.0;
        try { maxMark = Double.parseDouble(marksMaxField.getText().trim()); }
        catch (NumberFormatException ignored) {}

        final double finalMax = maxMark;
        List<GradeThreshold> thresholds;
        try { thresholds = gradeDAO.getAll(); }
        catch (SQLException ex) { showError("Database error", ex.getMessage()); return; }

        for (Mark mk : marksList) {
            mk.setMaxMark(finalMax);
            if (mk.getMarkObtained() >= 0)
                mk.setGradeSymbol(calcGrade(mk.getPercentage(), thresholds));
        }

        try {
            markDAO.saveAll(marksList, currentUserId);
            marksStatusLabel.setText("✔  Saved at "
                    + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
            marksTable.refresh();
        } catch (SQLException ex) {
            showError("Save failed", ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════
    //  TAB 3 — VIEW GRADES
    // ══════════════════════════════════════════════════════════

    private void setupGradesTab() throws SQLException {
        gradesClassCombo.setItems(FXCollections.observableArrayList(availableClasses));
        gradesClassCombo.valueProperty().addListener((obs, old, now) -> {
            gradesExamCombo.getItems().clear();
            if (now == null) return;
            try {
                gradesExamCombo.setItems(FXCollections.observableArrayList(examDAO.getByClass(now.getId())));
            } catch (SQLException ex) { showError("Database error", ex.getMessage()); }
        });

        grColStudent.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        grColSubject.setCellValueFactory(new PropertyValueFactory<>("subjectName"));
        grColMark.setCellValueFactory(cd -> new SimpleStringProperty(formatMark(cd.getValue().getMarkObtained())));
        grColMax.setCellValueFactory(cd  -> new SimpleStringProperty(formatMark(cd.getValue().getMaxMark())));
        grColPct.setCellValueFactory(cd  -> new SimpleDoubleProperty(cd.getValue().getPercentage()));

        grColGrade.setCellValueFactory(new PropertyValueFactory<>("gradeSymbol"));
        grColGrade.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(gradeStyle(item));
            }
        });

        // Threshold reference table
        thrColGrade.setCellValueFactory(new PropertyValueFactory<>("gradeSymbol"));
        thrColRange.setCellValueFactory(new PropertyValueFactory<>("range"));
        try {
            thresholdTable.setItems(FXCollections.observableArrayList(gradeDAO.getAll()));
        } catch (SQLException ex) { showError("Database error", ex.getMessage()); }
    }

    @FXML
    private void handleLoadGrades() {
        SchoolClass cls  = gradesClassCombo.getValue();
        Exam        exam = gradesExamCombo.getValue();
        if (cls == null || exam == null) {
            showError("Missing selection", "Please select a class and exam.");
            return;
        }
        try {
            gradesTable.setItems(FXCollections.observableArrayList(markDAO.getMarksWithGrades(exam.getId())));
        } catch (SQLException ex) {
            showError("Load failed", ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════
    //  SHARED HELPERS
    // ══════════════════════════════════════════════════════════

    /** Calculates grade symbol from percentage against the threshold list. */
    private String calcGrade(double pct, List<GradeThreshold> thresholds) {
        for (GradeThreshold gt : thresholds) {
            if (pct >= gt.getMinMark() && pct <= gt.getMaxMark()) return gt.getGradeSymbol();
        }
        return "E";
    }

    private String gradeStyle(String grade) {
        return switch (grade) {
            case "A+", "A" -> "-fx-text-fill:#16a34a;-fx-font-weight:bold;";
            case "B"       -> "-fx-text-fill:#2563eb;-fx-font-weight:bold;";
            case "C"       -> "-fx-text-fill:#d97706;-fx-font-weight:bold;";
            case "D"       -> "-fx-text-fill:#ea580c;-fx-font-weight:bold;";
            default        -> "-fx-text-fill:#dc2626;-fx-font-weight:bold;";
        };
    }

    private LocalTime parseTime(String s) {
        try { return (s == null || s.isBlank()) ? null : LocalTime.parse(s.trim(), TIME_FMT); }
        catch (Exception e) { return null; }
    }

    private int parseDuration(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return -1; }
    }

    private GridPane formGrid() {
        GridPane g = new GridPane();
        g.setHgap(12); g.setVgap(10); g.setPadding(new Insets(16));
        ColumnConstraints lc = new ColumnConstraints(150);
        ColumnConstraints fc = new ColumnConstraints(200, 200, Double.MAX_VALUE);
        fc.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(lc, fc);
        return g;
    }

    private Label label(String text) {
        Label l = new Label(text); l.getStyleClass().add("form-label"); return l;
    }

    private DatePicker buildDatePicker() {
        DatePicker dp = new DatePicker();
        dp.setMaxWidth(Double.MAX_VALUE);
        dp.setConverter(new StringConverter<>() {
            @Override public String toString(LocalDate d)   { return d == null ? "" : DATE_FMT.format(d); }
            @Override public LocalDate fromString(String s) {
                try { return (s == null || s.isBlank()) ? null : LocalDate.parse(s, DATE_FMT); }
                catch (Exception e) { return null; }
            }
        });
        return dp;
    }

    private void showError(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }

    // ══════════════════════════════════════════════════════════
    //  INNER: Mark entry cell (TextField for numeric input)
    // ══════════════════════════════════════════════════════════

    private class MarkEntryCell extends TableCell<Mark, Void> {

        private final TextField tf = new TextField();

        MarkEntryCell() {
            tf.setPrefWidth(90);
            tf.setPromptText("0–100");
            tf.textProperty().addListener((obs, old, now) -> {
                Mark mk = currentMark();
                if (mk == null) return;
                try {
                    double val = Double.parseDouble(now.trim());
                    mk.setMarkObtained(val);
                    // Live grade preview
                    try {
                        List<GradeThreshold> th = gradeDAO.getAll();
                        mk.setGradeSymbol(calcGrade(mk.getPercentage(), th));
                    } catch (SQLException ignored) {}
                } catch (NumberFormatException e) {
                    if (!now.isBlank()) tf.setStyle("-fx-border-color:red;");
                    else tf.setStyle("");
                }
            });
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) { setGraphic(null); return; }
            Mark mk = currentMark();
            if (mk == null) { setGraphic(null); return; }
            tf.setStyle("");
            if (!tf.isFocused()) {
                tf.setText(mk.getMarkObtained() < 0 ? "" : formatMark(mk.getMarkObtained()));
            }
            setGraphic(tf);
        }

        private Mark currentMark() {
            int idx = getIndex();
            if (idx < 0 || idx >= marksTable.getItems().size()) return null;
            return marksTable.getItems().get(idx);
        }
    }
    
    private String formatMark(double v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(v);
    }

    // ══════════════════════════════════════════════════════════
    //  TAB 4 — REPORT CARDS
    // ══════════════════════════════════════════════════════════
    
    private void setupReportCardsTab() {
        rcClassCombo.setItems(FXCollections.observableArrayList(availableClasses));
        rcClassCombo.valueProperty().addListener((obs, old, now) -> {
            rcTermCombo.getItems().clear();
            rcStudentTable.getItems().clear();
            if (now == null) return;
            try {
                AcademicYear ay = yearDAO.getCurrent();
                if (ay != null)
                    rcTermCombo.setItems(FXCollections.observableArrayList(termDAO.getByYear(ay.getId())));
            } catch (SQLException ex) { showError("Database error", ex.getMessage()); }
        });
    
        rcTermCombo.valueProperty().addListener((obs, old, now) -> {
            if (rcClassCombo.getValue() != null && now != null) loadRcStudents();
            else rcStudentTable.getItems().clear();
        });
    
        rcColName.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getFullName()));
        rcColNameSi.setCellValueFactory(new PropertyValueFactory<>("firstNameSi"));
    
        rcColActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnGenerate = new Button("Generate PDF");
            {
                btnGenerate.getStyleClass().add("btn-primary");
                btnGenerate.setOnAction(e ->
                        handleGenerateReportCard(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnGenerate);
            }
        });
    }
    
    private void loadRcStudents() {
        try {
            List<Student> students = reportCardDAO.getEnrolledStudents(rcClassCombo.getValue().getId());
            rcStudentTable.setItems(FXCollections.observableArrayList(students));
            rcStatusLabel.setText(students.size() + " students loaded.");
        } catch (SQLException ex) { showError("Database error", ex.getMessage()); }
    }
    
    private void handleGenerateReportCard(Student student) {
        SchoolClass cls  = rcClassCombo.getValue();
        Term        term = rcTermCombo.getValue();
        if (cls == null || term == null) {
            showError("Missing selection", "Please select a class and term first.");
            return;
        }
    
        // Ask user where to save
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose output folder");
        File dir = chooser.showDialog(rcStudentTable.getScene().getWindow());
        if (dir == null) return;
    
        try {
            Student      full       = reportCardDAO.getStudentWithClass(student.getId());
            List<Mark>   marks      = reportCardDAO.getMarksForTerm(student.getId(), term.getId());
            int[]        attendance = reportCardDAO.getAttendanceSummary(student.getId(), cls.getId());
            String       termLabel  = reportCardDAO.getTermLabel(term.getId());
    
            String fileName = "ReportCard_"
                    + student.getFirstName() + "_" + student.getLastName()
                    + "_" + termLabel.replaceAll("[^a-zA-Z0-9]", "_").replaceAll("_+", "_").replaceAll("_$", "")
                    + ".pdf";
    
            File outFile = new File(dir, fileName);
            reportCardService.generate(full, termLabel, marks, attendance,
                    "B/Bandarawela Central College", outFile);
    
            rcStatusLabel.setText("✔  Saved: " + outFile.getAbsolutePath());
    
            // Open the PDF immediately
            if (java.awt.Desktop.isDesktopSupported())
                java.awt.Desktop.getDesktop().open(outFile);
    
        } catch (Exception ex) {
            showError("PDF generation failed", ex.getMessage());
        }
    }
}