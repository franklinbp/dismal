package com.dismal.desktop;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;

public class QuotesView {

    private final BorderPane root;
    private final ApiClient apiClient;
    private final ObservableList<QuoteSummary> quotes;
    private final ListView<String> listView;
    private final Label statusLabel;
    private final Button refreshButton;
    private final Button createButton;
    private final Button convertButton;
    private QuoteDetail selected;

    private final ComboBox<Customer> customerField;
    private final TextArea notesField;
    private final VBox itemsBox;
    private final List<QuoteItemDraft> draftItems;
    private final ObservableList<Product> products;

    public QuotesView(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.root = new BorderPane();
        this.quotes = FXCollections.observableArrayList();
        this.listView = new ListView<>();
        this.statusLabel = new Label();
        this.itemsBox = new VBox(8);
        this.draftItems = new ArrayList<>();
        this.products = FXCollections.observableArrayList();

        root.setPadding(new Insets(16));
        root.getStyleClass().add("content-root");

        Label title = new Label("Cotizaciones");
        title.getStyleClass().add("page-title");

        refreshButton = new Button("Actualizar");
        refreshButton.getStyleClass().add("button-ghost");
        refreshButton.setOnAction(event -> loadQuotes());

        createButton = new Button("Crear cotizacion");
        createButton.getStyleClass().add("button-primary");
        createButton.setOnAction(event -> createQuote());

        convertButton = new Button("Convertir a venta");
        convertButton.getStyleClass().add("button-ghost");
        convertButton.setOnAction(event -> convertQuote());

        ToolBar toolBar = new ToolBar(title, refreshButton, createButton, convertButton);
        toolBar.getStyleClass().add("toolbar");
        root.setTop(toolBar);

        listView.setPrefWidth(420);
        listView.getSelectionModel().selectedIndexProperty().addListener((obs, prev, idx) -> {
            int index = idx == null ? -1 : idx.intValue();
            if (index >= 0 && index < quotes.size()) {
                selectQuote(quotes.get(index));
            }
        });

        customerField = new ComboBox<>();
        customerField.setPromptText("Cliente");
        customerField.setConverter(new StringConverter<>() {
            @Override
            public String toString(Customer customer) {
                return customer == null ? "" : customer.firstname() + " " + customer.lastname();
            }

            @Override
            public Customer fromString(String string) {
                return null;
            }
        });
        notesField = new TextArea();
        notesField.setPrefRowCount(2);

        Button addItemButton = new Button("Agregar item");
        addItemButton.getStyleClass().add("button-ghost");
        addItemButton.setOnAction(event -> addItem());

        VBox detailForm = new VBox(10,
                new Label("Cliente"),
                customerField,
                new Label("Notas"),
                notesField,
                new Label("Items"),
                itemsBox,
                addItemButton
        );
        detailForm.getStyleClass().add("panel");

        HBox main = new HBox(16, listView, detailForm);

        statusLabel.getStyleClass().add("page-subtitle");
        VBox center = new VBox(12, main, statusLabel);
        root.setCenter(center);

        addItem();
        loadAux();
        loadQuotes();
    }

    public Parent getRoot() {
        return root;
    }

