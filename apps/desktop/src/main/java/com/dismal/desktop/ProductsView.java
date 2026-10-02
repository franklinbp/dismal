package com.dismal.desktop;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.net.URI;

public class ProductsView {

    private final BorderPane root;
    private final ApiClient apiClient;
    private final ObservableList<Product> products;
    private final TableView<Product> table;
    private final Label statusLabel;
    private final Label priceListStatusLabel;

    private final Button refreshButton;
    private final Button newButton;

    private final VBox drawerPanel;
    private final Label drawerTitle;
    private final TextField nameField;
    private final TextArea descriptionField;
    private final TextField ecFinalPriceField;
    private final TextField ecDistributorPriceField;
    private final TextField peFinalPriceField;
    private final TextField peDistributorPriceField;
    private final TextField platformField;
    private final TextField imageUrlField;
    private final Button saveButton;
    private final Button deleteButton;
    private final Button closeDrawerButton;

    private Product selected;
    private boolean creating = false;
    private final SplitPane splitPane;

    private final TextField priceListEmailField;
    private final TextField priceListSubjectField;
    private final TextField priceListWhatsappField;
    private final ComboBox<String> priceListCountryBox;
    private final ComboBox<String> priceListCustomerTypeBox;
    private final CheckBox priceListPdfCheck;
    private final CheckBox priceListXlsxCheck;
    private final Button priceListSendButton;
    private final Button priceListCopyButton;
    private final Button priceListOpenButton;
    private String priceListWhatsappText;
    private String priceListWhatsappUrl;

