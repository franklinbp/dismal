package com.dismal.desktop;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.List;

public class LicensesView {

    private final BorderPane root;
    private final ApiClient apiClient;
    private final com.dismal.desktop.local.repository.LocalLicenseRepository localLicenseRepository;
    
    private final ObservableList<License> licenses;
    private final ObservableList<ProductLicenseSummary> summaries;
    private final ObservableList<LicenseAssignment> assignments;
    private final ObservableList<Product> products;
    
    private final TableView<License> table;
    private final TableView<ProductLicenseSummary> summaryTable;
    private final Label statusLabel;
    
    private final Button refreshButton;
    private final Button newKeyButton;
    
    // Filters
    private final ComboBox<Product> productFilter;
    private final ComboBox<String> statusFilter;
    private final TextField searchField;
    
    // Drawer Components
    private final VBox drawerPanel;
    private final Button closeDrawerButton;
    private final Label drawerTitle;
    private final ListView<String> assignmentList;
    private final ComboBox<Product> createProduct;
    private final TextField licenseKeyField;
    private final TextArea bulkKeysField;
    private final CheckBox bulkModeBox;
    private final TextField priceField;
    private final TextField activationsField;
    private final Button createButton;
    
    private final SplitPane splitPane;

    public LicensesView(ApiClient apiClient, com.dismal.desktop.local.repository.LocalLicenseRepository localLicenseRepository) {
        this.apiClient = apiClient;
        this.localLicenseRepository = localLicenseRepository;
        this.root = new BorderPane();
        this.licenses = FXCollections.observableArrayList();
        this.summaries = FXCollections.observableArrayList();
        this.assignments = FXCollections.observableArrayList();
        this.products = FXCollections.observableArrayList();
        this.table = new TableView<>(licenses);
        this.summaryTable = new TableView<>(summaries);
        this.statusLabel = new Label();

        root.getStyleClass().add("content-root");

        // --- Toolbar ---
        Label title = new Label("Gestión de Claves");
        title.getStyleClass().add("page-title");

        refreshButton = new Button("Actualizar");
        refreshButton.getStyleClass().add("button-ghost");
        refreshButton.setOnAction(event -> loadLicenses());
        
        newKeyButton = new Button("+ Nueva Clave");
        newKeyButton.getStyleClass().add("button-primary");
        newKeyButton.setOnAction(event -> openDrawer(true));

        ToolBar toolBar = new ToolBar(title, refreshButton, newKeyButton);
        toolBar.getStyleClass().add("toolbar");
        root.setTop(toolBar);

        // --- Filters ---
        productFilter = new ComboBox<>(products);
        productFilter.setPromptText("Todos los productos");
        productFilter.setConverter(productConverter());
        productFilter.setOnAction(event -> loadLicenses());

        statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("", "ACTIVE", "INACTIVE", "EXPIRED", "DAMAGED");
        statusFilter.setPromptText("Estado");
        statusFilter.setOnAction(event -> loadLicenses());

        searchField = new TextField();
        searchField.setPromptText("Buscar producto o clave...");
        searchField.setOnAction(event -> loadLicenses());

        Button applyFilters = new Button("filtrar");
        applyFilters.getStyleClass().add("button-ghost");
        applyFilters.setOnAction(event -> loadLicenses());

        HBox filters = new HBox(10, new Label("Filtros:"), productFilter, statusFilter, searchField, applyFilters);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.setPadding(new Insets(0, 0, 10, 0));
        HBox.setHgrow(searchField, Priority.ALWAYS);

        // --- Main Table ---
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().addAll(
                column("Producto", "softwareName"),
                column("License Key", "licenseKey"),
                column("Estado", "status"),
                column("Usadas", "usedActivations"),
                column("Max", "maxActivations"),
                column("Costo", "purchasePrice")
        );
        table.getSelectionModel().selectedItemProperty().addListener((obs, prev, next) -> {
            if (next != null) {
                openDrawer(false);
                loadAssignments(next.id());
            }
        });
        VBox.setVgrow(table, Priority.ALWAYS);
        
        summaryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        summaryTable.getColumns().addAll(
                columnSummary("Producto", "softwareName"),
                columnSummary("Disponibles", "availableSerials"),
                columnSummary("Total", "totalSerials"),
                columnSummary("Activaciones", "usedActivations"),
                columnSummary("Max", "maxActivations"),
                columnSummary("Costo total", "totalCost")
        );
        summaryTable.setPrefHeight(180);

        VBox mainContent = new VBox(10, filters, summaryTable, table);

        // --- Drawer (Right Panel) ---
        drawerTitle = new Label("Detalle de Licencia");
        drawerTitle.getStyleClass().add("section-title");
        
        closeDrawerButton = new Button("✕");
        closeDrawerButton.getStyleClass().add("button-ghost");
        closeDrawerButton.setOnAction(e -> closeDrawer());
        
        HBox drawerHeader = new HBox(10, drawerTitle, closeDrawerButton);
        HBox.setHgrow(drawerTitle, Priority.ALWAYS);
        closeDrawerButton.setAlignment(Pos.CENTER_RIGHT);

        // Drawer Content: History
        Label historyLabel = new Label("Historial de Activaciones");
        historyLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 5 0;");
        assignmentList = new ListView<>();
        assignmentList.setPrefHeight(150);
        assignmentList.setPlaceholder(new Label("Sin activaciones registradas"));
        
        assignments.addListener((javafx.collections.ListChangeListener<LicenseAssignment>) change -> {
            ObservableList<String> rows = FXCollections.observableArrayList();
            for (LicenseAssignment item : assignments) {
                rows.add(item.clientEmail() + "\n" + FormatUtils.dateTime(item.assignedAt()));
            }
            assignmentList.setItems(rows);
        });

        // Drawer Content: Create Form
        Label createLabel = new Label("Nueva Licencia");
        createLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 5 0;");
        
        createProduct = new ComboBox<>(products);
        createProduct.setPromptText("Seleccionar Producto...");
        createProduct.setConverter(productConverter());
        createProduct.setMaxWidth(Double.MAX_VALUE);
        
        licenseKeyField = new TextField(); licenseKeyField.setPromptText("Clave (Ej: XXXX-YYYY)");
        bulkKeysField = new TextArea();
        bulkKeysField.setPromptText("Pega una clave por linea");
        bulkKeysField.setPrefRowCount(6);
        bulkKeysField.setWrapText(true);
        bulkModeBox = new CheckBox("Carga masiva");
        bulkModeBox.setOnAction(event -> toggleBulkMode());
        priceField = new TextField(); priceField.setPromptText("Costo (0.00)");
        activationsField = new TextField("1"); activationsField.setPromptText("Max Activaciones");

        createButton = new Button("Guardar Clave");
        createButton.getStyleClass().add("button-primary");
        createButton.setMaxWidth(Double.MAX_VALUE);
        createButton.setOnAction(event -> createLicense());

        drawerPanel = new VBox(10);
        drawerPanel.setPadding(new Insets(20));
        drawerPanel.getStyleClass().add("drawer-panel");
        drawerPanel.setMinWidth(320);
        drawerPanel.setMaxWidth(360);
        // We populate drawer children dynamically in openDrawer based on mode

        // --- Split Pane Layout ---
        splitPane = new SplitPane();
        splitPane.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        splitPane.getItems().add(mainContent); // Start closed

        root.setCenter(splitPane);
        
        statusLabel.getStyleClass().add("page-subtitle");
        statusLabel.setPadding(new Insets(8));
        root.setBottom(statusLabel);

        loadProducts();
        loadLicenses();
    }

