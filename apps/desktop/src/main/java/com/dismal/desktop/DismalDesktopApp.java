package com.dismal.desktop;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.dismal.desktop")
public class DismalDesktopApp extends Application {

    private ConfigurableApplicationContext springContext;
    private final TokenStore tokenStore = new TokenStore();

    @Override
    public void init() {
        String[] args = getParameters().getRaw().toArray(new String[0]);
        this.springContext = new SpringApplicationBuilder(DismalDesktopApp.class)
                .headless(false) // Allow UI
                .run(args);
    }

    @Override
    public void stop() {
        if (springContext != null) {
            springContext.close();
        }
    }

    @Override
    public void start(Stage stage) {
        // Get ApiClient from Spring Context or create one if needed, 
        // but for now we keep the hybrid approach until full refactor.
        // ideally: ApiClient apiClient = springContext.getBean(ApiClient.class);
        ApiClient apiClient = new ApiClient(); // Temporary: still using direct instance for non-spring beans compatibility

        stage.setTitle("Dismal Desktop Control Center");
        stage.setMinWidth(960);
        stage.setMinHeight(600);
        showLoading(stage);
        tryAutoLogin(stage, apiClient);
        stage.show();
    }

    private void showLoading(Stage stage) {
        LoadingView loadingView = new LoadingView();
        stage.setScene(createScene(loadingView.getRoot(), 980, 620));
    }

    private void showLogin(Stage stage, ApiClient apiClient) {
        LoginView loginView = new LoginView(apiClient, outcome -> {
            if (!outcome.result().isAllowed()) {
                LoginView.showAlert("Access denied", "User does not have admin access.");
                return;
            }
            apiClient.setToken(outcome.result().token());
            tokenStore.saveToken(outcome.result().token(), outcome.remember());
            showDashboard(stage, outcome.result().user(), apiClient);
        });
        stage.setScene(createScene(loginView.getRoot(), 980, 620));
    }

    private void showDashboard(Stage stage, UserMe user, ApiClient apiClient) {
        // Here we pass the Spring Context to the dashboard so it can access Repositories/SyncService
        AdminShellView dashboardView = new AdminShellView(apiClient, user, springContext, () -> {
            apiClient.clearToken();
            tokenStore.clear();
            showLogin(stage, apiClient);
        });
        stage.setScene(createScene(dashboardView.getRoot(), 1180, 720));
    }

    private Scene createScene(javafx.scene.Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        root.getStyleClass().add("app-root");
        var stylesheet = getClass().getResource("/styles/dismal.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
        return scene;
    }

    private void tryAutoLogin(Stage stage, ApiClient apiClient) {
        String savedToken = tokenStore.loadToken();
        if (savedToken == null) {
            showLogin(stage, apiClient);
            return;
        }
        apiClient.setToken(savedToken);
        Thread worker = new Thread(() -> {
            try {
                UserMe user = apiClient.fetchMe();
                Platform.runLater(() -> showDashboard(stage, user, apiClient));
            } catch (Exception ex) {
                apiClient.clearToken();
                tokenStore.clear();
                Platform.runLater(() -> showLogin(stage, apiClient));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