    public ProductsView(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.root = new BorderPane();
        this.products = FXCollections.observableArrayList();
        this.table = new TableView<>(products);
        this.statusLabel = new Label();

        root.getStyleClass().add("content-root");

        Label title = new Label("Inventario de Productos");
        title.getStyleClass().add("page-title");

        refreshButton = new Button("Actualizar");
        refreshButton.getStyleClass().add("button-ghost");
        refreshButton.setOnAction(event -> loadProducts());

        newButton = new Button("+ Nuevo Producto");
        newButton.getStyleClass().add("button-primary");
        newButton.setOnAction(event -> openDrawer(true));

        ToolBar toolBar = new ToolBar(title, refreshButton, newButton);
        toolBar.getStyleClass().add("toolbar");
        root.setTop(toolBar);

        Label priceListTitle = new Label("Lista de precios");
        priceListTitle.getStyleClass().add("section-title");
        Label priceListSubtitle = new Label("Genera y envia listas por pais y tipo de cliente.");
        priceListSubtitle.getStyleClass().add("help-text");

        priceListEmailField = new TextField();
        priceListEmailField.setPromptText("cliente@correo.com");
        priceListSubjectField = new TextField("Lista de precios - Dismal");
        priceListWhatsappField = new TextField();
        priceListWhatsappField.setPromptText("593999999999");

        priceListCountryBox = new ComboBox<>(FXCollections.observableArrayList("EC", "PE"));
        priceListCountryBox.setValue("EC");
        priceListCustomerTypeBox = new ComboBox<>(FXCollections.observableArrayList("FINAL", "DISTRIBUTOR"));
        priceListCustomerTypeBox.setValue("FINAL");

        priceListPdfCheck = new CheckBox("Adjuntar PDF");
        priceListPdfCheck.setSelected(true);
        priceListXlsxCheck = new CheckBox("Adjuntar Excel");
        priceListXlsxCheck.setSelected(true);

        Button downloadPdfButton = new Button("Descargar PDF");
        downloadPdfButton.getStyleClass().add("button-ghost");
        downloadPdfButton.setOnAction(event -> downloadPriceList("pdf"));

        Button downloadXlsxButton = new Button("Descargar Excel");
        downloadXlsxButton.getStyleClass().add("button-ghost");
        downloadXlsxButton.setOnAction(event -> downloadPriceList("xlsx"));

        Button downloadTextButton = new Button("Descargar Texto");
        downloadTextButton.getStyleClass().add("button-ghost");
        downloadTextButton.setOnAction(event -> downloadPriceList("text"));

        priceListSendButton = new Button("Enviar por email");
        priceListSendButton.getStyleClass().add("button-primary");
        priceListSendButton.setOnAction(event -> sendPriceList());

        priceListCopyButton = new Button("Copiar WhatsApp");
        priceListCopyButton.getStyleClass().add("button-ghost");
        priceListCopyButton.setOnAction(event -> copyWhatsappText());
        priceListCopyButton.setDisable(true);

        priceListOpenButton = new Button("Abrir WhatsApp");
        priceListOpenButton.getStyleClass().add("button-ghost");
        priceListOpenButton.setOnAction(event -> openWhatsapp());
        priceListOpenButton.setDisable(true);

        GridPane priceListForm = new GridPane();
        priceListForm.setHgap(12);
        priceListForm.setVgap(8);
        priceListForm.addRow(0, new Label("Pais"), priceListCountryBox);
        priceListForm.addRow(1, new Label("Tipo cliente"), priceListCustomerTypeBox);
        priceListForm.addRow(2, new Label("Email destino"), priceListEmailField);
        priceListForm.addRow(3, new Label("Asunto"), priceListSubjectField);
        priceListForm.addRow(4, new Label("WhatsApp"), priceListWhatsappField);
        priceListForm.addRow(5, priceListPdfCheck, priceListXlsxCheck);

        HBox priceListActions = new HBox(8, priceListSendButton, priceListCopyButton, priceListOpenButton);
        HBox priceListDownloads = new HBox(8, downloadPdfButton, downloadXlsxButton, downloadTextButton);

        priceListStatusLabel = new Label();
        priceListStatusLabel.getStyleClass().add("help-text");

        VBox priceListPanel = new VBox(8,
                priceListTitle,
                priceListSubtitle,
                priceListForm,
                priceListActions,
                priceListDownloads,
                priceListStatusLabel
        );
        priceListPanel.getStyleClass().add("panel");
        priceListPanel.setPadding(new Insets(16));

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().addAll(
                column("Nombre", "name"),
                column("Plataforma", "platform"),
                column("EC Final", "ecFinalPrice"),
                column("PE Final", "peFinalPrice")
        );
        table.getSelectionModel().selectedItemProperty().addListener((obs, prev, next) -> {
            if (next != null) {
                selectProduct(next);
            }
        });
        VBox.setVgrow(table, Priority.ALWAYS);

        drawerTitle = new Label("Detalle del Producto");
        drawerTitle.getStyleClass().add("section-title");

        closeDrawerButton = new Button("X");
        closeDrawerButton.getStyleClass().add("button-ghost");
        closeDrawerButton.setOnAction(e -> closeDrawer());

        HBox drawerHeader = new HBox(10, drawerTitle, closeDrawerButton);
        HBox.setHgrow(drawerTitle, Priority.ALWAYS);

        nameField = new TextField();
        descriptionField = new TextArea();
        descriptionField.setPrefRowCount(3);
        descriptionField.setWrapText(true);
        ecFinalPriceField = new TextField();
        ecDistributorPriceField = new TextField();
        peFinalPriceField = new TextField();
        peDistributorPriceField = new TextField();
        platformField = new TextField();
        imageUrlField = new TextField();

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Nombre *"), nameField);
        form.addRow(1, new Label("Plataforma"), platformField);
        form.addRow(2, new Label("EC Final *"), ecFinalPriceField);
        form.addRow(3, new Label("EC Distribuidor *"), ecDistributorPriceField);
        form.addRow(4, new Label("PE Final *"), peFinalPriceField);
        form.addRow(5, new Label("PE Distribuidor *"), peDistributorPriceField);
        form.addRow(6, new Label("Imagen URL"), imageUrlField);
        form.add(new Label("Descripcion"), 0, 7);
        form.add(descriptionField, 0, 8, 2, 1);

        saveButton = new Button("Guardar Cambios");
        saveButton.getStyleClass().add("button-primary");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setOnAction(event -> saveProduct());

        deleteButton = new Button("Eliminar Producto");
        deleteButton.getStyleClass().add("button-danger");
        deleteButton.setMaxWidth(Double.MAX_VALUE);
        deleteButton.setOnAction(event -> deleteSelected());

        drawerPanel = new VBox(16,
                drawerHeader,
                form,
                new javafx.scene.control.Separator(),
                saveButton,
                deleteButton
        );
        drawerPanel.setPadding(new Insets(20));
        drawerPanel.getStyleClass().add("drawer-panel");
        drawerPanel.setMinWidth(380);
        drawerPanel.setMaxWidth(440);

        VBox listPane = new VBox(12, priceListPanel, table);
        VBox.setVgrow(table, Priority.ALWAYS);

        splitPane = new SplitPane();
        splitPane.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        splitPane.getItems().add(listPane);

        root.setCenter(splitPane);

        statusLabel.getStyleClass().add("page-subtitle");
        statusLabel.setPadding(new Insets(8));
        root.setBottom(statusLabel);

        loadProducts();
    }

