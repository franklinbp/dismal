package com.dismal.desktop;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class FormatUtils {

    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.US);
    private static final NumberFormat NUMBER = NumberFormat.getNumberInstance(Locale.US);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm", Locale.US);

    private FormatUtils() {}

    public static String currency(Number value) {
        if (value == null) {
            return "-";
        }
        return CURRENCY.format(value.doubleValue());
    }

    public static String currency(BigDecimal value) {
        if (value == null) {
            return "-";
        }
        return CURRENCY.format(value);
    }

    public static String number(Number value) {
        if (value == null) {
            return "-";
        }
        return NUMBER.format(value.doubleValue());
    }

    public static String date(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        try {
            return DATE.format(LocalDate.parse(value));
        } catch (Exception ignored) {
        }
        try {
            return DATE.format(OffsetDateTime.parse(value).toLocalDate());
        } catch (Exception ignored) {
        }
        return value;
    }

    public static String dateTime(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        try {
            return DATE_TIME.format(OffsetDateTime.parse(value));
        } catch (Exception ignored) {
        }
        return value;
    }
}
