package com.dismal.desktop;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class StrategyActionsView {

    private final BorderPane root = new BorderPane();
    private final ApiClient apiClient;
    private final ObservableList<StrategyAction> actions = FXCollections.observableArrayList();
    private final TableView<StrategyAction> table = new TableView<>(actions);
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final Label statusLabel = new Label();
    private final Button refreshButton = new Button("Actualizar");
    private final Button progressButton = new Button("En progreso");
    private final Button doneButton = new Button("Hecha");
    private final Button discardButton = new Button("Descartar");

    public StrategyActionsView(ApiClient apiClient) {
        this.apiClient = apiClient;
        root.getStyleClass().add("content-root");

        Label title = new Label("Acciones estrategicas");
        title.getStyleClass().add("page-title");
        refreshButton.getStyleClass().add("button-ghost");
        refreshButton.setOnAction(event -> loadActions());

        statusFilter.getItems().addAll("TODAS", "PENDIENTE", "EN_PROGRESO", "HECHA", "DESCARTADA");
        statusFilter.getSelectionModel().select("TODAS");
        statusFilter.setOnAction(event -> loadActions());

        ToolBar toolbar = new ToolBar(title, refreshButton, new Label("Estado"), statusFilter);
        toolbar.getStyleClass().add("toolbar");
        root.setTop(toolbar);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().addAll(
                column("Titulo", "title"),
                column("Producto", "productName"),
                column("Fuente", "source"),
                column("Prioridad", "priority"),
                column("Estado", "status"),
                column("Canal", "recommendedChannel"),
                column("Vence", "dueDate")
        );
        VBox.setVgrow(table, Priority.ALWAYS);

        progressButton.getStyleClass().add("button-ghost");
        doneButton.getStyleClass().add("button-primary");
        discardButton.getStyleClass().add("button-danger");
        progressButton.setOnAction(event -> updateSelected("EN_PROGRESO"));
        doneButton.setOnAction(event -> updateSelected("HECHA"));
        discardButton.setOnAction(event -> updateSelected("DESCARTADA"));

        HBox actionsRow = new HBox(10, progressButton, doneButton, discardButton);
        actionsRow.setPadding(new Insets(10, 0, 0, 0));

        VBox center = new VBox(10, table, actionsRow);
        center.setPadding(new Insets(16));
        root.setCenter(center);
        statusLabel.getStyleClass().add("page-subtitle");
        statusLabel.setPadding(new Insets(8));
        root.setBottom(statusLabel);

        loadActions();
    }

    public Parent getRoot() {
        return root;
    }

    private void loadActions() {
        setLoading(true);
        statusLabel.setText("Cargando acciones...");
        Thread worker = new Thread(() -> {
            try {
                String selected = statusFilter.getValue();
                String status = "TODAS".equals(selected) ? "" : selected;
                PageResponse<StrategyAction> page = apiClient.listStrategyActions(status, 0, 50);
                Platform.runLater(() -> {
                    actions.setAll(page.content());
                    statusLabel.setText("Acciones: " + page.totalElements());
                });
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("No se pudieron cargar acciones."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void updateSelected(String status) {
        StrategyAction selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Selecciona una accion.");
            return;
        }
        setLoading(true);
        Thread worker = new Thread(() -> {
            try {
                apiClient.updateStrategyActionStatus(selected.id(), status);
                Platform.runLater(() -> {
                    statusLabel.setText("Accion actualizada.");
                    loadActions();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("No se pudo actualizar accion."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void setLoading(boolean loading) {
        refreshButton.setDisable(loading);
        progressButton.setDisable(loading);
        doneButton.setDisable(loading);
        discardButton.setDisable(loading);
        statusFilter.setDisable(loading);
    }

    private <T> TableColumn<StrategyAction, T> column(String title, String property) {
        TableColumn<StrategyAction, T> column = new TableColumn<>(title);
        FxTable.bindColumn(column, property);
        return column;
    }
}