    public Parent getRoot() {
        return root;
    }

    private void openDrawer(boolean createMode) {
        if (!splitPane.getItems().contains(drawerPanel)) {
            splitPane.getItems().add(drawerPanel);
            splitPane.setDividerPositions(0.62);
        }

        if (createMode) {
            creating = true;
            selected = null;
            table.getSelectionModel().clearSelection();
            clearForm();
            drawerTitle.setText("Nuevo Producto");
            saveButton.setText("Crear Producto");
            deleteButton.setVisible(false);
        } else {
            creating = false;
            drawerTitle.setText("Editar Producto");
            saveButton.setText("Guardar Cambios");
            deleteButton.setVisible(true);
        }
    }

    private void closeDrawer() {
        splitPane.getItems().remove(drawerPanel);
        table.getSelectionModel().clearSelection();
        selected = null;
    }

    private void clearForm() {
        nameField.clear();
        descriptionField.clear();
        ecFinalPriceField.clear();
        ecDistributorPriceField.clear();
        peFinalPriceField.clear();
        peDistributorPriceField.clear();
        platformField.clear();
        imageUrlField.clear();
    }

    private void selectProduct(Product product) {
        openDrawer(false);
        selected = product;
        nameField.setText(product.getName());
        descriptionField.setText(product.getDescription() == null ? "" : product.getDescription());
        ecFinalPriceField.setText(String.valueOf(product.getEcFinalPrice()));
        ecDistributorPriceField.setText(String.valueOf(product.getEcDistributorPrice()));
        peFinalPriceField.setText(String.valueOf(product.getPeFinalPrice()));
        peDistributorPriceField.setText(String.valueOf(product.getPeDistributorPrice()));
        platformField.setText(product.getPlatform() == null ? "" : product.getPlatform());
        imageUrlField.setText(product.getImageUrl() == null ? "" : product.getImageUrl());
        statusLabel.setText("");
    }

