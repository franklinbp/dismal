package com.dismal.desktop;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class PlaceholderView {

    private final VBox root;

    public PlaceholderView(String titleText, String description) {
        root = new VBox(8);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: rgba(255,255,255,0.03);");

        Label title = new Label(titleText);
        title.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 18; -fx-font-weight: 600;");

        Label body = new Label(description);
        body.setStyle("-fx-text-fill: #94a3b8;");

        root.getChildren().addAll(title, body);
    }

    public Parent getRoot() {
        return root;
    }
}
