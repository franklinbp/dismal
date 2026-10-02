package com.dismal.desktop;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;

public class ReportsView {

    private final BorderPane root;
    private final ApiClient apiClient;
    private final ObservableList<SalesPeriodPoint> periodPoints;
    private final ObservableList<SalesVsTargetItem> targetItems;
    private final Label periodSummary;
    private final Label targetSummary;
    private final DatePicker fromDate;
    private final DatePicker toDate;
    private final Button dailyButton;
    private final Button weeklyButton;
    private final Button monthlyButton;
    private final Button vsTargetsButton;

    public ReportsView(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.root = new BorderPane();
        this.periodPoints = FXCollections.observableArrayList();
        this.targetItems = FXCollections.observableArrayList();
        this.periodSummary = new Label();
        this.targetSummary = new Label();
        this.fromDate = new DatePicker(LocalDate.now().minusDays(30));
        this.toDate = new DatePicker(LocalDate.now());

        root.setPadding(new Insets(16));
        root.getStyleClass().add("content-root");

        Label title = new Label("Reportes");
        title.getStyleClass().add("page-title");

        dailyButton = new Button("Diario");
        weeklyButton = new Button("Semanal");
        monthlyButton = new Button("Mensual");
        vsTargetsButton = new Button("Vs Metas");
        dailyButton.getStyleClass().add("button-ghost");
        weeklyButton.getStyleClass().add("button-ghost");
        monthlyButton.getStyleClass().add("button-ghost");
        vsTargetsButton.getStyleClass().add("button-ghost");

        dailyButton.setOnAction(event -> loadPeriod("daily"));
        weeklyButton.setOnAction(event -> loadPeriod("weekly"));
        monthlyButton.setOnAction(event -> loadPeriod("monthly"));
        vsTargetsButton.setOnAction(event -> loadVsTargets());

        ToolBar toolBar = new ToolBar(title, dailyButton, weeklyButton, monthlyButton, vsTargetsButton);
        toolBar.getStyleClass().add("toolbar");

        HBox dateRow = new HBox(8, new Label("Desde"), fromDate, new Label("Hasta"), toDate);
        dateRow.getStyleClass().add("page-subtitle");
        dateRow.setPadding(new Insets(8, 0, 12, 0));

        TableView<SalesPeriodPoint> periodTable = new TableView<>(periodPoints);
        periodTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        periodTable.getColumns().addAll(
                column("Periodo", "period"),
                column("Total", "total"),
                column("Conteo", "count")
        );

        periodSummary.getStyleClass().add("page-subtitle");

        TableView<SalesVsTargetItem> targetTable = new TableView<>(targetItems);
        targetTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        targetTable.getColumns().addAll(
                columnTarget("Producto", "softwareName"),
                columnTarget("Meta", "metaUnits"),
                columnTarget("Actual", "actualUnits"),
                columnTarget("Var", "varianceUnits"),
                columnTarget("Lograda", "achieved")
        );

        targetSummary.getStyleClass().add("page-subtitle");

        VBox content = new VBox(12, dateRow, new Label("Ventas por periodo"), periodTable, periodSummary,
                new Label("Ventas vs metas"), targetTable, targetSummary);
        content.setPadding(new Insets(8));
        content.getStyleClass().add("content-root");

        root.setTop(toolBar);
        root.setCenter(content);

        loadPeriod("daily");
        loadVsTargets();
    }

    public Parent getRoot() {
        return root;
    }

    private void loadPeriod(String period) {
        setLoading(true);
        periodSummary.setText("Cargando...");
        Thread worker = new Thread(() -> {
            try {
                SalesPeriodReport report = apiClient.getSalesPeriodReport(period, fromDate.getValue(), toDate.getValue());
                Platform.runLater(() -> {
                    periodPoints.setAll(report.points());
                    periodSummary.setText("Total: " + report.totalCount() + " · Monto: " + FormatUtils.currency(report.totalAmount()));
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> periodSummary.setText(resolveError(ex)));
            } catch (Exception ex) {
                Platform.runLater(() -> periodSummary.setText("No se pudo cargar reporte."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void loadVsTargets() {
        setLoading(true);
        targetSummary.setText("Cargando...");
        Thread worker = new Thread(() -> {
            try {
                SalesVsTargetReport report = apiClient.getSalesVsTargets(fromDate.getValue(), toDate.getValue());
                Platform.runLater(() -> {
                    targetItems.setAll(report.items());
                    targetSummary.setText("Metas: " + report.totalTargets() + " · Logradas: " + report.achievedTargets());
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> targetSummary.setText(resolveError(ex)));
            } catch (Exception ex) {
                Platform.runLater(() -> targetSummary.setText("No se pudo cargar VS metas."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private <T> TableColumn<SalesPeriodPoint, T> column(String title, String property) {
        TableColumn<SalesPeriodPoint, T> column = new TableColumn<>(title);
        FxTable.bindColumn(column, property);
        return column;
    }

    private <T> TableColumn<SalesVsTargetItem, T> columnTarget(String title, String property) {
        TableColumn<SalesVsTargetItem, T> column = new TableColumn<>(title);
        FxTable.bindColumn(column, property);
        return column;
    }

    private void setLoading(boolean loading) {
        dailyButton.setDisable(loading);
        weeklyButton.setDisable(loading);
        monthlyButton.setDisable(loading);
        vsTargetsButton.setDisable(loading);
    }

    private String resolveError(ApiException ex) {
        if (ex.getStatus() == 401 || ex.getStatus() == 403) {
            return "Acceso denegado.";
        }
        return ex.getMessage();
    }
}
