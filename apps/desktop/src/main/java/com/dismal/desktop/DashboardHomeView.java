package com.dismal.desktop;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.stream.Collectors;

public class DashboardHomeView {

    private final ScrollPane scrollPane;
    private final VBox root;
    private final Label statusLabel;
    private final Runnable onNewSale;
    private final Runnable onNewCustomerSale;

    private final ObservableList<RecentSale> recentSalesList = FXCollections.observableArrayList();
    private final ObservableList<ArItem> overdueArList = FXCollections.observableArrayList();
    private final ObservableList<ArItem> openArList = FXCollections.observableArrayList();
    private final ObservableList<DashboardTarget> targetList = FXCollections.observableArrayList();
    private final ObservableList<OutboxItem> outboxFailedList = FXCollections.observableArrayList();
    private final ObservableList<MarketingAnalysisResult> mipAlertsList = FXCollections.observableArrayList();
    private final BarChart<String, Number> monthlySalesChart = createMonthlySalesChart();

    public DashboardHomeView(ApiClient apiClient, Runnable onNewSale, Runnable onNewCustomerSale) {
        this.onNewSale = onNewSale != null ? onNewSale : () -> {};
        this.onNewCustomerSale = onNewCustomerSale != null ? onNewCustomerSale : () -> {};
        root = new VBox(20);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("content-root");
        root.setFillWidth(true);

        VBox hero = new VBox(8);
        hero.getStyleClass().add("dashboard-hero");
        Label eyebrow = new Label("VISION GENERAL");
        eyebrow.getStyleClass().add("eyebrow");
        Label title = new Label("Resumen Ejecutivo");
        title.getStyleClass().add("dashboard-hero-title");
        Label subtitle = new Label("Indicadores clave, alertas y actividad reciente en una sola vista para decisiones mas rapidas.");
        subtitle.getStyleClass().add("dashboard-hero-subtitle");
        subtitle.setWrapText(true);
        Button newSaleButton = new Button("Nueva venta");
        newSaleButton.getStyleClass().add("button-primary");
        newSaleButton.setOnAction(event -> this.onNewSale.run());

        Button newCustomerButton = new Button("Crear nuevo cliente");
        newCustomerButton.getStyleClass().add("button-ghost");
        newCustomerButton.setOnAction(event -> this.onNewCustomerSale.run());

        HBox actionRow = new HBox(10, newSaleButton, newCustomerButton);
        hero.getChildren().addAll(eyebrow, title, subtitle, actionRow);

        statusLabel = new Label("Cargando datos maestros...");
        statusLabel.getStyleClass().add("page-subtitle");

        HBox summaryRow = new HBox(16);
        summaryRow.setId("summaryRow");

        HBox row1 = new HBox(20);
        VBox recentSalesBox = createTableSection("Ventas recientes", createRecentSalesTable());
        VBox overdueArBox = createTableSection("AR vencido", createArTable(overdueArList));
        VBox openArBox = createTableSection("AR abierto", createArTable(openArList));
        HBox.setHgrow(recentSalesBox, Priority.ALWAYS);
        HBox.setHgrow(overdueArBox, Priority.ALWAYS);
        HBox.setHgrow(openArBox, Priority.ALWAYS);
        row1.getChildren().addAll(recentSalesBox, overdueArBox, openArBox);

        HBox row2 = new HBox(20);
        VBox targetsBox = createTableSection("Metas de venta", createTargetsTable());
        VBox outboxBox = createTableSection("Outbox fallidos", createOutboxTable());
        HBox.setHgrow(targetsBox, Priority.ALWAYS);
        HBox.setHgrow(outboxBox, Priority.ALWAYS);
        row2.getChildren().addAll(targetsBox, outboxBox);

        VBox mipBox = createTableSection("Alertas comerciales inteligentes", createMipAlertsTable());
        mipBox.setMaxWidth(Double.MAX_VALUE);

        Label chartTitle = new Label("Ventas mensuales");
        chartTitle.getStyleClass().add("chart-title");
        Label chartSubtitle = new Label("Tendencia del ano fiscal actual.");
        chartSubtitle.getStyleClass().add("chart-subtitle");
        VBox chartBox = new VBox(6, chartTitle, chartSubtitle, monthlySalesChart);
        chartBox.getStyleClass().add("section-card");
        monthlySalesChart.setMinHeight(240);

        root.getChildren().addAll(hero, statusLabel, summaryRow, chartBox, row1, row2, mipBox);

        scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        Thread worker = new Thread(() -> {
            try {
                DashboardSummary summary = apiClient.getDashboardSummary();
                PageResponse<RecentSale> recent = apiClient.getRecentSales(0, 10);
                PageResponse<ArItem> arOverdue = apiClient.getArItems("OVERDUE", 0, 8);
                PageResponse<ArItem> arOpen = apiClient.getArItems("OPEN", 0, 8);
                PageResponse<DashboardTarget> targets = apiClient.getDashboardTargets("profit", 0, 8);
                PageResponse<OutboxItem> outboxFailed = apiClient.getDashboardOutbox("FAILED", 0, 8);
                java.util.Map<String, Double> monthlySales = apiClient.getMonthlySales(java.time.LocalDate.now().getYear());

                List<MarketingAnalysisResult> allAnalysis = apiClient.getProductAnalysis();
                List<MarketingAnalysisResult> highPriorityAlerts = allAnalysis.stream()
                        .filter(res -> "ALTA".equals(res.priority()) || "ALERTA_COMERCIAL".equals(res.state()))
                        .collect(Collectors.toList());

                Platform.runLater(() -> {
                    renderSummary(summary);
                    recentSalesList.setAll(recent.content());
                    overdueArList.setAll(arOverdue.content());
                    openArList.setAll(arOpen.content());
                    targetList.setAll(targets.content());
                    outboxFailedList.setAll(outboxFailed.content());
                    mipAlertsList.setAll(highPriorityAlerts);
                    renderMonthlySales(monthlySales);
                    statusLabel.setText("Actualizado: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
                });
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Error de conexion: " + ex.getMessage()));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    public Parent getRoot() {
        return scrollPane;
    }

    private VBox createTableSection(String title, TableView<?> table) {
        VBox box = new VBox(10);
        box.getStyleClass().add("section-card");
        Label label = new Label(title);
        label.getStyleClass().add("section-title");
        table.setPrefHeight(250);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        box.getChildren().addAll(label, table);
        return box;
    }

    private TableView<RecentSale> createRecentSalesTable() {
        TableView<RecentSale> table = new TableView<>(recentSalesList);
        TableColumn<RecentSale, String> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(cell -> new SimpleStringProperty(FormatUtils.currency(cell.getValue().total())));
        table.getColumns().addAll(
                column("Cliente", "clientName"),
                column("Tipo", "saleType"),
                column("Estado", "status"),
                totalCol
        );
        return table;
    }

    private TableView<ArItem> createArTable(ObservableList<ArItem> data) {
        TableView<ArItem> table = new TableView<>(data);
        TableColumn<ArItem, String> balanceCol = new TableColumn<>("Balance");
        balanceCol.setCellValueFactory(cell -> new SimpleStringProperty(FormatUtils.currency(cell.getValue().balance())));
        table.getColumns().addAll(
                column("Cliente", "clientName"),
                column("Vence", "dueDate"),
                balanceCol
        );
        return table;
    }

    private TableView<DashboardTarget> createTargetsTable() {
        TableView<DashboardTarget> table = new TableView<>(targetList);
        table.getColumns().addAll(
                columnTarget("Producto", "productName"),
                columnTarget("Meta", "metaUnits"),
                columnTarget("Actual", "unitsSoldCurrent"),
                columnTarget("Margen", "marginUnit")
        );
        return table;
    }

    private TableView<OutboxItem> createOutboxTable() {
        TableView<OutboxItem> table = new TableView<>(outboxFailedList);
        table.getColumns().addAll(
                columnOutbox("Evento", "eventType"),
                columnOutbox("Estado", "status"),
                columnOutbox("Intentos", "attempts")
        );
        return table;
    }

    private TableView<MarketingAnalysisResult> createMipAlertsTable() {
        TableView<MarketingAnalysisResult> table = new TableView<>(mipAlertsList);
        table.getColumns().addAll(
                column("Producto", "productName"),
                column("Prioridad", "priority"),
                column("Diagnostico", "diagnosis"),
                column("Estado MIP", "state")
        );
        return table;
    }

    private void renderSummary(DashboardSummary summary) {
        HBox summaryRow = (HBox) root.lookup("#summaryRow");
        if (summaryRow == null) {
            return;
        }
        summaryRow.getChildren().clear();
        summaryRow.getChildren().addAll(
                summaryCard("Ventas hoy", summary.todaySalesCount(), summary.todaySalesAmount()),
                summaryRowSpacer(),
                summaryCard("Ventas del mes", summary.monthSalesCount(), summary.monthSalesAmount()),
                summaryRowSpacer(),
                summaryCard("Cobros vencidos", summary.overdueArCount(), summary.overdueArBalance())
        );
    }

    private BarChart<String, Number> createMonthlySalesChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCategoryGap(6);
        chart.setBarGap(2);
        return chart;
    }

    private void renderMonthlySales(java.util.Map<String, Double> data) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        data.forEach((month, total) -> {
            String label = month.substring(5);
            series.getData().add(new XYChart.Data<>(label, total));
        });
        monthlySalesChart.getData().setAll(series);
    }

    private VBox summaryCard(String label, long count, double amount) {
        VBox card = new VBox(4);
        card.getStyleClass().add("metric-card");
        card.setPadding(new Insets(15));
        card.setMinWidth(180);

        Label title = new Label(label.toUpperCase());
        title.getStyleClass().add("metric-label");

        Label val = new Label(String.valueOf(count));
        val.getStyleClass().add("metric-value");

        Label sub = new Label(amount != 0 ? FormatUtils.currency(amount) : "");
        sub.getStyleClass().add("metric-subvalue");

        card.getChildren().addAll(title, val, sub);
        return card;
    }

    private Region summaryRowSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private <S, T> TableColumn<S, T> column(String title, String property) {
        TableColumn<S, T> column = new TableColumn<>(title);
        FxTable.bindColumn(column, property);
        return column;
    }

    private <T> TableColumn<DashboardTarget, T> columnTarget(String title, String property) {
        TableColumn<DashboardTarget, T> column = new TableColumn<>(title);
        FxTable.bindColumn(column, property);
        return column;
    }

    private <T> TableColumn<OutboxItem, T> columnOutbox(String title, String property) {
        TableColumn<OutboxItem, T> column = new TableColumn<>(title);
        FxTable.bindColumn(column, property);
        return column;
    }
}
