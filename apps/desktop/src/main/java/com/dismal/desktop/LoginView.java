package com.dismal.desktop;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.InputStream;
import java.util.function.Consumer;

public class LoginView {

    private final BorderPane root;
    private final Label errorLabel;
    private final TextField emailField;
    private final PasswordField passwordField;
    private final Button submitButton;
    private final CheckBox rememberBox;

    public LoginView(ApiClient apiClient, Consumer<LoginOutcome> onSuccess) {
        root = new BorderPane();
        root.getStyleClass().add("login-shell");

        VBox card = new VBox(14);
        card.setMaxWidth(420);
        card.getStyleClass().add("login-card");
        card.setAlignment(Pos.CENTER_LEFT);

        VBox hero = new VBox(14);
        hero.setPrefWidth(420);
        hero.getStyleClass().add("login-hero");
        hero.setAlignment(Pos.TOP_LEFT);

        ImageView logo = createLogo(56);

        Label heroEyebrow = new Label("OPERACION Y CONTROL");
        heroEyebrow.getStyleClass().add("eyebrow");

        Label heroTitle = new Label("Gestiona ventas, catalogo y operaciones desde un panel mas claro.");
        heroTitle.getStyleClass().add("login-hero-title");
        heroTitle.setWrapText(true);

        Label heroText = new Label("La app de escritorio concentra operacion, seguimiento y sincronizacion en un espacio de trabajo estable para administradores, managers y operadores.");
        heroText.getStyleClass().add("login-hero-text");
        heroText.setWrapText(true);

        if (logo != null) {
            hero.getChildren().add(logo);
        }
        hero.getChildren().addAll(heroEyebrow, heroTitle, heroText);

        Label title = new Label("Acceso a Dismal Desktop");
        title.getStyleClass().add("login-title");
        Label subtitle = new Label("Ingresa con una cuenta autorizada para continuar.");
        subtitle.getStyleClass().add("page-subtitle");

        emailField = new TextField();
        emailField.setPromptText("Email");
        emailField.getStyleClass().add("text-field");

        passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.getStyleClass().add("password-field");

        rememberBox = new CheckBox("Recordarme en este dispositivo");
        rememberBox.getStyleClass().add("page-subtitle");

        errorLabel = new Label();
        errorLabel.getStyleClass().add("notice-error");

        submitButton = new Button("Ingresar");
        submitButton.setDefaultButton(true);
        submitButton.getStyleClass().add("button-primary");
        submitButton.setMaxWidth(Double.MAX_VALUE);
        submitButton.setOnAction(event -> handleLogin(apiClient, onSuccess));

        card.getChildren().addAll(title, subtitle, emailField, passwordField, rememberBox, errorLabel, submitButton);

        Region spacer = new Region();
        spacer.setMinWidth(0);
        HBox.setHgrow(hero, Priority.ALWAYS);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox centered = new HBox(24, hero, card, spacer);
        centered.setAlignment(Pos.CENTER);
        centered.setPadding(new Insets(36));

        BorderPane.setAlignment(centered, Pos.CENTER);
        root.setCenter(centered);
    }

    public Parent getRoot() {
        return root;
    }

    private void handleLogin(ApiClient apiClient, Consumer<LoginOutcome> onSuccess) {
        setLoading(true);
        errorLabel.setText("");
        String email = emailField.getText();
        String password = passwordField.getText();
        boolean remember = rememberBox.isSelected();

        Thread worker = new Thread(() -> {
            try {
                LoginResult result = apiClient.login(email, password);
                Platform.runLater(() -> onSuccess.accept(new LoginOutcome(result, remember)));
            } catch (ApiException ex) {
                Platform.runLater(() -> errorLabel.setText(resolveErrorMessage(ex)));
            } catch (Exception ex) {
                Platform.runLater(() -> errorLabel.setText("No se pudo iniciar sesion."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void setLoading(boolean loading) {
        submitButton.setDisable(loading);
        submitButton.setText(loading ? "Ingresando..." : "Ingresar");
    }

    private String resolveErrorMessage(ApiException ex) {
        if (ex.getStatus() == 401 || ex.getStatus() == 403) {
            return "Credenciales invalidas.";
        }
        return ex.getMessage();
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
        } catch (Exception e) {
            return null;
        }
    }

    public static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