    public Parent getRoot() {
        return root;
    }

    private void openDrawer(boolean createMode) {
        drawerPanel.getChildren().clear();
        drawerPanel.getChildren().add(new HBox(10, drawerTitle, closeDrawerButton)); // Header always present
        
        if (createMode) {
            drawerTitle.setText("Agregar Nueva Clave");
            table.getSelectionModel().clearSelection();
            drawerPanel.getChildren().addAll(
                new Label("Producto *"), createProduct,
                bulkModeBox,
                new Label("License Key *"), licenseKeyField,
                new Label("Claves (masivo)"), bulkKeysField,
                new HBox(10, 
                    new VBox(5, new Label("Costo"), priceField),
                    new VBox(5, new Label("Max Activaciones"), activationsField)
                ),
                new javafx.scene.control.Separator(),
                createButton
            );
            toggleBulkMode();
        } else {
            drawerTitle.setText("Historial de Clave");
            drawerPanel.getChildren().addAll(
                assignmentList,
                new Label("Detalles"),
                new Label("Solo lectura. Para editar, usa la versión Web.")
            );
        }

        if (!splitPane.getItems().contains(drawerPanel)) {
            splitPane.getItems().add(drawerPanel);
            splitPane.setDividerPositions(0.7);
        }
    }

    private void closeDrawer() {
        splitPane.getItems().remove(drawerPanel);
        table.getSelectionModel().clearSelection();
    }

