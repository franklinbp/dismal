package com.dismal.desktop;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public class UsersView {

    private final BorderPane root;
    private final ApiClient apiClient;
    private final UserMe currentUser;
    private final ObservableList<AdminUser> users;
    private final TableView<AdminUser> table;
    private final Label statusLabel;
    
    // Controls
    private final Button refreshButton;
    private final Button newButton;
    private final Button prevPageButton = new Button("Anterior");
    private final Button nextPageButton = new Button("Siguiente");
    private final Label pageLabel = new Label();
    private final TextField searchField = new TextField();
    private final Button searchButton = new Button("Buscar");

    private final Button importButton = new Button("Importar");
    private final ComboBox<String> importFormatBox = new ComboBox<>();
    private final Label importStatusLabel = new Label();
    
    // Drawer
    private final VBox drawerPanel;
    private final Label drawerTitle;
    private final Button closeDrawerButton;
    private final Label tempPasswordLabel;
    private final Label arStatusLabel = new Label();
    private final Label arBalanceLabel = new Label();
    private final Label arDaysLabel = new Label();
    
    private final TextField firstnameField;
    private final TextField lastnameField;
    private final TextField emailField;
    private final TextField phoneField;
    private final TextField taxIdField;
    private final TextField billingEmailField;
    private final TextField creditLimitField;
    private final TextField creditDaysField;
    private final TextField passwordField;
    private final CheckBox enabledCheck;
    private final CheckBox hasCreditCheck;
    private final ComboBox<String> roleField;
    
    private final Button saveButton;
    private final Button toggleButton;
    private final Button resetPasswordButton;

    private String activeTab = "CLIENT";
    private int page = 0;
    private int totalPages = 1;
    private AdminUser selected;
    private boolean creating = false;
    private final SplitPane splitPane;

    private static final ObservableList<String> INTERNAL_ROLES =
            FXCollections.observableArrayList("ADMIN", "MANAGER", "OPERATOR");
    private static final ObservableList<String> CLIENT_ROLES =
            FXCollections.observableArrayList("CUSTOMER", "USER");

    public UsersView(ApiClient apiClient, UserMe currentUser) {
        this.apiClient = apiClient;
        this.currentUser = currentUser;
        this.root = new BorderPane();
        this.users = FXCollections.observableArrayList();
        this.table = new TableView<>(users);
        this.statusLabel = new Label();
        this.tempPasswordLabel = new Label();

        root.getStyleClass().add("content-root");

        // --- Toolbar ---
        Label title = new Label("Gestión de Usuarios");
        title.getStyleClass().add("page-title");

        refreshButton = new Button("Actualizar");
        refreshButton.getStyleClass().add("button-ghost");
        refreshButton.setOnAction(event -> loadUsers());

        newButton = new Button("+ Nuevo Usuario");
        newButton.getStyleClass().add("button-primary");
        newButton.setOnAction(event -> openDrawer(true));

        ToolBar toolBar = new ToolBar(title, refreshButton, newButton);
        toolBar.getStyleClass().add("toolbar");
        root.setTop(toolBar);

        // --- Tabs ---
        Button internalTab = new Button("Internos");
        internalTab.getStyleClass().add("nav-button");
        internalTab.setOnAction(event -> {
            switchTab("INTERNAL");
            internalTab.getStyleClass().add("nav-button-active");
        });
        
        Button clientTab = new Button("Clientes");
        clientTab.getStyleClass().add("nav-button");
        clientTab.setOnAction(event -> {
            switchTab("CLIENT");
            clientTab.getStyleClass().add("nav-button-active");
        });
        
        if (!"ADMIN".equals(currentUser.role())) {
            internalTab.setDisable(true);
        }

        HBox tabs = new HBox(10, internalTab, clientTab);
        tabs.setPadding(new Insets(0, 0, 10, 0));

        searchField.setPromptText("Buscar por nombre o email...");
        searchButton.getStyleClass().add("button-ghost");
        searchButton.setOnAction(event -> {
            page = 0;
            loadUsers();
        });
        HBox searchRow = new HBox(10, searchField, searchButton);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        importFormatBox.setItems(FXCollections.observableArrayList("csv", "xlsx"));
        importFormatBox.getSelectionModel().select("csv");
        importButton.getStyleClass().add("button-ghost");
        importButton.setOnAction(event -> importUsers());
        importStatusLabel.getStyleClass().add("page-subtitle");
        HBox importRow = new HBox(10, new Label("Importar:"), importFormatBox, importButton, importStatusLabel);

        // --- Table ---
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<AdminUser, String> arStatusCol = new TableColumn<>("AR Estado");
        arStatusCol.setCellValueFactory(cell -> {
            ArSummary ar = cell.getValue().arSummary();
            return new javafx.beans.property.SimpleStringProperty(ar != null ? ar.estado() : "-");
        });
        TableColumn<AdminUser, String> arBalanceCol = new TableColumn<>("AR Saldo");
        arBalanceCol.setCellValueFactory(cell -> {
            ArSummary ar = cell.getValue().arSummary();
            double balance = ar != null ? ar.saldoActual() : 0;
            return new javafx.beans.property.SimpleStringProperty(FormatUtils.currency(balance));
        });
        table.getColumns().addAll(
                column("Nombre", "firstname"),
                column("Apellido", "lastname"),
                column("Email", "email"),
                column("Rol", "role"),
                column("Estado", "enabled"),
                arStatusCol,
                arBalanceCol
        );
        table.getSelectionModel().selectedItemProperty().addListener((obs, prev, next) -> {
            if (next != null) selectUser(next);
        });
        VBox.setVgrow(table, Priority.ALWAYS);
        
        HBox pager = new HBox(10, prevPageButton, nextPageButton, pageLabel);
        pager.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        prevPageButton.getStyleClass().add("button-ghost");
        nextPageButton.getStyleClass().add("button-ghost");
        pageLabel.getStyleClass().add("page-subtitle");
        prevPageButton.setOnAction(event -> {
            if (page > 0) {
                page -= 1;
                loadUsers();
            }
        });
        nextPageButton.setOnAction(event -> {
            if (page + 1 < totalPages) {
                page += 1;
                loadUsers();
            }
        });

        VBox mainContent = new VBox(10, tabs, searchRow, importRow, table, pager);

        // --- Drawer (Right Panel) ---
        drawerTitle = new Label("Detalle de Usuario");
        drawerTitle.getStyleClass().add("section-title");
        
        closeDrawerButton = new Button("✕");
        closeDrawerButton.getStyleClass().add("button-ghost");
        closeDrawerButton.setOnAction(e -> closeDrawer());
        
        HBox drawerHeader = new HBox(10, drawerTitle, closeDrawerButton);
        HBox.setHgrow(drawerTitle, Priority.ALWAYS);
        closeDrawerButton.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        firstnameField = new TextField(); firstnameField.setPromptText("Nombre");
        lastnameField = new TextField(); lastnameField.setPromptText("Apellido");
        emailField = new TextField(); emailField.setPromptText("email@example.com");
        phoneField = new TextField(); phoneField.setPromptText("Teléfono");
        taxIdField = new TextField(); taxIdField.setPromptText("RUC / CI");
        billingEmailField = new TextField(); billingEmailField.setPromptText("Email Facturación");
        creditLimitField = new TextField(); creditLimitField.setPromptText("Límite Crédito");
        creditDaysField = new TextField(); creditDaysField.setPromptText("Días Crédito");
        passwordField = new TextField(); passwordField.setPromptText("Password Temporal");
        
        enabledCheck = new CheckBox("Usuario Activo");
        hasCreditCheck = new CheckBox("Habilitar Crédito");
        roleField = new ComboBox<>();
        roleField.setMaxWidth(Double.MAX_VALUE);

        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(10);
        form.addRow(0, new Label("Nombre *"), firstnameField);
        form.addRow(1, new Label("Apellido *"), lastnameField);
        form.addRow(2, new Label("Email *"), emailField);
        form.addRow(3, new Label("Rol"), roleField);
        form.addRow(4, new Label("Teléfono"), phoneField);
        
        Label financialLabel = new Label("Datos Financieros");
        financialLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 5 0;");
        
        GridPane financialForm = new GridPane();
        financialForm.setHgap(10); financialForm.setVgap(10);
        financialForm.addRow(0, new Label("Tax ID"), taxIdField);
        financialForm.addRow(1, new Label("Billing Email"), billingEmailField);
        financialForm.addRow(2, new Label("Límite ($)"), creditLimitField);
        financialForm.addRow(3, new Label("Días"), creditDaysField);

        Label arLabel = new Label("Estado AR");
        arLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 5 0;");
        arStatusLabel.getStyleClass().add("page-subtitle");
        arBalanceLabel.getStyleClass().add("page-subtitle");
        arDaysLabel.getStyleClass().add("page-subtitle");
        VBox arBox = new VBox(4,
                new Label("Estado:"),
                arStatusLabel,
                new Label("Saldo:"),
                arBalanceLabel,
                new Label("Dias atraso:"),
                arDaysLabel
        );

        saveButton = new Button("Guardar Cambios");
        saveButton.getStyleClass().add("button-primary");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setOnAction(event -> saveUser());

        toggleButton = new Button("Desactivar Usuario");
        toggleButton.getStyleClass().add("button-danger");
        toggleButton.setMaxWidth(Double.MAX_VALUE);
        toggleButton.setOnAction(event -> toggleStatus());

        resetPasswordButton = new Button("Reiniciar Password");
        resetPasswordButton.getStyleClass().add("button-ghost");
        resetPasswordButton.setMaxWidth(Double.MAX_VALUE);
        resetPasswordButton.setOnAction(event -> resetPassword());
        
        tempPasswordLabel.getStyleClass().add("notice-success");
        tempPasswordLabel.setWrapText(true);

        drawerPanel = new VBox(12, 
            drawerHeader, 
            form, 
            new javafx.scene.control.Separator(),
            financialLabel,
            financialForm,
            arLabel,
            arBox,
            new HBox(10, enabledCheck, hasCreditCheck),
            passwordField, // Only visible on create usually
            new javafx.scene.control.Separator(),
            saveButton, 
            toggleButton, 
            resetPasswordButton, 
            tempPasswordLabel
        );
        drawerPanel.setPadding(new Insets(20));
        drawerPanel.getStyleClass().add("drawer-panel");
        drawerPanel.setMinWidth(360);
        drawerPanel.setMaxWidth(400);

        // --- Split Pane ---
        splitPane = new SplitPane();
        splitPane.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        splitPane.getItems().add(mainContent);

        root.setCenter(splitPane);
        
        statusLabel.getStyleClass().add("page-subtitle");
        statusLabel.setPadding(new Insets(8));
        root.setBottom(statusLabel);

        switchTab("CLIENT");
    }

    public Parent getRoot() {
        return root;
    }

    private void openDrawer(boolean createMode) {
        if (!splitPane.getItems().contains(drawerPanel)) {
            splitPane.getItems().add(drawerPanel);
            splitPane.setDividerPositions(0.65);
        }
        
        if (createMode) {
            startCreate();
            drawerTitle.setText("Nuevo Usuario");
            saveButton.setText("Crear Usuario");
            toggleButton.setVisible(false);
            resetPasswordButton.setVisible(false);
            passwordField.setVisible(true);
        } else {
            creating = false;
            drawerTitle.setText("Editar Usuario");
            saveButton.setText("Guardar Cambios");
            toggleButton.setVisible(true);
            resetPasswordButton.setVisible(true);
            passwordField.setVisible(false); // Hide manual password field on edit
        }
    }

    private void closeDrawer() {
        splitPane.getItems().remove(drawerPanel);
        table.getSelectionModel().clearSelection();
        selected = null;
    }

    private void switchTab(String tab) {
        activeTab = tab;
        page = 0;
        selected = null;
        creating = false;
        closeDrawer(); // Close drawer on tab switch
        tempPasswordLabel.setText("");
        updateRoleOptions();
        loadUsers();
    }

    private void loadUsers() {
        setLoading(true);
        statusLabel.setText("Cargando...");
        Thread worker = new Thread(() -> {
            try {
                String query = searchField.getText() != null ? searchField.getText().trim() : "";
                PageResponse<AdminUser> pageData = apiClient.listUsers(activeTab, page, 10, query);
                Platform.runLater(() -> {
                    users.setAll(pageData.content());
                    totalPages = Math.max(pageData.totalPages(), 1);
                    updatePager();
                    statusLabel.setText("Total usuarios: " + pageData.totalElements());
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> statusLabel.setText("Error: " + ex.getMessage()));
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Error de conexión."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void selectUser(AdminUser user) {
        selected = user;
        openDrawer(false); // Open in Edit mode
        
        firstnameField.setText(user.firstname());
        lastnameField.setText(user.lastname());
        emailField.setText(user.email());
        phoneField.setText(user.phone() == null ? "" : user.phone());
        roleField.setValue(user.role());
        taxIdField.setText(user.taxId() == null ? "" : user.taxId());
        billingEmailField.setText(user.billingEmail() == null ? "" : user.billingEmail());
        creditLimitField.setText(String.valueOf(user.creditLimit()));
        creditDaysField.setText(String.valueOf(user.creditDays()));
        enabledCheck.setSelected(user.enabled());
        hasCreditCheck.setSelected(user.hasCredit());
        passwordField.clear();
        tempPasswordLabel.setText("");

        if (user.arSummary() != null) {
            arStatusLabel.setText(user.arSummary().estado());
            arBalanceLabel.setText(FormatUtils.currency(user.arSummary().saldoActual()));
            arDaysLabel.setText(String.valueOf(user.arSummary().diasAtraso()));
        } else {
            arStatusLabel.setText("-");
            arBalanceLabel.setText(FormatUtils.currency(0));
            arDaysLabel.setText("-");
        }
        
        toggleButton.setText(user.enabled() ? "Desactivar Usuario" : "Activar Usuario");
        toggleButton.getStyleClass().removeAll("button-danger", "button-primary");
        toggleButton.getStyleClass().add(user.enabled() ? "button-danger" : "button-primary");
    }

    private void startCreate() {
        creating = true;
        selected = null;
        table.getSelectionModel().clearSelection();
        firstnameField.clear();
        lastnameField.clear();
        emailField.clear();
        phoneField.clear();
        taxIdField.clear();
        billingEmailField.clear();
        creditLimitField.clear();
        creditDaysField.clear();
        roleField.setValue(activeTab.equals("INTERNAL") ? "MANAGER" : "CUSTOMER");
        enabledCheck.setSelected(true);
        hasCreditCheck.setSelected(false);
        passwordField.setText(generateTempPassword());
        arStatusLabel.setText("-");
        arBalanceLabel.setText(FormatUtils.currency(0));
        arDaysLabel.setText("-");
    }

    private void saveUser() {
        String firstname = firstnameField.getText().trim();
        String lastname = lastnameField.getText().trim();
        String email = emailField.getText().trim();
        if (firstname.isEmpty() || lastname.isEmpty() || email.isEmpty()) {
            statusLabel.setText("Nombre, Apellido y Email son obligatorios.");
            return;
        }
        
        double creditLimit = parseDouble(creditLimitField.getText());
        int creditDays = (int) parseDouble(creditDaysField.getText());
        AdminUser payload = new AdminUser(
                selected != null ? selected.id() : null,
                firstname,
                lastname,
                email,
                emptyToNull(phoneField.getText()),
                roleField.getValue(),
                enabledCheck.isSelected(),
                emptyToNull(taxIdField.getText()),
                emptyToNull(billingEmailField.getText()),
                hasCreditCheck.isSelected(),
                creditLimit,
                selected != null ? selected.creditUsed() : 0,
                creditDays,
                selected != null ? selected.arSummary() : null
        );
        String password = creating ? passwordField.getText().trim() : null;

        setLoading(true);
        statusLabel.setText("Guardando...");
        Thread worker = new Thread(() -> {
            try {
                AdminUser result = creating || selected == null
                        ? apiClient.createUser(payload, password)
                        : apiClient.updateUser(selected.id(), payload);
                Platform.runLater(() -> {
                    loadUsers();
                    if (password != null && !password.isBlank()) {
                        tempPasswordLabel.setText("Usuario creado. Password temporal: " + password);
                    } else {
                        statusLabel.setText("Usuario guardado.");
                        closeDrawer();
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Error al guardar: " + ex.getMessage()));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void toggleStatus() {
        if (selected == null) return;
        setLoading(true);
        Thread worker = new Thread(() -> {
            try {
                apiClient.updateUserStatus(selected.id(), !selected.enabled());
                Platform.runLater(() -> {
                    loadUsers();
                    closeDrawer();
                    statusLabel.setText("Estado actualizado.");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Error: " + ex.getMessage()));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void resetPassword() {
        if (selected == null) return;
        setLoading(true);
        Thread worker = new Thread(() -> {
            try {
                ApiClient.TempPassword data = apiClient.resetUserPassword(selected.id());
                Platform.runLater(() -> tempPasswordLabel.setText("Nuevo Password Temporal: " + data.tempPassword()));
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Error: " + ex.getMessage()));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private String generateTempPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < 12; i += 1) {
            value.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return value.toString();
    }

    private double parseDouble(String value) {
        try {
            return value == null || value.isBlank() ? 0 : Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String emptyToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private <T> TableColumn<AdminUser, T> column(String title, String property) {
        TableColumn<AdminUser, T> column = new TableColumn<>(title);
        FxTable.bindColumn(column, property);
        return column;
    }

    private void setLoading(boolean loading) {
        refreshButton.setDisable(loading);
        newButton.setDisable(loading);
        saveButton.setDisable(loading);
        toggleButton.setDisable(loading);
        resetPasswordButton.setDisable(loading);
        prevPageButton.setDisable(loading || page <= 0);
        nextPageButton.setDisable(loading || page + 1 >= totalPages);
        searchButton.setDisable(loading);
        importButton.setDisable(loading);
    }

    private void updatePager() {
        prevPageButton.setDisable(page <= 0);
        nextPageButton.setDisable(page + 1 >= totalPages);
        pageLabel.setText("Pagina " + (page + 1) + " de " + totalPages);
    }

    private void importUsers() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Importar usuarios");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV", "*.csv"),
                new FileChooser.ExtensionFilter("Excel", "*.xlsx")
        );
        var file = chooser.showOpenDialog(root.getScene().getWindow());
        if (file == null) {
            return;
        }
        String format = importFormatBox.getValue() != null ? importFormatBox.getValue() : "csv";
        setLoading(true);
        importStatusLabel.setText("Importando...");
        Thread worker = new Thread(() -> {
            try {
                AdminImportResult result = apiClient.importUsers(file.toPath(), format);
                Platform.runLater(() -> {
                    importStatusLabel.setText("Importados: " + result.imported() +
                            " · Omitidos: " + result.skipped());
                    loadUsers();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> importStatusLabel.setText("Error: " + ex.getMessage()));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void updateRoleOptions() {
        if (activeTab.equals("INTERNAL")) {
            roleField.setItems(INTERNAL_ROLES);
        } else {
            roleField.setItems(CLIENT_ROLES);
        }
    }
}
