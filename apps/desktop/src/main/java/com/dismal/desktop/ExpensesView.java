package com.dismal.desktop;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ExpensesView {

    private final BorderPane root;
    private final ApiClient apiClient;
    private final ObservableList<ApiClient.ExpenseItem> expenses;
    private final TableView<ApiClient.ExpenseItem> table;
    private final Label statusLabel;

    private final Label paymentsLabel = new Label("-");
    private final Label expensesLabel = new Label("-");
    private final Label balanceLabel = new Label("-");
    private final Label recommendationLabel = new Label("-");

    private final TextField nameField = new TextField();
    private final TextField categoryField = new TextField();
    private final TextField totalAmountField = new TextField();
    private final TextField monthsField = new TextField();
    private final TextField dueDayField = new TextField();
    private final TextField priorityField = new TextField();
    private final ComboBox<String> typeField = new ComboBox<>();
    private ApiClient.ExpenseItem selected;

    public ExpensesView(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.root = new BorderPane();
        this.expenses = FXCollections.observableArrayList();
        this.table = new TableView<>(expenses);
        this.statusLabel = new Label();

        root.getStyleClass().add("content-root");
        root.setPadding(new Insets(16));

        Label title = new Label("Gastos y pagos");
        title.getStyleClass().add("page-title");
        Button refreshButton = new Button("Actualizar");
        refreshButton.getStyleClass().add("button-ghost");
        refreshButton.setOnAction(event -> loadData());
        Button newButton = new Button("+ Nuevo gasto");
        newButton.getStyleClass().add("button-primary");
        newButton.setOnAction(event -> clearForm());

        ToolBar toolBar = new ToolBar(title, refreshButton, newButton);
        toolBar.getStyleClass().add("toolbar");
        root.setTop(toolBar);

        HBox summary = new HBox(12,
                summaryCard("Pagos del mes", paymentsLabel),
                summaryCard("Gastos del mes", expensesLabel),
                summaryCard("Saldo", balanceLabel)
        );
        summary.setPadding(new Insets(8, 0, 8, 0));

        recommendationLabel.getStyleClass().add("help-text");

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().addAll(
                column("Nombre", "name"),
                column("Categoría", "category"),
                column("Tipo", "type"),
                column("Total", "totalAmount"),
                column("Meses", "months"),
                column("Cuota", "monthlyAmount"),
                column("Vence", "dueDay"),
                column("Prioridad", "priority")
        );
        table.getSelectionModel().selectedItemProperty().addListener((obs, prev, next) -> {
            if (next != null) {
                selectExpense(next);
            }
        });

        VBox listPanel = new VBox(10, summary, recommendationLabel, table);
        VBox formPanel = buildForm();
        HBox content = new HBox(12, listPanel, formPanel);
        HBox.setHgrow(listPanel, javafx.scene.layout.Priority.ALWAYS);

        statusLabel.getStyleClass().add("page-subtitle");
        root.setCenter(content);
        root.setBottom(statusLabel);

        loadData();
    }

    public Parent getRoot() {
        return root;
    }

    private VBox summaryCard(String title, Label valueLabel) {
        VBox box = new VBox(4);
        box.getStyleClass().add("panel");
        box.setPadding(new Insets(10));
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("help-text");
        valueLabel.getStyleClass().add("metric-value");
        box.getChildren().addAll(titleLabel, valueLabel);
        return box;
    }

    private VBox buildForm() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("panel");
        panel.setPadding(new Insets(12));

        Label formTitle = new Label("Registrar gasto");
        formTitle.getStyleClass().add("section-title");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Nombre"), nameField);
        form.addRow(1, new Label("Categoría"), categoryField);
        typeField.getItems().addAll("MENSUAL", "DIFERIDO");
        typeField.getSelectionModel().select("MENSUAL");
        typeField.valueProperty().addListener((obs, prev, next) -> updateTypeState(next));
        form.addRow(2, new Label("Tipo"), typeField);
        form.addRow(3, new Label("Total/Mensual"), totalAmountField);
        form.addRow(4, new Label("Meses"), monthsField);
        form.addRow(5, new Label("Vence (día)"), dueDayField);
        form.addRow(6, new Label("Prioridad"), priorityField);

        Button saveButton = new Button("Guardar");
        saveButton.getStyleClass().add("button-primary");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setOnAction(event -> saveExpense());

        Button deleteButton = new Button("Eliminar");
        deleteButton.getStyleClass().add("button-danger");
        deleteButton.setMaxWidth(Double.MAX_VALUE);
        deleteButton.setOnAction(event -> deleteExpense());

        panel.getChildren().addAll(formTitle, form, saveButton, deleteButton);
        updateTypeState(typeField.getValue());
        return panel;
    }

    private void loadData() {
        statusLabel.setText("Cargando gastos...");
        Thread worker = new Thread(() -> {
            try {
                var data = apiClient.listExpenses();
                var summary = apiClient.getExpenseSummary();
                Platform.runLater(() -> {
                    expenses.setAll(data);
                    paymentsLabel.setText(FormatUtils.currency(summary.monthPayments()));
                    expensesLabel.setText(FormatUtils.currency(summary.monthExpenses()));
                    balanceLabel.setText(FormatUtils.currency(summary.balance()));
                    recommendationLabel.setText(summary.recommendation());
                    statusLabel.setText("Gastos cargados.");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("No se pudo cargar gastos."));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void selectExpense(ApiClient.ExpenseItem item) {
        selected = item;
        nameField.setText(item.name());
        categoryField.setText(item.category());
        typeField.getSelectionModel().select(item.type() != null ? item.type() : "MENSUAL");
        totalAmountField.setText(String.valueOf(item.totalAmount()));
        monthsField.setText(String.valueOf(item.months()));
        dueDayField.setText(String.valueOf(item.dueDay()));
        priorityField.setText(item.priority());
        updateTypeState(typeField.getValue());
    }

    private void saveExpense() {
        ApiClient.ExpenseRequest payload;
        try {
            String type = typeField.getValue() != null ? typeField.getValue() : "MENSUAL";
            int dueDay = parseIntOrDefault(dueDayField.getText(), 1);
            payload = new ApiClient.ExpenseRequest(
                    nameField.getText().trim(),
                    categoryField.getText().trim(),
                    type,
                    Double.parseDouble(totalAmountField.getText().trim()),
                    "MENSUAL".equalsIgnoreCase(type) ? 1 : parseIntOrDefault(monthsField.getText(), 1),
                    dueDay,
                    priorityField.getText().isBlank() ? "MEDIA" : priorityField.getText().trim(),
                    true
            );
        } catch (Exception ex) {
            statusLabel.setText("Datos inválidos.");
            return;
        }
        Thread worker = new Thread(() -> {
            try {
                if (selected == null) {
                    apiClient.createExpense(payload);
                } else {
                    apiClient.updateExpense(selected.id(), payload);
                }
                Platform.runLater(this::loadData);
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("No se pudo guardar."));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void deleteExpense() {
        if (selected == null) return;
        Thread worker = new Thread(() -> {
            try {
                apiClient.deleteExpense(selected.id());
                Platform.runLater(this::loadData);
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("No se pudo eliminar."));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void clearForm() {
        selected = null;
        nameField.clear();
        categoryField.clear();
        totalAmountField.clear();
        monthsField.clear();
        dueDayField.clear();
        priorityField.clear();
        typeField.getSelectionModel().select("MENSUAL");
        updateTypeState(typeField.getValue());
    }

    private void updateTypeState(String type) {
        boolean mensual = type == null || "MENSUAL".equalsIgnoreCase(type);
        monthsField.setDisable(mensual);
        if (mensual) {
            monthsField.setText("1");
        }
        if (dueDayField.getText().isBlank()) {
            dueDayField.setText("1");
        }
    }

    private int parseIntOrDefault(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return fallback;
        }
        return Integer.parseInt(trimmed);
    }

    private <T> TableColumn<ApiClient.ExpenseItem, T> column(String title, String property) {
        TableColumn<ApiClient.ExpenseItem, T> column = new TableColumn<>(title);
        FxTable.bindColumn(column, property);
        return column;
    }
}
