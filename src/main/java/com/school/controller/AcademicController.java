package com.school.controller;

import com.school.dao.*;
import com.school.model.*;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Controller for views/academic.fxml.
 *
 * Manages 5 tabs: Academic Years, Terms, Subjects, Classes, Class Subjects.
 * All dialogs are built programmatically.
 */
public class AcademicController {

    // ── DAOs ──────────────────────────────────────────────────
    private final AcademicYearDAO    yearDAO       = new AcademicYearDAO();
    private final TermDAO            termDAO       = new TermDAO();
    private final SubjectDAO         subjectDAO    = new SubjectDAO();
    private final ClassDAO           classDAO      = new ClassDAO();
    private final ClassSubjectDAO    csDAO         = new ClassSubjectDAO();
    private final ClassEnrollmentDAO enrollmentDAO = new ClassEnrollmentDAO();
    private final TimetableDAO       timetableDAO  = new TimetableDAO();

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Shared data ───────────────────────────────────────────
    private ObservableList<AcademicYear>  allYears    = FXCollections.observableArrayList();
    private List<TeacherProfile>          allTeachers;

    // ══════════════════════════════════════════════════════════
    //  FXML INJECTIONS
    // ══════════════════════════════════════════════════════════

    // Tab 1 — Academic Years
    @FXML private TableView<AcademicYear>           yearTable;
    @FXML private TableColumn<AcademicYear, String> yearColLabel;
    @FXML private TableColumn<AcademicYear, String> yearColStart;
    @FXML private TableColumn<AcademicYear, String> yearColEnd;
    @FXML private TableColumn<AcademicYear, String> yearColCurrent;
    @FXML private TableColumn<AcademicYear, Void>   yearColActions;

    // Tab 2 — Terms
    @FXML private ComboBox<AcademicYear>        termYearFilter;
    @FXML private Button                         btnAddTerm;
    @FXML private TableView<Term>               termTable;
    @FXML private TableColumn<Term, String>     termColNumber;
    @FXML private TableColumn<Term, String>     termColStart;
    @FXML private TableColumn<Term, String>     termColEnd;
    @FXML private TableColumn<Term, Void>       termColActions;

    // Tab 3 — Subjects
    @FXML private ComboBox<String>               subjectGradeFilter;
    @FXML private TableView<Subject>            subjectTable;
    @FXML private TableColumn<Subject, String>  subjectColName;
    @FXML private TableColumn<Subject, String>  subjectColNameSi;
    @FXML private TableColumn<Subject, String>  subjectColGrade;
    @FXML private TableColumn<Subject, Void>    subjectColActions;

    private final ObservableList<Subject> allSubjects      = FXCollections.observableArrayList();
    private final FilteredList<Subject>   filteredSubjects = new FilteredList<>(allSubjects, s -> true);

    // Tab 4 — Classes
    @FXML private ComboBox<AcademicYear>           classYearFilter;
    @FXML private Button                            btnAddClass;
    @FXML private TableView<SchoolClass>           classTable;
    @FXML private TableColumn<SchoolClass, String> classColGrade;
    @FXML private TableColumn<SchoolClass, String> classColSection;
    @FXML private TableColumn<SchoolClass, String> classColTeacher;
    @FXML private TableColumn<SchoolClass, Void>   classColActions;

    // Tab 5 — Class Subjects
    @FXML private ComboBox<AcademicYear>             csYearFilter;
    @FXML private TableView<SchoolClass>             csClassTable;
    @FXML private TableColumn<SchoolClass, String>   csClassColGrade;
    @FXML private TableColumn<SchoolClass, String>   csClassColSection;
    @FXML private Label                               csTitle;
    @FXML private Button                              btnAssignSubject;
    @FXML private TableView<ClassSubject>            csTable;
    @FXML private TableColumn<ClassSubject, String>  csColSubject;
    @FXML private TableColumn<ClassSubject, String>  csColSubjectSi;
    @FXML private TableColumn<ClassSubject, String>  csColTeacher;
    @FXML private TableColumn<ClassSubject, Void>    csColActions;

    // Tab 6 — Enrollments
    @FXML private ComboBox<AcademicYear>           enrollYearFilter;
    @FXML private TableView<SchoolClass>           enrollClassTable;
    @FXML private TableColumn<SchoolClass, String> enrollClassColGrade;
    @FXML private TableColumn<SchoolClass, String> enrollClassColSection;
    @FXML private Label                            enrollTitle;
    @FXML private Button                           btnEnrollStudents;
    @FXML private TableView<Student>              enrolledTable;
    @FXML private TableColumn<Student, String>    enrollColName;
    @FXML private TableColumn<Student, String>    enrollColNameSi;
    @FXML private TableColumn<Student, String>    enrollColGender;
    @FXML private TableColumn<Student, String>    enrollColNic;
    @FXML private TableColumn<Student, Void>      enrollColActions;

    private SchoolClass selectedEnrollClass = null;
    private SchoolClass selectedClass = null;

    // Tab 7 — TimeTable
    @FXML private ComboBox<AcademicYear> ttYearFilter;
    @FXML private ComboBox<SchoolClass>  ttClassCombo;
    @FXML private VBox                   ttGridContainer;
    
    private List<PeriodConfig>              periodConfigs = new ArrayList<>();
    private Map<String, TimetableEntry>     currentSlots  = new HashMap<>();
    private SchoolClass                     ttSelectedClass = null;

    // ══════════════════════════════════════════════════════════
    //  INIT
    // ══════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        try {
            allTeachers = classDAO.getAllTeachers();

