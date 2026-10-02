package com.dismal.desktop;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.List;

public class CampaignsView {

    private final BorderPane root;
    private final ApiClient apiClient;

    private final ObservableList<Campaign> campaignsList = FXCollections.observableArrayList();
    private final ObservableList<Campaign> filteredCampaigns = FXCollections.observableArrayList();
    private final ObservableList<Product> products = FXCollections.observableArrayList();

    private final ListView<Campaign> campaignListView = new ListView<>(filteredCampaigns);
    private final VBox detailPanel;

    private final Label statusLabel = new Label();
    private final Button refreshButton = new Button("Actualizar");
    private final Button newCampaignButton = new Button("+ Nueva campaña");
    private final ComboBox<String> statusFilterBox = new ComboBox<>();
    private final ComboBox<String> channelFilterBox = new ComboBox<>();
    private final TextField searchField = new TextField();

    private final Label detailTitle = new Label("Detalle de campaña");
    private final TextField titleField = new TextField();
    private final TextArea messageField = new TextArea();
    private final ComboBox<String> targetRoleField = new ComboBox<>();
    private final ComboBox<String> channelField = new ComboBox<>();
    private final ComboBox<Product> productField = new ComboBox<>();
    private final TextField scheduledAtField = new TextField();
    private final Button saveCampaignButton = new Button("Guardar cambios");
    private final Button clearButton = new Button("Limpiar");
    private final Label previewTitle = new Label("Vista previa");
    private final Label previewChannel = new Label("-");
    private final Label previewTarget = new Label("-");
    private final Label previewProduct = new Label("-");
    private final Label previewMessage = new Label("-");

    private Campaign selectedCampaign;
    private boolean creatingCampaign = false;

