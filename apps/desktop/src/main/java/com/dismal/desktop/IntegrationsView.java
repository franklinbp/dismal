package com.dismal.desktop;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class IntegrationsView {

    private final BorderPane root;
    private final ApiClient apiClient;
    private final ObservableList<OutboxItem> outboxItems;
    private final Label statusLabel;
    private final Button refreshButton;
    private final Button retryButton;
    private final Button prevButton;
    private final Button nextButton;
    private int page = 0;
    private int totalPages = 1;

    private final boolean canManageSettings;
    private final TextField webhookField;
    private final CheckBox dispatchEnabled;
    private final TextField rateField;
    private final TextField maxAttemptsField;
    private final PasswordField secretField;
    private final CheckBox smtpEnabled;
    private final TextField smtpHostField;
    private final TextField smtpPortField;
    private final TextField smtpUserField;
    private final PasswordField smtpPasswordField;
    private final CheckBox smtpTlsField;
    private final TextField smtpFromEmailField;
    private final TextField smtpFromNameField;
    private final TextField smtpTestToField;
    private final Button smtpTestButton;
    private final Button n8nTestButton;
    private final TextField n8nTestToField;
    private final TextArea n8nTestMessageField;
    private final Button quickGmailButton;
    private final Button quickN8nButton;
    private final Button quickWhatsAppTemplateButton;

    private final ObservableList<NotificationTemplate> templates;
    private final TableView<NotificationTemplate> templateTable;
    private final ComboBox<String> templateEventBox;
    private final ComboBox<String> templateChannelBox;
    private final TextField templateSubjectField;
    private final TextArea templateBodyField;
    private final CheckBox templateEnabledBox;
    private final Button templateSaveButton;
    private final Button templateNewButton;
    private final Button templateDeleteButton;
    private String editingTemplateId;

    private static final String[] EVENT_TYPES = {
            "SALE_CONFIRMED",
            "INVOICE_ISSUED",
            "PAYMENT_RECEIVED",
            "LICENSES_DELIVERED",
            "AR_OVERDUE",
            "SALE_CANCELLED",
            "CAMPAIGN_SCHEDULED",
            "CAMPAIGN_SENT"
    };

    public IntegrationsView(ApiClient apiClient, boolean canManageSettings) {
        this.apiClient = apiClient;
        this.canManageSettings = canManageSettings;
        this.root = new BorderPane();
        this.outboxItems = FXCollections.observableArrayList();
        this.templates = FXCollections.observableArrayList();
        this.statusLabel = new Label();

        root.setPadding(new Insets(16));
        root.getStyleClass().add("content-root");

        Label title = new Label("Integraciones");
        title.getStyleClass().add("page-title");

        refreshButton = new Button("Actualizar");
        refreshButton.getStyleClass().add("button-ghost");
        refreshButton.setOnAction(event -> loadOutbox());
        prevButton = new Button("Anterior");
        prevButton.getStyleClass().add("button-ghost");
        prevButton.setOnAction(event -> {
            if (page > 0) {
                page -= 1;
                loadOutbox();
            }
        });
        nextButton = new Button("Siguiente");
        nextButton.getStyleClass().add("button-ghost");
        nextButton.setOnAction(event -> {
            if (page + 1 < totalPages) {
                page += 1;
                loadOutbox();
            }
        });

        n8nTestButton = new Button("Test n8n");
        n8nTestButton.getStyleClass().add("button-ghost");
        n8nTestButton.setOnAction(event -> testN8n());
        n8nTestButton.setDisable(!canManageSettings);

        ToolBar toolBar = new ToolBar(title, refreshButton, prevButton, nextButton, n8nTestButton);
        toolBar.getStyleClass().add("toolbar");

        webhookField = new TextField();
        webhookField.setPromptText("https://n8n.tu-dominio/webhook/...");
        dispatchEnabled = new CheckBox("Despacho habilitado");
        rateField = new TextField();
        rateField.setPromptText("5000");
        maxAttemptsField = new TextField();
        maxAttemptsField.setPromptText("10");
        secretField = new PasswordField();
        secretField.setPromptText("Opcional");
        smtpEnabled = new CheckBox("Email SMTP habilitado");
        smtpHostField = new TextField();
        smtpHostField.setPromptText("smtp.gmail.com");
        smtpPortField = new TextField();
        smtpPortField.setPromptText("587");
        smtpUserField = new TextField();
        smtpUserField.setPromptText("usuario@correo.com");
        smtpPasswordField = new PasswordField();
        smtpTlsField = new CheckBox("TLS habilitado");
        smtpFromEmailField = new TextField();
        smtpFromEmailField.setPromptText("from@correo.com");
        smtpFromNameField = new TextField();
        smtpFromNameField.setPromptText("Dismal");
        smtpTestToField = new TextField();
        smtpTestToField.setPromptText("destino@correo.com");
        smtpTestButton = new Button("Probar email");
        smtpTestButton.getStyleClass().add("button-ghost");
        smtpTestButton.setOnAction(event -> testSmtp());

        n8nTestToField = new TextField();
        n8nTestToField.setPromptText("+593999000000 o correo@dominio.com");
        n8nTestMessageField = new TextArea();
        n8nTestMessageField.setPromptText("Mensaje de prueba para WhatsApp o Email...");
        n8nTestMessageField.setPrefRowCount(3);
        n8nTestToField.setDisable(!canManageSettings);
        n8nTestMessageField.setDisable(!canManageSettings);

        quickGmailButton = new Button("1. Gmail");
        quickGmailButton.getStyleClass().add("button-ghost");
        quickGmailButton.setDisable(!canManageSettings);
        quickGmailButton.setOnAction(event -> {
            applyGmailPreset();
            smtpEnabled.setSelected(true);
            saveSettings();
        });
        quickN8nButton = new Button("2. Test n8n");
        quickN8nButton.getStyleClass().add("button-ghost");
        quickN8nButton.setDisable(!canManageSettings);
        quickN8nButton.setOnAction(event -> testN8n());
        quickWhatsAppTemplateButton = new Button("3. Plantilla WhatsApp");
        quickWhatsAppTemplateButton.getStyleClass().add("button-ghost");
        quickWhatsAppTemplateButton.setDisable(!canManageSettings);
        quickWhatsAppTemplateButton.setOnAction(event -> ensureWhatsAppTemplate());

        GridPane settingsGrid = new GridPane();
        settingsGrid.setHgap(8);
        settingsGrid.setVgap(8);
        settingsGrid.add(new Label("Webhook"), 0, 0);
        settingsGrid.add(webhookField, 1, 0);
        settingsGrid.add(dispatchEnabled, 1, 1);
        settingsGrid.add(new Label("Rate (ms)"), 0, 2);
        settingsGrid.add(rateField, 1, 2);
        settingsGrid.add(new Label("Max intentos"), 0, 3);
        settingsGrid.add(maxAttemptsField, 1, 3);
        settingsGrid.add(new Label("Nuevo secreto"), 0, 4);
        settingsGrid.add(secretField, 1, 4);

        settingsGrid.add(new Separator(), 0, 5, 2, 1);
        settingsGrid.add(new Label("Email SMTP"), 0, 6);
        settingsGrid.add(smtpEnabled, 1, 6);
        settingsGrid.add(new Label("Host"), 0, 7);
        settingsGrid.add(smtpHostField, 1, 7);
        settingsGrid.add(new Label("Puerto"), 0, 8);
        settingsGrid.add(smtpPortField, 1, 8);
        settingsGrid.add(new Label("Usuario"), 0, 9);
        settingsGrid.add(smtpUserField, 1, 9);
        settingsGrid.add(new Label("Password"), 0, 10);
        settingsGrid.add(smtpPasswordField, 1, 10);
        settingsGrid.add(new Label("TLS"), 0, 11);
        settingsGrid.add(smtpTlsField, 1, 11);
        settingsGrid.add(new Label("From email"), 0, 12);
        settingsGrid.add(smtpFromEmailField, 1, 12);
        settingsGrid.add(new Label("From name"), 0, 13);
        settingsGrid.add(smtpFromNameField, 1, 13);

        Button gmailPreset = new Button("Usar Gmail");
        gmailPreset.getStyleClass().add("button-ghost");
        gmailPreset.setOnAction(event -> applyGmailPreset());
        settingsGrid.add(gmailPreset, 1, 14);

        settingsGrid.add(new Label("Email de prueba"), 0, 15);
        settingsGrid.add(smtpTestToField, 1, 15);
        settingsGrid.add(smtpTestButton, 1, 16);

        settingsGrid.add(new Separator(), 0, 17, 2, 1);
        settingsGrid.add(new Label("Test n8n (WhatsApp/Email)"), 0, 18);
        settingsGrid.add(n8nTestToField, 1, 18);
        settingsGrid.add(new Label("Mensaje n8n"), 0, 19);
        settingsGrid.add(n8nTestMessageField, 1, 19);

        Button saveSettings = new Button("Guardar configuracion");
        saveSettings.getStyleClass().add("button-primary");
        saveSettings.setOnAction(event -> saveSettings());

        TableView<OutboxItem> table = new TableView<>(outboxItems);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().addAll(
                column("Evento", "eventType"),
                column("Estado", "status"),
                column("Intentos", "attempts"),
                column("Error", "lastError")
        );

        retryButton = new Button("Reintentar seleccionado");
        retryButton.getStyleClass().add("button-ghost");
        retryButton.setOnAction(event -> {
            OutboxItem selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                retryOutbox(selected.id());
            }
        });

        statusLabel.getStyleClass().add("page-subtitle");

        templateTable = new TableView<>(templates);
        templateTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        templateTable.getColumns().addAll(
                columnTemplate("Evento", "eventType"),
                columnTemplate("Canal", "channel"),
                columnTemplate("Activo", "enabled")
        );
        templateTable.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> {
            if (value != null) {
                fillTemplateForm(value);
            }
        });

        templateEventBox = new ComboBox<>();
        templateEventBox.setItems(FXCollections.observableArrayList(EVENT_TYPES));
        templateEventBox.getSelectionModel().selectFirst();
        templateChannelBox = new ComboBox<>();
        templateChannelBox.setItems(FXCollections.observableArrayList("EMAIL", "WHATSAPP"));
        templateChannelBox.getSelectionModel().selectFirst();
        templateSubjectField = new TextField();
        templateSubjectField.setPromptText("Solo para EMAIL");
        templateBodyField = new TextArea();
        templateBodyField.setPrefRowCount(5);
        templateBodyField.setWrapText(true);
        templateEnabledBox = new CheckBox("Habilitado");
        templateEnabledBox.setSelected(true);
        templateChannelBox.valueProperty().addListener((obs, old, value) -> {
            boolean isEmail = "EMAIL".equalsIgnoreCase(value);
            templateSubjectField.setDisable(!isEmail);
            if (!isEmail) {
                templateSubjectField.clear();
            }
        });
        templateSubjectField.setDisable(!"EMAIL".equalsIgnoreCase(templateChannelBox.getValue()));
        templateSaveButton = new Button("Guardar plantilla");
        templateSaveButton.getStyleClass().add("button-primary");
        templateSaveButton.setOnAction(event -> saveTemplate());
        templateNewButton = new Button("Nueva");
        templateNewButton.getStyleClass().add("button-ghost");
        templateNewButton.setOnAction(event -> resetTemplateForm());
        templateDeleteButton = new Button("Eliminar");
        templateDeleteButton.getStyleClass().add("button-danger");
        templateDeleteButton.setOnAction(event -> deleteTemplate());

        GridPane templateForm = new GridPane();
        templateForm.setHgap(8);
        templateForm.setVgap(8);
        templateForm.add(new Label("Evento"), 0, 0);
        templateForm.add(templateEventBox, 1, 0);
        templateForm.add(new Label("Canal"), 0, 1);
        templateForm.add(templateChannelBox, 1, 1);
        templateForm.add(new Label("Asunto"), 0, 2);
        templateForm.add(templateSubjectField, 1, 2);
        templateForm.add(new Label("Mensaje"), 0, 3);
        templateForm.add(templateBodyField, 1, 3);
        templateForm.add(templateEnabledBox, 1, 4);
        templateForm.add(new HBox(8, templateNewButton, templateSaveButton, templateDeleteButton), 1, 5);

        VBox content = new VBox(16);
        if (canManageSettings) {
            Label settingsTitle = new Label("Configuracion");
            settingsTitle.getStyleClass().add("section-title");
            Label settingsHint = new Label("Conecta n8n para WhatsApp y el correo de entrega. Usa los tests para validar.");
            settingsHint.getStyleClass().add("help-text");
            Label quickTitle = new Label("Configuracion rapida");
            quickTitle.getStyleClass().add("section-title");
            Label quickHint = new Label("Sigue los pasos 1, 2 y 3 para activar Gmail, validar n8n y crear plantilla WhatsApp.");
            quickHint.getStyleClass().add("help-text");
            HBox quickButtons = new HBox(8, quickGmailButton, quickN8nButton, quickWhatsAppTemplateButton);
            VBox settingsSection = new VBox(8, settingsTitle, settingsHint, settingsGrid, saveSettings,
                    new Separator(), quickTitle, quickHint, quickButtons);
            settingsSection.getStyleClass().add("panel-soft");

            Label templatesTitle = new Label("Plantillas");
            templatesTitle.getStyleClass().add("section-title");
            Label templatesHint = new Label("Define el mensaje que recibiran los clientes por canal.");
            templatesHint.getStyleClass().add("help-text");
            VBox templatesSection = new VBox(8, templatesTitle, templatesHint, templateTable, templateForm);
            templatesSection.getStyleClass().add("panel-soft");

            content.getChildren().addAll(settingsSection, templatesSection);
        } else {
            Label readOnly = new Label("Solo lectura");
            readOnly.getStyleClass().add("help-text");
            content.getChildren().add(readOnly);
        }

        Label outboxTitle = new Label("Outbox fallidos");
        outboxTitle.getStyleClass().add("section-title");
        Label outboxHint = new Label("Reintenta eventos que no pudieron enviarse.");
        outboxHint.getStyleClass().add("help-text");
        VBox outboxSection = new VBox(8, outboxTitle, outboxHint, table, retryButton, statusLabel);
        outboxSection.getStyleClass().add("panel-soft");
        content.getChildren().add(outboxSection);
        content.setPadding(new Insets(8));

        root.setTop(toolBar);
        root.setCenter(content);

        if (canManageSettings) {
            loadSettings();
            loadTemplates();
        }
        loadOutbox();
    }

    public Parent getRoot() {
        return root;
    }

    private void loadSettings() {
        if (!canManageSettings) {
            return;
        }
        setLoading(true);
        Thread worker = new Thread(() -> {
            try {
                IntegrationSettings settings = apiClient.getIntegrationSettings();
                Platform.runLater(() -> {
                    webhookField.setText(settings.webhookUrl());
                    dispatchEnabled.setSelected(settings.dispatchEnabled());
                    rateField.setText(String.valueOf(settings.dispatchRateMs()));
                    maxAttemptsField.setText(String.valueOf(settings.dispatchMaxAttempts()));
                    smtpEnabled.setSelected(settings.smtpEnabled());
                    smtpHostField.setText(settings.smtpHost() != null ? settings.smtpHost() : "");
                    smtpPortField.setText(settings.smtpPort() != null ? String.valueOf(settings.smtpPort()) : "");
                    smtpUserField.setText(settings.smtpUser() != null ? settings.smtpUser() : "");
                    smtpTlsField.setSelected(settings.smtpTls());
                    smtpFromEmailField.setText(settings.smtpFromEmail() != null ? settings.smtpFromEmail() : "");
                    smtpFromNameField.setText(settings.smtpFromName() != null ? settings.smtpFromName() : "");
                    smtpPasswordField.clear();
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> statusLabel.setText(resolveError(ex)));
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("No se pudo cargar configuracion."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void loadTemplates() {
        if (!canManageSettings) {
            return;
        }
        setLoading(true);
        Thread worker = new Thread(() -> {
            try {
                var data = apiClient.listTemplates();
                Platform.runLater(() -> templates.setAll(data));
            } catch (ApiException ex) {
                Platform.runLater(() -> statusLabel.setText(resolveError(ex)));
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("No se pudo cargar plantillas."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void fillTemplateForm(NotificationTemplate template) {
        editingTemplateId = template.id();
        templateEventBox.getSelectionModel().select(template.eventType());
        templateChannelBox.getSelectionModel().select(template.channel());
        templateSubjectField.setText(template.subject() != null ? template.subject() : "");
        templateBodyField.setText(template.body() != null ? template.body() : "");
        templateEnabledBox.setSelected(template.enabled());
    }

    private void resetTemplateForm() {
        editingTemplateId = null;
        templateEventBox.getSelectionModel().selectFirst();
        templateChannelBox.getSelectionModel().selectFirst();
        templateSubjectField.clear();
        templateBodyField.clear();
        templateEnabledBox.setSelected(true);
        templateTable.getSelectionModel().clearSelection();
    }

    private void saveTemplate() {
        if (!canManageSettings) return;
        String eventType = templateEventBox.getValue();
        String channel = templateChannelBox.getValue();
        String subject = templateSubjectField.getText();
        String body = templateBodyField.getText();
        boolean enabled = templateEnabledBox.isSelected();
        if (eventType == null || channel == null || body == null || body.isBlank()) {
            statusLabel.setText("Completa evento, canal y body.");
            return;
        }
        setLoading(true);
        Thread worker = new Thread(() -> {
            try {
                NotificationTemplatePayload payload =
                        new NotificationTemplatePayload(eventType, channel,
                                "EMAIL".equals(channel) ? subject : null, body, enabled);
                NotificationTemplate saved;
                if (editingTemplateId != null) {
                    saved = apiClient.updateTemplate(editingTemplateId, payload);
                } else {
                    saved = apiClient.createTemplate(payload);
                }
                Platform.runLater(() -> {
                    loadTemplates();
                    fillTemplateForm(saved);
                    statusLabel.setText("Plantilla guardada.");
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> statusLabel.setText(resolveError(ex)));
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("No se pudo guardar plantilla."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void deleteTemplate() {
        if (!canManageSettings || editingTemplateId == null) return;
        setLoading(true);
        Thread worker = new Thread(() -> {
            try {
                apiClient.deleteTemplate(editingTemplateId);
                Platform.runLater(() -> {
                    resetTemplateForm();
                    loadTemplates();
                    statusLabel.setText("Plantilla eliminada.");
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> statusLabel.setText(resolveError(ex)));
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("No se pudo eliminar plantilla."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void testN8n() {
        if (!canManageSettings) return;
        String to = n8nTestToField.getText() != null ? n8nTestToField.getText().trim() : "";
        String message = n8nTestMessageField.getText() != null ? n8nTestMessageField.getText().trim() : "";
        if (to.isBlank() || message.isBlank()) {
            statusLabel.setText("Completa destino y mensaje para test n8n.");
            return;
        }
        setLoading(true);
        Thread worker = new Thread(() -> {
            try {
                apiClient.testN8n(to, message);
                Platform.runLater(() -> statusLabel.setText("Evento de prueba enviado a n8n."));
            } catch (ApiException ex) {
                Platform.runLater(() -> statusLabel.setText(resolveError(ex)));
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("No se pudo enviar test a n8n."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void testSmtp() {
        if (!canManageSettings) return;
        setLoading(true);
        String to = smtpTestToField.getText();
        Thread worker = new Thread(() -> {
            try {
                apiClient.testSmtp(to, null, null);
                Platform.runLater(() -> statusLabel.setText("Email de prueba enviado."));
            } catch (ApiException ex) {
                Platform.runLater(() -> statusLabel.setText(resolveError(ex)));
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("No se pudo enviar email de prueba."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void saveSettings() {
        if (!canManageSettings) {
            return;
        }
        setLoading(true);
        statusLabel.setText("Guardando...");
        Thread worker = new Thread(() -> {
            try {
                IntegrationSettingsUpdate update = new IntegrationSettingsUpdate(
                        webhookField.getText(),
                        secretField.getText().isBlank() ? null : secretField.getText(),
                        dispatchEnabled.isSelected(),
                        parseLong(rateField.getText()),
                        parseInt(maxAttemptsField.getText()),
                        smtpEnabled.isSelected(),
                        smtpHostField.getText(),
                        parseInt(smtpPortField.getText()),
                        smtpUserField.getText(),
                        smtpPasswordField.getText().isBlank() ? null : smtpPasswordField.getText(),
                        smtpTlsField.isSelected(),
                        smtpFromEmailField.getText(),
                        smtpFromNameField.getText()
                );
                apiClient.updateIntegrationSettings(update);
                Platform.runLater(() -> {
                    secretField.clear();
                    smtpPasswordField.clear();
                    statusLabel.setText("Configuracion guardada.");
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> statusLabel.setText(resolveError(ex)));
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("No se pudo guardar configuracion."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void loadOutbox() {
        setLoading(true);
        statusLabel.setText("Cargando outbox...");
        Thread worker = new Thread(() -> {
            try {
                OutboxPage pageData = apiClient.listOutboxFailed(page, 20);
                Platform.runLater(() -> {
                    outboxItems.setAll(pageData.content());
                    totalPages = Math.max(pageData.totalPages(), 1);
                    updateControls();
                    statusLabel.setText("Fallidos: " + pageData.totalElements() +
                            " · Pagina " + (page + 1) + " de " + totalPages);
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> statusLabel.setText(resolveError(ex)));
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("No se pudo cargar outbox."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void retryOutbox(String id) {
        setLoading(true);
        statusLabel.setText("Reintentando...");
        Thread worker = new Thread(() -> {
            try {
                apiClient.retryOutbox(id);
                loadOutbox();
            } catch (ApiException ex) {
                Platform.runLater(() -> statusLabel.setText(resolveError(ex)));
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("No se pudo reintentar."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private Long parseLong(String value) {
        try {
            return value == null || value.isBlank() ? null : Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer parseInt(String value) {
        try {
            return value == null || value.isBlank() ? null : Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private <T> TableColumn<OutboxItem, T> column(String title, String property) {
        TableColumn<OutboxItem, T> column = new TableColumn<>(title);
        FxTable.bindColumn(column, property);
        return column;
    }

    private <T> TableColumn<NotificationTemplate, T> columnTemplate(String title, String property) {
        TableColumn<NotificationTemplate, T> column = new TableColumn<>(title);
        FxTable.bindColumn(column, property);
        return column;
    }

    private void setLoading(boolean loading) {
        refreshButton.setDisable(loading);
        retryButton.setDisable(loading);
        prevButton.setDisable(loading);
        nextButton.setDisable(loading);
    }

    private void updateControls() {
        prevButton.setDisable(page <= 0);
        nextButton.setDisable(page + 1 >= totalPages);
    }

    private String resolveError(ApiException ex) {
        if (ex.getStatus() == 401 || ex.getStatus() == 403) {
            return "Acceso denegado.";
        }
        return ex.getMessage();
    }

    private void applyGmailPreset() {
        smtpHostField.setText("smtp.gmail.com");
        smtpPortField.setText("587");
        smtpTlsField.setSelected(true);
    }

    private void ensureWhatsAppTemplate() {
        if (!canManageSettings) return;
        setLoading(true);
        Thread worker = new Thread(() -> {
            try {
                String eventType = "LICENSES_DELIVERED";
                String channel = "WHATSAPP";
                String body = "Hola {clientName}, aqui esta tu licencia: {licenses}. Gracias por tu compra.";
                NotificationTemplate existing = templates.stream()
                        .filter(t -> eventType.equalsIgnoreCase(t.eventType())
                                && channel.equalsIgnoreCase(t.channel()))
                        .findFirst()
                        .orElse(null);
                NotificationTemplatePayload payload = new NotificationTemplatePayload(eventType, channel, null, body, true);
                if (existing != null) {
                    apiClient.updateTemplate(existing.id(), payload);
                } else {
                    apiClient.createTemplate(payload);
                }
                Platform.runLater(() -> {
                    loadTemplates();
                    statusLabel.setText("Plantilla WhatsApp lista.");
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> statusLabel.setText(resolveError(ex)));
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("No se pudo crear plantilla WhatsApp."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }
}