    private void loadAux() {
        Thread worker = new Thread(() -> {
            try {
                List<Customer> customers = apiClient.listCustomers();
                List<Product> productList = apiClient.listProducts();
                Platform.runLater(() -> {
                    customerField.getItems().setAll(customers);
                    products.setAll(productList);
                    draftItems.forEach(QuoteItemDraft::refreshProducts);
                });
            } catch (Exception ignored) {
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void loadQuotes() {
        setLoading(true);
        statusLabel.setText("Cargando cotizaciones...");
        Thread worker = new Thread(() -> {
            try {
                PageResponse<QuoteSummary> page = apiClient.listQuotes("", 0, 25);
                Platform.runLater(() -> {
                    quotes.setAll(page.content());
                    listView.setItems(quotes.stream()
                            .map(item -> item.clientName() + " · " + FormatUtils.currency(item.total()) + " · " + item.status())
                            .collect(FXCollections::observableArrayList, ObservableList::add, ObservableList::addAll));
                    statusLabel.setText("Total: " + page.totalElements());
                    if (!quotes.isEmpty()) {
                        selectQuote(quotes.get(0));
                        listView.getSelectionModel().select(0);
                    }
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> statusLabel.setText(resolveError(ex)));
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("No se pudo cargar cotizaciones."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void selectQuote(QuoteSummary summary) {
        setLoading(true);
        Thread worker = new Thread(() -> {
            try {
                QuoteDetail detail = apiClient.getQuote(summary.id());
                Platform.runLater(() -> {
                    selected = detail;
                    statusLabel.setText("Seleccionado: " + summary.id());
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> statusLabel.setText(resolveError(ex)));
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("No se pudo cargar detalle."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void addItem() {
        QuoteItemDraft draft = new QuoteItemDraft(products);
        draftItems.add(draft);
        itemsBox.getChildren().add(draft.getRow());
    }

    private void createQuote() {
        Customer customer = customerField.getValue();
        if (customer == null) {
            statusLabel.setText("Selecciona un cliente.");
            return;
        }
        List<ApiClient.QuoteCreateItem> items = new ArrayList<>();
        for (QuoteItemDraft draft : draftItems) {
            if (draft.isComplete()) {
                items.add(draft.toRequest());
            }
        }
        if (items.isEmpty()) {
            statusLabel.setText("Agrega al menos un item.");
            return;
        }
        ApiClient.QuoteCreateRequest payload = new ApiClient.QuoteCreateRequest(
                customer.id(),
                notesField.getText().trim(),
                items
        );
        setLoading(true);
        Thread worker = new Thread(() -> {
            try {
                QuoteDetail created = apiClient.createQuote(payload);
                Platform.runLater(() -> {
                    selected = created;
                    notesField.clear();
                    itemsBox.getChildren().clear();
                    draftItems.clear();
                    addItem();
                    loadQuotes();
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> statusLabel.setText(resolveError(ex)));
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("No se pudo crear cotizacion."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void convertQuote() {
        if (selected == null) {
            statusLabel.setText("Selecciona una cotizacion.");
            return;
        }
        setLoading(true);
        Thread worker = new Thread(() -> {
            try {
                apiClient.convertQuote(selected.id());
                Platform.runLater(() -> loadQuotes());
            } catch (ApiException ex) {
                Platform.runLater(() -> statusLabel.setText(resolveError(ex)));
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("No se pudo convertir."));
            } finally {
                Platform.runLater(() -> setLoading(false));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void setLoading(boolean loading) {
        refreshButton.setDisable(loading);
        createButton.setDisable(loading);
        convertButton.setDisable(loading);
    }

    private String resolveError(ApiException ex) {
        if (ex.getStatus() == 401 || ex.getStatus() == 403) {
            return "Acceso denegado.";
        }
        return ex.getMessage();
    }

    private static class QuoteItemDraft {
        private final ComboBox<Product> productBox;
        private final TextField qtyField;
        private final TextField priceField;
        private final HBox row;

        QuoteItemDraft(ObservableList<Product> products) {
            productBox = new ComboBox<>(products);
            productBox.setPromptText("Producto");
            productBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(Product product) {
                    return product == null ? "" : product.name();
                }

                @Override
                public Product fromString(String string) {
                    return null;
                }
            });
            qtyField = new TextField("1");
            qtyField.setPrefWidth(60);
            priceField = new TextField();
            priceField.setPrefWidth(90);
            row = new HBox(8, productBox, qtyField, priceField);
        }

        HBox getRow() {
            return row;
        }

        void refreshProducts() {
            productBox.setItems(productBox.getItems());
        }

        boolean isComplete() {
            return productBox.getValue() != null && !qtyField.getText().isBlank() && !priceField.getText().isBlank();
        }

        ApiClient.QuoteCreateItem toRequest() {
            Product product = productBox.getValue();
            int qty = Integer.parseInt(qtyField.getText().trim());
            double price = Double.parseDouble(priceField.getText().trim());
            return new ApiClient.QuoteCreateItem(product.id(), qty, price);
        }
    }
}
