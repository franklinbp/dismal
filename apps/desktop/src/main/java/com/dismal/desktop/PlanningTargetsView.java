package com.dismal.desktop;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlanningTargetsView {

    private final BorderPane root;
    private final ApiClient apiClient;
    private final com.dismal.desktop.local.repository.LocalSalesTargetRepository localSalesTargetRepository;
    private final ObservableList<SalesTarget> targets;
    private final ObservableList<Product> products;
    private final TableView<SalesTarget> table;
    private final Label summaryLabel;
    private final Label statusLabel;
    
    private final Button refreshButton;
    private final Button newTargetButton;
    private final TextField fixedCostGlobalField = new TextField();
    private final Button saveFixedCostButton = new Button("Guardar costo global");
    
    // Drawer
    private final VBox drawerPanel;
    private final Label drawerTitle;
    private final Button closeDrawerButton;
    
    private final Button saveButton;
    private final Button deleteButton;
    private SalesTarget selected;
    private boolean creating = false;
    private boolean usingLocalTargets = false;
    private final SplitPane splitPane;
    
    private final Map<String, Double> realCosts = new HashMap<>();

    // Form Fields
    private final ComboBox<Product> productField;
    private final TextField metaUnitsField;
    private final TextField salePriceField;
    private final TextField realCostField;
    private final TextField variableCostField;
    private final TextField fixedCostField;
    private final DatePicker deadlineField;
    private final TextArea notesField;

    public PlanningTargetsView(ApiClient apiClient, com.dismal.desktop.local.repository.LocalSalesTargetRepository localSalesTargetRepository) {
        this.apiClient = apiClient;
        this.localSalesTargetRepository = localSalesTargetRepository;
        this.root = new BorderPane();
        this.targets = FXCollections.observableArrayList();
        this.products = FXCollections.observableArrayList();
        this.table = new TableView<>(targets);
        this.summaryLabel = new Label();
        this.statusLabel = new Label();

        root.getStyleClass().add("content-root");

        // --- Toolbar ---
        Label title = new Label("Planeacion de Ventas");
        title.getStyleClass().add("page-title");

        refreshButton = new Button("Actualizar");
        refreshButton.getStyleClass().add("button-ghost");
        refreshButton.setOnAction(event -> loadData());
        
        newTargetButton = new Button("+ Nueva Meta");
        newTargetButton.getStyleClass().add("button-primary");
        newTargetButton.setOnAction(event -> openDrawer(true));

        fixedCostGlobalField.setPromptText("Costo fijo global");
        fixedCostGlobalField.setMaxWidth(140);
        saveFixedCostButton.getStyleClass().add("button-ghost");
        saveFixedCostButton.setOnAction(event -> saveFixedCostGlobal());

        ToolBar toolBar = new ToolBar(title, refreshButton, newTargetButton,
                new Label("Costo fijo global"), fixedCostGlobalField, saveFixedCostButton);
        toolBar.getStyleClass().add("toolbar");
        root.setTop(toolBar);

        // --- Table ---
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().addAll(
                column("Producto", "softwareName"),
                column("Meta (U)", "metaUnits"),
                column("Vendidos", "unitsSoldCurrent"),
                column("Punto equilibrio", "breakEvenUnits"),
                currencyColumn("Margen/U", SalesTarget::marginUnit),
                currencyColumn("Ganancia Est.", SalesTarget::expectedProfit),
                currencyColumn("Costo fijo", t -> t.getFixedCostProduct() != null ? t.getFixedCostProduct() : 0.0),
                column("Rentable", "profitable"),
                dateColumn("Fecha Limite", SalesTarget::deadline)
        );
        table.getSelectionModel().selectedItemProperty().addListener((obs, prev, next) -> {
            if (next != null) selectTarget(next);
        });
        VBox.setVgrow(table, Priority.ALWAYS);
        
        VBox mainContent = new VBox(10, summaryLabel, table);

        // --- Drawer (Right Panel) ---
        drawerTitle = new Label("Detalle de Meta");
        drawerTitle.getStyleClass().add("section-title");
        
        closeDrawerButton = new Button("✕");
        closeDrawerButton.getStyleClass().add("button-ghost");
        closeDrawerButton.setOnAction(e -> closeDrawer());
        
        HBox drawerHeader = new HBox(10, drawerTitle, closeDrawerButton);
        HBox.setHgrow(drawerTitle, Priority.ALWAYS);
        closeDrawerButton.setAlignment(Pos.CENTER_RIGHT);

        productField = new ComboBox<>(products);
        productField.setPromptText("Seleccionar Producto...");
        productField.setMaxWidth(Double.MAX_VALUE);
        productField.setConverter(productConverter());
        productField.valueProperty().addListener((obs, prev, next) -> onProductSelected(next));

        metaUnitsField = new TextField(); metaUnitsField.setPromptText("Unidades");
        deadlineField = new DatePicker(); deadlineField.setPromptText("Fecha limite");
        deadlineField.setMaxWidth(Double.MAX_VALUE);
        
        salePriceField = new TextField(); salePriceField.setPromptText("0.00");
        realCostField = new TextField(); realCostField.setEditable(false); realCostField.setPromptText("Auto");
        variableCostField = new TextField(); variableCostField.setPromptText("0.00");
        fixedCostField = new TextField(); fixedCostField.setPromptText("0.00");
        
        notesField = new TextArea();
        notesField.setPrefRowCount(3);
        notesField.setPromptText("Notas adicionales...");

        // Layout: Sections
        VBox generalSection = new VBox(8, 
            new Label("Datos Generales"), 
            productField, 
            new HBox(8, metaUnitsField, deadlineField)
        );
        
        VBox financialSection = new VBox(8,
            new Label("Financieros"),
            new HBox(8, new Label("Precio Venta:"), salePriceField),
            new HBox(8, new Label("Costo Real (Auto):"), realCostField),
            new HBox(8, new Label("Costo Variable:"), variableCostField),
            new HBox(8, new Label("Costo Fijo:"), fixedCostField)
        );
        
        saveButton = new Button("Guardar Cambios");
        saveButton.getStyleClass().add("button-primary");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setOnAction(event -> saveTarget());

        deleteButton = new Button("Eliminar Meta");
        deleteButton.getStyleClass().add("button-danger");
        deleteButton.setMaxWidth(Double.MAX_VALUE);
        deleteButton.setOnAction(event -> deleteTarget());

        drawerPanel = new VBox(16, 
            drawerHeader,
            generalSection, 
            new javafx.scene.control.Separator(), 
            financialSection,
            new Label("Notas"), notesField,
            new javafx.scene.control.Separator(),
            saveButton, 
            deleteButton
        );
        drawerPanel.setPadding(new Insets(20));
        drawerPanel.getStyleClass().add("drawer-panel");
        drawerPanel.setMinWidth(320);
        drawerPanel.setMaxWidth(360);

        // --- Split Pane ---
        splitPane = new SplitPane();
        splitPane.getItems().addAll(mainContent); // Start with only table
        splitPane.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        
        root.setCenter(splitPane);
        root.setBottom(statusLabel);
        
        summaryLabel.getStyleClass().add("page-subtitle");
        statusLabel.setPadding(new Insets(8));

        loadData();
    }

    public Parent getRoot() { return root; }

    private void openDrawer(boolean createMode) {
        if (!splitPane.getItems().contains(drawerPanel)) {
            splitPane.getItems().add(drawerPanel);
            splitPane.setDividerPositions(0.7);
        }
        
        if (createMode) {
            creating = true;
            selected = null;
            table.getSelectionModel().clearSelection();
            
            // Clear fields
            productField.setValue(null);
            metaUnitsField.clear();
            salePriceField.clear();
            variableCostField.clear();
            fixedCostField.clear();
            deadlineField.setValue(null);
            notesField.clear();
            
            drawerTitle.setText("Nueva Meta");
            saveButton.setText("Crear Meta");
            deleteButton.setVisible(false);
        } else {
            creating = false;
            drawerTitle.setText("Editar Meta");
            saveButton.setText("Guardar Cambios");
            deleteButton.setVisible(true);
        }
    }

    private void closeDrawer() {
        splitPane.getItems().remove(drawerPanel);
        table.getSelectionModel().clearSelection();
        selected = null;
    }

    private void loadData() {
        setLoading(true);
        statusLabel.setText("Cargando metas (Local)...");
        Thread worker = new Thread(() -> {
            try {
                // Try Local First
                List<com.dismal.desktop.local.domain.LocalSalesTarget> localData = localSalesTargetRepository.findAll();
                
                List<SalesTarget> targetList;
                ApiClient.ExpenseSummary expenseSummary = apiClient.getExpenseSummary();
                final double monthExpenses = expenseSummary != null ? expenseSummary.monthExpenses() : 0.0;
                if (!localData.isEmpty()) {
                     usingLocalTargets = true;
                     targetList = localData.stream()
                        .map(t -> new SalesTarget(
                                t.getId(),
                                t.getSoftwareId(),
                                t.getSoftwareName(),
                                t.getMetaUnits(),
                                t.getSalePrice().doubleValue(),
                                t.getVariableCost().doubleValue(),
                                t.getFixedCostProduct() != null ? t.getFixedCostProduct().doubleValue() : null,
                                t.getUnitsSoldCurrent(),
                                t.getNotes(),
                                t.getMarginUnit().doubleValue(),
                                t.getExpectedProfit().doubleValue(),
                                calculateBreakEvenUnits(monthExpenses, t.getMarginUnit() != null ? t.getMarginUnit().doubleValue() : 0.0),
                                t.getDeadline(),
                                t.isProfitable(),
                                false 
                        ))
                        .collect(java.util.stream.Collectors.toList());
                } else {
                    usingLocalTargets = false;
                    targetList = apiClient.listSalesTargets();
                }
                if (monthExpenses > 0) {
                    targetList = applyBreakEvenFallback(targetList, monthExpenses);
                }
                final List<SalesTarget> finalTargets = targetList;

                final List<Product> productList = apiClient.listProducts();
                final List<RealActivationCost> realCostList = apiClient.getRealActivationCosts();
                final ApiClient.SalesTargetSettings settings = apiClient.getSalesTargetSettings();
                
                Platform.runLater(() -> {
                    targets.setAll(finalTargets);
                    products.setAll(productList);
                    realCosts.clear();
                    for (RealActivationCost item : realCostList) {
                        if (item.softwareId() != null && item.averageActivationCost() != null) {
                            realCosts.put(item.softwareId(), item.averageActivationCost());
                        }
                    }
                    if (settings != null && settings.fixedCostGlobal() != null) {
                        fixedCostGlobalField.setText(String.valueOf(settings.fixedCostGlobal()));
                    }
                    if (finalTargets.isEmpty()) statusLabel.setText("Sin metas registradas.");
                    else statusLabel.setText("Metas cargadas: " + finalTargets.size());
                });
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Error: " + ex.getMessage()));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private List<SalesTarget> applyBreakEvenFallback(List<SalesTarget> targetList, double monthExpenses) {
        return targetList.stream()
                .map(target -> {
                    if (target.getBreakEvenUnits() != null) {
                        return target;
                    }
                    Integer calc = calculateBreakEvenUnits(monthExpenses, target.getMarginUnit());
                    return new SalesTarget(
                            target.getId(),
                            target.getSoftwareId(),
                            target.getSoftwareName(),
                            target.getMetaUnits(),
                            target.getSalePrice(),
                            target.getVariableCost(),
                            target.getFixedCostProduct(),
                            target.getUnitsSoldCurrent(),
                            target.getNotes(),
                            target.getMarginUnit(),
                            target.getExpectedProfit(),
                            calc,
                            target.getDeadline(),
                            target.getProfitable(),
                            target.getTargetAchieved()
                    );
                })
                .collect(java.util.stream.Collectors.toList());
    }

    private Integer calculateBreakEvenUnits(double monthExpenses, double marginUnit) {
        if (monthExpenses <= 0 || marginUnit <= 0) {
            return null;
        }
        return (int) Math.ceil(monthExpenses / marginUnit);
    }

    private void selectTarget(SalesTarget target) {
        selected = target;
        openDrawer(false); // Edit Mode
        
        products.stream().filter(p -> p.getId().equals(target.getSoftwareId())).findFirst().ifPresent(productField::setValue);
        metaUnitsField.setText(String.valueOf(target.getMetaUnits()));
        salePriceField.setText(String.valueOf(target.getSalePrice()));
        variableCostField.setText(String.valueOf(target.getVariableCost()));
        fixedCostField.setText(target.getFixedCostProduct() == null ? "" : String.valueOf(target.getFixedCostProduct()));
        updateRealCostField(target.getSoftwareId());
        if (target.getDeadline() != null && !target.getDeadline().isBlank()) {
            deadlineField.setValue(java.time.LocalDate.parse(target.getDeadline()));
        }
        notesField.setText(target.getNotes() == null ? "" : target.getNotes());
    }

    private void saveTarget() {
        Product product = productField.getValue();
        if (product == null) {
            statusLabel.setText("Selecciona un producto.");
            return;
        }
        ApiClient.SalesTargetSaveRequest payload = new ApiClient.SalesTargetSaveRequest(
                product.getId(),
                parseInt(metaUnitsField.getText()),
                parseDouble(salePriceField.getText()),
                parseDouble(variableCostField.getText()),
                parseNullableDouble(fixedCostField.getText()),
                0,
                deadlineField.getValue() != null ? deadlineField.getValue().toString() : null,
                notesField.getText().trim()
        );
        setLoading(true);
        Thread worker = new Thread(() -> {
            try {
                if (usingLocalTargets) {
                    saveLocalTarget(product, payload);
                    trySyncRemoteSave(payload);
                } else if (creating || selected == null) {
                    apiClient.createSalesTarget(payload);
                } else {
                    apiClient.updateSalesTarget(selected.getId(), payload);
                }
                Platform.runLater(() -> {
                    loadData();
                    if (creating) {
                        closeDrawer();
                    } else {
                        statusLabel.setText(usingLocalTargets ? "Guardado localmente (sync remoto aplicado)." : "Guardado exitosamente.");
                    }
                });
            } catch (Exception ex) {
                if (ex instanceof ApiException apiEx) {
                    Platform.runLater(() -> statusLabel.setText("Error al guardar: " + resolveError(apiEx)));
                } else {
                    Platform.runLater(() -> statusLabel.setText("Error al guardar: " + ex.getMessage()));
                }
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void deleteTarget() {
        if (selected == null) return;
        setLoading(true);
        Thread worker = new Thread(() -> {
            try {
                if (usingLocalTargets) {
                    localSalesTargetRepository.deleteById(selected.getId());
                    trySyncRemoteDelete(selected.getId());
                } else {
                    apiClient.deleteSalesTarget(selected.getId());
                }
                Platform.runLater(() -> {
                    closeDrawer();
                    loadData();
                    statusLabel.setText(usingLocalTargets ? "Eliminado localmente (sync remoto aplicado)." : "Eliminado.");
                });
            } catch (Exception ex) {
                if (ex instanceof ApiException apiEx) {
                    Platform.runLater(() -> statusLabel.setText("Error al eliminar: " + resolveError(apiEx)));
                } else {
                    Platform.runLater(() -> statusLabel.setText("Error al eliminar."));
                }
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private int parseInt(String value) { try { return Integer.parseInt(value.trim()); } catch (Exception e) { return 0; } }
    private double parseDouble(String value) { try { return Double.parseDouble(value.trim()); } catch (Exception e) { return 0; } }
    private Double parseNullableDouble(String value) { try { return Double.parseDouble(value.trim()); } catch (Exception e) { return null; } }

    private void saveLocalTarget(Product product, ApiClient.SalesTargetSaveRequest payload) {
        String id = (creating || selected == null || selected.getId() == null)
                ? java.util.UUID.randomUUID().toString()
                : selected.getId();
        com.dismal.desktop.local.domain.LocalSalesTarget local = com.dismal.desktop.local.domain.LocalSalesTarget.builder()
                .id(id)
                .softwareId(product.getId())
                .softwareName(product.getName())
                .metaUnits(payload.metaUnits())
                .unitsSoldCurrent(selected != null ? selected.getUnitsSoldCurrent() : 0)
                .salePrice(java.math.BigDecimal.valueOf(payload.salePrice()))
                .variableCost(java.math.BigDecimal.valueOf(payload.variableCost()))
                .fixedCostProduct(payload.fixedCostProduct() != null ? java.math.BigDecimal.valueOf(payload.fixedCostProduct()) : null)
                .deadline(payload.deadline())
                .notes(payload.notes())
                .profitable(false)
                .marginUnit(java.math.BigDecimal.ZERO)
                .expectedProfit(java.math.BigDecimal.ZERO)
                .build();
        localSalesTargetRepository.save(local);
    }

    private void trySyncRemoteSave(ApiClient.SalesTargetSaveRequest payload) {
        try {
            if (!creating && selected != null && isUuid(selected.getId())) {
                apiClient.updateSalesTarget(selected.getId(), payload);
            } else {
                apiClient.createSalesTarget(payload);
            }
        } catch (Exception ignored) {
        }
    }

    private void trySyncRemoteDelete(String id) {
        if (!isUuid(id)) {
            return;
        }
        try {
            apiClient.deleteSalesTarget(id);
        } catch (Exception ignored) {
        }
    }

    private boolean isUuid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            java.util.UUID.fromString(value);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private String resolveError(ApiException ex) {
        if (ex.getStatus() == 401 || ex.getStatus() == 403) {
            return "Acceso denegado.";
        }
        return ex.getMessage();
    }

    private void saveFixedCostGlobal() {
        String raw = fixedCostGlobalField.getText();
        Double value;
        try {
            value = Double.parseDouble(raw.trim());
        } catch (Exception ex) {
            statusLabel.setText("Costo fijo global inválido.");
            return;
        }
        if (value < 0) {
            statusLabel.setText("Costo fijo global debe ser >= 0.");
            return;
        }
        setLoading(true);
        Thread worker = new Thread(() -> {
            try {
                apiClient.updateSalesTargetSettings(new ApiClient.SalesTargetSettings(value));
                Platform.runLater(() -> statusLabel.setText("Costo fijo global actualizado."));
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("No se pudo actualizar costo global."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void onProductSelected(Product product) {
        if (product == null) {
            realCostField.setText("Sin datos");
            return;
        }
        if (salePriceField.getText().isBlank() || "0".equals(salePriceField.getText())) {
            salePriceField.setText(String.valueOf(product.getPrice()));
        }
        updateRealCostField(product.getId());
    }

    private void updateRealCostField(String softwareId) {
        if (softwareId == null || softwareId.isBlank()) {
            realCostField.setText("Sin datos");
            return;
        }
        Double cost = realCosts.get(softwareId);
        if (cost == null) {
            realCostField.setText("Sin datos");
            return;
        }
        realCostField.setText(FormatUtils.currency(cost));
    }

    private StringConverter<Product> productConverter() {
        return new StringConverter<>() {
            @Override public String toString(Product p) { return p == null ? "" : p.getName(); }
            @Override public Product fromString(String s) { return null; }
        };
    }

    private <T> TableColumn<SalesTarget, T> column(String title, String property) {
        TableColumn<SalesTarget, T> column = new TableColumn<>(title);
        FxTable.bindColumn(column, property);
        return column;
    }

    private TableColumn<SalesTarget, String> currencyColumn(String title, java.util.function.Function<SalesTarget, Number> mapper) {
        TableColumn<SalesTarget, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(FormatUtils.currency(mapper.apply(cell.getValue()))));
        return column;
    }

    private TableColumn<SalesTarget, String> dateColumn(String title, java.util.function.Function<SalesTarget, String> mapper) {
        TableColumn<SalesTarget, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(FormatUtils.date(mapper.apply(cell.getValue()))));
        return column;
    }

    private TableColumn<SalesTarget, String> realCostColumn() {
        TableColumn<SalesTarget, String> column = new TableColumn<>("Costo real");
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                resolveRealCostText(cell.getValue().getSoftwareId())
        ));
        return column;
    }

    private String resolveRealCostText(String softwareId) {
        // Mock or retrieve from map
        return "-"; 
    }

    private void setLoading(boolean loading) {
        refreshButton.setDisable(loading);
        saveButton.setDisable(loading);
        deleteButton.setDisable(loading);
    }
}
