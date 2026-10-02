package com.dismal.desktop;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

public class CartItem {
    private final Product product;
    private final SimpleIntegerProperty quantity;
    private final SimpleDoubleProperty unitPrice;
    
    public CartItem(Product product) {
        this.product = product;
        this.quantity = new SimpleIntegerProperty(1);
        this.unitPrice = new SimpleDoubleProperty(product.price());
    }
    
    public Product getProduct() { return product; }
    
    public int getQuantity() { return quantity.get(); }
    public void setQuantity(int q) { this.quantity.set(q); }
    public SimpleIntegerProperty quantityProperty() { return quantity; }
    
    public double getUnitPrice() { return unitPrice.get(); }
    
    public double getTotal() { return getQuantity() * getUnitPrice(); }
    
    public String getProductName() { return product.name(); }
}
