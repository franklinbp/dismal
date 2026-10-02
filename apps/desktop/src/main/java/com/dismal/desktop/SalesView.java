package com.dismal.desktop;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TableCell;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.util.stream.Collectors;
import java.math.BigDecimal;

public class SalesView {

    private final BorderPane root;
    private final ApiClient apiClient;
    private final Runnable onSaleCreated;

    private final ObservableList<SaleRow> sales = FXCollections.observableArrayList();
    private final TableView<SaleRow> salesTable = new TableView<>(sales);
    private final Label statusLabel = new Label();
    private final Button refreshButton = new Button("Actualizar");
    private final Button newSaleButton = new Button("Nueva venta");
    private final Button toggleDetailsButton = new Button("Detalles");
    private final Button confirmButton = new Button("Confirmar venta");
    private final ObservableList<DeliveryLog> deliveryLogs = FXCollections.observableArrayList();
    private final Label deliveryStatusLabel = new Label();
    private final Button retryDeliveryButton = new Button("Reintentar envio");
    private String lastDeliveryOutboxId;
    private final Map<String, String> deliveryStatusBySaleId = new HashMap<>();

    private final ComboBox<String> statusFilterBox = new ComboBox<>();
    private final ComboBox<String> typeFilterBox = new ComboBox<>();
    private final ComboBox<Customer> clientFilterBox = new ComboBox<>();
    private final TextField searchField = new TextField();
    private final DatePicker fromDate = new DatePicker();
    private final DatePicker toDate = new DatePicker();
    private final Button applyFiltersButton = new Button("Filtrar");
    private final Button clearFiltersButton = new Button("Limpiar");
    private final Button prevPageButton = new Button("Anterior");
    private final Button nextPageButton = new Button("Siguiente");
    private final Label pageLabel = new Label();
    private int page = 0;
    private int totalPages = 1;

    private final ObservableList<PaymentItem> payments = FXCollections.observableArrayList();
    private final ObservableList<SaleLicense> saleLicenses = FXCollections.observableArrayList();
    private ArDetail currentAr;

    private final ComboBox<Customer> clientBox = new ComboBox<>();
    private final ComboBox<String> saleTypeBox = new ComboBox<>();
    private final javafx.scene.control.CheckBox emailDeliveryBox = new javafx.scene.control.CheckBox("Correo");
    private final javafx.scene.control.CheckBox whatsappDeliveryBox = new javafx.scene.control.CheckBox("WhatsApp");
    private final VBox itemsBox = new VBox(8);
    private final Label totalLabel = new Label("Total: $0.00");
    private final Button addItemButton = new Button("Agregar item");
    private final Button createButton = new Button("Crear venta");
    private final Button createCustomerButton = new Button("Crear cliente");
    private final Button refreshClientsButton = new Button("Actualizar clientes");

    private final List<Product> products = new ArrayList<>();
    private final List<Customer> customers = new ArrayList<>();
    private final List<ItemRow> itemRows = new ArrayList<>();
    private final Map<String, String> customerNames = new HashMap<>();
    private final Map<String, Long> stockByProductId = new HashMap<>();

    private SaleRow selectedSale;
    private String selectedSaleId;
    private boolean refreshingSales = false;
    private final Label summaryCustomerLabel = new Label("-");
    private final Label summaryTotalLabel = new Label("-");
    private final Label summaryStatusLabel = new Label("-");
    private final Label summaryDateLabel = new Label("-");
    private final Button payButton = new Button("Cobrar");
    private final Label detailStatusBadge = new Label("-");
    private final Label detailTotalLabel = new Label("-");
    private final Label detailPaidLabel = new Label("-");
    private final Label detailBalanceLabel = new Label("-");
    private final Label detailCustomerNameLabel = new Label("-");
    private final Label detailCustomerEmailLabel = new Label("-");
    private final Label detailCustomerPhoneLabel = new Label("-");
    private final Label detailSaleTypeLabel = new Label("-");
    private final Label detailCreatedLabel = new Label("-");
    private final Label detailPaymentMethodLabel = new Label("-");
    private final VBox detailPaymentsBox = new VBox(4);
    private final VBox detailLicensesBox = new VBox(4);
    private final Label flowDraftLabel = new Label("Borrador");
    private final Label flowConfirmedLabel = new Label("Confirmada");
    private final Label flowPaidLabel = new Label("Pagada");
    private final VBox detailItemsBox = new VBox(6);
    private final Label detailDeliveryEmailLabel = new Label("-");
    private final Label detailDeliveryPhoneLabel = new Label("-");
    private final Label detailEmptyHint = new Label("Selecciona una venta para ver el detalle.");
    private final Label detailCreditInfoLabel = new Label("-");
    private final Label detailConfirmHintLabel = new Label("");
    private VBox detailContentBox;
    private VBox createPanel;
    private boolean createInProgress = false;
    private boolean createModeEnabled = false;
    private boolean loading = false;
    private boolean detailsVisible = true;
    private VBox detailsColumn;

    public SalesView(ApiClient apiClient, Runnable onSaleCreated) {
        this.apiClient = apiClient;
        this.onSaleCreated = onSaleCreated != null ? onSaleCreated : () -> {};
        this.root = new BorderPane();
        root.getStyleClass().add("content-root");

        Label title = new Label("Ventas");
        title.getStyleClass().add("page-title");

        refreshButton.getStyleClass().add("button-ghost");
        refreshButton.setOnAction(event -> loadSales());

        newSaleButton.getStyleClass().add("button-primary");
        newSaleButton.setOnAction(event -> openCreateSale());

        toggleDetailsButton.getStyleClass().add("button-ghost");
        toggleDetailsButton.setOnAction(event -> toggleDetails());

        ToolBar toolBar = new ToolBar(title, refreshButton, newSaleButton, toggleDetailsButton);
        toolBar.getStyleClass().add("toolbar");
        root.setTop(toolBar);

        configureSalesTable();
        VBox filters = buildFilters();
        Label listTitle = new Label("Historial de ventas");
        listTitle.getStyleClass().add("section-title");
        Label listHint = new Label("Selecciona una venta para ver detalle y confirmar envio.");
        listHint.getStyleClass().add("help-text");
        VBox historyPanel = new VBox(10, listTitle, listHint, filters, salesTable, buildPager());
        VBox.setVgrow(salesTable, Priority.ALWAYS);

        Parent formPanel = createFormPanel();
        HBox topRow = new HBox(12, formPanel);
        topRow.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(formPanel, Priority.ALWAYS);

        VBox content = new VBox(16, topRow, historyPanel);
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        root.setCenter(scrollPane);

        statusLabel.getStyleClass().add("page-subtitle");
        statusLabel.setPadding(new Insets(8));
        root.setBottom(statusLabel);

        setCreateModeEnabled(false);
        loadAuxData();
        loadSales();
    }

    public Parent getRoot() {
        return root;
    }

    public void startNewSale() {
        openCreateSale();
    }

    public void startNewSaleWithNewCustomer() {
        openCreateSale();
        openQuickCustomerDialog();
    }

