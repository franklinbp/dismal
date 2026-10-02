package com.dismal.desktop;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class DashboardView {

    private final BorderPane root;

    public DashboardView(UserMe user, Runnable onLogout) {
        root = new BorderPane();
        root.setStyle("-fx-background-color: #0b1120;");

        HBox topBar = new HBox(12);
        topBar.setPadding(new Insets(18, 24, 18, 24));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: rgba(15,23,42,0.85);");

        Label title = new Label("Dismal Admin Desktop");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: 700; -fx-text-fill: #f8fafc;");

        Label userLabel = new Label(user.email() + " (" + user.role() + ")");
        userLabel.setStyle("-fx-text-fill: #cbd5f5;");

        Button logout = new Button("Salir");
        logout.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #f8fafc; -fx-background-radius: 14;");
        logout.setOnAction(event -> onLogout.run());

        topBar.getChildren().addAll(title, userLabel, logout);

        VBox content = new VBox(16);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: rgba(255,255,255,0.04);");

        Label welcome = new Label("Dashboard de administracion");
        welcome.setStyle("-fx-font-size: 20; -fx-text-fill: #e2e8f0; -fx-font-weight: 600;");

        Label hint = new Label("Conecta los modulos de ventas, reportes e integraciones aqui.");
        hint.setStyle("-fx-text-fill: #94a3b8;");

        content.getChildren().addAll(welcome, hint, placeholderCard("Ventas"), placeholderCard("Reportes"), placeholderCard("Integraciones"));

        root.setTop(topBar);
        root.setCenter(content);
    }

    public Parent getRoot() {
        return root;
    }

    private VBox placeholderCard(String title) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: rgba(15,23,42,0.85); -fx-background-radius: 16;");
        Label heading = new Label(title);
        heading.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 14; -fx-font-weight: 600;");
        Label body = new Label("Modulo pendiente de conectar con API");
        body.setStyle("-fx-text-fill: #94a3b8;");
        card.getChildren().addAll(heading, body);
        return card;
    }
}
