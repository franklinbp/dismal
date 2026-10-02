package com.dismal.desktop;

import com.dismal.desktop.service.SyncService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AdminShellView {

    private final BorderPane root;
    private final VBox content;
    private final Map<String, Parent> screens = new LinkedHashMap<>();
    private final Map<String, Button> navButtons = new HashMap<>();
    private final ConfigurableApplicationContext springContext;
    private final Label syncStatusLabel = new Label("Sync listo");
    private final AtomicBoolean syncRunning = new AtomicBoolean(false);
    private final ScheduledExecutorService autoSyncExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "dismal-desktop-auto-sync");
        t.setDaemon(true);
        return t;
    });

    public AdminShellView(ApiClient apiClient, UserMe user, ConfigurableApplicationContext springContext, Runnable onLogout) {
        this.springContext = springContext;
        boolean operatorOnly = "OPERATOR".equals(user.role());
        root = new BorderPane();
        root.getStyleClass().add("app-root");

        HBox topBar = new HBox(16);
        topBar.getStyleClass().add("topbar");
        topBar.setAlignment(Pos.CENTER_LEFT);

        HBox brand = new HBox(10);
        brand.setAlignment(Pos.CENTER_LEFT);
        ImageView logo = createLogo(28);
        Label title = new Label("Dismal Admin");
        title.getStyleClass().add("app-title");
        Label brandMeta = new Label("Control operativo central");
        brandMeta.getStyleClass().add("shell-brand-meta");
        VBox brandCopy = new VBox(2, title, brandMeta);
        if (logo != null) {
            brand.getChildren().addAll(logo, brandCopy);
        } else {
            brand.getChildren().add(brandCopy);
        }

        Label userLabel = new Label(user.email());
        userLabel.getStyleClass().add("topbar-user");

        Label roleLabel = new Label(user.role());
        roleLabel.getStyleClass().add("pill");

        syncStatusLabel.getStyleClass().add("sync-status");

        Button syncButton = new Button("Sync");
        syncButton.getStyleClass().add("button-primary");
        syncButton.setStyle("-fx-font-size: 12px;");
        syncButton.setOnAction(event -> runSync(apiClient, syncButton, false));

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button logoutButton = new Button("Cerrar sesion");
        logoutButton.getStyleClass().add("button-ghost");
        logoutButton.setStyle("-fx-font-size: 12px;");
        logoutButton.setOnAction(event -> {
            autoSyncExecutor.shutdownNow();
            onLogout.run();
        });

        topBar.getChildren().addAll(brand, userLabel, roleLabel, spacer, syncStatusLabel, syncButton, logoutButton);

        Accordion accordion = new Accordion();
        accordion.getStyleClass().add("sidebar");

        content = new VBox();
        content.getStyleClass().add("content-root");
        content.setPadding(new Insets(24));

        if (operatorOnly) {
            screens.put("Integraciones", new IntegrationsView(apiClient, false).getRoot());
            accordion.getPanes().add(createNavGroup("Integraciones", List.of("Integraciones")));
            setScreen("Integraciones");
        } else {
            var localLicenseRepo = springContext.getBean(com.dismal.desktop.local.repository.LocalLicenseRepository.class);
            var localSalesTargetRepo = springContext.getBean(com.dismal.desktop.local.repository.LocalSalesTargetRepository.class);
            SalesView salesView = new SalesView(apiClient, () -> {});

            screens.put("Dashboard", new DashboardHomeView(
                    apiClient,
                    () -> {
                        setScreen("Ventas");
                        salesView.startNewSale();
                    },
                    () -> {
                        setScreen("Ventas");
                        salesView.startNewSaleWithNewCustomer();
                    }
            ).getRoot());
            screens.put("Ventas", salesView.getRoot());
            screens.put("AR", new ArView(apiClient).getRoot());
            screens.put("Gastos y pagos", new ExpensesView(apiClient).getRoot());
            screens.put("Productos", new ProductsView(apiClient).getRoot());
            screens.put("Gestion Claves", new LicensesView(apiClient, localLicenseRepo).getRoot());
            screens.put("Stock", new InventoryView(apiClient).getRoot());
            screens.put("Usuarios", new UsersView(apiClient, user).getRoot());
            screens.put("Facturas", new InvoicesView(apiClient, () -> setScreen("Ventas")).getRoot());
            screens.put("Cotizaciones", new QuotesView(apiClient).getRoot());
            screens.put("Reportes", new ReportsView(apiClient).getRoot());
            screens.put("Marketing", new CampaignsView(apiClient).getRoot());
            screens.put("Planeacion", new PlanningTargetsView(apiClient, localSalesTargetRepo).getRoot());
            screens.put("Integraciones", new IntegrationsView(apiClient, true).getRoot());
            screens.put("Metas", new TargetsOverviewView(apiClient).getRoot());
            screens.put("Estrategia", new StrategyOverviewView(apiClient).getRoot());
            screens.put("Inteligencia MIP", new MipView(apiClient).getRoot());
            screens.put("Acciones", new StrategyActionsView(apiClient).getRoot());

            TitledPane mainGroup = createNavGroup(
                    "Operaciones",
                    List.of("Dashboard", "Ventas", "Facturas", "Cotizaciones", "AR", "Gastos y pagos")
            );
            accordion.getPanes().add(mainGroup);
            accordion.getPanes().add(createNavGroup("Catalogo", List.of("Productos", "Stock", "Gestion Claves")));
            accordion.getPanes().add(createNavGroup("Estrategia", List.of("Estrategia", "Acciones", "Marketing", "Metas", "Planeacion", "Inteligencia MIP")));
            accordion.getPanes().add(createNavGroup("Administracion", List.of("Usuarios", "Integraciones")));
            accordion.getPanes().add(createNavGroup("Analisis", List.of("Reportes")));

            accordion.setExpandedPane(mainGroup);
            setScreen("Dashboard");
        }

        VBox sidebarHeader = new VBox(6);
        sidebarHeader.getStyleClass().add("sidebar-header");
        ImageView sideLogo = createLogo(48);
        Label sideTitle = new Label("Dismal");
        sideTitle.getStyleClass().add("sidebar-title");
        Label sideSubtitle = new Label("Operacion comercial y seguimiento");
        sideSubtitle.getStyleClass().add("sidebar-subtitle");
        if (sideLogo != null) {
            sidebarHeader.getChildren().addAll(sideLogo, sideTitle, sideSubtitle);
        } else {
            sidebarHeader.getChildren().addAll(sideTitle, sideSubtitle);
        }

        VBox sidebarContainer = new VBox(sidebarHeader, accordion);
        sidebarContainer.getStyleClass().add("sidebar");

        root.setTop(topBar);
        root.setLeft(sidebarContainer);
        root.setCenter(content);

        if (!operatorOnly) {
            scheduleAutoSync(apiClient, syncButton);
            runSync(apiClient, syncButton, true);
        }
    }

    public Parent getRoot() {
        return root;
    }

    private void scheduleAutoSync(ApiClient apiClient, Button button) {
        autoSyncExecutor.scheduleWithFixedDelay(
                () -> runSync(apiClient, button, true),
                5,
                5,
                TimeUnit.MINUTES
        );
    }

    private void runSync(ApiClient apiClient, Button button, boolean backgroundSync) {
        if (springContext == null || !syncRunning.compareAndSet(false, true)) {
            return;
        }
        Platform.runLater(() -> {
            button.setDisable(true);
            button.setText(backgroundSync ? "Auto" : "...");
            setSyncStatus("Sincronizando...", "sync-status-busy");
        });

        Thread worker = new Thread(() -> {
            try {
                SyncService syncService = springContext.getBean(SyncService.class);
                syncService.syncAll(apiClient);
                Platform.runLater(() -> {
                    button.setText("OK");
                    setSyncStatus("Sincronizado", "sync-status-ok");
                    new java.util.Timer(true).schedule(new java.util.TimerTask() {
                        @Override
                        public void run() {
                            Platform.runLater(() -> {
                                button.setText("Sync");
                                setSyncStatus("Sync listo", null);
                            });
                        }
                    }, 1800);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    button.setText("Error");
                    setSyncStatus("Sync con error", "sync-status-error");
                });
            } finally {
                syncRunning.set(false);
                Platform.runLater(() -> button.setDisable(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void setSyncStatus(String text, String stateClass) {
        syncStatusLabel.setText(text);
        syncStatusLabel.getStyleClass().removeAll("sync-status-ok", "sync-status-busy", "sync-status-error");
        if (stateClass != null) {
            syncStatusLabel.getStyleClass().add(stateClass);
        }
    }

    private void setScreen(String key) {
        content.getChildren().setAll(screens.get(key));
        navButtons.forEach((label, button) -> {
            button.getStyleClass().remove("nav-button-active");
            if (label.equals(key)) {
                button.getStyleClass().add("nav-button-active");
            }
        });
    }

    private TitledPane createNavGroup(String title, List<String> items) {
        VBox box = new VBox(0);
        for (String item : items) {
            Button button = new Button(item);
            button.setMaxWidth(Double.MAX_VALUE);
            button.getStyleClass().add("nav-button");
            button.setOnAction(event -> setScreen(item));
            navButtons.put(item, button);
            box.getChildren().add(button);
        }
        TitledPane pane = new TitledPane(title, box);
        pane.setAnimated(true);
        return pane;
    }

    private ImageView createLogo(double size) {
        try (InputStream stream = getClass().getResourceAsStream("/images/mi_logo.png")) {
            if (stream == null) {
                return null;
            }
            Image image = new Image(stream);
            ImageView view = new ImageView(image);
            view.setFitHeight(size);
            view.setPreserveRatio(true);
            view.getStyleClass().add("logo-img");
            return view;
        } catch (Exception ex) {
            return null;
        }
    }
}