            setupYearTab();
            setupTermTab();
            setupSubjectTab();
            setupClassTab();
            setupCsTab();
            setupEnrollmentTab();
            setupTimetableTab();
            loadYears();           // populates year table + all year ComboBoxes
            loadSubjects();

        } catch (SQLException ex) {
            showError("Startup error", ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════
    //  TAB 1 — ACADEMIC YEARS
    // ══════════════════════════════════════════════════════════

    private void setupYearTab() {
        yearColLabel.setCellValueFactory(new PropertyValueFactory<>("yearLabel"));

        yearColStart.setCellValueFactory(cd ->
                new SimpleStringProperty(fmt(cd.getValue().getStartDate())));

        yearColEnd.setCellValueFactory(cd ->
                new SimpleStringProperty(fmt(cd.getValue().getEndDate())));

        // Highlight current year
        yearColCurrent.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getCurrentDisplay()));
        yearColCurrent.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(item.isEmpty() ? "" : "-fx-text-fill: #22c55e; -fx-font-weight: bold;");
            }
        });

        yearColActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnSetCurrent = new Button("Set as Current");
            private final HBox   box           = new HBox(6, btnSetCurrent);
            {
                btnSetCurrent.getStyleClass().add("btn-secondary");
                box.setAlignment(Pos.CENTER_LEFT);
                btnSetCurrent.setOnAction(e -> {
                    AcademicYear ay = getTableView().getItems().get(getIndex());
                    handleSetCurrentYear(ay);
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                AcademicYear ay = getTableView().getItems().get(getIndex());
                btnSetCurrent.setDisable(ay.isCurrent());
                setGraphic(box);
            }
        });

        yearTable.setItems(allYears);
    }

    @FXML
    private void handleAddYear() {
        showYearDialog().ifPresent(ay -> {
            try {
                yearDAO.insert(ay);
                loadYears();
            } catch (SQLException ex) {
                showError("Save failed", ex.getMessage());
            }
        });
    }

    private void handleSetCurrentYear(AcademicYear ay) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Set \"" + ay.getYearLabel() + "\" as the current academic year?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    yearDAO.setCurrent(ay.getId());
                    loadYears();
                } catch (SQLException ex) {
                    showError("Update failed", ex.getMessage());
                }
            }
        });
    }

    private void loadYears() throws SQLException {
        List<AcademicYear> years = yearDAO.getAll();
        allYears.setAll(years);

        // Sync all year ComboBoxes
        AcademicYear termSel  = termYearFilter.getValue();
        AcademicYear classSel = classYearFilter.getValue();
        AcademicYear csSel    = csYearFilter.getValue();

        termYearFilter.setItems(allYears);
        classYearFilter.setItems(allYears);
        csYearFilter.setItems(allYears);

        // Re-select previous value if it still exists, otherwise pick current year
        AcademicYear current = years.stream().filter(AcademicYear::isCurrent).findFirst().orElse(null);

        restoreOrDefault(termYearFilter,  termSel,  current);
        restoreOrDefault(classYearFilter, classSel, current);
        restoreOrDefault(csYearFilter,    csSel,    current);
    }

    private void restoreOrDefault(ComboBox<AcademicYear> cb, AcademicYear prev, AcademicYear fallback) {
        if (prev != null && cb.getItems().stream().anyMatch(y -> y.getId() == prev.getId())) {
            cb.getItems().stream().filter(y -> y.getId() == prev.getId()).findFirst().ifPresent(cb::setValue);
        } else if (fallback != null) {
            cb.getItems().stream().filter(y -> y.getId() == fallback.getId()).findFirst().ifPresent(cb::setValue);
        }
    }

    private Optional<AcademicYear> showYearDialog() {
        Dialog<AcademicYear> dlg = new Dialog<>();
        dlg.setTitle("New Academic Year");
        dlg.setHeaderText(null);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(400);

        TextField  tfLabel = new TextField(); tfLabel.setPromptText("e.g. 2026/2027 *");
        DatePicker dpStart = buildDatePicker();
        DatePicker dpEnd   = buildDatePicker();

        GridPane grid = formGrid();
        grid.addRow(0, label("Year Label *"), tfLabel);
        grid.addRow(1, label("Start Date *"), dpStart);
        grid.addRow(2, label("End Date *"),   dpEnd);
        dlg.getDialogPane().setContent(grid);

        Button okBtn = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            StringBuilder err = new StringBuilder();
            if (tfLabel.getText().isBlank())  err.append("• Year label is required\n");
            if (dpStart.getValue() == null)   err.append("• Start date is required\n");
            if (dpEnd.getValue() == null)     err.append("• End date is required\n");
            if (!err.isEmpty()) { ev.consume(); showError("Validation", err.toString().trim()); }
        });

        dlg.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            return new AcademicYear(tfLabel.getText().trim(), dpStart.getValue(), dpEnd.getValue());
        });

        return dlg.showAndWait();
    }

    // ══════════════════════════════════════════════════════════
    //  TAB 2 — TERMS
    // ══════════════════════════════════════════════════════════

    private void setupTermTab() {
        termColNumber.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getDisplayName()));
        termColStart.setCellValueFactory(cd ->
                new SimpleStringProperty(fmt(cd.getValue().getStartDate())));
        termColEnd.setCellValueFactory(cd ->
                new SimpleStringProperty(fmt(cd.getValue().getEndDate())));

        termColActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox   box       = new HBox(6, btnEdit, btnDelete);
            {
                btnEdit.getStyleClass().add("btn-secondary");
                btnDelete.getStyleClass().add("btn-danger");
                box.setAlignment(Pos.CENTER_LEFT);
                btnEdit.setOnAction(e -> handleEditTerm(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDeleteTerm(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        // Year filter change → reload terms
        termYearFilter.valueProperty().addListener((obs, old, now) -> {
            btnAddTerm.setDisable(now == null);
            if (now != null) loadTerms(now.getId());
            else termTable.getItems().clear();
        });
    }

    @FXML
    private void handleAddTerm() {
        AcademicYear year = termYearFilter.getValue();
        if (year == null) return;

        try {
            int existing = termDAO.countByYear(year.getId());
            if (existing >= 3) {
                showError("Limit reached", "A maximum of 3 terms are allowed per academic year.");
                return;
            }
            int nextNumber = existing + 1;
            showTermDialog(null, nextNumber, year.getId()).ifPresent(t -> {
                try {
                    termDAO.insert(t);
                    loadTerms(year.getId());
                } catch (SQLException ex) {
                    showError("Save failed", ex.getMessage());
                }
            });
        } catch (SQLException ex) {
            showError("Database error", ex.getMessage());
        }
    }

    private void handleEditTerm(Term t) {
        showTermDialog(t, t.getTermNumber(), t.getAcademicYearId()).ifPresent(updated -> {
            try {
                termDAO.update(updated);
                loadTerms(updated.getAcademicYearId());
            } catch (SQLException ex) {
                showError("Save failed", ex.getMessage());
            }
        });
    }

    private void handleDeleteTerm(Term t) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete " + t.getDisplayName() + "? This will also remove associated exams.",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    termDAO.delete(t.getId());
                    loadTerms(t.getAcademicYearId());
                } catch (SQLException ex) {
                    showError("Delete failed", ex.getMessage());
                }
            }
        });
    }

    private void loadTerms(int yearId) {
        try {
            termTable.setItems(FXCollections.observableArrayList(termDAO.getByYear(yearId)));
        } catch (SQLException ex) {
            showError("Database error", ex.getMessage());
        }
    }

    private Optional<Term> showTermDialog(Term existing, int termNumber, int yearId) {
        boolean editing = existing != null;
        Dialog<Term> dlg = new Dialog<>();
        dlg.setTitle(editing ? "Edit Term " + termNumber : "Add Term " + termNumber);
        dlg.setHeaderText(null);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(380);

        DatePicker dpStart = buildDatePicker();
        DatePicker dpEnd   = buildDatePicker();
        if (editing) { dpStart.setValue(existing.getStartDate()); dpEnd.setValue(existing.getEndDate()); }

        GridPane grid = formGrid();
        grid.addRow(0, label("Start Date *"), dpStart);
        grid.addRow(1, label("End Date *"),   dpEnd);
        dlg.getDialogPane().setContent(grid);

        Button ok = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            StringBuilder err = new StringBuilder();
            if (dpStart.getValue() == null) err.append("• Start date is required\n");
            if (dpEnd.getValue() == null)   err.append("• End date is required\n");
            if (dpStart.getValue() != null && dpEnd.getValue() != null
                    && !dpEnd.getValue().isAfter(dpStart.getValue()))
                err.append("• End date must be after start date\n");
            if (!err.isEmpty()) { ev.consume(); showError("Validation", err.toString().trim()); }
        });

        dlg.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            Term t = editing ? existing : new Term();
            t.setAcademicYearId(yearId);
            t.setTermNumber(termNumber);
            t.setStartDate(dpStart.getValue());
            t.setEndDate(dpEnd.getValue());
            return t;
        });

        return dlg.showAndWait();
    }

    // ══════════════════════════════════════════════════════════
    //  TAB 3 — SUBJECTS
    // ══════════════════════════════════════════════════════════

    private void setupSubjectTab() {
        subjectColName.setCellValueFactory(new PropertyValueFactory<>("name"));
        subjectColNameSi.setCellValueFactory(new PropertyValueFactory<>("nameSi"));
        subjectColGrade.setCellValueFactory(cd ->
                new SimpleStringProperty("Grade " + cd.getValue().getGradeLevel()));

        subjectColActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox   box       = new HBox(6, btnEdit, btnDelete);
            {
                btnEdit.getStyleClass().add("btn-secondary");
                btnDelete.getStyleClass().add("btn-danger");
                box.setAlignment(Pos.CENTER_LEFT);
                btnEdit.setOnAction(e -> handleEditSubject(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDeleteSubject(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        // Grade filter
        ObservableList<String> gradeOptions = FXCollections.observableArrayList("All Grades");
        for (int g = 1; g <= 13; g++) gradeOptions.add("Grade " + g);
        subjectGradeFilter.setItems(gradeOptions);
        subjectGradeFilter.setValue("All Grades");
        subjectGradeFilter.valueProperty().addListener((obs, old, now) -> applySubjectFilter(now));

        subjectTable.setItems(filteredSubjects);
    }

    @FXML
    private void handleAddSubject() {
        showSubjectDialog(null).ifPresent(s -> {
            try {
                subjectDAO.insert(s);
                loadSubjects();
            } catch (SQLException ex) {
                showError("Save failed", ex.getMessage());
            }
        });
    }

    private void handleEditSubject(Subject s) {
        showSubjectDialog(s).ifPresent(updated -> {
            try {
                subjectDAO.update(updated);
                loadSubjects();
            } catch (SQLException ex) {
                showError("Save failed", ex.getMessage());
            }
        });
    }

    private void handleDeleteSubject(Subject s) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete subject \"" + s.getName() + "\" (Grade " + s.getGradeLevel() + ")?\" \nThis will fail if marks or class assignments reference it.",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    subjectDAO.delete(s.getId());
                    loadSubjects();
                } catch (SQLException ex) {
                    showError("Delete failed", "Could not delete subject — it may still be in use.\n" + ex.getMessage());
                }
            }
        });
    }

    private void loadSubjects() {
        try {
            allSubjects.setAll(subjectDAO.getAll());
            applySubjectFilter(subjectGradeFilter.getValue());
        } catch (SQLException ex) {
            showError("Database error", ex.getMessage());
        }
    }

    private void applySubjectFilter(String grade) {
        if (grade == null || grade.equals("All Grades")) {
            filteredSubjects.setPredicate(s -> true);
        } else {
            int g = Integer.parseInt(grade.replace("Grade ", "").trim());
            filteredSubjects.setPredicate(s -> s.getGradeLevel() == g);
        }
    }

    private Optional<Subject> showSubjectDialog(Subject existing) {
        boolean editing = existing != null;
        Dialog<Subject> dlg = new Dialog<>();
        dlg.setTitle(editing ? "Edit Subject" : "Add Subject");
        dlg.setHeaderText(null);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(400);

        TextField tfName  = new TextField(); tfName.setPromptText("Subject name (EN) *");
        TextField tfNameSi = new TextField(); tfNameSi.setPromptText("Subject name (සිංහල)");
        ComboBox<String> cbGrade = new ComboBox<>();
        ObservableList<String> gradeOpts = FXCollections.observableArrayList();
        for (int g = 1; g <= 13; g++) gradeOpts.add("Grade " + g);
        cbGrade.setItems(gradeOpts);
        cbGrade.setPromptText("Select grade *");

        if (editing) {
            tfName.setText(existing.getName());
            tfNameSi.setText(existing.getNameSi() != null ? existing.getNameSi() : "");
            cbGrade.setValue("Grade " + existing.getGradeLevel());
        }

        GridPane grid = formGrid();
        grid.addRow(0, label("Subject Name (EN) *"),      tfName);
        grid.addRow(1, label("Subject Name (සිංහල)"),    tfNameSi);
        grid.addRow(2, label("Grade Level *"),             cbGrade);
        dlg.getDialogPane().setContent(grid);

        Button ok = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            StringBuilder err = new StringBuilder();
            if (tfName.getText().isBlank())  err.append("• Subject name is required\n");
            if (cbGrade.getValue() == null)  err.append("• Grade level is required\n");
            if (!err.isEmpty()) { ev.consume(); showError("Validation", err.toString().trim()); }
        });

        dlg.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            Subject s = editing ? existing : new Subject();
            s.setName(tfName.getText().trim());
            s.setNameSi(tfNameSi.getText().trim());
            s.setGradeLevel(Integer.parseInt(cbGrade.getValue().replace("Grade ", "").trim()));
            return s;
        });

        return dlg.showAndWait();
    }

    // ══════════════════════════════════════════════════════════
    //  TAB 4 — CLASSES
    // ══════════════════════════════════════════════════════════

    private void setupClassTab() {
        classColGrade.setCellValueFactory(cd ->
                new SimpleStringProperty("Grade " + cd.getValue().getGradeLevel()));
        classColSection.setCellValueFactory(new PropertyValueFactory<>("section"));
        classColTeacher.setCellValueFactory(new PropertyValueFactory<>("teacherName"));

        classColActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("Edit");
            private final HBox   box     = new HBox(6, btnEdit);
            {
                btnEdit.getStyleClass().add("btn-secondary");
                box.setAlignment(Pos.CENTER_LEFT);
                btnEdit.setOnAction(e -> handleEditClass(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        classYearFilter.valueProperty().addListener((obs, old, now) -> {
            btnAddClass.setDisable(now == null);
            if (now != null) loadClasses(now.getId());
            else classTable.getItems().clear();
        });
    }

    @FXML
    private void handleAddClass() {
        AcademicYear year = classYearFilter.getValue();
        if (year == null) return;
        showClassDialog(null, year).ifPresent(c -> {
            try {
                classDAO.insert(c);
                loadClasses(year.getId());
            } catch (SQLException ex) {
                showError("Save failed",
                        ex.getMessage().contains("Duplicate")
                                ? "That grade/section combination already exists for this year."
                                : ex.getMessage());
            }
        });
    }

    private void handleEditClass(SchoolClass c) {
        AcademicYear year = classYearFilter.getValue();
        showClassDialog(c, year).ifPresent(updated -> {
            try {
                classDAO.update(updated);
                if (year != null) loadClasses(year.getId());
            } catch (SQLException ex) {
                showError("Save failed", ex.getMessage());
            }
        });
    }

    private void loadClasses(int yearId) {
        try {
            classTable.setItems(FXCollections.observableArrayList(classDAO.getByYear(yearId)));
        } catch (SQLException ex) {
            showError("Database error", ex.getMessage());
        }
    }

    private Optional<SchoolClass> showClassDialog(SchoolClass existing, AcademicYear year) {
        boolean editing = existing != null;
        Dialog<SchoolClass> dlg = new Dialog<>();
        dlg.setTitle((editing ? "Edit" : "Add") + " Class — " + year.getYearLabel());
        dlg.setHeaderText(null);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(420);

        ComboBox<String> cbGrade = new ComboBox<>();
        ObservableList<String> gradeOpts = FXCollections.observableArrayList();
        for (int g = 1; g <= 13; g++) gradeOpts.add("Grade " + g);
        cbGrade.setItems(gradeOpts);
        cbGrade.setPromptText("Select grade *");

        TextField tfSection = new TextField(); tfSection.setPromptText("e.g. A, B *");

        ComboBox<TeacherProfile> cbTeacher = new ComboBox<>(FXCollections.observableArrayList(allTeachers));
        cbTeacher.setPromptText("Select homeroom teacher (optional)");
        cbTeacher.setMaxWidth(Double.MAX_VALUE);

        if (editing) {
            cbGrade.setValue("Grade " + existing.getGradeLevel());
            tfSection.setText(existing.getSection());
            if (existing.getHomeroomTeacherId() > 0) {
                allTeachers.stream()
                        .filter(t -> t.getId() == existing.getHomeroomTeacherId())
                        .findFirst().ifPresent(cbTeacher::setValue);
            }
        }

        GridPane grid = formGrid();
        grid.addRow(0, label("Grade *"),             cbGrade);
        grid.addRow(1, label("Section *"),           tfSection);
        grid.addRow(2, label("Homeroom Teacher"),    cbTeacher);
        GridPane.setHgrow(cbTeacher, Priority.ALWAYS);
        dlg.getDialogPane().setContent(grid);

        Button ok = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            StringBuilder err = new StringBuilder();
            if (cbGrade.getValue() == null)      err.append("• Grade is required\n");
            if (tfSection.getText().isBlank())   err.append("• Section is required\n");
            if (!err.isEmpty()) { ev.consume(); showError("Validation", err.toString().trim()); }
        });

        dlg.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            SchoolClass c = editing ? existing : new SchoolClass();
            c.setAcademicYearId(year.getId());
            c.setGradeLevel(Integer.parseInt(cbGrade.getValue().replace("Grade ", "").trim()));
            c.setSection(tfSection.getText().trim().toUpperCase());
            TeacherProfile tp = cbTeacher.getValue();
            c.setHomeroomTeacherId(tp != null ? tp.getId() : 0);
            c.setTeacherName(tp != null ? tp.getFullName() : null);
            return c;
        });

        return dlg.showAndWait();
    }

    // ══════════════════════════════════════════════════════════
    //  TAB 5 — CLASS SUBJECTS
    // ══════════════════════════════════════════════════════════

    private void setupCsTab() {
        csClassColGrade.setCellValueFactory(cd ->
                new SimpleStringProperty(String.valueOf(cd.getValue().getGradeLevel())));
        csClassColSection.setCellValueFactory(new PropertyValueFactory<>("section"));

        csColSubject.setCellValueFactory(new PropertyValueFactory<>("subjectName"));
        csColSubjectSi.setCellValueFactory(new PropertyValueFactory<>("subjectNameSi"));
        csColTeacher.setCellValueFactory(new PropertyValueFactory<>("teacherName"));

        csColActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnChangeTeacher = new Button("Change Teacher");
            private final Button btnRemove        = new Button("Remove");
            private final HBox   box              = new HBox(6, btnChangeTeacher, btnRemove);
            {
                btnChangeTeacher.getStyleClass().add("btn-secondary");
                btnRemove.getStyleClass().add("btn-danger");
                box.setAlignment(Pos.CENTER_LEFT);
                btnChangeTeacher.setOnAction(e -> handleChangeTeacher(getTableView().getItems().get(getIndex())));
                btnRemove.setOnAction(e -> handleRemoveAssignment(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        // Class table selection → load assignments
        csClassTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, now) -> onClassSelected(now));

        // Year filter → reload class list in left panel
        csYearFilter.valueProperty().addListener((obs, old, now) -> {
            if (now != null) loadCsClasses(now.getId());
            else csClassTable.getItems().clear();
        });
    }

    private void onClassSelected(SchoolClass c) {
        selectedClass = c;
        if (c == null) {
            csTitle.setText("Select a class on the left");
            btnAssignSubject.setDisable(true);
            csTable.getItems().clear();
            return;
        }
        csTitle.setText("Subjects — " + c.getDisplayName());
        btnAssignSubject.setDisable(false);
        loadCsAssignments(c.getId());
    }

    @FXML
    private void handleAssignSubject() {
        if (selectedClass == null) return;
        try {
            List<Subject> unassigned = csDAO.getUnassignedSubjects(
                    selectedClass.getId(), selectedClass.getGradeLevel());
            if (unassigned.isEmpty()) {
                showError("Nothing to assign",
                        "All subjects for Grade " + selectedClass.getGradeLevel() + " are already assigned.");
                return;
            }
            showAssignDialog(selectedClass, unassigned).ifPresent(cs -> {
                try {
                    csDAO.insert(cs);
                    loadCsAssignments(selectedClass.getId());
                } catch (SQLException ex) {
                    showError("Save failed", ex.getMessage());
                }
            });
        } catch (SQLException ex) {
            showError("Database error", ex.getMessage());
        }
    }

    private void handleChangeTeacher(ClassSubject cs) {
        showChangeTeacherDialog(cs).ifPresent(updated -> {
            try {
                csDAO.update(updated);
                loadCsAssignments(cs.getClassId());
            } catch (SQLException ex) {
                showError("Save failed", ex.getMessage());
            }
        });
    }

    private void handleRemoveAssignment(ClassSubject cs) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove \"" + cs.getSubjectName() + "\" from this class?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    csDAO.delete(cs.getId());
                    loadCsAssignments(cs.getClassId());
                } catch (SQLException ex) {
                    showError("Delete failed", ex.getMessage());
                }
            }
        });
    }

    private void loadCsClasses(int yearId) {
        try {
            csClassTable.setItems(FXCollections.observableArrayList(classDAO.getByYear(yearId)));
            csClassTable.getSelectionModel().clearSelection();
            onClassSelected(null);
        } catch (SQLException ex) {
            showError("Database error", ex.getMessage());
        }
    }

    private void loadCsAssignments(int classId) {
        try {
            csTable.setItems(FXCollections.observableArrayList(csDAO.getByClass(classId)));
        } catch (SQLException ex) {
            showError("Database error", ex.getMessage());
        }
    }

    private Optional<ClassSubject> showAssignDialog(SchoolClass cls, List<Subject> unassigned) {
        Dialog<ClassSubject> dlg = new Dialog<>();
        dlg.setTitle("Assign Subject — " + cls.getDisplayName());
        dlg.setHeaderText(null);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(420);

        ComboBox<Subject>        cbSubject = new ComboBox<>(FXCollections.observableArrayList(unassigned));
        cbSubject.setPromptText("Select subject *");
        cbSubject.setMaxWidth(Double.MAX_VALUE);

        ComboBox<TeacherProfile> cbTeacher = new ComboBox<>(FXCollections.observableArrayList(allTeachers));
        cbTeacher.setPromptText("Select teacher *");
        cbTeacher.setMaxWidth(Double.MAX_VALUE);

        GridPane grid = formGrid();
        grid.addRow(0, label("Subject *"), cbSubject);
        grid.addRow(1, label("Teacher *"), cbTeacher);
        GridPane.setHgrow(cbSubject, Priority.ALWAYS);
        GridPane.setHgrow(cbTeacher, Priority.ALWAYS);
        dlg.getDialogPane().setContent(grid);

        Button ok = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            StringBuilder err = new StringBuilder();
            if (cbSubject.getValue() == null) err.append("• Subject is required\n");
            if (cbTeacher.getValue() == null) err.append("• Teacher is required\n");
            if (!err.isEmpty()) { ev.consume(); showError("Validation", err.toString().trim()); }
        });

        dlg.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            ClassSubject cs = new ClassSubject(cls.getId(),
                    cbSubject.getValue().getId(),
                    cbTeacher.getValue().getId());
            cs.setSubjectName(cbSubject.getValue().getName());
            cs.setTeacherName(cbTeacher.getValue().getFullName());
            return cs;
        });

        return dlg.showAndWait();
    }

    private Optional<ClassSubject> showChangeTeacherDialog(ClassSubject existing) {
        Dialog<ClassSubject> dlg = new Dialog<>();
        dlg.setTitle("Change Teacher — " + existing.getSubjectName());
        dlg.setHeaderText(null);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(400);

        ComboBox<TeacherProfile> cbTeacher = new ComboBox<>(FXCollections.observableArrayList(allTeachers));
        cbTeacher.setMaxWidth(Double.MAX_VALUE);
        allTeachers.stream().filter(t -> t.getId() == existing.getTeacherId())
                .findFirst().ifPresent(cbTeacher::setValue);

        GridPane grid = formGrid();
        grid.addRow(0, label("Subject"), new Label(existing.getSubjectName()));
        grid.addRow(1, label("New Teacher *"), cbTeacher);
        GridPane.setHgrow(cbTeacher, Priority.ALWAYS);
        dlg.getDialogPane().setContent(grid);

        Button ok = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            if (cbTeacher.getValue() == null) {
                ev.consume(); showError("Validation", "• Teacher is required");
            }
        });

        dlg.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            existing.setTeacherId(cbTeacher.getValue().getId());
            existing.setTeacherName(cbTeacher.getValue().getFullName());
            return existing;
        });

        return dlg.showAndWait();
    }

    // ══════════════════════════════════════════════════════════
    //  TAB 6 — ENROLLMENTS
    // ══════════════════════════════════════════════════════════

    private void setupEnrollmentTab() {
        enrollClassColGrade.setCellValueFactory(cd ->
                new SimpleStringProperty(String.valueOf(cd.getValue().getGradeLevel())));
        enrollClassColSection.setCellValueFactory(new PropertyValueFactory<>("section"));

        enrollColName.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getFullName()));
        enrollColNameSi.setCellValueFactory(new PropertyValueFactory<>("firstNameSi"));
        enrollColGender.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getGenderDisplay()));
        enrollColNic.setCellValueFactory(new PropertyValueFactory<>("nic"));

        enrollColActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnRemove = new Button("Remove");
            {
                btnRemove.getStyleClass().add("btn-danger");
                btnRemove.setOnAction(e -> handleUnenroll(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnRemove);
            }
        });

        enrollClassTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, now) -> onEnrollClassSelected(now));

        enrollYearFilter.setItems(allYears);
        enrollYearFilter.valueProperty().addListener((obs, old, now) -> {
            if (now != null) loadEnrollClasses(now.getId());
            else enrollClassTable.getItems().clear();
        });

        // Sync with the shared allYears list — pick current year by default
        allYears.stream().filter(AcademicYear::isCurrent).findFirst()
                .ifPresent(enrollYearFilter::setValue);
    }

    private void onEnrollClassSelected(SchoolClass c) {
        selectedEnrollClass = c;
        if (c == null) {
            enrollTitle.setText("Select a class on the left");
            btnEnrollStudents.setDisable(true);
            enrolledTable.getItems().clear();
            return;
        }
        enrollTitle.setText("Enrolled — " + c.getDisplayName());
        btnEnrollStudents.setDisable(false);
        loadEnrolled(c.getId());
    }

    private void loadEnrollClasses(int yearId) {
        try {
            enrollClassTable.setItems(FXCollections.observableArrayList(classDAO.getByYear(yearId)));
            enrollClassTable.getSelectionModel().clearSelection();
            onEnrollClassSelected(null);
        } catch (SQLException ex) { showError("Database error", ex.getMessage()); }
    }

    private void loadEnrolled(int classId) {
        try {
            enrolledTable.setItems(FXCollections.observableArrayList(
                    enrollmentDAO.getEnrolled(classId)));
        } catch (SQLException ex) { showError("Database error", ex.getMessage()); }
    }

    @FXML
    private void handleEnrollStudents() {
        if (selectedEnrollClass == null) return;
        try {
            List<Student> available = enrollmentDAO.getAvailable(
                    selectedEnrollClass.getId(), selectedEnrollClass.getGradeLevel());
            if (available.isEmpty()) {
                showError("No students available",
                        "All active students are already enrolled in this class.");
                return;
            }
            showEnrollDialog(available).ifPresent(selected -> {
                if (selected.isEmpty()) return;
                try {
                    List<Integer> ids = selected.stream().map(Student::getId).toList();
                    enrollmentDAO.enroll(selectedEnrollClass.getId(), ids);
                    loadEnrolled(selectedEnrollClass.getId());
                } catch (SQLException ex) { showError("Enroll failed", ex.getMessage()); }
            });
        } catch (SQLException ex) { showError("Database error", ex.getMessage()); }
    }

    private void handleUnenroll(Student s) {
        if (selectedEnrollClass == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove " + s.getFullName() + " from " + selectedEnrollClass.getDisplayName() + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    enrollmentDAO.unenroll(selectedEnrollClass.getId(), s.getId());
                    loadEnrolled(selectedEnrollClass.getId());
                } catch (SQLException ex) { showError("Remove failed", ex.getMessage()); }
            }
        });
    }

    private Optional<List<Student>> showEnrollDialog(List<Student> available) {
        Dialog<List<Student>> dlg = new Dialog<>();
        dlg.setTitle("Enroll Students — " + selectedEnrollClass.getDisplayName());
        dlg.setHeaderText("Hold Ctrl / Cmd to select multiple students.");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(420);
        dlg.getDialogPane().setPrefHeight(480);

        ListView<Student> listView = new ListView<>(FXCollections.observableArrayList(available));
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Student s, boolean empty) {
                super.updateItem(s, empty);
                setText((empty || s == null) ? null : s.getFullName()
                        + (s.getNic() != null ? "  (" + s.getNic() + ")" : ""));
            }
        });

        dlg.getDialogPane().setContent(listView);

        Button ok = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            if (listView.getSelectionModel().getSelectedItems().isEmpty()) {
                ev.consume();
                showError("Nothing selected", "Please select at least one student.");
            }
        });

        dlg.setResultConverter(btn ->
                btn == ButtonType.OK
                        ? new java.util.ArrayList<>(listView.getSelectionModel().getSelectedItems())
                        : null);

        return dlg.showAndWait();
    }

    // ══════════════════════════════════════════════════════════
    //  TAB 7 — TIMETABLE
    // ══════════════════════════════════════════════════════════
    
    private void setupTimetableTab() throws SQLException {
        periodConfigs = timetableDAO.getPeriodConfigs();
    
        ttYearFilter.setItems(allYears);
        allYears.stream().filter(AcademicYear::isCurrent)
                .findFirst().ifPresent(ttYearFilter::setValue);
    
        ttYearFilter.valueProperty().addListener((obs, old, now) -> {
            ttClassCombo.getItems().clear();
            ttGridContainer.getChildren().clear();
            if (now == null) return;
            try {
                ttClassCombo.setItems(
                        FXCollections.observableArrayList(classDAO.getByYear(now.getId())));
            } catch (SQLException ex) { showError("Database error", ex.getMessage()); }
        });
    
        ttClassCombo.valueProperty().addListener((obs, old, now) -> {
            ttSelectedClass = now;
            if (now == null) { ttGridContainer.getChildren().clear(); return; }
            loadTimetableGrid(now, false);
        });
    
        // Trigger year load
        if (ttYearFilter.getValue() != null) {
            try {
                ttClassCombo.setItems(FXCollections.observableArrayList(
                        classDAO.getByYear(ttYearFilter.getValue().getId())));
            } catch (SQLException ex) { showError("Database error", ex.getMessage()); }
        }
    }
    
    private void loadTimetableGrid(SchoolClass cls, boolean readOnly) {
        try {
            currentSlots = timetableDAO.getByClass(cls.getId());
            ttGridContainer.getChildren().clear();
            ttGridContainer.getChildren().add(
                    buildTimetableGrid(cls, currentSlots, periodConfigs, readOnly));
        } catch (SQLException ex) { showError("Database error", ex.getMessage()); }
    }
    
    @FXML
    private void handleClearTimetable() {
        if (ttSelectedClass == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Clear the entire timetable for " + ttSelectedClass.getDisplayName() + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    timetableDAO.deleteByClass(ttSelectedClass.getId());
                    loadTimetableGrid(ttSelectedClass, false);
                } catch (SQLException ex) { showError("Delete failed", ex.getMessage()); }
            }
        });
    }
    
    // ── Grid builder (shared by principal edit + teacher read-only) ──
    
    public GridPane buildTimetableGrid(SchoolClass cls,
                                        Map<String, TimetableEntry> slots,
                                        List<PeriodConfig> configs,
                                        boolean readOnly) throws SQLException {
        GridPane grid = new GridPane();
        grid.setHgap(2);
        grid.setVgap(2);
        grid.setPadding(new Insets(4));
    
        String[] days     = { "Mon", "Tue", "Wed", "Thu", "Fri" };
        String[] dayFull  = { "Monday","Tuesday","Wednesday","Thursday","Friday" };
    
        // Column constraints: label col + 5 day cols
        ColumnConstraints labelCol = new ColumnConstraints(110);
        grid.getColumnConstraints().add(labelCol);
        for (int d = 0; d < 5; d++) {
            ColumnConstraints dc = new ColumnConstraints();
            dc.setHgrow(Priority.ALWAYS);
            dc.setMinWidth(100);
            grid.getColumnConstraints().add(dc);
        }
    
        // Header row
        Label corner = new Label("");
        corner.setMinWidth(110);
        grid.add(corner, 0, 0);
    
        for (int d = 0; d < 5; d++) {
            Label dayLabel = new Label(days[d]);
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            dayLabel.setAlignment(Pos.CENTER);
            dayLabel.setStyle(
                    "-fx-background-color:#1e293b;-fx-text-fill:white;" +
                    "-fx-font-weight:bold;-fx-font-size:12px;" +
                    "-fx-padding:8 4;-fx-background-radius:4;");
            GridPane.setHgrow(dayLabel, Priority.ALWAYS);
            grid.add(dayLabel, d + 1, 0);
        }
    
        // Period rows
        for (int row = 0; row < configs.size(); row++) {
            PeriodConfig pc = configs.get(row);
            int gridRow = row + 1;
    
            // Row label
            VBox rowLabel = new VBox(2);
            rowLabel.setAlignment(Pos.CENTER_LEFT);
            rowLabel.setPadding(new Insets(4, 8, 4, 4));
            rowLabel.setMinWidth(110);
            rowLabel.setStyle("-fx-background-color:" +
                    (pc.isInterval() ? "#f1f5f9" : "#f8fafc") + ";-fx-background-radius:4;");
    
            Label lbl = new Label(pc.getLabel());
            lbl.setStyle("-fx-font-weight:bold;-fx-font-size:11px;-fx-text-fill:#334155;");
            Label time = new Label(pc.getTimeRange());
            time.setStyle("-fx-font-size:9px;-fx-text-fill:#94a3b8;");
            rowLabel.getChildren().addAll(lbl, time);
            grid.add(rowLabel, 0, gridRow);
    
            if (pc.isInterval()) {
                // Interval row spans all 5 day columns
                Label intervalLabel = new Label("— Interval  " + pc.getTimeRange() + " —");
                intervalLabel.setMaxWidth(Double.MAX_VALUE);
                intervalLabel.setAlignment(Pos.CENTER);
                intervalLabel.setStyle(
                        "-fx-background-color:#e2e8f0;-fx-text-fill:#64748b;" +
                        "-fx-font-style:italic;-fx-font-size:11px;" +
                        "-fx-padding:10 4;-fx-background-radius:4;");
                GridPane.setColumnSpan(intervalLabel, 5);
                GridPane.setHgrow(intervalLabel, Priority.ALWAYS);
                grid.add(intervalLabel, 1, gridRow);
            } else {
                // Day cells
                for (int d = 1; d <= 5; d++) {
                    String key   = TimetableEntry.slotKey(d, pc.getPeriod());
                    TimetableEntry entry = slots.get(key);
                    VBox cell = buildCell(cls, entry, d, pc, readOnly);
                    GridPane.setHgrow(cell, Priority.ALWAYS);
                    grid.add(cell, d, gridRow);
                }
            }
        }
    
        return grid;
    }
    
    // ── Single timetable cell ──────────────────────────────────
    
    private VBox buildCell(SchoolClass cls, TimetableEntry entry,
                            int day, PeriodConfig pc, boolean readOnly) throws SQLException {
        VBox cell = new VBox(4);
        cell.setAlignment(Pos.CENTER);
        cell.setPadding(new Insets(6, 4, 6, 4));
        cell.setMinHeight(60);
        cell.setMaxWidth(Double.MAX_VALUE);
    
        if (entry != null) {
            // Filled slot
            cell.setStyle(
                    "-fx-background-color:#ede9fe;-fx-background-radius:6;" +
                    "-fx-border-color:#c4b5fd;-fx-border-radius:6;-fx-border-width:1;");
    
            Label subj = new Label(entry.getSubjectName());
            subj.setStyle("-fx-font-weight:bold;-fx-font-size:11px;-fx-text-fill:#5b21b6;");
            subj.setWrapText(true);
            subj.setAlignment(Pos.CENTER);
    
            Label teacher = new Label(entry.getTeacherName());
            teacher.setStyle("-fx-font-size:9px;-fx-text-fill:#7c3aed;");
            teacher.setWrapText(true);
            teacher.setAlignment(Pos.CENTER);
    
            cell.getChildren().addAll(subj, teacher);
    
            if (!readOnly) {
                Button btnClear = new Button("✕");
                btnClear.setStyle(
                        "-fx-background-color:transparent;-fx-text-fill:#dc2626;" +
                        "-fx-font-size:10px;-fx-cursor:hand;-fx-padding:0;");
                btnClear.setOnAction(e -> {
                    try {
                        timetableDAO.delete(cls.getId(), day, pc.getPeriod());
                        loadTimetableGrid(cls, false);
                    } catch (SQLException ex) { showError("Delete failed", ex.getMessage()); }
                });
                cell.getChildren().add(btnClear);
    
                // Click cell to reassign
                cell.setOnMouseClicked(e -> {
                    if (e.getTarget() != btnClear)
                        showSlotDialog(cls, day, pc, entry);
                });
                cell.setStyle(cell.getStyle() + "-fx-cursor:hand;");
            }
    
        } else if (!readOnly) {
            // Empty slot — show + button
            cell.setStyle(
                    "-fx-background-color:#f8fafc;-fx-background-radius:6;" +
                    "-fx-border-color:#e2e8f0;-fx-border-radius:6;-fx-border-width:1;" +
                    "-fx-cursor:hand;");
    
            Label plus = new Label("＋");
            plus.setStyle("-fx-font-size:18px;-fx-text-fill:#cbd5e1;");
            cell.getChildren().add(plus);
    
            cell.setOnMouseClicked(e -> showSlotDialog(cls, day, pc, null));
    
            // Hover effect
            cell.setOnMouseEntered(e -> cell.setStyle(
                    "-fx-background-color:#f1f5f9;-fx-background-radius:6;" +
                    "-fx-border-color:#c4b5fd;-fx-border-radius:6;-fx-border-width:1;" +
                    "-fx-cursor:hand;"));
            cell.setOnMouseExited(e -> cell.setStyle(
                    "-fx-background-color:#f8fafc;-fx-background-radius:6;" +
                    "-fx-border-color:#e2e8f0;-fx-border-radius:6;-fx-border-width:1;" +
                    "-fx-cursor:hand;"));
        } else {
            // Read-only empty
            cell.setStyle(
                    "-fx-background-color:#f8fafc;-fx-background-radius:6;" +
                    "-fx-border-color:#e2e8f0;-fx-border-radius:6;-fx-border-width:1;");
            Label empty = new Label("—");
            empty.setStyle("-fx-text-fill:#e2e8f0;");
            cell.getChildren().add(empty);
        }
    
        return cell;
    }
    
    // ── Assign/reassign dialog ─────────────────────────────────
    
    private void showSlotDialog(SchoolClass cls, int day,
                                PeriodConfig pc, TimetableEntry existing) {
        boolean editing = existing != null;
        Dialog<TimetableEntry> dlg = new Dialog<>();
        dlg.setTitle((editing ? "Reassign" : "Assign") + " — "
                + new TimetableEntry().getDayName() + " " + pc.getLabel());
    
        // Build day name inline
        String dayName = switch (day) {
            case 1 -> "Monday"; case 2 -> "Tuesday"; case 3 -> "Wednesday";
            case 4 -> "Thursday"; default -> "Friday";
        };
        dlg.setTitle((editing ? "Reassign" : "Assign") + " — " + dayName + " / " + pc.getLabel());
        dlg.setHeaderText(pc.getTimeRange());
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(400);
    
        // Load subjects assigned to this class
        ComboBox<com.school.model.Subject> cbSubject = new ComboBox<>();
        cbSubject.setPromptText("Select subject *");
        cbSubject.setMaxWidth(Double.MAX_VALUE);
        cbSubject.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(com.school.model.Subject s) {
                return s == null ? "" : s.getName();
            }
            @Override public com.school.model.Subject fromString(String s) { return null; }
        });
    
        ComboBox<TeacherProfile> cbTeacher = new ComboBox<>(
                FXCollections.observableArrayList(allTeachers));
        cbTeacher.setPromptText("Select teacher *");
        cbTeacher.setMaxWidth(Double.MAX_VALUE);
    
        try {
            List<com.school.model.Subject> subjects = new ClassSubjectDAO()
                    .getByClass(cls.getId())
                    .stream()
                    .map(cs -> {
                        com.school.model.Subject s = new com.school.model.Subject();
                        s.setId(cs.getSubjectId());
                        s.setName(cs.getSubjectName());
                        return s;
                    })
                    .toList();
            cbSubject.setItems(FXCollections.observableArrayList(subjects));
    
            if (editing) {
                cbSubject.getItems().stream()
                        .filter(s -> s.getId() == existing.getSubjectId())
                        .findFirst().ifPresent(cbSubject::setValue);
                allTeachers.stream()
                        .filter(t -> t.getId() == existing.getTeacherId())
                        .findFirst().ifPresent(cbTeacher::setValue);
            }
        } catch (SQLException ex) {
            showError("Database error", ex.getMessage());
            return;
        }
    
        GridPane grid = formGrid();
        grid.addRow(0, label("Subject *"), cbSubject);
        grid.addRow(1, label("Teacher *"), cbTeacher);
        GridPane.setHgrow(cbSubject, Priority.ALWAYS);
        GridPane.setHgrow(cbTeacher, Priority.ALWAYS);
        dlg.getDialogPane().setContent(grid);
    
        Button ok = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            StringBuilder err = new StringBuilder();
            if (cbSubject.getValue() == null) err.append("• Subject is required\n");
            if (cbTeacher.getValue() == null) err.append("• Teacher is required\n");
            if (!err.isEmpty()) { ev.consume(); showError("Validation", err.toString().trim()); }
        });
    
        dlg.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            TimetableEntry e = new TimetableEntry(
                    cls.getId(), day, pc.getPeriod(),
                    cbSubject.getValue().getId(),
                    cbTeacher.getValue().getId());
            e.setSubjectName(cbSubject.getValue().getName());
            e.setTeacherName(cbTeacher.getValue().getFullName());
            return e;
        });
    
        dlg.showAndWait().ifPresent(e -> {
            try {
                timetableDAO.upsert(e);
                loadTimetableGrid(cls, false);
            } catch (SQLException ex) { showError("Save failed", ex.getMessage()); }
        });
    }

    // ══════════════════════════════════════════════════════════
    //  SHARED HELPERS
    // ══════════════════════════════════════════════════════════

    private GridPane formGrid() {
        GridPane g = new GridPane();
        g.setHgap(12); g.setVgap(10);
        g.setPadding(new Insets(16));
        ColumnConstraints lc = new ColumnConstraints(150);
        ColumnConstraints fc = new ColumnConstraints(200, 200, Double.MAX_VALUE);
        fc.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(lc, fc);
        return g;
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("form-label");
        return l;
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

    private String fmt(LocalDate d) {
        return d == null ? "—" : DATE_FMT.format(d);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}