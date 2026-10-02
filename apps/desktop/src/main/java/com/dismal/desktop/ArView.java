package com.dismal.desktop;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ArView {

    private final BorderPane root;
    private final ApiClient apiClient;
    private final ObservableList<ArItem> overdueItems;
    private final ObservableList<ArItem> openItems;
    private final Label statusLabel;
    private final Button refreshButton;

    public ArView(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.root = new BorderPane();
        this.overdueItems = FXCollections.observableArrayList();
        this.openItems = FXCollections.observableArrayList();
        this.statusLabel = new Label();

        root.setPadding(new Insets(16));
        root.getStyleClass().add("content-root");

        Label title = new Label("Cuentas por cobrar");
        title.getStyleClass().add("page-title");

        refreshButton = new Button("Actualizar");
        refreshButton.getStyleClass().add("button-ghost");
        refreshButton.setOnAction(event -> loadData());

        ToolBar toolBar = new ToolBar(title, refreshButton);
        toolBar.getStyleClass().add("toolbar");
        root.setTop(toolBar);

        TableView<ArItem> overdueTable = buildTable(overdueItems);
        TableView<ArItem> openTable = buildTable(openItems);

        VBox overduePane = new VBox(8, sectionLabel("AR vencido"), overdueTable);
        VBox openPane = new VBox(8, sectionLabel("AR por vencer"), openTable);
        HBox tables = new HBox(16, overduePane, openPane);

        overduePane.getStyleClass().add("panel");
        openPane.getStyleClass().add("panel");
        overduePane.setPrefWidth(520);
        openPane.setPrefWidth(520);

        statusLabel.getStyleClass().add("page-subtitle");
        VBox content = new VBox(12, tables, statusLabel);
        root.setCenter(content);

        loadData();
    }

    public Parent getRoot() {
        return root;
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("page-subtitle");
        return label;
    }

    private TableView<ArItem> buildTable(ObservableList<ArItem> items) {
        TableView<ArItem> table = new TableView<>(items);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<ArItem, String> dueColumn = new TableColumn<>("Vence");
        dueColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(FormatUtils.date(cell.getValue().dueDate())));

        TableColumn<ArItem, String> balanceColumn = new TableColumn<>("Saldo");
        balanceColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(FormatUtils.currency(cell.getValue().balance())));

        table.getColumns().addAll(
                column("Cliente", "clientName"),
                column("Email", "clientEmail"),
                dueColumn,
                balanceColumn
        );
        return table;
    }

    private <T> TableColumn<ArItem, T> column(String title, String property) {
        TableColumn<ArItem, T> column = new TableColumn<>(title);
        FxTable.bindColumn(column, property);
        return column;
    }

    private void loadData() {
        setLoading(true);
        statusLabel.setText("Cargando AR...");
        Thread worker = new Thread(() -> {
            try {
                PageResponse<ArItem> overdue = apiClient.getArItems("OVERDUE", 0, 20);
                PageResponse<ArItem> open = apiClient.getArItems("OPEN", 0, 20);
                Platform.runLater(() -> {
                    overdueItems.setAll(overdue.content());
                    openItems.setAll(open.content());
                    statusLabel.setText("Vencidas: " + overdue.totalElements() + " · Abiertas: " + open.totalElements());
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> statusLabel.setText(resolveError(ex)));
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("No se pudo cargar AR."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void setLoading(boolean loading) {
        refreshButton.setDisable(loading);
    }

    private String resolveError(ApiException ex) {
        if (ex.getStatus() == 401 || ex.getStatus() == 403) {
            return "Acceso denegado.";
        }
        return ex.getMessage();
    }
}