    private void loadProducts() {
        setLoading(true);
        statusLabel.setText("Cargando...");
        Thread worker = new Thread(() -> {
            try {
                var data = apiClient.listProducts();
                Platform.runLater(() -> {
                    products.setAll(data);
                    statusLabel.setText("Total productos: " + products.size());
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> statusLabel.setText("Error: " + ex.getMessage()));
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Error de conexion."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void saveProduct() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            statusLabel.setText("Nombre obligatorio.");
            return;
        }

        Double ecFinalPrice = parseRequiredPrice(ecFinalPriceField, "EC Final");
        Double ecDistributorPrice = parseRequiredPrice(ecDistributorPriceField, "EC Distribuidor");
        Double peFinalPrice = parseRequiredPrice(peFinalPriceField, "PE Final");
        Double peDistributorPrice = parseRequiredPrice(peDistributorPriceField, "PE Distribuidor");
        if (ecFinalPrice == null || ecDistributorPrice == null || peFinalPrice == null || peDistributorPrice == null) {
            return;
        }

        Product payload = new Product(
                selected != null ? selected.getId() : null,
                name,
                descriptionField.getText().trim().isEmpty() ? null : descriptionField.getText().trim(),
                ecFinalPrice,
                platformField.getText().trim().isEmpty() ? null : platformField.getText().trim(),
                imageUrlField.getText().trim().isEmpty() ? null : imageUrlField.getText().trim(),
                ecFinalPrice,
                ecDistributorPrice,
                peFinalPrice,
                peDistributorPrice
        );
        setLoading(true);
        statusLabel.setText("Guardando...");
        Thread worker = new Thread(() -> {
            try {
                if (creating || selected == null) {
                    apiClient.createProduct(payload);
                } else {
                    apiClient.updateProduct(selected.getId(), payload);
                }
                Platform.runLater(() -> {
                    loadProducts();
                    statusLabel.setText("Guardado exitosamente.");
                    if (creating) {
                        clearForm();
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Error al guardar: " + ex.getMessage()));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void deleteSelected() {
        if (selected == null) {
            return;
        }
        setLoading(true);
        statusLabel.setText("Eliminando...");
        Thread worker = new Thread(() -> {
            try {
                apiClient.deleteProduct(selected.getId(), true);
                Platform.runLater(() -> {
                    closeDrawer();
                    loadProducts();
                    statusLabel.setText("Producto eliminado.");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Error al eliminar."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private <T> TableColumn<Product, T> column(String title, String property) {
        TableColumn<Product, T> column = new TableColumn<>(title);
        FxTable.bindColumn(column, property);
        return column;
    }

    private void setLoading(boolean loading) {
        refreshButton.setDisable(loading);
        newButton.setDisable(loading);
        saveButton.setDisable(loading);
        deleteButton.setDisable(loading);
        priceListSendButton.setDisable(loading);
    }

    private void sendPriceList() {
        String email = priceListEmailField.getText() != null ? priceListEmailField.getText().trim() : "";
        if (email.isEmpty()) {
            priceListStatusLabel.setText("Email destino requerido.");
            return;
        }
        priceListStatusLabel.setText("Enviando...");
        setLoading(true);
        Thread worker = new Thread(() -> {
            try {
                PriceListSendRequest payload = new PriceListSendRequest(
                        email,
                        priceListSubjectField.getText(),
                        priceListWhatsappField.getText(),
                        priceListPdfCheck.isSelected(),
                        priceListXlsxCheck.isSelected(),
                        selectedCountryCode(),
                        selectedCustomerType()
                );
                PriceListSendResponse response = apiClient.sendPriceList(payload);
                Platform.runLater(() -> {
                    priceListWhatsappText = response.whatsappText();
                    priceListWhatsappUrl = response.whatsappUrl();
                    priceListCopyButton.setDisable(priceListWhatsappText == null || priceListWhatsappText.isBlank());
                    priceListOpenButton.setDisable(priceListWhatsappUrl == null || priceListWhatsappUrl.isBlank());
                    priceListStatusLabel.setText("Lista enviada por email.");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> priceListStatusLabel.setText("Error: " + ex.getMessage()));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void downloadPriceList(String format) {
        setLoading(true);
        priceListStatusLabel.setText("Descargando " + format + "...");
        Thread worker = new Thread(() -> {
            try {
                byte[] data;
                String extension;
                if ("pdf".equals(format)) {
                    data = apiClient.downloadPriceListPdf(selectedCountryCode(), selectedCustomerType());
                    extension = "pdf";
                } else if ("xlsx".equals(format)) {
                    data = apiClient.downloadPriceListXlsx(selectedCountryCode(), selectedCustomerType());
                    extension = "xlsx";
                } else {
                    String text = apiClient.getPriceListText(selectedCountryCode(), selectedCustomerType());
                    data = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    extension = "txt";
                }
                FileChooser chooser = new FileChooser();
                chooser.setTitle("Guardar lista de precios");
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(extension.toUpperCase(), "*." + extension));
                chooser.setInitialFileName("lista-precios-" + selectedCountryCode().toLowerCase() + "-" + selectedCustomerType().toLowerCase() + "." + extension);
                var file = chooser.showSaveDialog(root.getScene().getWindow());
                if (file != null) {
                    java.nio.file.Files.write(file.toPath(), data);
                    Platform.runLater(() -> priceListStatusLabel.setText("Archivo guardado."));
                } else {
                    Platform.runLater(() -> priceListStatusLabel.setText("Descarga cancelada."));
                }
            } catch (Exception ex) {
                Platform.runLater(() -> priceListStatusLabel.setText("Error: " + ex.getMessage()));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void copyWhatsappText() {
        if (priceListWhatsappText == null || priceListWhatsappText.isBlank()) {
            priceListStatusLabel.setText("No hay texto de WhatsApp.");
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(priceListWhatsappText);
        Clipboard.getSystemClipboard().setContent(content);
        priceListStatusLabel.setText("Texto de WhatsApp copiado.");
    }

    private void openWhatsapp() {
        if (priceListWhatsappUrl == null || priceListWhatsappUrl.isBlank()) {
            priceListStatusLabel.setText("No hay enlace de WhatsApp.");
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(priceListWhatsappUrl));
                priceListStatusLabel.setText("Abriendo WhatsApp...");
            } else {
                priceListStatusLabel.setText("No se puede abrir el navegador.");
            }
        } catch (Exception ex) {
            priceListStatusLabel.setText("Error al abrir WhatsApp.");
        }
    }

    private Double parseRequiredPrice(TextField field, String label) {
        String value = field.getText() == null ? "" : field.getText().trim();
        if (value.isEmpty()) {
            statusLabel.setText(label + " es obligatorio.");
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            statusLabel.setText(label + " es invalido.");
            return null;
        }
    }

    private String selectedCountryCode() {
        return priceListCountryBox.getValue() != null ? priceListCountryBox.getValue() : "EC";
    }

    private String selectedCustomerType() {
        return priceListCustomerTypeBox.getValue() != null ? priceListCustomerTypeBox.getValue() : "FINAL";
    }
}