    public CampaignsView(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.root = new BorderPane();
        root.getStyleClass().add("content-root");

        Label title = new Label("Marketing");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Crea campañas enfocadas en clientes finales y programa envíos.");
        subtitle.getStyleClass().add("page-subtitle");

        refreshButton.getStyleClass().add("button-ghost");
        refreshButton.setOnAction(e -> loadCampaigns());

        newCampaignButton.getStyleClass().add("button-primary");
        newCampaignButton.setOnAction(e -> openCampaignForm(true));

        ToolBar toolBar = new ToolBar(title, refreshButton, newCampaignButton);
        toolBar.getStyleClass().add("toolbar");
        VBox header = new VBox(6, toolBar, subtitle);

        statusFilterBox.getItems().addAll("TODAS", "DRAFT", "SCHEDULED", "SENT", "CANCELLED");
        statusFilterBox.getSelectionModel().select("TODAS");
        channelFilterBox.getItems().addAll("TODOS", "EMAIL", "WHATSAPP");
        channelFilterBox.getSelectionModel().select("TODOS");
        searchField.setPromptText("Buscar por título o producto...");
        statusFilterBox.valueProperty().addListener((obs, old, value) -> applyFilters());
        channelFilterBox.valueProperty().addListener((obs, old, value) -> applyFilters());
        searchField.textProperty().addListener((obs, old, value) -> applyFilters());

        HBox filters = new HBox(8,
                new Label("Estado"), statusFilterBox,
                new Label("Canal"), channelFilterBox,
                searchField
        );
        filters.setAlignment(Pos.CENTER_LEFT);

        campaignListView.setPlaceholder(new Label("Sin campañas registradas"));
        campaignListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Campaign item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label name = new Label(item.title());
                name.getStyleClass().add("section-title");

                String product = item.productName() != null ? item.productName() : "-";
                String channel = item.channel() != null ? item.channel() : "-";
                String status = item.status() != null ? item.status() : "-";
                Label meta = new Label(product + " · " + channel);
                meta.getStyleClass().add("help-text");

                Label badge = new Label(status);
                badge.getStyleClass().add("badge");
                badge.getStyleClass().add(resolveStatusStyle(status));

                HBox row = new HBox(8, meta, badge);
                row.setAlignment(Pos.CENTER_LEFT);
                VBox box = new VBox(4, name, row);
                setGraphic(box);
            }
        });
        campaignListView.getSelectionModel().selectedItemProperty().addListener((obs, prev, next) -> {
            if (next != null) {
                selectCampaign(next);
            }
        });

        VBox leftPane = new VBox(10, new Label("Campañas"), filters, campaignListView);
        leftPane.getStyleClass().add("panel");
        VBox.setVgrow(campaignListView, Priority.ALWAYS);

        detailPanel = buildDetailPanel();
        SplitPane splitPane = new SplitPane(leftPane, detailPanel);
        splitPane.setDividerPositions(0.45);
        splitPane.setStyle("-fx-background-color: transparent;");

        statusLabel.getStyleClass().add("page-subtitle");

        root.setTop(header);
        root.setCenter(splitPane);
        root.setBottom(statusLabel);

        loadAux();
        loadCampaigns();
        openCampaignForm(true);
    }

    public Parent getRoot() {
        return root;
    }

    private VBox buildDetailPanel() {
        VBox panel = new VBox(14);
        panel.setPadding(new Insets(16));
        panel.getStyleClass().add("panel");

        detailTitle.getStyleClass().add("section-title");

        targetRoleField.getItems().addAll("CUSTOMER", "USER");
        channelField.getItems().addAll("EMAIL", "WHATSAPP");
        productField.setConverter(productConverter());

        messageField.setPrefRowCount(4);
        messageField.setWrapText(true);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Título"), titleField);
        form.addRow(1, new Label("Canal"), channelField);
        form.addRow(2, new Label("Audiencia"), targetRoleField);
        form.addRow(3, new Label("Producto"), productField);
        form.addRow(4, new Label("Programar"), scheduledAtField);
        form.add(new Label("Mensaje"), 0, 5);
        form.add(messageField, 0, 6, 2, 1);

        saveCampaignButton.getStyleClass().add("button-primary");
        saveCampaignButton.setMaxWidth(Double.MAX_VALUE);
        saveCampaignButton.setOnAction(e -> {
            if (creatingCampaign) {
                createCampaign();
            } else {
                updateCampaign();
            }
        });

        clearButton.getStyleClass().add("button-ghost");
        clearButton.setMaxWidth(Double.MAX_VALUE);
        clearButton.setOnAction(e -> openCampaignForm(true));

        HBox actions = new HBox(10, saveCampaignButton, clearButton);
        HBox.setHgrow(saveCampaignButton, Priority.ALWAYS);
        HBox.setHgrow(clearButton, Priority.ALWAYS);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox previewBox = new VBox(6,
                previewTitle,
                new Label("Canal"), previewChannel,
                new Label("Audiencia"), previewTarget,
                new Label("Producto"), previewProduct,
                new Label("Mensaje"), previewMessage
        );
        previewBox.getStyleClass().add("panel-soft");
        previewBox.setPadding(new Insets(10));
        previewTitle.getStyleClass().add("section-title");
        previewMessage.getStyleClass().add("help-text");
        previewMessage.setWrapText(true);

        titleField.textProperty().addListener((obs, old, value) -> updatePreview());
        messageField.textProperty().addListener((obs, old, value) -> updatePreview());
        channelField.valueProperty().addListener((obs, old, value) -> updatePreview());
        targetRoleField.valueProperty().addListener((obs, old, value) -> updatePreview());
        productField.valueProperty().addListener((obs, old, value) -> updatePreview());

        panel.getChildren().addAll(detailTitle, form, actions, previewBox);
        return panel;
    }

    private void loadCampaigns() {
        setLoading(true);
        statusLabel.setText("Cargando campañas...");
        Thread worker = new Thread(() -> {
            try {
                PageResponse<Campaign> page = apiClient.listCampaigns("", 0, 50);
                List<Campaign> list = page.content();
                Platform.runLater(() -> {
                    campaignsList.setAll(list);
                    applyFilters();
                    statusLabel.setText("Campañas: " + page.totalElements());
                });
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("No se pudieron cargar campañas."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void selectCampaign(Campaign c) {
        selectedCampaign = c;
        openCampaignForm(false);
        titleField.setText(c.title());
        messageField.setText(c.messageBody());
        scheduledAtField.setText(c.scheduledAt());
        targetRoleField.getSelectionModel().select(c.targetRole());
        channelField.getSelectionModel().select(c.channel());
        if (c.productId() != null) {
            products.stream().filter(p -> p.getId().equals(c.productId())).findFirst().ifPresent(productField::setValue);
        }
        updatePreview();
    }

    private void openCampaignForm(boolean createMode) {
        creatingCampaign = createMode;
        selectedCampaign = null;
        detailTitle.setText(createMode ? "Nueva campaña" : "Editar campaña");
        saveCampaignButton.setText(createMode ? "Crear campaña" : "Guardar cambios");
        titleField.clear();
        messageField.clear();
        scheduledAtField.clear();
        targetRoleField.getSelectionModel().clearSelection();
        channelField.getSelectionModel().clearSelection();
        productField.getSelectionModel().clearSelection();
        updatePreview();
    }

    private void createCampaign() {
        statusLabel.setText("Función de crear campaña en desarrollo.");
    }

    private void updateCampaign() {
        statusLabel.setText("Función de editar campaña en desarrollo.");
    }

    private void loadAux() {
        Thread worker = new Thread(() -> {
            try {
                List<Product> productList = apiClient.listProducts();
                Platform.runLater(() -> products.setAll(productList));
            } catch (Exception ignored) {
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void setLoading(boolean loading) {
        refreshButton.setDisable(loading);
        newCampaignButton.setDisable(loading);
    }

    private void applyFilters() {
        String status = statusFilterBox.getValue();
        String channel = channelFilterBox.getValue();
        String query = searchField.getText() != null ? searchField.getText().toLowerCase() : "";
        filteredCampaigns.setAll(campaignsList.stream()
                .filter(item -> status == null || "TODAS".equals(status) || status.equalsIgnoreCase(item.status()))
                .filter(item -> channel == null || "TODOS".equals(channel) || channel.equalsIgnoreCase(item.channel()))
                .filter(item -> {
                    if (query.isBlank()) return true;
                    String title = item.title() != null ? item.title().toLowerCase() : "";
                    String product = item.productName() != null ? item.productName().toLowerCase() : "";
                    return title.contains(query) || product.contains(query);
                })
                .toList());
    }

    private void updatePreview() {
        previewChannel.setText(channelField.getValue() != null ? channelField.getValue() : "-");
        previewTarget.setText(targetRoleField.getValue() != null ? targetRoleField.getValue() : "-");
        Product product = productField.getValue();
        previewProduct.setText(product != null ? product.getName() : "-");
        String message = messageField.getText();
        previewMessage.setText(message == null || message.isBlank() ? "-" : message);
    }

    private String resolveStatusStyle(String status) {
        if (status == null) return "badge-neutral";
        return switch (status.toUpperCase()) {
            case "SENT", "DELIVERED", "ACTIVE" -> "badge-success";
            case "SCHEDULED", "PENDING" -> "badge-warn";
            case "FAILED", "CANCELLED", "ERROR" -> "badge-error";
            default -> "badge-neutral";
        };
    }

    private StringConverter<Product> productConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Product p) {
                return p == null ? "" : p.getName();
            }

            @Override
            public Product fromString(String s) {
                return null;
            }
        };
    }
}
