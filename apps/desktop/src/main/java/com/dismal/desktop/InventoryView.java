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

public class InventoryView {

    private final BorderPane root;
    private final TableView<StockSummary> table;
    private final ObservableList<StockSummary> rows;
    private final Label statusLabel;
    private final ApiClient apiClient;
    private final Button refreshButton;

    public InventoryView(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.root = new BorderPane();
        this.table = new TableView<>();
        this.rows = FXCollections.observableArrayList();
        this.statusLabel = new Label();

        root.setPadding(new Insets(16));
        root.getStyleClass().add("content-root");

        Label title = new Label("Inventario");
        title.getStyleClass().add("page-title");

        refreshButton = new Button("Actualizar");
        refreshButton.getStyleClass().add("button-ghost");
        refreshButton.setOnAction(event -> loadInventory());

        ToolBar toolBar = new ToolBar(title, refreshButton);
        toolBar.getStyleClass().add("toolbar");

        statusLabel.getStyleClass().add("page-subtitle");
        statusLabel.setText("Cargando inventario...");

        table.setItems(rows);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().addAll(
                column("Producto", "softwareName"),
                column("Disponibles", "available"),
                column("Asignadas", "assigned")
        );

        root.setTop(toolBar);
        root.setCenter(table);
        root.setBottom(statusLabel);
        BorderPane.setMargin(statusLabel, new Insets(8, 0, 0, 0));

        loadInventory();
    }

    public Parent getRoot() {
        return root;
    }

    private void loadInventory() {
        refreshButton.setDisable(true);
        statusLabel.setText("Cargando inventario...");
        Thread worker = new Thread(() -> {
            try {
                StockSummary[] data = apiClient.getInventorySummary();
                Platform.runLater(() -> {
                    rows.setAll(data);
                    statusLabel.setText("Productos: " + rows.size());
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> statusLabel.setText(resolveError(ex)));
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("No se pudo cargar inventario."));
            } finally {
                Platform.runLater(() -> refreshButton.setDisable(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private <T> TableColumn<StockSummary, T> column(String title, String property) {
        TableColumn<StockSummary, T> column = new TableColumn<>(title);
        FxTable.bindColumn(column, property);
        return column;
    }

    private String resolveError(ApiException ex) {
        if (ex.getStatus() == 401 || ex.getStatus() == 403) {
            return "Acceso denegado.";
        }
        return ex.getMessage();
    }
}
