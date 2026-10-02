package com.dismal.desktop;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

public class MipView {

    private final BorderPane root;
    private final ApiClient apiClient;
    private final ObservableList<MarketingAnalysisResult> analysisItems;
    private final TableView<MarketingAnalysisResult> table;
    private final Label statusLabel;
    private final Button refreshButton;

    private final VBox detailsPanel;
    private final Label detailsTitle;
    private final Label detailsExplanation;
    private final ListView<String> actionsList;

    public MipView(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.root = new BorderPane();
        this.analysisItems = FXCollections.observableArrayList();
        this.table = new TableView<>(analysisItems);
        this.statusLabel = new Label();
        this.refreshButton = new Button("Actualizar");

        root.getStyleClass().add("content-root");
        root.setPadding(new Insets(16));

        Label title = new Label("Inteligencia MIP");
        title.getStyleClass().add("page-title");
        refreshButton.getStyleClass().add("button-ghost");
        refreshButton.setOnAction(event -> loadAnalysis());
        ToolBar toolBar = new ToolBar(title, refreshButton);
        toolBar.getStyleClass().add("toolbar");
        root.setTop(toolBar);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().addAll(
                column("Producto", "productName"),
                column("Estado", "state"),
                column("Prioridad", "priority"),
                column("Diagnóstico", "diagnosis")
        );
        table.getSelectionModel().selectedItemProperty().addListener((obs, prev, next) -> {
            if (next != null) {
                showDetails(next);
            }
        });

        detailsTitle = new Label("Diagnóstico MIP");
        detailsTitle.getStyleClass().add("section-title");
        detailsExplanation = new Label();
        detailsExplanation.getStyleClass().add("help-text");
        detailsExplanation.setWrapText(true);
        actionsList = new ListView<>();
        actionsList.setPlaceholder(new Label("Selecciona un producto para ver acciones"));

        detailsPanel = new VBox(10,
                detailsTitle,
                new Label("Explicación"), detailsExplanation,
                new javafx.scene.control.Separator(),
                new Label("Acciones sugeridas"), actionsList
        );
        detailsPanel.getStyleClass().add("panel");
        detailsPanel.setPadding(new Insets(16));
        detailsPanel.setMinWidth(320);
        detailsPanel.setMaxWidth(380);

        SplitPane splitPane = new SplitPane();
        VBox tableBox = new VBox(10, new Label("Análisis de rendimiento comercial"), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        splitPane.getItems().addAll(tableBox, detailsPanel);
        splitPane.setDividerPositions(0.65);
        splitPane.setStyle("-fx-background-color: transparent;");

        root.setCenter(splitPane);
        statusLabel.getStyleClass().add("page-subtitle");
        root.setBottom(statusLabel);

        loadAnalysis();
    }

    public Parent getRoot() {
        return root;
    }

    private void loadAnalysis() {
        setLoading(true);
        statusLabel.setText("Cargando análisis MIP...");
        Thread worker = new Thread(() -> {
            try {
                List<MarketingAnalysisResult> analysis = apiClient.getProductAnalysis();
                List<SalesTarget> targets = apiClient.listSalesTargets();
                java.time.LocalDate today = java.time.LocalDate.now();
                java.util.Set<String> activeTargetIds = targets.stream()
                        .filter(target -> target.getMetaUnits() > 0)
                        .filter(target -> {
                            String deadline = target.getDeadline();
                            if (deadline == null || deadline.isBlank()) {
                                return true;
                            }
                            try {
                                return java.time.LocalDate.parse(deadline).isAfter(today) ||
                                        java.time.LocalDate.parse(deadline).isEqual(today);
                            } catch (Exception ex) {
                                return true;
                            }
                        })
                        .map(SalesTarget::getSoftwareId)
                        .filter(id -> id != null && !id.isBlank())
                        .collect(java.util.stream.Collectors.toSet());
                List<MarketingAnalysisResult> data = analysis.stream()
                        .filter(item -> activeTargetIds.contains(item.productId()))
                        .toList();
                Platform.runLater(() -> {
                    analysisItems.setAll(data);
                    statusLabel.setText("Productos analizados: " + data.size());
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> statusLabel.setText(resolveError(ex)));
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("No se pudo cargar MIP."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void showDetails(MarketingAnalysisResult result) {
        detailsTitle.setText("Diagnóstico: " + result.productName());
        detailsExplanation.setText(result.explanation() != null ? result.explanation() : "-");
        if (result.suggestedActions() != null && !result.suggestedActions().isEmpty()) {
            actionsList.setItems(FXCollections.observableArrayList(result.suggestedActions()));
        } else {
            actionsList.setItems(FXCollections.observableArrayList("Sin acciones sugeridas."));
        }
    }

    private <T> TableColumn<MarketingAnalysisResult, T> column(String title, String property) {
        TableColumn<MarketingAnalysisResult, T> column = new TableColumn<>(title);
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
