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
import javafx.scene.layout.VBox;

public class TargetsOverviewView {

    private final BorderPane root;
    private final ApiClient apiClient;
    private final ObservableList<DashboardTarget> targets;
    private final TableView<DashboardTarget> table;
    private final Label statusLabel;
    private final Button refreshButton;

    public TargetsOverviewView(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.root = new BorderPane();
        this.targets = FXCollections.observableArrayList();
        this.table = new TableView<>(targets);
        this.statusLabel = new Label();

        root.setPadding(new Insets(16));
        root.getStyleClass().add("content-root");

        Label title = new Label("Metas");
        title.getStyleClass().add("page-title");

        refreshButton = new Button("Actualizar");
        refreshButton.getStyleClass().add("button-ghost");
        refreshButton.setOnAction(event -> loadTargets());

        ToolBar toolBar = new ToolBar(title, refreshButton);
        toolBar.getStyleClass().add("toolbar");
        root.setTop(toolBar);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().addAll(
                column("Producto", "productName"),
                column("Meta", "metaUnits"),
                column("Vendidas", "unitsSoldCurrent"),
                column("Punto equilibrio", "breakEvenUnits"),
                column("Margen", "marginUnit"),
                column("Ganancia", "expectedProfit"),
                column("Deadline", "deadline")
        );

        statusLabel.getStyleClass().add("page-subtitle");
        VBox center = new VBox(12, table, statusLabel);
        root.setCenter(center);

        loadTargets();
    }

    public Parent getRoot() {
        return root;
    }

    private void loadTargets() {
        setLoading(true);
        statusLabel.setText("Cargando metas...");
        Thread worker = new Thread(() -> {
            try {
                PageResponse<DashboardTarget> page = apiClient.getDashboardTargets("profit", 0, 20);
                Platform.runLater(() -> {
                    targets.setAll(page.content());
                    statusLabel.setText("Total: " + page.totalElements());
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> statusLabel.setText(resolveError(ex)));
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("No se pudo cargar metas."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private <T> TableColumn<DashboardTarget, T> column(String title, String property) {
        TableColumn<DashboardTarget, T> column = new TableColumn<>(title);
        FxTable.bindColumn(column, property);
        return column;
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
