package com.dismal.desktop;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

public class LoadingView {

    private final BorderPane root;

    public LoadingView() {
        root = new BorderPane();
        root.getStyleClass().add("content-root");
        Label label = new Label("Cargando...");
        label.getStyleClass().add("page-title");
        BorderPane.setAlignment(label, Pos.CENTER);
        BorderPane.setMargin(label, new Insets(24));
        root.setCenter(label);
    }

    public Parent getRoot() {
        return root;
    }
}
