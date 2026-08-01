package com.school.controller;

import com.school.dao.FeeStructureDAO;
import com.school.dao.InvoiceDAO;
import com.school.dao.TermDAO;
import com.school.dao.AcademicYearDAO;
import com.school.model.*;
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
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class FeesController {

    // ── DAOs ──────────────────────────────────────────────────
    private final FeeStructureDAO structureDAO = new FeeStructureDAO();
    private final InvoiceDAO      invoiceDAO   = new InvoiceDAO();
    private final TermDAO         termDAO      = new TermDAO();
    private final AcademicYearDAO yearDAO      = new AcademicYearDAO();

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private int currentUserId = 0;

    // ── FXML: Tab 1 — Structures ──────────────────────────────
    @FXML private ComboBox<Term>                    structureTermFilter;
    @FXML private TableView<FeeStructure>           structureTable;
    @FXML private TableColumn<FeeStructure, String> fsColGrade;
    @FXML private TableColumn<FeeStructure, String> fsColTerm;
    @FXML private TableColumn<FeeStructure, String> fsColYear;
    @FXML private TableColumn<FeeStructure, String> fsColDesc;
    @FXML private TableColumn<FeeStructure, Number> fsColTotal;
    @FXML private TableColumn<FeeStructure, Void>   fsColActions;

    @FXML private Label                         itemsTitle;
    @FXML private Button                        btnAddItem;
    @FXML private TableView<FeeItem>            itemsTable;
    @FXML private TableColumn<FeeItem, String>  fiColName;
    @FXML private TableColumn<FeeItem, Number>  fiColAmount;
    @FXML private TableColumn<FeeItem, Void>    fiColActions;

    private FeeStructure selectedStructure = null;
    private final ObservableList<FeeStructure> allStructures    = FXCollections.observableArrayList();
    private final FilteredList<FeeStructure>   filteredStructures = new FilteredList<>(allStructures, s -> true);

    // ── FXML: Tab 2 — Invoices ────────────────────────────────
    @FXML private TextField                    invoiceSearchField;
    @FXML private ComboBox<String>             statusFilter;
    @FXML private Label                        lblTotalOutstanding;
    @FXML private Label                        lblTotalPaid;
    @FXML private Label                        lblInvoiceCount;

    @FXML private TableView<Invoice>                invoiceTable;
    @FXML private TableColumn<Invoice, String>      invColStudent;
    @FXML private TableColumn<Invoice, String>      invColDue;
    @FXML private TableColumn<Invoice, Number>      invColTotal;
    @FXML private TableColumn<Invoice, Number>      invColDiscount;
    @FXML private TableColumn<Invoice, Number>      invColPaid;
    @FXML private TableColumn<Invoice, Number>      invColOutstanding;
    @FXML private TableColumn<Invoice, String>      invColStatus;
    @FXML private TableColumn<Invoice, Void>        invColActions;

    @FXML private Label                        paymentsTitle;
    @FXML private Button                       btnRecordPayment;
    @FXML private TableView<Payment>           paymentsTable;
    @FXML private TableColumn<Payment, String> pyColDate;
    @FXML private TableColumn<Payment, Number> pyColAmount;
    @FXML private TableColumn<Payment, String> pyColMethod;
    @FXML private TableColumn<Payment, String> pyColReceipt;
    @FXML private TableColumn<Payment, Void>   pyColActions;

    private Invoice selectedInvoice = null;
    private final ObservableList<Invoice> allInvoices      = FXCollections.observableArrayList();
    private final FilteredList<Invoice>   filteredInvoices = new FilteredList<>(allInvoices, i -> true);

    // ══════════════════════════════════════════════════════════
    //  INIT
    // ══════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        currentUserId = SessionManager.getInstance().getCurrentUser().getId();
        try {
            setupStructureTab();
            setupInvoiceTab();
            loadStructures();
            loadInvoices(null);
        } catch (SQLException ex) {
            showError("Startup error", ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════
    //  TAB 1 — FEE STRUCTURES
    // ══════════════════════════════════════════════════════════

    private void setupStructureTab() throws SQLException {
        // Term filter — load all terms from current year
        AcademicYear current = yearDAO.getCurrent();
        if (current != null) {
            List<Term> terms = termDAO.getByYear(current.getId());
            ObservableList<Term> termItems = FXCollections.observableArrayList(terms);
            structureTermFilter.setItems(termItems);
            structureTermFilter.setConverter(new StringConverter<>() {
                @Override public String toString(Term t)   { return t == null ? "All terms" : t.getDisplayName() + " — " + current.getYearLabel(); }
                @Override public Term fromString(String s) { return null; }
            });
        }
        structureTermFilter.valueProperty().addListener((obs, old, now) -> applyStructureFilter(now));

        // Columns
        fsColGrade.setCellValueFactory(new PropertyValueFactory<>("gradeDisplay"));
        fsColTerm.setCellValueFactory(new PropertyValueFactory<>("termDisplay"));
        fsColYear.setCellValueFactory(new PropertyValueFactory<>("yearLabel"));
        fsColDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        fsColTotal.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleDoubleProperty(cd.getValue().getTotalAmount()));
        fsColTotal.setCellFactory(col -> currencyCell());

        fsColActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit    = new Button("Edit");
            private final Button btnDelete  = new Button("Delete");
            private final Button btnInvoice = new Button("Generate");
            private final HBox   box        = new HBox(6, btnEdit, btnDelete, btnInvoice);
            {
                btnEdit.getStyleClass().add("btn-secondary");
                btnDelete.getStyleClass().add("btn-danger");
                btnInvoice.getStyleClass().add("btn-primary");
                box.setAlignment(Pos.CENTER_LEFT);
                btnEdit.setOnAction(e    -> handleEditStructure(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e  -> handleDeleteStructure(getTableView().getItems().get(getIndex())));
                btnInvoice.setOnAction(e -> handleGenerateBulk(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        structureTable.setItems(filteredStructures);
        structureTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, now) -> onStructureSelected(now));

        // Fee items columns
        fiColName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        fiColAmount.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleDoubleProperty(cd.getValue().getAmount()));
        fiColAmount.setCellFactory(col -> currencyCell());

        fiColActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox   box       = new HBox(6, btnEdit, btnDelete);
            {
                btnEdit.getStyleClass().add("btn-secondary");
                btnDelete.getStyleClass().add("btn-danger");
                box.setAlignment(Pos.CENTER_LEFT);
                btnEdit.setOnAction(e   -> handleEditItem(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDeleteItem(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void onStructureSelected(FeeStructure fs) {
        selectedStructure = fs;
        if (fs == null) {
            itemsTitle.setText("Fee Items — select a structure above");
            btnAddItem.setDisable(true);
            itemsTable.getItems().clear();
            return;
        }
        itemsTitle.setText("Items — Grade " + fs.getGradeLevel() + " / " + fs.getTermDisplay());
        btnAddItem.setDisable(false);
        loadItems(fs.getId());
    }

    private void loadItems(int structureId) {
        try {
            itemsTable.setItems(FXCollections.observableArrayList(
                    structureDAO.getItemsByStructure(structureId)));
        } catch (SQLException ex) { showError("Database error", ex.getMessage()); }
    }

    private void applyStructureFilter(Term term) {
        if (term == null) {
            filteredStructures.setPredicate(s -> true);
        } else {
            filteredStructures.setPredicate(s -> s.getTermId() == term.getId());
        }
    }

    private void loadStructures() throws SQLException {
        allStructures.setAll(structureDAO.getAll());
    }

    @FXML
    private void handleAddStructure() {
        showStructureDialog(null).ifPresent(fs -> {
            try {
                structureDAO.insert(fs);
                loadStructures();
            } catch (SQLException ex) {
                showError("Save failed",
                        ex.getMessage().contains("Duplicate")
                                ? "A fee structure for that grade and term already exists."
                                : ex.getMessage());
            }
        });
    }

    private void handleEditStructure(FeeStructure fs) {
        showStructureDialog(fs).ifPresent(updated -> {
            try { structureDAO.update(updated); loadStructures(); }
            catch (SQLException ex) { showError("Save failed", ex.getMessage()); }
        });
    }

    private void handleDeleteStructure(FeeStructure fs) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete fee structure for " + fs.getGradeDisplay() + " / " + fs.getTermDisplay()
                        + "?\nAll fee items will also be deleted.",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try { structureDAO.delete(fs.getId()); loadStructures(); onStructureSelected(null); }
                catch (SQLException ex) { showError("Delete failed", ex.getMessage()); }
            }
        });
    }

    /** "Generate" button on a structure row → bulk-generate invoices for the whole grade. */
    private void handleGenerateBulk(FeeStructure fs) {
        showBulkGenerateDialog(fs).ifPresent(dates -> {
            try {
                int count = invoiceDAO.generateBulkInvoices(
                        fs.getId(), fs.getGradeLevel(), fs.getTermId(),
                        dates[0], dates[1]);
                showInfo("Invoices Generated",
                        count + " invoice(s) generated for Grade " + fs.getGradeLevel()
                                + " — " + fs.getTermDisplay() + ".\n"
                                + "(Students already invoiced were skipped.)");
                loadInvoices(null);
            } catch (SQLException ex) { showError("Generate failed", ex.getMessage()); }
        });
    }

    @FXML
    private void handleAddItem() {
        if (selectedStructure == null) return;
        showItemDialog(null, selectedStructure.getId()).ifPresent(item -> {
            try {
                structureDAO.insertItem(item);
                loadItems(selectedStructure.getId());
                loadStructures();   // refresh total
            } catch (SQLException ex) { showError("Save failed", ex.getMessage()); }
        });
    }

    private void handleEditItem(FeeItem item) {
        showItemDialog(item, item.getFeeStructureId()).ifPresent(updated -> {
            try {
                structureDAO.updateItem(updated);
                loadItems(item.getFeeStructureId());
                loadStructures();
            } catch (SQLException ex) { showError("Save failed", ex.getMessage()); }
        });
    }

    private void handleDeleteItem(FeeItem item) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete fee item \"" + item.getItemName() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    structureDAO.deleteItem(item.getId());
                    loadItems(item.getFeeStructureId());
                    loadStructures();
                } catch (SQLException ex) { showError("Delete failed", ex.getMessage()); }
            }
        });
    }

    // ══════════════════════════════════════════════════════════
    //  TAB 2 — INVOICES
    // ══════════════════════════════════════════════════════════

    private void setupInvoiceTab() {
        statusFilter.setItems(FXCollections.observableArrayList(
                "All", "unpaid", "partial", "paid", "overdue"));
        statusFilter.setValue("All");
        statusFilter.valueProperty().addListener((obs, old, now) -> applyInvoiceFilter());
        invoiceSearchField.textProperty().addListener((obs, old, now) -> applyInvoiceFilter());

        // Columns
        invColStudent.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        invColDue.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(
                        cd.getValue().getDueDate() != null
                                ? DATE_FMT.format(cd.getValue().getDueDate()) : "—"));
        invColTotal.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleDoubleProperty(cd.getValue().getNetPayable()));
        invColTotal.setCellFactory(col -> currencyCell());
        invColDiscount.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleDoubleProperty(cd.getValue().getDiscountAmount()));
        invColDiscount.setCellFactory(col -> currencyCell());
        invColPaid.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleDoubleProperty(cd.getValue().getTotalPaid()));
        invColPaid.setCellFactory(col -> currencyCell());
        invColOutstanding.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleDoubleProperty(cd.getValue().getOutstanding()));
        invColOutstanding.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(String.format("%.2f", item.doubleValue()));
                setStyle(item.doubleValue() > 0
                        ? "-fx-text-fill:#dc2626;-fx-font-weight:bold;"
                        : "-fx-text-fill:#16a34a;-fx-font-weight:bold;");
            }
        });

        invColStatus.setCellValueFactory(new PropertyValueFactory<>("statusDisplay"));
        invColStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(switch (item) {
                    case "Paid"    -> "-fx-text-fill:#16a34a;-fx-font-weight:bold;";
                    case "Partial" -> "-fx-text-fill:#d97706;-fx-font-weight:bold;";
                    case "Overdue" -> "-fx-text-fill:#dc2626;-fx-font-weight:bold;";
                    default        -> "-fx-text-fill:#64748b;-fx-font-weight:bold;";
                });
            }
        });

        invColActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnPay = new Button("Record Payment");
            {
                btnPay.getStyleClass().add("btn-primary");
                btnPay.setOnAction(e -> {
                    selectedInvoice = getTableView().getItems().get(getIndex());
                    handleRecordPayment();
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Invoice inv = getTableView().getItems().get(getIndex());
                btnPay.setDisable(inv.isFullyPaid());
                setGraphic(btnPay);
            }
        });

        invoiceTable.setItems(filteredInvoices);
        invoiceTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, now) -> onInvoiceSelected(now));

        // Payment columns
        pyColDate.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(
                        cd.getValue().getPaymentDate() != null
                                ? DATE_FMT.format(cd.getValue().getPaymentDate()) : "—"));
        pyColAmount.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleDoubleProperty(cd.getValue().getAmount()));
        pyColAmount.setCellFactory(col -> currencyCell());
        pyColMethod.setCellValueFactory(new PropertyValueFactory<>("methodDisplay"));
        pyColReceipt.setCellValueFactory(new PropertyValueFactory<>("receiptNumber"));

        pyColActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnDelete = new Button("Delete");
            {
                btnDelete.getStyleClass().add("btn-danger");
                btnDelete.setOnAction(e -> handleDeletePayment(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDelete);
            }
        });
    }

    private void onInvoiceSelected(Invoice inv) {
        selectedInvoice = inv;
        if (inv == null) {
            paymentsTitle.setText("Payments — select an invoice above");
            btnRecordPayment.setDisable(true);
            paymentsTable.getItems().clear();
            return;
        }
        paymentsTitle.setText("Payments — " + inv.getStudentName());
        btnRecordPayment.setDisable(inv.isFullyPaid());
        loadPayments(inv.getId());
    }

    private void loadPayments(int invoiceId) {
        try {
            paymentsTable.setItems(FXCollections.observableArrayList(
                    invoiceDAO.getPaymentsByInvoice(invoiceId)));
        } catch (SQLException ex) { showError("Database error", ex.getMessage()); }
    }

    private void loadInvoices(String status) {
        try {
            String filter = "All".equals(status) ? null : status;
            allInvoices.setAll(invoiceDAO.getAll(filter));
            applyInvoiceFilter();
            updateInvoiceSummary();
        } catch (SQLException ex) { showError("Database error", ex.getMessage()); }
    }

    private void applyInvoiceFilter() {
        String search = invoiceSearchField.getText().trim().toLowerCase();
        String status = statusFilter.getValue();

        filteredInvoices.setPredicate(inv -> {
            boolean matchStatus = "All".equals(status) || status == null
                    || status.equalsIgnoreCase(inv.getStatus());
            boolean matchSearch = search.isEmpty()
                    || inv.getStudentName().toLowerCase().contains(search);
            return matchStatus && matchSearch;
        });
        updateInvoiceSummary();
    }

    private void updateInvoiceSummary() {
        double outstanding = filteredInvoices.stream().mapToDouble(Invoice::getOutstanding).sum();
        double paid        = filteredInvoices.stream().mapToDouble(Invoice::getTotalPaid).sum();
        int    count       = filteredInvoices.size();

        lblTotalOutstanding.setText(String.format("Outstanding: LKR %.2f", outstanding));
        lblTotalPaid.setText(String.format("Collected: LKR %.2f", paid));
        lblInvoiceCount.setText(count + " invoice(s)");
    }

    @FXML
    private void handleInvoiceSearch() { applyInvoiceFilter(); }

    @FXML
    private void handleGenerateInvoices() {
        // Let user pick a fee structure and generate for all eligible students
        try {
            List<FeeStructure> structures = structureDAO.getAll();
            if (structures.isEmpty()) {
                showError("No structures", "Please create at least one fee structure first.");
                return;
            }
            showSelectStructureDialog(structures).ifPresent(fs ->
                    handleGenerateBulk(fs));
        } catch (SQLException ex) { showError("Database error", ex.getMessage()); }
    }

    @FXML
    private void handleRecordPayment() {
        if (selectedInvoice == null) return;
        showPaymentDialog(selectedInvoice).ifPresent(p -> {
            try {
                invoiceDAO.recordPayment(p);
                loadPayments(selectedInvoice.getId());
                loadInvoices(null);                    // refresh status + totals
                invoiceTable.getSelectionModel().clearSelection();
            } catch (SQLException ex) { showError("Save failed", ex.getMessage()); }
        });
    }

    private void handleDeletePayment(Payment p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete this payment of LKR " + String.format("%.2f", p.getAmount()) + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    invoiceDAO.deletePayment(p.getId(), p.getInvoiceId());
                    if (selectedInvoice != null) loadPayments(selectedInvoice.getId());
                    loadInvoices(null);
                } catch (SQLException ex) { showError("Delete failed", ex.getMessage()); }
            }
        });
    }

    // ══════════════════════════════════════════════════════════
    //  DIALOGS
    // ══════════════════════════════════════════════════════════

    private Optional<FeeStructure> showStructureDialog(FeeStructure existing) {
        boolean editing = existing != null;
        Dialog<FeeStructure> dlg = new Dialog<>();
        dlg.setTitle(editing ? "Edit Fee Structure" : "Add Fee Structure");
        dlg.setHeaderText(null);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(420);

        ComboBox<Term> cbTerm = new ComboBox<>();
        cbTerm.setMaxWidth(Double.MAX_VALUE);
        cbTerm.setPromptText("Select term *");
        try {
            AcademicYear ay = yearDAO.getCurrent();
            if (ay != null) cbTerm.setItems(FXCollections.observableArrayList(termDAO.getByYear(ay.getId())));
        } catch (SQLException ex) { showError("Database error", ex.getMessage()); }

        ComboBox<String> cbGrade = new ComboBox<>();
        ObservableList<String> gradeOpts = FXCollections.observableArrayList();
        for (int g = 1; g <= 13; g++) gradeOpts.add("Grade " + g);
        cbGrade.setItems(gradeOpts);
        cbGrade.setPromptText("Select grade *");
        cbGrade.setMaxWidth(Double.MAX_VALUE);

        TextField tfDesc = new TextField();
        tfDesc.setPromptText("Description (optional)");

        if (editing) {
            cbGrade.setValue("Grade " + existing.getGradeLevel());
            tfDesc.setText(existing.getDescription());
            cbTerm.getItems().stream()
                    .filter(t -> t.getId() == existing.getTermId())
                    .findFirst().ifPresent(cbTerm::setValue);
            cbTerm.setDisable(true);   // can't change term after creation
            cbGrade.setDisable(true);
        }

        GridPane grid = formGrid();
        grid.addRow(0, label("Term *"),        cbTerm);
        grid.addRow(1, label("Grade *"),       cbGrade);
        grid.addRow(2, label("Description"),   tfDesc);
        GridPane.setHgrow(cbTerm,  Priority.ALWAYS);
        GridPane.setHgrow(cbGrade, Priority.ALWAYS);
        dlg.getDialogPane().setContent(grid);

        Button ok = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            StringBuilder err = new StringBuilder();
            if (!editing && cbTerm.getValue() == null)  err.append("• Term is required\n");
            if (!editing && cbGrade.getValue() == null) err.append("• Grade is required\n");
            if (!err.isEmpty()) { ev.consume(); showError("Validation", err.toString().trim()); }
        });

        dlg.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            FeeStructure fs = editing ? existing : new FeeStructure();
            if (!editing) {
                fs.setTermId(cbTerm.getValue().getId());
                fs.setGradeLevel(Integer.parseInt(cbGrade.getValue().replace("Grade ", "").trim()));
            }
            fs.setDescription(tfDesc.getText().trim());
            return fs;
        });

        return dlg.showAndWait();
    }

    private Optional<FeeItem> showItemDialog(FeeItem existing, int structureId) {
        boolean editing = existing != null;
        Dialog<FeeItem> dlg = new Dialog<>();
        dlg.setTitle(editing ? "Edit Fee Item" : "Add Fee Item");
        dlg.setHeaderText(null);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(380);

        TextField tfName   = new TextField(); tfName.setPromptText("Item name *");
        TextField tfAmount = new TextField(); tfAmount.setPromptText("Amount *");

        if (editing) {
            tfName.setText(existing.getItemName());
            tfAmount.setText(String.valueOf(existing.getAmount()));
        }

        GridPane grid = formGrid();
        grid.addRow(0, label("Item Name *"),   tfName);
        grid.addRow(1, label("Amount (LKR) *"), tfAmount);
        dlg.getDialogPane().setContent(grid);

        Button ok = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            StringBuilder err = new StringBuilder();
            if (tfName.getText().isBlank()) err.append("• Item name is required\n");
            double amt = -1;
            try { amt = Double.parseDouble(tfAmount.getText().trim()); }
            catch (NumberFormatException e) { err.append("• Amount must be a valid number\n"); }
            if (amt <= 0 && err.isEmpty()) err.append("• Amount must be greater than 0\n");
            if (!err.isEmpty()) { ev.consume(); showError("Validation", err.toString().trim()); }
        });

        dlg.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            FeeItem fi = editing ? existing : new FeeItem();
            fi.setFeeStructureId(structureId);
            fi.setItemName(tfName.getText().trim());
            fi.setAmount(Double.parseDouble(tfAmount.getText().trim()));
            return fi;
        });

        return dlg.showAndWait();
    }

    /** Dialog to pick issue + due dates before bulk-generating invoices. */
    private Optional<LocalDate[]> showBulkGenerateDialog(FeeStructure fs) {
        Dialog<LocalDate[]> dlg = new Dialog<>();
        dlg.setTitle("Generate Invoices — Grade " + fs.getGradeLevel() + " / " + fs.getTermDisplay());
        dlg.setHeaderText("Invoices will be created for all active enrolled students in Grade "
                + fs.getGradeLevel() + ".\nStudents already invoiced will be skipped.");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(420);

        DatePicker dpIssued = buildDatePicker(); dpIssued.setValue(LocalDate.now());
        DatePicker dpDue    = buildDatePicker(); dpDue.setValue(LocalDate.now().plusDays(30));

        GridPane grid = formGrid();
        grid.addRow(0, label("Issue Date *"), dpIssued);
        grid.addRow(1, label("Due Date *"),   dpDue);
        dlg.getDialogPane().setContent(grid);

        Button ok = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            StringBuilder err = new StringBuilder();
            if (dpIssued.getValue() == null) err.append("• Issue date is required\n");
            if (dpDue.getValue() == null)    err.append("• Due date is required\n");
            if (!err.isEmpty()) { ev.consume(); showError("Validation", err.toString().trim()); }
        });

        dlg.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            return new LocalDate[]{ dpIssued.getValue(), dpDue.getValue() };
        });

        return dlg.showAndWait();
    }

    /** "Generate Invoices" button on Tab 2 — pick a structure first. */
    private Optional<FeeStructure> showSelectStructureDialog(List<FeeStructure> structures) {
        Dialog<FeeStructure> dlg = new Dialog<>();
        dlg.setTitle("Select Fee Structure");
        dlg.setHeaderText("Choose the fee structure to generate invoices for.");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(380);

        ComboBox<FeeStructure> cbStruct = new ComboBox<>(FXCollections.observableArrayList(structures));
        cbStruct.setPromptText("Select structure *");
        cbStruct.setMaxWidth(Double.MAX_VALUE);

        GridPane grid = formGrid();
        grid.addRow(0, label("Fee Structure *"), cbStruct);
        GridPane.setHgrow(cbStruct, Priority.ALWAYS);
        dlg.getDialogPane().setContent(grid);

        Button ok = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            if (cbStruct.getValue() == null) { ev.consume(); showError("Validation", "• Please select a structure"); }
        });

        dlg.setResultConverter(btn -> btn == ButtonType.OK ? cbStruct.getValue() : null);
        return dlg.showAndWait();
    }

    private Optional<Payment> showPaymentDialog(Invoice inv) {
        Dialog<Payment> dlg = new Dialog<>();
        dlg.setTitle("Record Payment — " + inv.getStudentName());
        dlg.setHeaderText(String.format("Outstanding: LKR %.2f", inv.getOutstanding()));
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.getDialogPane().setPrefWidth(400);

        TextField  tfAmount  = new TextField(String.format("%.2f", inv.getOutstanding()));
        DatePicker dpDate    = buildDatePicker(); dpDate.setValue(LocalDate.now());
        ComboBox<String> cbMethod = new ComboBox<>(
                FXCollections.observableArrayList("cash", "cheque", "bank"));
        cbMethod.setValue("cash");
        cbMethod.setMaxWidth(Double.MAX_VALUE);
        TextField tfReceipt = new TextField(); tfReceipt.setPromptText("Receipt number (optional)");

        GridPane grid = formGrid();
        grid.addRow(0, label("Amount (LKR) *"), tfAmount);
        grid.addRow(1, label("Date *"),         dpDate);
        grid.addRow(2, label("Method *"),       cbMethod);
        grid.addRow(3, label("Receipt No."),    tfReceipt);
        dlg.getDialogPane().setContent(grid);

        Button ok = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            StringBuilder err = new StringBuilder();
            double amt = -1;
            try { amt = Double.parseDouble(tfAmount.getText().trim()); }
            catch (NumberFormatException e) { err.append("• Amount must be a valid number\n"); }
            if (err.isEmpty() && amt <= 0)
                err.append("• Amount must be greater than 0\n");
            if (err.isEmpty() && amt > inv.getOutstanding())
                err.append("• Amount cannot exceed outstanding balance (LKR "
                        + String.format("%.2f", inv.getOutstanding()) + ")\n");
            if (dpDate.getValue() == null) err.append("• Date is required\n");
            if (!err.isEmpty()) { ev.consume(); showError("Validation", err.toString().trim()); }
        });

        dlg.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            return new Payment(
                    inv.getId(),
                    Double.parseDouble(tfAmount.getText().trim()),
                    dpDate.getValue(),
                    cbMethod.getValue(),
                    tfReceipt.getText().trim(),
                    currentUserId);
        });

        return dlg.showAndWait();
    }

    // ══════════════════════════════════════════════════════════
    //  SHARED HELPERS
    // ══════════════════════════════════════════════════════════

    private <T> TableCell<T, Number> currencyCell() {
        return new TableCell<>() {
            @Override protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : String.format("%.2f", item.doubleValue()));
            }
        };
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

    private void showInfo(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}
