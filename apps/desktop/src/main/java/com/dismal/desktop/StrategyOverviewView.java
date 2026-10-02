package com.dismal.desktop;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

public class StrategyOverviewView {

    private final BorderPane root;
    private final ApiClient apiClient;
    private final Label statusLabel;
    private final Button refreshButton;

    private final Label alertCountLabel = new Label("-");
    private final Label highCountLabel = new Label("-");
    private final Label targetsCountLabel = new Label("-");
    private final Label campaignsCountLabel = new Label("-");
    private final ObservableList<String> focusItems = FXCollections.observableArrayList();
    private final ListView<String> focusList = new ListView<>(focusItems);

    public StrategyOverviewView(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.root = new BorderPane();
        this.statusLabel = new Label();
        this.refreshButton = new Button("Actualizar");

        root.getStyleClass().add("content-root");
        root.setPadding(new Insets(16));

        Label title = new Label("Estrategia");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Resumen ejecutivo enfocado en el cliente final.");
        subtitle.getStyleClass().add("page-subtitle");

        refreshButton.getStyleClass().add("button-ghost");
        refreshButton.setOnAction(event -> loadData());

        ToolBar toolBar = new ToolBar(title, refreshButton);
        toolBar.getStyleClass().add("toolbar");

        VBox header = new VBox(4, toolBar, subtitle);

        HBox metrics = new HBox(12,
                metricCard("Alertas MIP", alertCountLabel, "Productos con riesgo comercial."),
                metricCard("Prioridad alta", highCountLabel, "Acciones inmediatas sugeridas."),
                metricCard("Metas activas", targetsCountLabel, "Productos con objetivos vigentes."),
                metricCard("Campañas", campaignsCountLabel, "Acciones de marketing en curso.")
        );
        metrics.setAlignment(Pos.CENTER_LEFT);

        Label focusTitle = new Label("Enfoque recomendado");
        focusTitle.getStyleClass().add("section-title");
        focusList.setPlaceholder(new Label("Sin alertas relevantes."));

        VBox focusPanel = new VBox(8, focusTitle, focusList);
        focusPanel.getStyleClass().add("panel");
        VBox.setVgrow(focusList, Priority.ALWAYS);

        VBox content = new VBox(14, metrics, focusPanel);
        VBox.setVgrow(focusPanel, Priority.ALWAYS);

        statusLabel.getStyleClass().add("page-subtitle");
        statusLabel.setPadding(new Insets(8, 0, 0, 0));

        root.setTop(header);
        root.setCenter(content);
        root.setBottom(statusLabel);

        loadData();
    }

    public Parent getRoot() {
        return root;
    }

    private VBox metricCard(String title, Label valueLabel, String help) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("help-text");
        valueLabel.getStyleClass().add("metric-value");
        Label helpLabel = new Label(help);
        helpLabel.getStyleClass().add("help-text");
        VBox box = new VBox(6, titleLabel, valueLabel, helpLabel);
        box.getStyleClass().add("panel");
        box.setMinWidth(180);
        return box;
    }

    private void loadData() {
        setLoading(true);
        statusLabel.setText("Cargando estrategia...");
        Thread worker = new Thread(() -> {
            try {
                List<MarketingAnalysisResult> analysis = apiClient.getProductAnalysis();
                PageResponse<DashboardTarget> targets = apiClient.getDashboardTargets("profit", 0, 20);
                PageResponse<Campaign> campaigns = apiClient.listCampaigns("", 0, 50);

                long alerts = analysis.stream()
                        .filter(item -> "ALTA".equalsIgnoreCase(item.priority())
                                || "ALERTA_COMERCIAL".equalsIgnoreCase(item.state()))
                        .count();
                long high = analysis.stream()
                        .filter(item -> "ALTA".equalsIgnoreCase(item.priority()))
                        .count();
                List<String> focus = analysis.stream()
                        .filter(item -> "ALTA".equalsIgnoreCase(item.priority())
                                || "ALERTA_COMERCIAL".equalsIgnoreCase(item.state()))
                        .limit(6)
                        .map(item -> item.productName() + " · " + formatMipState(item.state()))
                        .toList();

                Platform.runLater(() -> {
                    alertCountLabel.setText(String.valueOf(alerts));
                    highCountLabel.setText(String.valueOf(high));
                    targetsCountLabel.setText(String.valueOf(targets.totalElements()));
                    campaignsCountLabel.setText(String.valueOf(campaigns.totalElements()));
                    focusItems.setAll(focus);
                    statusLabel.setText("Resumen actualizado.");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("No se pudo cargar estrategia."));
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

    private String formatMipState(String state) {
        if (state == null) return "-";
        return switch (state) {
            case "RENTABLE" -> "Rentable";
            case "BAJA_TRACCION" -> "Baja tracción";
            case "ALERTA_COMERCIAL" -> "Alerta comercial";
            case "PAUSADO" -> "Pausado";
            default -> state;
        };
    }
}