    private void loadProducts() {
        Thread worker = new Thread(() -> {
            try {
                List<Product> data = apiClient.listProducts();
                Platform.runLater(() -> products.setAll(data));
            } catch (Exception ignored) {}
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void loadLicenses() {
        setLoading(true);
        statusLabel.setText("Cargando...");
        String productId = productFilter.getValue() != null ? productFilter.getValue().getId() : null; // Use getId() directly
        String statusFilterValue = statusFilter.getValue() != null ? statusFilter.getValue() : "";
        String search = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";

        Thread worker = new Thread(() -> {
            try {
                List<com.dismal.desktop.local.domain.LocalLicense> localData = localLicenseRepository.findAll();
                
                if (!localData.isEmpty()) {
                    List<License> mapped = localData.stream()
                        .filter(l -> productId == null || l.getSoftwareId().equals(productId))
                        .filter(l -> statusFilterValue.isBlank() || statusFilterValue.equalsIgnoreCase(l.getStatus()))
                        .map(l -> new License(
                            l.getId(),
                            l.getLicenseKey(),
                            l.getStatus(),
                            true,
                            l.getUsedActivations(),
                            l.getMaxActivations(),
                            l.getPurchasePrice().doubleValue(),
                            l.getSoftwareId(),
                            l.getSoftwareName(),
                            null, null
                        ))
                        .filter(l -> search.isBlank() || l.softwareName().toLowerCase().contains(search)
                                || l.licenseKey().toLowerCase().contains(search))
                        .collect(java.util.stream.Collectors.toList());
                        
                    Platform.runLater(() -> {
                        licenses.setAll(mapped);
                        summaries.setAll(buildSummaries(mapped));
                        statusLabel.setText(mapped.size() + " claves (Local)");
                    });
                } else {
                    PageResponse<License> page = apiClient.listLicenses(
                        productId != null ? productId : "", 
                        statusFilterValue, 
                        0, 200
                    );
                    List<License> filtered = page.content().stream()
                            .filter(l -> search.isBlank() || l.softwareName().toLowerCase().contains(search)
                                    || l.licenseKey().toLowerCase().contains(search))
                            .collect(java.util.stream.Collectors.toList());
                    Platform.runLater(() -> {
                        licenses.setAll(filtered);
                        summaries.setAll(buildSummaries(filtered));
                        statusLabel.setText(page.totalElements() + " claves (Nube)");
                    });
                }
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Error: " + ex.getMessage()));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void loadAssignments(String licenseId) {
        Thread worker = new Thread(() -> {
            try {
                List<LicenseAssignment> data = apiClient.getLicenseAssignments(licenseId);
                Platform.runLater(() -> assignments.setAll(data));
            } catch (Exception ignored) {
                Platform.runLater(assignments::clear);
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void createLicense() {
        Product product = createProduct.getValue();
        if (product == null) {
            statusLabel.setText("Selecciona un producto.");
            return;
        }
        boolean bulkMode = bulkModeBox.isSelected();
        String key = licenseKeyField.getText().trim();
        List<String> bulkKeys = bulkKeysField.getText() == null ? List.of() :
                bulkKeysField.getText().lines().map(String::trim).filter(s -> !s.isBlank()).toList();
        if (!bulkMode && key.isEmpty()) {
            statusLabel.setText("Clave requerida.");
            return;
        }
        if (bulkMode && bulkKeys.isEmpty()) {
            statusLabel.setText("Ingresa al menos una clave.");
            return;
        }
        double price;
        int maxActivations;
        try {
            price = Double.parseDouble(priceField.getText().trim());
            maxActivations = Integer.parseInt(activationsField.getText().trim());
        } catch (NumberFormatException ex) {
            statusLabel.setText("Valores numéricos inválidos.");
            return;
        }
        setLoading(true);
        statusLabel.setText("Guardando...");
        Thread worker = new Thread(() -> {
            try {
                if (bulkMode) {
                    List<ApiClient.LicenseCreateRequest> payloads = bulkKeys.stream()
                            .map(item -> new ApiClient.LicenseCreateRequest(
                                    product.getId(),
                                    item,
                                    price,
                                    maxActivations
                            ))
                            .toList();
                    apiClient.createLicensesBulk(payloads);
                } else {
                    ApiClient.LicenseCreateRequest payload = new ApiClient.LicenseCreateRequest(
                            product.getId(),
                            key,
                            price,
                            maxActivations
                    );
                    apiClient.createLicense(payload);
                }
                Platform.runLater(() -> {
                    licenseKeyField.clear();
                    bulkKeysField.clear();
                    priceField.clear();
                    activationsField.setText("1");
                    loadLicenses();
                    statusLabel.setText("Creada exitosamente.");
                    closeDrawer(); // Auto close on success
                });
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Error al crear."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void toggleBulkMode() {
        boolean bulk = bulkModeBox.isSelected();
        licenseKeyField.setDisable(bulk);
        bulkKeysField.setDisable(!bulk);
        licenseKeyField.setManaged(!bulk);
        bulkKeysField.setManaged(bulk);
        licenseKeyField.setVisible(!bulk);
        bulkKeysField.setVisible(bulk);
    }

    private List<ProductLicenseSummary> buildSummaries(List<License> rows) {
        java.util.Map<String, List<License>> grouped = rows.stream()
                .collect(java.util.stream.Collectors.groupingBy(License::softwareId));
        List<ProductLicenseSummary> result = new java.util.ArrayList<>();
        for (var entry : grouped.entrySet()) {
            List<License> items = entry.getValue();
            if (items.isEmpty()) {
                continue;
            }
            String name = items.get(0).softwareName();
            long available = items.stream()
                    .filter(l -> "ACTIVE".equalsIgnoreCase(l.status()) && l.usedActivations() < l.maxActivations())
                    .count();
            long total = items.size();
            long used = items.stream().mapToLong(License::usedActivations).sum();
            long max = items.stream().mapToLong(License::maxActivations).sum();
            double totalCost = items.stream().mapToDouble(License::purchasePrice).sum();
            double avgCost = total > 0 ? totalCost / total : 0;
            double costPerAct = max > 0 ? totalCost / max : 0;
            java.util.Set<String> statuses = items.stream().map(License::status).collect(java.util.stream.Collectors.toSet());
            String status = statuses.size() == 1 ? items.get(0).status() : "MIXED";
            result.add(new ProductLicenseSummary(entry.getKey(), name, available, total, used, max,
                    avgCost, totalCost, costPerAct, status));
        }
        return result;
    }

    private StringConverter<Product> productConverter() {
        return new StringConverter<>() {
            @Override public String toString(Product p) { return p == null ? "" : p.getName(); } // use getName()
            @Override public Product fromString(String s) { return null; }
        };
    }

    private <T> TableColumn<License, T> column(String title, String property) {
        TableColumn<License, T> column = new TableColumn<>(title);
        FxTable.bindColumn(column, property);
        return column;
    }

    private TableColumn<ProductLicenseSummary, ?> columnSummary(String title, String property) {
        if ("totalCost".equals(property)) {
            TableColumn<ProductLicenseSummary, String> column = new TableColumn<>(title);
            column.setCellValueFactory(cell ->
                    new javafx.beans.property.SimpleStringProperty(FormatUtils.currency(cell.getValue().totalCost())));
            return column;
        }
        TableColumn<ProductLicenseSummary, Object> column = new TableColumn<>(title);
        FxTable.bindColumn(column, property);
        return column;
    }

    private void setLoading(boolean loading) {
        refreshButton.setDisable(loading);
        newKeyButton.setDisable(loading);
        createButton.setDisable(loading);
    }
}
