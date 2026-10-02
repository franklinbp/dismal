package com.dismal.desktop;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TableColumn;

import java.lang.reflect.Method;

public final class FxTable {
    private FxTable() {}

    public static <T, V> void bindColumn(TableColumn<T, V> column, String property) {
        column.setCellValueFactory(cell -> {
            Object row = cell.getValue();
            Object value = readProperty(row, property);
            return new SimpleObjectProperty<>((V) value);
        });
    }

    private static Object readProperty(Object row, String property) {
        if (row == null || property == null || property.isBlank()) {
            return null;
        }
        Class<?> type = row.getClass();
        String cap = property.substring(0, 1).toUpperCase() + property.substring(1);
        Method method = findMethod(type, property);
        if (method == null) {
            method = findMethod(type, "get" + cap);
        }
        if (method == null) {
            method = findMethod(type, "is" + cap);
        }
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(row);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Method findMethod(Class<?> type, String name) {
        try {
            return type.getMethod(name);
        } catch (Exception ex) {
            return null;
        }
    }
}
