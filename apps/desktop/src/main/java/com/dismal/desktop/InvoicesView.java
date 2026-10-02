package com.dismal.desktop;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InvoicesView {

    private final BorderPane root;
    private final ApiClient apiClient;
    private final Runnable onCreateSale;
    private final ObservableList<InvoiceSummary> invoices;
    private final ListView<String> listView;
    private final Label statusLabel;
    
    private final Button refreshButton;
    private final Button voidButton;
    private Button createButton; // Initialized in createCreationPanel
    
    private InvoiceDetail selected;

    // Filters
    private final TextField searchField;
    private final DatePicker fromDate;
    private final DatePicker toDate;

    // Creation / Detail Fields
    private javafx.scene.control.ComboBox<SaleRow> saleBox;
    private DatePicker dueDateField;
    private Label saleMetaLabel;
    private TextField paymentAmountField;
    private ComboBox<String> paymentMethodBox;
    private TextField paymentReferenceField;
    private Button confirmButton;
    private Button payButton;
    
    // UI Panels
    private final VBox detailPanel;
    private final VBox createPanel;
    private ObservableList<SaleRow> eligibleSales = FXCollections.observableArrayList();
    private final Map<String, String> customerNames = new HashMap<>();

    public InvoicesView(ApiClient apiClient, Runnable onCreateSale) {
        this.apiClient = apiClient;
        this.onCreateSale = onCreateSale != null ? onCreateSale : () -> {};
        this.root = new BorderPane();
        this.invoices = FXCollections.observableArrayList();
        this.listView = new ListView<>();
        this.statusLabel = new Label();

        root.getStyleClass().add("content-root");

        // --- Toolbar ---
        Label title = new Label("Facturación");
        title.getStyleClass().add("page-title");

        refreshButton = new Button("Actualizar");
        refreshButton.getStyleClass().add("button-ghost");
        refreshButton.setOnAction(event -> loadInvoices());

        Button newInvoiceBtn = new Button("+ Nueva Factura");
        newInvoiceBtn.getStyleClass().add("button-primary");
        newInvoiceBtn.setOnAction(event -> showCreateForm());

        ToolBar toolBar = new ToolBar(title, refreshButton, newInvoiceBtn);
        toolBar.getStyleClass().add("toolbar");
        root.setTop(toolBar);

        // --- Left: List & Filters ---
        searchField = new TextField();
        searchField.setPromptText("Buscar factura/cliente...");
        searchField.setMaxWidth(Double.MAX_VALUE);
        
        fromDate = new DatePicker(); fromDate.setPromptText("Desde"); fromDate.setMaxWidth(110);
        toDate = new DatePicker(); toDate.setPromptText("Hasta"); toDate.setMaxWidth(110);
        
        Button applyFilters = new Button("filtrar");
        applyFilters.getStyleClass().add("button-ghost");
        applyFilters.setOnAction(event -> loadInvoices());

        HBox filters = new HBox(8, searchField, fromDate, toDate, applyFilters);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        listView.setPlaceholder(new Label("No hay facturas para mostrar"));
        listView.getSelectionModel().selectedIndexProperty().addListener((obs, prev, idx) -> {
            int index = idx == null ? -1 : idx.intValue();
            if (index >= 0 && index < invoices.size()) {
                selectInvoice(invoices.get(index));
            }
        });
        VBox.setVgrow(listView, Priority.ALWAYS);

        VBox listContainer = new VBox(10, filters, listView);
        listContainer.setPadding(new Insets(0, 10, 0, 0));

        // --- Right: Panels ---
        voidButton = new Button("Anular Factura");
        voidButton.getStyleClass().add("button-danger");
        voidButton.setMaxWidth(Double.MAX_VALUE);
        voidButton.setOnAction(event -> voidInvoice());
        voidButton.setDisable(true);

        detailPanel = new VBox(16);
        detailPanel.setPadding(new Insets(16));
        detailPanel.getStyleClass().add("panel");
        
        createPanel = createCreationPanel();

        // --- Split Pane ---
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(listContainer, detailPanel);
        splitPane.setDividerPositions(0.4);
        splitPane.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

        root.setCenter(splitPane);
        
        statusLabel.getStyleClass().add("page-subtitle");
        statusLabel.setPadding(new Insets(8));
        root.setBottom(statusLabel);

        showDetailPanel(); // Initialize with detail view empty state
        loadInvoices();
    }

    private VBox createCreationPanel() {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(16));
        panel.getStyleClass().add("panel");

        Label title = new Label("Generar Nueva Factura");
        title.getStyleClass().add("section-title");

        saleBox = new javafx.scene.control.ComboBox<>(eligibleSales);
        saleBox.setPromptText("Selecciona venta");
        saleBox.setMaxWidth(Double.MAX_VALUE);
        saleBox.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(SaleRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String customerName = customerNames.getOrDefault(item.clientId(), item.clientId());
                    setText(customerName + " · " + FormatUtils.currency(item.total()) + " · " + item.status());
                }
            }
        });
        saleBox.setButtonCell(saleBox.getCellFactory().call(null));
        saleBox.setOnAction(event -> updateSaleMeta());

        saleMetaLabel = new Label("Selecciona una venta para ver detalles.");
        saleMetaLabel.setStyle("-fx-text-fill: #94a3b8;");

        Button createSaleButton = new Button("Crear venta");
        createSaleButton.getStyleClass().add("button-ghost");
        createSaleButton.setMaxWidth(Double.MAX_VALUE);
        createSaleButton.setOnAction(event -> onCreateSale.run());

        paymentAmountField = new TextField();
        paymentAmountField.setPromptText("Monto a pagar");

        paymentMethodBox = new ComboBox<>(FXCollections.observableArrayList("CASH", "CREDIT", "TRANSFER"));
        paymentMethodBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(String value) {
                return paymentMethodLabel(value);
            }

            @Override
            public String fromString(String value) {
                if ("Contado".equalsIgnoreCase(value)) return "CASH";
                if ("Credito".equalsIgnoreCase(value)) return "CREDIT";
                if ("Transferencia".equalsIgnoreCase(value)) return "TRANSFER";
                return value;
            }
        });
        paymentMethodBox.setPromptText("Metodo de pago");
        paymentMethodBox.setMaxWidth(Double.MAX_VALUE);
        paymentMethodBox.getSelectionModel().select("CASH");

        paymentReferenceField = new TextField();
        paymentReferenceField.setPromptText("Referencia (opcional)");

        dueDateField = new DatePicker(); 
        dueDateField.setPromptText("Fecha de Vencimiento");
        dueDateField.setMaxWidth(Double.MAX_VALUE);

        confirmButton = new Button("Confirmar Venta");
        confirmButton.getStyleClass().add("button-ghost");
        confirmButton.setMaxWidth(Double.MAX_VALUE);
        confirmButton.setOnAction(event -> confirmSelectedSale());

        payButton = new Button("Registrar Pago");
        payButton.getStyleClass().add("button-primary");
        payButton.setMaxWidth(Double.MAX_VALUE);
        payButton.setOnAction(event -> registerPayment());

        createButton = new Button("Generar Factura");
        createButton.getStyleClass().add("button-primary");
        createButton.setMaxWidth(Double.MAX_VALUE);
        createButton.setOnAction(event -> createInvoice());

        Button cancelBtn = new Button("Cancelar");
        cancelBtn.getStyleClass().add("button-ghost");
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setOnAction(event -> showDetailPanel());

        panel.getChildren().addAll(
            title, 
            new Label("Venta"), saleBox,
            saleMetaLabel,
            createSaleButton,
            new Label("Confirmacion"),
            confirmButton,
            new Label("Pago"),
            new Label("Monto"), paymentAmountField,
            new Label("Metodo"), paymentMethodBox,
            new Label("Referencia"), paymentReferenceField,
            new Label("Vencimiento"), dueDateField,
            new javafx.scene.control.Separator(),
            createButton, cancelBtn
        );
        return panel;
    }

    private void showCreateForm() {
        SplitPane split = (SplitPane) root.getCenter();
        split.getItems().set(1, createPanel);
        saleBox.getSelectionModel().clearSelection();
        saleMetaLabel.setText("Selecciona una venta para ver detalles.");
        paymentAmountField.clear();
        paymentMethodBox.getSelectionModel().select("CASH");
        paymentReferenceField.clear();
        dueDateField.setValue(java.time.LocalDate.now().plusDays(30));
        statusLabel.setText("Modo: Creación de Factura");
        loadEligibleSales();
    }

    private void showDetailPanel() {
        SplitPane split = (SplitPane) root.getCenter();
        if (split.getItems().size() > 1) {
            split.getItems().set(1, detailPanel);
        }
        if (selected == null) {
            detailPanel.getChildren().setAll(new Label("Detalle de Factura"), new Label("Selecciona una factura de la lista."));
        }
        statusLabel.setText("");
    }

    public Parent getRoot() {
        return root;
    }

    private void loadInvoices() {
        setLoading(true);
        statusLabel.setText("Cargando facturas...");
        String query = searchField.getText().trim();
        String from = fromDate.getValue() != null ? fromDate.getValue().toString() : "";
        String to = toDate.getValue() != null ? toDate.getValue().toString() : "";
        
        Thread worker = new Thread(() -> {
            try {
                PageResponse<InvoiceSummary> page = apiClient.listInvoices(query, from, to, 0, 50);
                Platform.runLater(() -> {
                    invoices.setAll(page.content());
                    listView.setItems(invoices.stream()
                            .map(item -> String.format("%s · %s\n%s · %s", 
                                item.invoiceNumber(), FormatUtils.currency(item.totalAmount()), 
                                item.clientName(), item.status()))
                            .collect(FXCollections::observableArrayList, ObservableList::add, ObservableList::addAll));
                    statusLabel.setText("Total facturas: " + page.totalElements());
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

    private void selectInvoice(InvoiceSummary summary) {
        if (summary == null) return;
        showDetailPanel();
        setLoading(true);
        
        Thread worker = new Thread(() -> {
            try {
                InvoiceDetail detail = apiClient.getInvoice(summary.id());
                Platform.runLater(() -> {
                    selected = detail;
                    updateDetailView(detail);
                    voidButton.setDisable("VOID".equals(detail.status()) || "PAID".equals(detail.status()));
                });
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Error al cargar detalle."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void updateDetailView(InvoiceDetail detail) {
        detailPanel.getChildren().clear();
        
        Label title = new Label("Factura " + detail.invoiceNumber());
        title.getStyleClass().add("section-title");
        title.setStyle("-fx-font-size: 18px;");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(8);
        
        addDetailRow(grid, 0, "Cliente:", detail.clientName());
        addDetailRow(grid, 1, "Emisión:", FormatUtils.date(detail.issueDate()));
        addDetailRow(grid, 2, "Vencimiento:", FormatUtils.date(detail.dueDate()));
        addDetailRow(grid, 3, "Estado:", detail.status());
        addDetailRow(grid, 4, "Total:", FormatUtils.currency(detail.totalAmount()));
        addDetailRow(grid, 5, "Sale ID:", detail.saleId());

        detailPanel.getChildren().addAll(title, grid, new javafx.scene.control.Separator(), voidButton);
    }

    private void addDetailRow(GridPane grid, int row, String label, String value) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #94a3b8;");
        Label val = new Label(value != null ? value : "-");
        val.setStyle("-fx-font-weight: bold;");
        grid.addRow(row, lbl, val);
    }

    private void createInvoice() {
        SaleRow selectedSale = saleBox.getValue();
        if (selectedSale == null) {
            statusLabel.setText("Selecciona una venta.");
            return;
        }
        if (!"CONFIRMED".equalsIgnoreCase(selectedSale.status())
                && !"PAID".equalsIgnoreCase(selectedSale.status())) {
            statusLabel.setText("Confirma la venta antes de generar la factura.");
            return;
        }
        String dueDate = dueDateField.getValue() != null ? dueDateField.getValue().toString() : null;
        
        setLoading(true);
        statusLabel.setText("Generando factura...");
        
        Thread worker = new Thread(() -> {
            try {
                InvoiceDetail created = apiClient.createInvoice(selectedSale.id(), dueDate);
                Platform.runLater(() -> {
                    loadInvoices();
                    statusLabel.setText("Factura " + created.invoiceNumber() + " creada.");
                    showDetailPanel();
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

    private void voidInvoice() {
        if (selected == null) return;
        setLoading(true);
        Thread worker = new Thread(() -> {
            try {
                apiClient.voidInvoice(selected.id());
                Platform.runLater(() -> {
                    loadInvoices();
                    statusLabel.setText("Factura anulada.");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Error al anular."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void setLoading(boolean loading) {
        refreshButton.setDisable(loading);
        if (createButton != null) createButton.setDisable(loading);
        voidButton.setDisable(loading);
        listView.setDisable(loading);
        if (confirmButton != null) confirmButton.setDisable(loading);
        if (payButton != null) payButton.setDisable(loading);
    }

    private void loadEligibleSales() {
        setLoading(true);
        statusLabel.setText("Cargando ventas disponibles...");
        Thread worker = new Thread(() -> {
            try {
                List<Customer> fetchedCustomers = apiClient.listCustomers();
                List<String> invoicedIds = apiClient.listInvoices("", "", "", 0, 200).content().stream()
                        .map(InvoiceSummary::saleId)
                        .filter(id -> id != null && !id.isBlank())
                        .toList();
                SalesPage page = apiClient.listSales(null, 0, 200);
                List<SaleRow> filtered = (page != null && page.content() != null ? page.content() : List.<SaleRow>of())
                        .stream()
                        .filter(sale -> sale != null && sale.id() != null && !invoicedIds.contains(sale.id()))
                        .filter(sale -> !"CANCELLED".equalsIgnoreCase(sale.status()))
                        .toList();
                Platform.runLater(() -> {
                    customerNames.clear();
                    for (Customer customer : fetchedCustomers) {
                        String name = (customer.firstname() + " " + customer.lastname()).trim();
                        customerNames.put(customer.id(), name.isBlank() ? customer.email() : name);
                    }
                    eligibleSales.setAll(filtered);
                    statusLabel.setText("Ventas disponibles: " + filtered.size());
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

    private void updateSaleMeta() {
        SaleRow sale = saleBox.getValue();
        if (sale == null) {
            saleMetaLabel.setText("Selecciona una venta para ver detalles.");
            return;
        }
        String customerName = customerNames.getOrDefault(sale.clientId(), sale.clientId());
        String meta = "Cliente: " + customerName
                + " · Total: " + FormatUtils.currency(sale.total())
                + " · Estado: " + sale.status();
        saleMetaLabel.setText(meta);
        if (sale.balance() != null) {
            paymentAmountField.setText(sale.balance().toPlainString());
        }
    }

    private void confirmSelectedSale() {
        SaleRow selectedSale = saleBox.getValue();
        if (selectedSale == null) {
            statusLabel.setText("Selecciona una venta.");
            return;
        }
        setLoading(true);
        statusLabel.setText("Confirmando venta...");
        Thread worker = new Thread(() -> {
            try {
                SaleRow confirmed = apiClient.confirmSale(selectedSale.id(), List.of("EMAIL", "WHATSAPP"));
                Platform.runLater(() -> {
                    replaceSale(confirmed);
                    saleBox.getSelectionModel().select(confirmed);
                    updateSaleMeta();
                    statusLabel.setText("Venta confirmada.");
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

    private void registerPayment() {
        SaleRow selectedSale = saleBox.getValue();
        if (selectedSale == null) {
            statusLabel.setText("Selecciona una venta.");
            return;
        }
        String amountRaw = paymentAmountField.getText().trim();
        String method = paymentMethodBox.getValue() != null ? paymentMethodBox.getValue() : "";
        String reference = paymentReferenceField.getText().trim();
        double amount;
        try {
            amount = Double.parseDouble(amountRaw);
        } catch (NumberFormatException ex) {
            statusLabel.setText("Monto invalido.");
            return;
        }
        if (amount <= 0) {
            statusLabel.setText("Monto debe ser mayor a cero.");
            return;
        }
        if (!"CASH".equals(method) && !"CREDIT".equals(method) && !"CARD".equals(method) && !"TRANSFER".equals(method)) {
            statusLabel.setText("Metodo de pago invalido.");
            return;
        }
        setLoading(true);
        statusLabel.setText("Registrando pago...");
        Thread worker = new Thread(() -> {
            try {
                SaleRow updated = apiClient.addPayment(selectedSale.id(), amount, method, reference.isBlank() ? null : reference);
                Platform.runLater(() -> {
                    replaceSale(updated);
                    saleBox.getSelectionModel().select(updated);
                    updateSaleMeta();
                    statusLabel.setText("Pago registrado.");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Error al registrar pago: " + ex.getMessage()));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void replaceSale(SaleRow sale) {
        if (sale == null || sale.id() == null) {
            return;
        }
        for (int i = 0; i < eligibleSales.size(); i++) {
            SaleRow existing = eligibleSales.get(i);
            if (existing != null && sale.id().equals(existing.id())) {
                eligibleSales.set(i, sale);
                return;
            }
        }
        eligibleSales.add(sale);
    }

    private String paymentMethodLabel(String method) {
        if ("CASH".equals(method)) return "Contado";
        if ("CREDIT".equals(method) || "CARD".equals(method)) return "Credito";
        if ("TRANSFER".equals(method)) return "Transferencia";
        return method == null || method.isBlank() ? "-" : method;
    }
}