    private void configureSalesTable() {
        salesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        salesTable.setPlaceholder(new Label("No hay ventas"));

        TableColumn<SaleRow, String> customerCol = new TableColumn<>("Cliente");
        customerCol.setCellValueFactory(cell -> {
            String name = customerNames.getOrDefault(cell.getValue().clientId(), cell.getValue().clientId());
            return new SimpleStringProperty(name);
        });

        TableColumn<SaleRow, String> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(cell ->
                new SimpleStringProperty(FormatUtils.currency(cell.getValue().total())));

        TableColumn<SaleRow, String> statusCol = new TableColumn<>("Estado");
        statusCol.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().status()));

        TableColumn<SaleRow, String> typeCol = new TableColumn<>("Tipo");
        typeCol.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().saleType()));

        TableColumn<SaleRow, String> dateCol = new TableColumn<>("Fecha");
        dateCol.setCellValueFactory(cell -> {
            if (cell.getValue().createdAt() == null) {
                return new SimpleStringProperty("-");
            }
            String value = cell.getValue().createdAt().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm"));
            return new SimpleStringProperty(value);
        });

        salesTable.getColumns().setAll(customerCol, totalCol, statusCol, typeCol, dateCol);
        salesTable.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> {
            if (refreshingSales && value == null) {
                return;
            }
            selectedSale = value;
            selectedSaleId = value != null ? value.id() : selectedSaleId;
            updateSaleSummary();
            updateConfirmState();
            loadDeliveryLogs();
            loadSaleDetails();
        });
    }

    private Parent createFormPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(12));
        panel.getStyleClass().add("panel");
        createPanel = panel;

        Label formTitle = new Label("Crear venta");
        formTitle.getStyleClass().add("section-title");

        clientBox.setPromptText("Selecciona cliente");
        clientBox.setMaxWidth(Double.MAX_VALUE);
        clientBox.valueProperty().addListener((obs, old, value) -> {
            applyClientCreditRules(value);
            updateCreateState();
        });
        clientBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Customer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.firstname() + " " + item.lastname() + " · " + item.email());
                }
            }
        });
        clientBox.setButtonCell(clientBox.getCellFactory().call(null));

        saleTypeBox.setItems(FXCollections.observableArrayList("CASH", "CREDIT"));
        saleTypeBox.getSelectionModel().select("CASH");
        saleTypeBox.setMaxWidth(Double.MAX_VALUE);
        saleTypeBox.valueProperty().addListener((obs, old, value) -> updateCreateState());

        emailDeliveryBox.setSelected(true);
        whatsappDeliveryBox.setSelected(true);

        itemsBox.getChildren().clear();
        addItemRow();

        addItemButton.getStyleClass().add("button-ghost");
        addItemButton.setOnAction(event -> addItemRow());

        totalLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        createCustomerButton.getStyleClass().add("button-primary");
        createCustomerButton.setOnAction(event -> openQuickCustomerDialog());

        refreshClientsButton.getStyleClass().add("button-ghost");
        refreshClientsButton.setOnAction(event -> loadCustomersOnly());

        createButton.getStyleClass().add("button-primary");
        createButton.setMaxWidth(Double.MAX_VALUE);
        createButton.setOnAction(event -> createSale());
        updateCreateState();

        confirmButton.getStyleClass().add("button-primary");
        confirmButton.setOnAction(event -> confirmSelectedSale());
        updateConfirmState();

        TableView<DeliveryLog> deliveryTable = new TableView<>(deliveryLogs);
        deliveryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<DeliveryLog, String> errorColumn = columnDelivery("Error", "error");
        errorColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    Tooltip tip = new Tooltip(item);
                    setTooltip(tip);
                }
            }
        });
        deliveryTable.getColumns().addAll(
                columnDelivery("Canal", "channel"),
                columnDelivery("Estado", "status"),
                errorColumn
        );
        deliveryTable.setPlaceholder(new Label("Sin envios registrados"));
        deliveryStatusLabel.getStyleClass().add("page-subtitle");
        retryDeliveryButton.getStyleClass().add("button-ghost");
        retryDeliveryButton.setOnAction(event -> retryDelivery());

        Label deliveryHint = new Label("Selecciona los canales para enviar licencias.");
        deliveryHint.getStyleClass().add("help-text");
        Label step1 = new Label("Paso 1 · Cliente");
        step1.getStyleClass().add("help-text");
        Label step2 = new Label("Paso 2 · Tipo y entrega");
        step2.getStyleClass().add("help-text");
        Label step3 = new Label("Paso 3 · Items");
        step3.getStyleClass().add("help-text");
        Label step4 = new Label("Paso 4 · Confirmar");
        step4.getStyleClass().add("help-text");

        HBox actionRow = new HBox(10, createButton);
        HBox.setHgrow(createButton, Priority.ALWAYS);
        createButton.setMaxWidth(Double.MAX_VALUE);
        confirmButton.setMaxWidth(Double.MAX_VALUE);

        panel.getChildren().addAll(
                formTitle,
                step1,
                new Label("Cliente"),
                new HBox(8, clientBox, createCustomerButton, refreshClientsButton),
                step2,
                new Label("Tipo"), saleTypeBox,
                new Label("Entrega de licencia"),
                deliveryHint,
                new HBox(12, emailDeliveryBox, whatsappDeliveryBox),
                step3,
                new Label("Items"), itemsBox,
                addItemButton,
                new Separator(),
                totalLabel,
                actionRow,
                step4,
                new Separator()
        );
        VBox detailsPanel = createSaleDetailsPanel();
        detailsPanel.getStyleClass().add("drawer-panel");
        detailsPanel.setMinWidth(260);
        detailsPanel.setMaxWidth(320);
        detailsColumn = new VBox(10, detailsPanel);
        HBox container = new HBox(10, panel, detailsColumn);
        HBox.setHgrow(panel, Priority.ALWAYS);
        updateFormDisabledState();
        updateDetailsVisibility();
        return container;
    }

    private void addItemRow() {
        ItemRow row = new ItemRow();
        itemRows.add(row);
        itemsBox.getChildren().add(row.container);
        updateTotal();
        updateCreateState();
    }

    private void openCreateSale() {
        loadCustomersOnly();
        setCreateModeEnabled(true);
        selectedSale = null;
        selectedSaleId = null;
        salesTable.getSelectionModel().clearSelection();
        clientBox.setValue(null);
        saleTypeBox.getSelectionModel().select("CASH");
        emailDeliveryBox.setSelected(true);
        whatsappDeliveryBox.setSelected(true);
        itemsBox.getChildren().clear();
        itemRows.clear();
        addItemRow();
        updateConfirmState();
        updateSaleSummary();
        statusLabel.setText("Completa el formulario para crear la venta.");
        if (createPanel != null) {
            createPanel.setStyle("-fx-border-color: #2563eb; -fx-border-width: 2; -fx-border-radius: 8;");
            PauseTransition pause = new PauseTransition(Duration.seconds(1.2));
            pause.setOnFinished(e -> createPanel.setStyle(""));
            pause.play();
        }
        Platform.runLater(clientBox::requestFocus);
    }

    private void removeItemRow(ItemRow row) {
        itemRows.remove(row);
        itemsBox.getChildren().remove(row.container);
        updateTotal();
        updateCreateState();
    }

    private void updateTotal() {
        double sum = 0.0;
        for (ItemRow row : itemRows) {
            sum += row.getSubtotal();
        }
        totalLabel.setText("Total: " + FormatUtils.currency(sum));
        updateCreateState();
    }

    private void loadAuxData() {
        setLoading(true);
        statusLabel.setText("Cargando clientes y productos...");
        Thread worker = new Thread(() -> {
            try {
                List<Customer> fetchedCustomers = apiClient.listCustomers();
                List<Product> fetchedProducts = apiClient.listProducts();
                StockSummary[] summary = apiClient.getInventorySummary();
                Platform.runLater(() -> {
                    customers.clear();
                    customers.addAll(fetchedCustomers);
                    customerNames.clear();
                    for (Customer customer : fetchedCustomers) {
                        String name = (customer.firstname() + " " + customer.lastname()).trim();
                        customerNames.put(customer.id(), name.isBlank() ? customer.email() : name);
                    }
                    products.clear();
                    products.addAll(fetchedProducts);
                    stockByProductId.clear();
                    if (summary != null) {
                        for (StockSummary row : summary) {
                            stockByProductId.put(row.softwareId(), row.available());
                        }
                    }
                    clientBox.setItems(FXCollections.observableArrayList(customers));
                    clientFilterBox.setItems(FXCollections.observableArrayList(customers));
                    itemRows.forEach(ItemRow::reloadProducts);
                    itemRows.forEach(ItemRow::updateStockLabel);
                    salesTable.refresh();
                    statusLabel.setText("Listo para crear ventas.");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Error al cargar datos."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void loadCustomersOnly() {
        loadCustomersOnly(null);
    }

    private void loadCustomersOnly(Runnable onLoaded) {
        setLoading(true);
        statusLabel.setText("Actualizando clientes...");
        Thread worker = new Thread(() -> {
            try {
                List<Customer> fetchedCustomers = apiClient.listCustomers();
                Platform.runLater(() -> {
                    customers.clear();
                    customers.addAll(fetchedCustomers);
                    customerNames.clear();
                    for (Customer customer : fetchedCustomers) {
                        String name = (customer.firstname() + " " + customer.lastname()).trim();
                        customerNames.put(customer.id(), name.isBlank() ? customer.email() : name);
                    }
                    clientBox.setItems(FXCollections.observableArrayList(customers));
                    clientFilterBox.setItems(FXCollections.observableArrayList(customers));
                    applyClientCreditRules(clientBox.getValue());
                    salesTable.refresh();
                    updateSaleSummary();
                    updateConfirmHint();
                    updateConfirmState();
                    statusLabel.setText("Clientes actualizados.");
                    if (onLoaded != null) {
                        onLoaded.run();
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Error al cargar clientes."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void loadSales() {
        setLoading(true);
        statusLabel.setText("Cargando ventas...");
        Thread worker = new Thread(() -> {
            try {
                String clientId = clientFilterBox.getValue() != null ? clientFilterBox.getValue().id() : null;
                String status = statusFilterBox.getValue();
                String type = typeFilterBox.getValue();
                LocalDate from = fromDate.getValue();
                LocalDate to = toDate.getValue();
                SalesPage pageData = apiClient.listSalesFiltered(clientId, status, type, from, to, page, 25);
                List<SaleRow> filtered = applySearch(pageData.content(), searchField.getText());
                Platform.runLater(() -> {
                    refreshingSales = true;
                    sales.setAll(filtered);
                    totalPages = Math.max(pageData.totalPages(), 1);
                    updatePager();
                    if (selectedSaleId != null) {
                        SaleRow match = filtered.stream()
                                .filter(row -> selectedSaleId.equals(row.id()))
                                .findFirst()
                                .orElse(null);
                        if (match != null) {
                            selectedSale = match;
                            salesTable.getSelectionModel().select(match);
                        } else {
                            salesTable.getSelectionModel().clearSelection();
                            selectedSale = null;
                        }
                    } else {
                        salesTable.getSelectionModel().clearSelection();
                        selectedSale = null;
                    }
                    statusLabel.setText("Total ventas: " + pageData.totalElements());
                    refreshDeliveryStatuses();
                    updateSaleSummary();
                    updateConfirmState();
                    refreshingSales = false;
                });
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Error al cargar ventas."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void createSale() {
        if (createInProgress) {
            return;
        }
        createInProgress = true;
        createButton.setDisable(true);
        Customer client = clientBox.getValue();
        if (client == null) {
            statusLabel.setText("Selecciona un cliente.");
            createInProgress = false;
            updateCreateState();
            return;
        }
        String saleType = saleTypeBox.getValue();
        if (saleType == null || saleType.isBlank()) {
            statusLabel.setText("Selecciona un tipo de venta.");
            createInProgress = false;
            updateCreateState();
            return;
        }
        if (itemRows.isEmpty()) {
            statusLabel.setText("Agrega al menos un item.");
            createInProgress = false;
            updateCreateState();
            return;
        }

        List<ApiClient.SaleItemPayload> items = new ArrayList<>();
        for (ItemRow row : itemRows) {
            Product product = row.productBox.getValue();
            if (product == null) {
                statusLabel.setText("Selecciona un producto en cada item.");
                createInProgress = false;
                updateCreateState();
                return;
            }
            Integer qty = row.parseQuantity();
            Double price = row.parsePrice();
            if (qty == null || qty <= 0 || price == null || price <= 0) {
                statusLabel.setText("Cantidad y precio deben ser validos.");
                createInProgress = false;
                updateCreateState();
                return;
            }
            items.add(new ApiClient.SaleItemPayload(product.getId(), qty, price));
        }

        setLoading(true);
        statusLabel.setText("Creando venta...");
        Thread worker = new Thread(() -> {
            try {
                SaleRow created = apiClient.createSale(client.id(), saleType, items);
                Platform.runLater(() -> {
                    loadSales();
                    selectedSale = created;
                    selectedSaleId = created.id();
                    salesTable.getSelectionModel().select(created);
                    updateSaleSummary();
                    updateConfirmState();
                    loadSaleDetails();
                    loadDeliveryLogs();
                    setCreateModeEnabled(false);
                    statusLabel.setText("Venta creada: " + created.id());
                    onSaleCreated.run();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Error al crear venta: " + ex.getMessage()));
            } finally {
                Platform.runLater(() -> {
                    createInProgress = false;
                    setLoading(false);
                    updateCreateState();
                });
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void openQuickCustomerDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Crear cliente");
        dialog.setHeaderText("Crea el cliente y continua directo con la venta.");

        ButtonType saveButtonType = new ButtonType("Crear cliente", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField firstnameField = new TextField();
        firstnameField.setPromptText("Nombre");
        TextField lastnameField = new TextField();
        lastnameField.setPromptText("Apellido");
        TextField emailField = new TextField();
        emailField.setPromptText("correo@cliente.com");
        TextField phoneField = new TextField();
        phoneField.setPromptText("+593...");
        TextField taxIdField = new TextField();
        taxIdField.setPromptText("Cedula / RUC");
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 0, 0, 0));
        grid.add(new Label("Nombre"), 0, 0);
        grid.add(firstnameField, 1, 0);
        grid.add(new Label("Apellido"), 0, 1);
        grid.add(lastnameField, 1, 1);
        grid.add(new Label("Email"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label("Telefono"), 0, 3);
        grid.add(phoneField, 1, 3);
        grid.add(new Label("Documento"), 0, 4);
        grid.add(taxIdField, 1, 4);
        dialog.getDialogPane().setContent(grid);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        Runnable validate = () -> saveButton.setDisable(
                firstnameField.getText().isBlank()
                        || lastnameField.getText().isBlank()
                        || emailField.getText().isBlank()
        );
        firstnameField.textProperty().addListener((obs, old, value) -> validate.run());
        lastnameField.textProperty().addListener((obs, old, value) -> validate.run());
        emailField.textProperty().addListener((obs, old, value) -> validate.run());
        validate.run();

        Platform.runLater(firstnameField::requestFocus);
        dialog.showAndWait().ifPresent(result -> {
            if (result != saveButtonType) {
                return;
            }

            AdminUser payload = new AdminUser(
                    null,
                    firstnameField.getText().trim(),
                    lastnameField.getText().trim(),
                    emailField.getText().trim(),
                    phoneField.getText().trim(),
                    "CUSTOMER",
                    true,
                    blankToNull(taxIdField.getText()),
                    emailField.getText().trim(),
                    false,
                    0.0,
                    0.0,
                    0,
                    null
            );
            createQuickCustomer(payload);
        });
    }

    private void createQuickCustomer(AdminUser payload) {
        setLoading(true);
        statusLabel.setText("Creando cliente...");
        Thread worker = new Thread(() -> {
            try {
                AdminUser created = apiClient.createUser(payload, null);
                Platform.runLater(() -> {
                    loadCustomersOnly(() -> selectCreatedCustomer(created));
                    statusLabel.setText("Cliente creado. Continua con la venta.");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setLoading(false);
                    statusLabel.setText("Error al crear cliente: " + ex.getMessage());
                });
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void selectCreatedCustomer(AdminUser created) {
        Customer selected = customers.stream()
                .filter(customer -> created.id() != null && created.id().equals(customer.id()))
                .findFirst()
                .orElseGet(() -> new Customer(
                        created.id(),
                        created.firstname(),
                        created.lastname(),
                        created.phone(),
                        created.email(),
                        created.hasCredit(),
                        created.creditLimit(),
                        created.creditDays()
                ));
        if (created.id() != null && customers.stream().noneMatch(customer -> created.id().equals(customer.id()))) {
            customers.add(selected);
            clientBox.setItems(FXCollections.observableArrayList(customers));
            clientFilterBox.setItems(FXCollections.observableArrayList(customers));
        }
        clientBox.setValue(selected);
        applyClientCreditRules(selected);
        updateCreateState();
        Platform.runLater(() -> {
            if (!itemRows.isEmpty()) {
                itemRows.get(0).productBox.requestFocus();
            } else {
                clientBox.requestFocus();
            }
        });
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private void confirmSelectedSale() {
        if (selectedSale == null) {
            statusLabel.setText("Selecciona una venta para confirmar.");
            return;
        }
        String status = selectedSale.status() == null ? "" : selectedSale.status().toUpperCase();
        if ("CONFIRMED".equals(status) || "CANCELLED".equals(status) || "PAID".equals(status)) {
            statusLabel.setText("La venta ya esta confirmada.");
            return;
        }
        if (!isReadyToConfirm(selectedSale)) {
            String hint = detailConfirmHintLabel.getText();
            statusLabel.setText(hint == null || hint.isBlank() ? "No se puede confirmar la venta." : hint);
            return;
        }
        if (!emailDeliveryBox.isSelected() && !whatsappDeliveryBox.isSelected()) {
            statusLabel.setText("Selecciona al menos un canal de envio.");
            return;
        }
        setLoading(true);
        statusLabel.setText("Confirmando venta...");
        Thread worker = new Thread(() -> {
            try {
                List<String> channels = new ArrayList<>();
                if (emailDeliveryBox.isSelected()) {
                    channels.add("EMAIL");
                }
                if (whatsappDeliveryBox.isSelected()) {
                    channels.add("WHATSAPP");
                }
                SaleRow confirmed = apiClient.confirmSale(selectedSale.id(), channels);
                Platform.runLater(() -> {
                    loadSales();
                    loadDeliveryLogs();
                    selectedSale = confirmed;
                    salesTable.getSelectionModel().select(confirmed);
                    updateSaleSummary();
                    statusLabel.setText("Venta confirmada. Envio en proceso.");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Error al confirmar: " + ex.getMessage()));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void updateConfirmState() {
        ensureSelectedSaleReference();
        if (loading || selectedSale == null) {
            confirmButton.setDisable(true);
            return;
        }
        String status = selectedSale.status() == null ? "" : selectedSale.status().toUpperCase();
        boolean alreadyConfirmed = "CONFIRMED".equals(status) || "CANCELLED".equals(status) || "PAID".equals(status);
        boolean canConfirm = !alreadyConfirmed && isReadyToConfirm(selectedSale);
        confirmButton.setDisable(!canConfirm);
    }

    private void updateCreateState() {
        if (!createModeEnabled) {
            createButton.setDisable(true);
            return;
        }
        if (loading || createInProgress) {
            createButton.setDisable(true);
            return;
        }
        boolean valid = isFormValid();
        createButton.setDisable(!valid);
    }

    private boolean isFormValid() {
        if (clientBox.getValue() == null) {
            return false;
        }
        String saleType = saleTypeBox.getValue();
        if (saleType == null || saleType.isBlank()) {
            return false;
        }
        if (itemRows.isEmpty()) {
            return false;
        }
        for (ItemRow row : itemRows) {
            Product product = row.productBox.getValue();
            Integer qty = row.parseQuantity();
            Double price = row.parsePrice();
            if (product == null || qty == null || qty <= 0 || price == null || price <= 0) {
                return false;
            }
        }
        return true;
    }

    private VBox buildFilters() {
        statusFilterBox.getItems().setAll("", "DRAFT", "CONFIRMED", "PAID", "CANCELLED");
        statusFilterBox.setPromptText("Estado");
        typeFilterBox.getItems().setAll("", "CASH", "CREDIT");
        typeFilterBox.setPromptText("Tipo");
        clientFilterBox.setPromptText("Cliente");
        clientFilterBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Customer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.firstname() + " " + item.lastname());
                }
            }
        });
        clientFilterBox.setButtonCell(clientFilterBox.getCellFactory().call(null));

        searchField.setPromptText("Buscar por cliente o ID");

        applyFiltersButton.getStyleClass().add("button-ghost");
        applyFiltersButton.setOnAction(event -> {
            page = 0;
            loadSales();
        });
        clearFiltersButton.getStyleClass().add("button-ghost");
        clearFiltersButton.setOnAction(event -> {
            statusFilterBox.setValue("");
            typeFilterBox.setValue("");
            clientFilterBox.setValue(null);
            searchField.clear();
            fromDate.setValue(null);
            toDate.setValue(null);
            page = 0;
            loadSales();
        });

        HBox row1 = new HBox(8, statusFilterBox, typeFilterBox, clientFilterBox);
        HBox row2 = new HBox(8, new Label("Desde"), fromDate, new Label("Hasta"), toDate);
        HBox row3 = new HBox(8, searchField, applyFiltersButton, clearFiltersButton);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        VBox box = new VBox(8, row1, row2, row3);
        box.getStyleClass().add("panel-soft");
        return box;
    }

    private HBox buildPager() {
        prevPageButton.getStyleClass().add("button-ghost");
        nextPageButton.getStyleClass().add("button-ghost");
        prevPageButton.setOnAction(event -> {
            if (page > 0) {
                page -= 1;
                loadSales();
            }
        });
        nextPageButton.setOnAction(event -> {
            if (page + 1 < totalPages) {
                page += 1;
                loadSales();
            }
        });
        pageLabel.getStyleClass().add("page-subtitle");
        updatePager();
        HBox pager = new HBox(8, prevPageButton, nextPageButton, pageLabel);
        pager.setAlignment(Pos.CENTER_LEFT);
        return pager;
    }

    private void updatePager() {
        prevPageButton.setDisable(page <= 0);
        nextPageButton.setDisable(page + 1 >= totalPages);
        pageLabel.setText("Pagina " + (page + 1) + " de " + totalPages);
    }

    private List<SaleRow> applySearch(List<SaleRow> rows, String term) {
        if (term == null || term.isBlank()) {
            return rows;
        }
        String query = term.trim().toLowerCase();
        return rows.stream()
                .filter(row -> {
                    String customerName = customerNames.getOrDefault(row.clientId(), row.clientId());
                    return row.id().toLowerCase().contains(query)
                            || customerName.toLowerCase().contains(query);
                })
                .collect(Collectors.toList());
    }

    private VBox createSaleDetailsPanel() {
        detailEmptyHint.getStyleClass().add("help-text");

        detailStatusBadge.getStyleClass().addAll("badge", "badge-neutral");

        HBox headerRow = new HBox(8);
        Label detailTitle = new Label("DETALLE DE LA VENTA");
        detailTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");
        HBox.setHgrow(detailTitle, Priority.ALWAYS);
        headerRow.getChildren().addAll(detailTitle, detailStatusBadge);

        HBox actionsRow = new HBox(8);
        Button refreshDetail = new Button("Actualizar");
        refreshDetail.getStyleClass().add("button-ghost");
        refreshDetail.setOnAction(event -> loadSales());
        Button modifyButton = new Button("Modificar");
        modifyButton.getStyleClass().add("button-ghost");
        modifyButton.setDisable(true);
        Button deleteButton = new Button("Eliminar");
        deleteButton.getStyleClass().add("button-danger");
        deleteButton.setDisable(true);
        actionsRow.getChildren().addAll(refreshDetail, modifyButton, deleteButton);

        HBox flowRow = new HBox(8,
                flowDraftLabel,
                new Label("→"),
                flowConfirmedLabel,
                new Label("→"),
                flowPaidLabel
        );
        flowDraftLabel.getStyleClass().add("badge");
        flowConfirmedLabel.getStyleClass().add("badge");
        flowPaidLabel.getStyleClass().add("badge");

        VBox totalBox = metricBox("TOTAL", detailTotalLabel);
        VBox paidBox = metricBox("PAGADO", detailPaidLabel);
        VBox balanceBox = metricBox("SALDO", detailBalanceLabel);
        HBox metricsRow = new HBox(6, totalBox, paidBox, balanceBox);
        HBox.setHgrow(totalBox, Priority.ALWAYS);
        HBox.setHgrow(paidBox, Priority.ALWAYS);
        HBox.setHgrow(balanceBox, Priority.ALWAYS);

        VBox customerBox = new VBox(3,
                new Label("CLIENTE"),
                detailCustomerNameLabel,
                detailCustomerEmailLabel,
                detailCustomerPhoneLabel
        );
        customerBox.getStyleClass().add("panel-soft");
        customerBox.setPadding(new Insets(6));

        VBox metaBox = new VBox(3,
                new Label("TIPO"), detailSaleTypeLabel,
                new Label("CREADA"), detailCreatedLabel,
                new Label("PAGO"), detailPaymentMethodLabel
        );
        metaBox.getStyleClass().add("panel-soft");
        metaBox.setPadding(new Insets(6));

        HBox customerRow = new HBox(6, customerBox, metaBox);
        HBox.setHgrow(customerBox, Priority.ALWAYS);
        HBox.setHgrow(metaBox, Priority.ALWAYS);

        Label itemsTitle = new Label("Items");
        itemsTitle.getStyleClass().add("section-title");
        detailItemsBox.getStyleClass().add("panel-soft");
        detailItemsBox.setPadding(new Insets(6));

        Label deliveryTitle = new Label("Envio");
        deliveryTitle.getStyleClass().add("section-title");
        VBox deliveryBox = new VBox(3,
                new Label("EMAIL"), detailDeliveryEmailLabel,
                new Label("WHATSAPP"), detailDeliveryPhoneLabel
        );
        deliveryBox.getStyleClass().add("panel-soft");
        deliveryBox.setPadding(new Insets(6));

        Label paymentsTitle = new Label("Pagos");
        paymentsTitle.getStyleClass().add("section-title");
        detailPaymentsBox.getStyleClass().add("panel-soft");
        detailPaymentsBox.setPadding(new Insets(6));
        Label licensesTitle = new Label("Licencias asignadas");
        licensesTitle.getStyleClass().add("section-title");
        detailLicensesBox.getStyleClass().add("panel-soft");
        detailLicensesBox.setPadding(new Insets(6));
        payButton.getStyleClass().add("button-primary");
        payButton.setOnAction(event -> openPaymentDialog());
        payButton.setMaxWidth(Double.MAX_VALUE);

        confirmButton.setMaxWidth(Double.MAX_VALUE);

        detailContentBox = new VBox(6,
                actionsRow,
                flowRow,
                metricsRow,
                customerRow,
                itemsTitle,
                detailItemsBox,
                deliveryTitle,
                deliveryBox,
                paymentsTitle,
                detailPaymentsBox,
                licensesTitle,
                detailLicensesBox,
                payButton,
                detailCreditInfoLabel,
                detailConfirmHintLabel,
                confirmButton
        );

        VBox box = new VBox(6,
                headerRow,
                detailEmptyHint,
                detailContentBox
        );
        box.getStyleClass().add("panel-soft");
        return box;
    }

    private void loadDeliveryLogs() {
        if (selectedSale == null) {
            deliveryLogs.clear();
            deliveryStatusLabel.setText("");
            updateDeliveryStatusStyle(null);
            lastDeliveryOutboxId = null;
            retryDeliveryButton.setDisable(true);
            return;
        }
        setLoading(true);
        deliveryStatusLabel.setText("Cargando envios...");
        Thread worker = new Thread(() -> {
            try {
                OutboxPage page = apiClient.listOutboxForSale(selectedSale.id());
                OutboxItem outbox = page.content().stream()
                        .filter(item -> item.createdAt() != null)
                        .max((a, b) -> a.createdAt().compareTo(b.createdAt()))
                        .orElse(page.content().isEmpty() ? null : page.content().get(0));
                if (outbox == null) {
                    Platform.runLater(() -> {
                        deliveryLogs.clear();
                        deliveryStatusLabel.setText("Sin envios para esta venta.");
                        updateDeliveryStatusStyle(null);
                        lastDeliveryOutboxId = null;
                        retryDeliveryButton.setDisable(true);
                    });
                    return;
                }
                List<DeliveryLog> logs = apiClient.listDeliveryLogs(outbox.id());
                Platform.runLater(() -> {
                    deliveryLogs.setAll(logs);
                    deliveryStatusLabel.setText("Outbox: " + outbox.status());
                    updateDeliveryStatusStyle(outbox.status());
                    lastDeliveryOutboxId = outbox.id();
                    retryDeliveryButton.setDisable(false);
                    deliveryStatusBySaleId.put(selectedSale.id(), normalizeDeliveryStatus(outbox.status()));
                    salesTable.refresh();
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> {
                    deliveryStatusLabel.setText(resolveError(ex));
                    updateDeliveryStatusStyle("FAILED");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    deliveryStatusLabel.setText("No se pudo cargar envios.");
                    updateDeliveryStatusStyle("FAILED");
                });
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void loadSaleDetails() {
        payments.clear();
        saleLicenses.clear();
        currentAr = null;
        if (selectedSale == null) {
            return;
        }
        SaleRow saleSnapshot = selectedSale;
        Thread worker = new Thread(() -> {
            try {
                List<PaymentItem> paymentData = apiClient.listPayments(saleSnapshot.id());
                List<SaleLicense> licenseData = apiClient.listSaleLicenses(saleSnapshot.id());
                ArDetail arDetail = null;
                if ("CREDIT".equalsIgnoreCase(saleSnapshot.saleType())) {
                    try {
                        arDetail = apiClient.getArBySale(saleSnapshot.id());
                    } catch (ApiException apiEx) {
                        if (apiEx.getStatus() != 404) {
                            throw apiEx;
                        }
                    }
                }
                final ArDetail arSnapshot = arDetail;
                Platform.runLater(() -> {
                    payments.setAll(paymentData);
                    saleLicenses.setAll(licenseData);
                    currentAr = arSnapshot;
                    updateDetailItems();
                    updatePaymentInfo();
                    updateLicenseInfo();
                    updateConfirmHint();
                    updateConfirmState();
                    updateCreditInfo();
                });
            } catch (Exception ignored) {
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void updateSaleSummary() {
        ensureSelectedSaleReference();
        if (selectedSale == null) {
            summaryCustomerLabel.setText("-");
            summaryTotalLabel.setText("-");
            summaryStatusLabel.setText("-");
            summaryDateLabel.setText("-");
            payButton.setDisable(true);
            detailStatusBadge.setText("-");
            updateDetailStatusBadge(null);
            detailTotalLabel.setText("-");
            detailPaidLabel.setText("-");
            detailBalanceLabel.setText("-");
            detailCustomerNameLabel.setText("-");
            detailCustomerEmailLabel.setText("-");
            detailCustomerPhoneLabel.setText("-");
            detailSaleTypeLabel.setText("-");
            detailCreatedLabel.setText("-");
            detailPaymentMethodLabel.setText("-");
            detailPaymentsBox.getChildren().setAll(new Label("-"));
            detailLicensesBox.getChildren().setAll(new Label("-"));
            detailDeliveryEmailLabel.setText("-");
            detailDeliveryPhoneLabel.setText("-");
            detailCreditInfoLabel.setText("-");
            currentAr = null;
            detailConfirmHintLabel.setText("");
            updateFlowBadges(null);
            detailItemsBox.getChildren().setAll(new Label("Sin items"));
            updateDetailVisibility(false);
            return;
        }
        String customerName = customerNames.getOrDefault(selectedSale.clientId(), selectedSale.clientId());
        summaryCustomerLabel.setText(customerName);
        summaryTotalLabel.setText(FormatUtils.currency(selectedSale.total()));
        summaryStatusLabel.setText(selectedSale.status());
        if (selectedSale.createdAt() != null) {
            summaryDateLabel.setText(selectedSale.createdAt().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")));
        } else {
            summaryDateLabel.setText("-");
        }
        BigDecimal balance = getSaleBalance(selectedSale);
        boolean canPay = balance != null && balance.doubleValue() > 0.0;
        payButton.setDisable(loading || !canPay);

        detailStatusBadge.setText(selectedSale.status());
        updateDetailStatusBadge(selectedSale.status());
        detailTotalLabel.setText(FormatUtils.currency(selectedSale.total()));
        detailPaidLabel.setText(FormatUtils.currency(selectedSale.paid()));
        detailBalanceLabel.setText(FormatUtils.currency(balance));
        detailCustomerNameLabel.setText(customerName);
        Customer customer = customers.stream()
                .filter(c -> c.id().equals(selectedSale.clientId()))
                .findFirst()
                .orElse(null);
        if (customer != null) {
            detailCustomerEmailLabel.setText(customer.email() != null ? customer.email() : "-");
            detailDeliveryEmailLabel.setText(customer.email() != null ? customer.email() : "-");
            detailCustomerPhoneLabel.setText(customer.phone() != null ? customer.phone() : "-");
            detailDeliveryPhoneLabel.setText(customer.phone() != null ? customer.phone() : "-");
        } else {
            detailCustomerEmailLabel.setText("-");
            detailCustomerPhoneLabel.setText("-");
            detailDeliveryEmailLabel.setText("-");
            detailDeliveryPhoneLabel.setText("-");
        }
        detailSaleTypeLabel.setText(selectedSale.saleType());
        if (selectedSale.createdAt() != null) {
            detailCreatedLabel.setText(selectedSale.createdAt().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")));
        } else {
            detailCreatedLabel.setText("-");
        }
        updatePaymentInfo();
        updateFlowBadges(selectedSale.status());
        updateDetailItems();
        updateCreditInfo();
        updateConfirmHint();
        updateDetailVisibility(true);
    }

    private Label createStatusBadge(String status) {
        Label badge = new Label(status == null ? "-" : status.toUpperCase());
        badge.getStyleClass().add("badge");
        if (status == null) {
            badge.getStyleClass().add("badge-neutral");
            return badge;
        }
        switch (status.toUpperCase()) {
            case "PAID" -> badge.getStyleClass().add("badge-success");
            case "CONFIRMED" -> badge.getStyleClass().add("badge-success");
            case "DRAFT" -> badge.getStyleClass().add("badge-warn");
            case "CANCELLED" -> badge.getStyleClass().add("badge-error");
            default -> badge.getStyleClass().add("badge-neutral");
        }
        return badge;
    }

    private void openPaymentDialog() {
        if (selectedSale == null) {
            statusLabel.setText("Selecciona una venta para cobrar.");
            return;
        }
        BigDecimal balance = getSaleBalance(selectedSale);
        if (balance == null || balance.doubleValue() <= 0.0) {
            statusLabel.setText("La venta no tiene saldo pendiente.");
            return;
        }

        Dialog<PaymentInput> dialog = new Dialog<>();
        dialog.setTitle("Registrar pago");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField amountField = new TextField(balance.toPlainString());
        TextField referenceField = new TextField();
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("help-text");
        errorLabel.setStyle("-fx-text-fill: #b00020;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.addRow(0, new Label("Monto"), amountField);
        grid.addRow(1, new Label("Referencia"), referenceField);
        grid.add(errorLabel, 1, 2);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return new PaymentInput(amountField.getText(), referenceField.getText());
            }
            return null;
        });

        var owner = payButton.getScene() != null ? payButton.getScene().getWindow() : null;
        if (owner != null) {
            dialog.initOwner(owner);
        }
        var okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String raw = amountField.getText();
            BigDecimal parsed = parseAmount(raw);
            if (parsed == null || parsed.signum() <= 0) {
                errorLabel.setText("Monto invalido. Ej: 1200.50 o 1.200,50");
                event.consume();
                return;
            }
            errorLabel.setText("");
        });

        dialog.showAndWait().ifPresent(this::registerPayment);
    }

    private void registerPayment(PaymentInput input) {
        if (selectedSale == null) return;
        BigDecimal amount = parseAmount(input.amount());
        if (amount == null || amount.signum() <= 0) {
            statusLabel.setText("Monto debe ser mayor a cero.");
            return;
        }
        setLoading(true);
        statusLabel.setText("Iniciando cobro...");
        Thread worker = new Thread(() -> {
            try {
                Platform.runLater(() -> statusLabel.setText("Procesando cobro..."));
                String saleType = selectedSale.saleType() == null ? "" : selectedSale.saleType().toUpperCase();
                if ("DRAFT".equalsIgnoreCase(selectedSale.status())) {
                    Platform.runLater(() -> statusLabel.setText("Confirmando venta..."));
                    List<String> channels = resolveDeliveryChannels();
                    try {
                        SaleRow confirmed = apiClient.confirmSale(selectedSale.id(), channels);
                        if (confirmed == null
                                || (!"CONFIRMED".equalsIgnoreCase(confirmed.status())
                                && !"PAID".equalsIgnoreCase(confirmed.status()))) {
                            Platform.runLater(() -> statusLabel.setText("No se pudo confirmar la venta."));
                            return;
                        }
                        selectedSale = confirmed;
                    } catch (ApiException apiEx) {
                        Platform.runLater(() -> statusLabel.setText("No se pudo confirmar: " + resolveError(apiEx)));
                        return;
                    }
                    if ("CREDIT".equals(saleType)) {
                        Platform.runLater(() -> {
                            loadSales();
                            loadSaleDetails();
                            statusLabel.setText("Venta confirmada. Pendiente de cobro.");
                        });
                        return;
                    }
                }
                Platform.runLater(() -> statusLabel.setText("Registrando pago..."));
                SaleRow updated = apiClient.addPayment(selectedSale.id(), amount.doubleValue(), "CASH", input.reference());
                Platform.runLater(() -> {
                    selectedSale = updated;
                    loadSales();
                    loadSaleDetails();
                    String paymentStatus = updated != null && updated.status() != null
                            ? updated.status().toUpperCase()
                            : "";
                    statusLabel.setText("PAID".equals(paymentStatus)
                            ? "Pago registrado. Venta pagada."
                            : "Pago registrado.");
                });
            } catch (Exception ex) {
                if (ex instanceof ApiException apiEx) {
                    Platform.runLater(() -> statusLabel.setText("Error al registrar pago: " + resolveError(apiEx)));
                } else {
                    Platform.runLater(() -> statusLabel.setText("Error al registrar pago: " + ex.getMessage()));
                }
            } catch (Throwable t) {
                Platform.runLater(() -> statusLabel.setText("Fallo inesperado: " + t.getClass().getSimpleName()));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private record PaymentInput(String amount, String reference) {}

    private List<String> resolveDeliveryChannels() {
        List<String> channels = new ArrayList<>();
        if (emailDeliveryBox.isSelected()) {
            channels.add("EMAIL");
        }
        if (whatsappDeliveryBox.isSelected()) {
            channels.add("WHATSAPP");
        }
        return channels.isEmpty() ? null : channels;
    }

    private BigDecimal parseAmount(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.trim();
        if (cleaned.isBlank()) {
            return null;
        }
        // Keep digits and separators only (allow both 1,200.50 and 1.200,50).
        cleaned = cleaned.replaceAll("[^0-9,\\.]", "");
        if (cleaned.isBlank()) {
            return null;
        }
        int lastComma = cleaned.lastIndexOf(',');
        int lastDot = cleaned.lastIndexOf('.');
        if (lastComma >= 0 && lastDot >= 0) {
            if (lastComma > lastDot) {
                cleaned = cleaned.replace(".", "");
                cleaned = cleaned.replace(',', '.');
            } else {
                cleaned = cleaned.replace(",", "");
            }
        } else if (lastComma >= 0) {
            cleaned = cleaned.replace(",", ".");
        }
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void retryDelivery() {
        if (lastDeliveryOutboxId == null) {
            deliveryStatusLabel.setText("No hay envio para reintentar.");
            return;
        }
        setLoading(true);
        deliveryStatusLabel.setText("Reintentando envio...");
        Thread worker = new Thread(() -> {
            try {
                apiClient.retryOutbox(lastDeliveryOutboxId);
                Platform.runLater(() -> {
                    deliveryStatusLabel.setText("Reintento enviado. Espera confirmacion.");
                    updateDeliveryStatusStyle("PENDING");
                    loadDeliveryLogs();
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> deliveryStatusLabel.setText(resolveError(ex)));
            } catch (Exception ex) {
                Platform.runLater(() -> deliveryStatusLabel.setText("No se pudo reintentar envio."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private VBox createDeliveryPanel(TableView<DeliveryLog> deliveryTable) {
        Label title = new Label("Envio de licencias");
        title.getStyleClass().add("section-title");
        Label hint = new Label("Muestra el ultimo envio y permite reintentar si falla.");
        hint.getStyleClass().add("help-text");
        VBox container = new VBox(8, title, hint, deliveryTable, retryDeliveryButton, deliveryStatusLabel);
        container.getStyleClass().add("panel-soft");
        return container;
    }

    private Label createDeliveryBadge(String status) {
        Label badge = new Label(status);
        badge.getStyleClass().add("badge");
        String normalized = status == null ? "" : status.toLowerCase();
        if (normalized.contains("enviado")) {
            badge.setText("[OK] " + status);
            badge.getStyleClass().add("badge-success");
        } else if (normalized.contains("pendiente")) {
            badge.setText("[..] " + status);
            badge.getStyleClass().add("badge-warn");
        } else if (normalized.contains("error")) {
            badge.setText("[X] " + status);
            badge.getStyleClass().add("badge-error");
        } else {
            badge.setText("[i] " + status);
            badge.getStyleClass().add("badge-neutral");
        }
        return badge;
    }

    private void updateDeliveryStatusStyle(String raw) {
        deliveryStatusLabel.getStyleClass().removeAll("status-success", "status-warn", "status-error");
        String normalized = raw == null ? "" : raw.toUpperCase();
        if ("SENT".equals(normalized)) {
            deliveryStatusLabel.getStyleClass().add("status-success");
        } else if ("FAILED".equals(normalized)) {
            deliveryStatusLabel.getStyleClass().add("status-error");
        } else if ("PENDING".equals(normalized)) {
            deliveryStatusLabel.getStyleClass().add("status-warn");
        }
    }

    private VBox metricBox(String title, Label valueLabel) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #64748b; -fx-font-weight: 700;");
        valueLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        VBox box = new VBox(3, titleLabel, valueLabel);
        box.getStyleClass().add("panel-soft");
        box.setPadding(new Insets(6));
        return box;
    }

    private void updateFlowBadges(String status) {
        flowDraftLabel.getStyleClass().removeAll("badge-success", "badge-warn", "badge-neutral");
        flowConfirmedLabel.getStyleClass().removeAll("badge-success", "badge-warn", "badge-neutral");
        flowPaidLabel.getStyleClass().removeAll("badge-success", "badge-warn", "badge-neutral");
        String normalized = status == null ? "" : status.toUpperCase();
        if ("DRAFT".equals(normalized)) {
            flowDraftLabel.getStyleClass().add("badge-success");
            flowConfirmedLabel.getStyleClass().add("badge-neutral");
            flowPaidLabel.getStyleClass().add("badge-neutral");
        } else if ("CONFIRMED".equals(normalized)) {
            flowDraftLabel.getStyleClass().add("badge-neutral");
            flowConfirmedLabel.getStyleClass().add("badge-success");
            flowPaidLabel.getStyleClass().add("badge-neutral");
        } else if ("PAID".equals(normalized)) {
            flowDraftLabel.getStyleClass().add("badge-neutral");
            flowConfirmedLabel.getStyleClass().add("badge-neutral");
            flowPaidLabel.getStyleClass().add("badge-success");
        } else {
            flowDraftLabel.getStyleClass().add("badge-neutral");
            flowConfirmedLabel.getStyleClass().add("badge-neutral");
            flowPaidLabel.getStyleClass().add("badge-neutral");
        }
    }

    private void updateDetailStatusBadge(String status) {
        detailStatusBadge.getStyleClass().removeAll("badge-success", "badge-warn", "badge-error", "badge-neutral");
        if (status == null || status.isBlank()) {
            detailStatusBadge.getStyleClass().add("badge-neutral");
            return;
        }
        switch (status.toUpperCase()) {
            case "PAID" -> detailStatusBadge.getStyleClass().add("badge-success");
            case "CONFIRMED" -> detailStatusBadge.getStyleClass().add("badge-success");
            case "DRAFT" -> detailStatusBadge.getStyleClass().add("badge-warn");
            case "CANCELLED" -> detailStatusBadge.getStyleClass().add("badge-error");
            default -> detailStatusBadge.getStyleClass().add("badge-neutral");
        }
    }

    private void updateDetailVisibility(boolean hasSelection) {
        if (detailContentBox == null) {
            return;
        }
        detailContentBox.setVisible(hasSelection);
        detailContentBox.setManaged(hasSelection);
        detailEmptyHint.setVisible(!hasSelection);
        detailEmptyHint.setManaged(!hasSelection);
    }

    private void updateDetailItems() {
        detailItemsBox.getChildren().clear();
        if (selectedSale == null || selectedSale.items() == null || selectedSale.items().isEmpty()) {
            detailItemsBox.getChildren().add(new Label("Sin items"));
            return;
        }
        for (SaleItemRow item : selectedSale.items()) {
            Label name = new Label(item.softwareName());
            name.getStyleClass().add("section-title");
            Label meta = new Label(item.quantity() + " x " + FormatUtils.currency(item.unitPrice()));
            meta.getStyleClass().add("help-text");
            Label subtotal = new Label(FormatUtils.currency(item.subtotal()));
            subtotal.getStyleClass().add("section-title");
            VBox left = new VBox(2, name, meta);
            HBox row = new HBox(8, left, subtotal);
            HBox.setHgrow(left, Priority.ALWAYS);
            row.setAlignment(Pos.CENTER_LEFT);
            detailItemsBox.getChildren().add(row);
        }
    }

    private void updatePaymentInfo() {
        if (selectedSale == null) {
            detailPaymentMethodLabel.setText("-");
            return;
        }
        if (payments.isEmpty()) {
            detailPaymentMethodLabel.setText("-");
            detailPaymentsBox.getChildren().setAll(new Label("-"));
            return;
        }
        detailPaymentsBox.getChildren().clear();
        PaymentItem latest = payments.stream()
                .filter(p -> p.createdAt() != null)
                .max((a, b) -> a.createdAt().compareTo(b.createdAt()))
                .orElse(payments.get(0));
        String method = paymentMethodLabel(latest.method());
        String reference = latest.reference();
        detailPaymentMethodLabel.setText(reference != null && !reference.isBlank() ? method + " · " + reference : method);

        for (PaymentItem payment : payments) {
            String itemMethod = paymentMethodLabel(payment.method());
            String itemRef = payment.reference();
            String amount = FormatUtils.currency(payment.amount());
            String line = itemRef != null && !itemRef.isBlank()
                    ? itemMethod + " · " + itemRef + " · " + amount
                    : itemMethod + " · " + amount;
            Label row = new Label(line);
            row.getStyleClass().add("help-text");
            detailPaymentsBox.getChildren().add(row);
        }
    }

    private void updateLicenseInfo() {
        detailLicensesBox.getChildren().clear();
        if (saleLicenses.isEmpty()) {
            detailLicensesBox.getChildren().add(new Label("Sin licencias asignadas"));
            return;
        }
        for (SaleLicense license : saleLicenses) {
            String licenseKey = license.licenseKey() != null ? license.licenseKey() : "-";
            String assignedAt = license.assignedAt() != null
                    ? license.assignedAt().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm"))
                    : "";
            Label row = new Label("Serial · " + licenseKey + (assignedAt.isBlank() ? "" : " · " + assignedAt));
            row.getStyleClass().add("help-text");
            detailLicensesBox.getChildren().add(row);
        }
    }

    private String paymentMethodLabel(String method) {
        if ("CASH".equals(method)) return "Contado";
        if ("CREDIT".equals(method) || "CARD".equals(method)) return "Credito";
        if ("TRANSFER".equals(method)) return "Transferencia";
        return method == null || method.isBlank() ? "-" : method;
    }

    private void refreshDeliveryStatuses() {
        if (sales.isEmpty()) {
            return;
        }
        Thread worker = new Thread(() -> {
            for (SaleRow sale : sales) {
                try {
                    OutboxPage page = apiClient.listOutboxForSale(sale.id());
                    OutboxItem outbox = page.content().stream()
                            .filter(item -> item.createdAt() != null)
                            .max((a, b) -> a.createdAt().compareTo(b.createdAt()))
                            .orElse(page.content().isEmpty() ? null : page.content().get(0));
                    String status = outbox != null ? normalizeDeliveryStatus(outbox.status()) : "-";
                    deliveryStatusBySaleId.put(sale.id(), status);
                } catch (Exception ignored) {
                    deliveryStatusBySaleId.put(sale.id(), "-");
                }
            }
            Platform.runLater(salesTable::refresh);
        });
        worker.setDaemon(true);
        worker.start();
    }

    private String normalizeDeliveryStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return "-";
        }
        return switch (raw.toUpperCase()) {
            case "SENT" -> "Enviado";
            case "FAILED" -> "Error";
            case "PENDING" -> "Pendiente";
            default -> raw;
        };
    }

    private <T> TableColumn<DeliveryLog, T> columnDelivery(String title, String property) {
        TableColumn<DeliveryLog, T> column = new TableColumn<>(title);
        FxTable.bindColumn(column, property);
        return column;
    }

    private <T> TableColumn<PaymentItem, T> columnPayment(String title, String property) {
        TableColumn<PaymentItem, T> column = new TableColumn<>(title);
        FxTable.bindColumn(column, property);
        return column;
    }

    private <T> TableColumn<SaleLicense, T> columnLicense(String title, String property) {
        TableColumn<SaleLicense, T> column = new TableColumn<>(title);
        FxTable.bindColumn(column, property);
        return column;
    }

    private void setLoading(boolean loading) {
        this.loading = loading;
        refreshButton.setDisable(loading);
        newSaleButton.setDisable(loading);
        toggleDetailsButton.setDisable(loading);
        createCustomerButton.setDisable(loading);
        salesTable.setDisable(loading);
        retryDeliveryButton.setDisable(loading || lastDeliveryOutboxId == null);
        applyFiltersButton.setDisable(loading);
        clearFiltersButton.setDisable(loading);
        prevPageButton.setDisable(loading || page <= 0);
        nextPageButton.setDisable(loading || page + 1 >= totalPages);
        updateFormDisabledState();
        updateCreateState();
        updateConfirmState();
        updateSaleSummary();
    }

    private BigDecimal getSaleBalance(SaleRow sale) {
        if (sale == null) {
            return null;
        }
        if (sale.balance() != null) {
            return sale.balance();
        }
        BigDecimal total = sale.total() != null ? sale.total() : BigDecimal.ZERO;
        BigDecimal paid = sale.paid() != null ? sale.paid() : BigDecimal.ZERO;
        BigDecimal balance = total.subtract(paid);
        return balance.signum() < 0 ? BigDecimal.ZERO : balance;
    }

    private String resolveError(ApiException ex) {
        if (ex.getStatus() == 401 || ex.getStatus() == 403) {
            return "Acceso denegado.";
        }
        return ex.getMessage();
    }

    private class ItemRow {
        private final HBox container;
        private final ComboBox<Product> productBox;
        private final TextField qtyField;
        private final TextField priceField;
        private final Label subtotalLabel;
        private final Label stockLabel;
        private final Button removeButton;

        ItemRow() {
            productBox = new ComboBox<>();
            productBox.setPromptText("Producto");
            productBox.setMaxWidth(Double.MAX_VALUE);
            productBox.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(Product item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getName());
                    }
                }
            });
            productBox.setButtonCell(productBox.getCellFactory().call(null));
            productBox.valueProperty().addListener((obs, old, value) -> updateCreateState());
            reloadProducts();

            qtyField = new TextField();
            qtyField.setPromptText("Cant.");
            qtyField.setPrefWidth(70);
            qtyField.textProperty().addListener((obs, old, val) -> updateCreateState());

            priceField = new TextField();
            priceField.setPromptText("Precio");
            priceField.setPrefWidth(90);
            priceField.textProperty().addListener((obs, old, val) -> updateCreateState());

            subtotalLabel = new Label("$0.00");
            subtotalLabel.setStyle("-fx-text-fill: #94a3b8;");

            stockLabel = new Label("Stock: -");
            stockLabel.setStyle("-fx-text-fill: #64748b;");

            removeButton = new Button("-");
            removeButton.getStyleClass().add("button-danger");
            removeButton.setOnAction(event -> removeItemRow(this));

            productBox.setOnAction(event -> {
                Product selected = productBox.getValue();
                if (selected != null && (priceField.getText() == null || priceField.getText().isBlank())) {
                    priceField.setText(String.valueOf(selected.getPrice()));
                }
                updateStockLabel();
                updateSubtotal();
            });

            qtyField.textProperty().addListener((obs, old, val) -> updateSubtotal());
            priceField.textProperty().addListener((obs, old, val) -> updateSubtotal());

            container = new HBox(8, productBox, qtyField, priceField, stockLabel, subtotalLabel, removeButton);
            container.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(productBox, Priority.ALWAYS);
        }

        void reloadProducts() {
            productBox.setItems(FXCollections.observableArrayList(products));
        }

        void updateStockLabel() {
            Product selected = productBox.getValue();
            if (selected == null) {
                stockLabel.setText("Stock: -");
                return;
            }
            long available = stockByProductId.getOrDefault(selected.getId(), 0L);
            stockLabel.setText("Stock: " + available);
        }

        void updateSubtotal() {
            double subtotal = getSubtotal();
            subtotalLabel.setText(FormatUtils.currency(subtotal));
            updateTotal();
        }

        double getSubtotal() {
            Integer qty = parseQuantity();
            Double price = parsePrice();
            if (qty == null || price == null) {
                return 0.0;
            }
            return qty * price;
        }

        Integer parseQuantity() {
            try {
                return Integer.parseInt(qtyField.getText().trim());
            } catch (Exception ex) {
                return null;
            }
        }

        Double parsePrice() {
            try {
                return Double.parseDouble(priceField.getText().trim());
            } catch (Exception ex) {
                return null;
            }
        }

        void setDisabled(boolean disabled) {
            productBox.setDisable(disabled);
            qtyField.setDisable(disabled);
            priceField.setDisable(disabled);
            removeButton.setDisable(disabled);
        }
    }

    private void setCreateModeEnabled(boolean enabled) {
        createModeEnabled = enabled;
        updateFormDisabledState();
        updateCreateState();
    }

    private void updateFormDisabledState() {
        boolean disabled = loading || !createModeEnabled;
        clientBox.setDisable(disabled);
        saleTypeBox.setDisable(disabled);
        emailDeliveryBox.setDisable(disabled);
        whatsappDeliveryBox.setDisable(disabled);
        addItemButton.setDisable(disabled);
        itemsBox.setDisable(disabled);
        itemRows.forEach(row -> row.setDisabled(disabled));
        createButton.setDisable(disabled || createInProgress);
    }

    private boolean isCashSalePendingPayment(SaleRow sale) {
        if (sale == null) {
            return false;
        }
        String saleType = sale.saleType() == null ? "" : sale.saleType().toUpperCase();
        if (!"CASH".equals(saleType)) {
            return false;
        }
        BigDecimal balance = sale.balance();
        return balance != null && balance.doubleValue() > 0.0;
    }

    private boolean isReadyToConfirm(SaleRow sale) {
        if (!hasStockForSale(sale)) {
            return false;
        }
        String saleType = sale.saleType() == null ? "" : sale.saleType().toUpperCase();
        if ("CASH".equals(saleType)) {
            BigDecimal balance = sale.balance();
            if (balance == null) {
                String status = sale.status() == null ? "" : sale.status().toUpperCase();
                return "PAID".equals(status);
            }
            return balance.doubleValue() <= 0.0;
        }
        if ("CREDIT".equals(saleType)) {
            Customer customer = findCustomerForSale(sale);
            if (customer == null || !customer.hasCredit()) {
                return false;
            }
            double total = sale.total() != null ? sale.total().doubleValue() : 0.0;
            if (customer.creditLimit() > 0 && total > customer.creditLimit()) {
                return false;
            }
            return true;
        }
        return true;
    }

    private void ensureSelectedSaleReference() {
        if (selectedSale != null || selectedSaleId == null) {
            return;
        }
        for (SaleRow row : sales) {
            if (selectedSaleId.equals(row.id())) {
                selectedSale = row;
                break;
            }
        }
    }

    private void toggleDetails() {
        detailsVisible = !detailsVisible;
        updateDetailsVisibility();
    }

    private void updateDetailsVisibility() {
        if (detailsColumn == null) {
            return;
        }
        detailsColumn.setVisible(detailsVisible);
        detailsColumn.setManaged(detailsVisible);
        toggleDetailsButton.setText(detailsVisible ? "Ocultar detalles" : "Mostrar detalles");
    }

    private boolean hasStockForSale(SaleRow sale) {
        if (sale == null || sale.items() == null) {
            return true;
        }
        for (SaleItemRow item : sale.items()) {
            long available = stockByProductId.getOrDefault(item.softwareId(), 0L);
            if (available < item.quantity()) {
                return false;
            }
        }
        return true;
    }

    private Customer findCustomerForSale(SaleRow sale) {
        if (sale == null) {
            return null;
        }
        return customers.stream()
                .filter(c -> c.id().equals(sale.clientId()))
                .findFirst()
                .orElse(null);
    }

    private void updateCreditInfo() {
        detailCreditInfoLabel.getStyleClass().clear();
        detailCreditInfoLabel.getStyleClass().add("panel-soft");
        detailCreditInfoLabel.setPadding(new Insets(6));
        if (selectedSale == null) {
            detailCreditInfoLabel.setText("-");
            return;
        }
        String saleType = selectedSale.saleType() == null ? "" : selectedSale.saleType().toUpperCase();
        if (!"CREDIT".equals(saleType)) {
            detailCreditInfoLabel.setText("Tipo de cuenta: Contado");
            return;
        }
        Customer customer = findCustomerForSale(selectedSale);
        if (customer == null) {
            detailCreditInfoLabel.setText("Tipo de cuenta: Crédito (cliente no encontrado)");
            return;
        }
        String status = customer.hasCredit() ? "Crédito habilitado" : "Crédito no habilitado";
        String limit = customer.creditLimit() > 0 ? FormatUtils.currency(customer.creditLimit()) : "Sin límite";
        String days = customer.creditDays() != null ? customer.creditDays().toString() : "-";
        StringBuilder text = new StringBuilder();
        text.append(status).append(" · Límite: ").append(limit).append(" · Días: ").append(days);
        if (currentAr != null) {
            String arStatus = currentAr.status() != null ? currentAr.status() : "-";
            String arBalance = currentAr.balance() != null ? FormatUtils.currency(currentAr.balance()) : "-";
            String arDue = currentAr.dueDate() != null ? FormatUtils.date(currentAr.dueDate().toString()) : "-";
            text.append("\nAR: ").append(arStatus).append(" · Saldo: ").append(arBalance).append(" · Vence: ").append(arDue);
        } else {
            text.append("\nAR: no generada");
        }
        detailCreditInfoLabel.setText(text.toString());
    }

    private void updateConfirmHint() {
        detailConfirmHintLabel.getStyleClass().clear();
        detailConfirmHintLabel.getStyleClass().add("panel-soft");
        detailConfirmHintLabel.setPadding(new Insets(6));
        if (selectedSale == null) {
            detailConfirmHintLabel.setText("");
            return;
        }
        String status = selectedSale.status() == null ? "" : selectedSale.status().toUpperCase();
        if ("CONFIRMED".equals(status) || "PAID".equals(status)) {
            detailConfirmHintLabel.setText("Venta confirmada. Licencias asignadas.");
            detailConfirmHintLabel.getStyleClass().add("status-success");
            return;
        }
        if ("CANCELLED".equals(status)) {
            detailConfirmHintLabel.setText("Venta cancelada.");
            detailConfirmHintLabel.getStyleClass().add("status-error");
            return;
        }
        if (!hasStockForSale(selectedSale)) {
            detailConfirmHintLabel.setText("No hay activaciones suficientes para confirmar.");
            detailConfirmHintLabel.getStyleClass().add("status-error");
            return;
        }
        String saleType = selectedSale.saleType() == null ? "" : selectedSale.saleType().toUpperCase();
        if ("CASH".equals(saleType)) {
            if (isCashSalePendingPayment(selectedSale)) {
                detailConfirmHintLabel.setText("Primero debes registrar el pago para confirmar.");
                detailConfirmHintLabel.getStyleClass().add("status-warn");
            } else {
                detailConfirmHintLabel.setText("Pago recibido. Puedes confirmar la venta.");
                detailConfirmHintLabel.getStyleClass().add("status-success");
            }
            return;
        }
        if ("CREDIT".equals(saleType)) {
            Customer customer = findCustomerForSale(selectedSale);
            if (customer == null || !customer.hasCredit()) {
                detailConfirmHintLabel.setText("El cliente no tiene crédito habilitado.");
                detailConfirmHintLabel.getStyleClass().add("status-error");
                return;
            }
            double total = selectedSale.total() != null ? selectedSale.total().doubleValue() : 0.0;
            if (customer.creditLimit() > 0 && total > customer.creditLimit()) {
                detailConfirmHintLabel.setText("Límite de crédito insuficiente para confirmar.");
                detailConfirmHintLabel.getStyleClass().add("status-error");
                return;
            }
            detailConfirmHintLabel.setText("Crédito disponible. Puedes confirmar la venta.");
            detailConfirmHintLabel.getStyleClass().add("status-success");
            return;
        }
        detailConfirmHintLabel.setText("");
    }

    private void applyClientCreditRules(Customer customer) {
        if (customer == null) {
            saleTypeBox.setItems(FXCollections.observableArrayList("CASH", "CREDIT"));
            saleTypeBox.setDisable(false);
            if (saleTypeBox.getValue() == null || saleTypeBox.getValue().isBlank()) {
                saleTypeBox.getSelectionModel().select("CASH");
            }
            return;
        }
        if (customer.hasCredit()) {
            saleTypeBox.setItems(FXCollections.observableArrayList("CASH", "CREDIT"));
            saleTypeBox.setDisable(false);
            saleTypeBox.getSelectionModel().select("CREDIT");
        } else {
            saleTypeBox.setItems(FXCollections.observableArrayList("CASH"));
            saleTypeBox.getSelectionModel().select("CASH");
            saleTypeBox.setDisable(true);
        }
    }
}
